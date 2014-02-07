package org.myeslib.example.routes;

import java.util.UUID;

import javax.inject.Inject;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.UseLatestAggregationStrategy;
import org.myeslib.core.Command;
import org.myeslib.core.data.Snapshot;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.core.function.CommandHandlerInvoker;
import org.myeslib.core.storage.SnapshotReader;
import org.myeslib.example.Example;
import org.myeslib.example.ExampleModule.HzUnitOfWorkWriterFactory;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleDomain.InventoryItemCommandHandler;
import org.myeslib.example.infra.HazelcastMaps;
import org.myeslib.hazelcast.HzStringTxMapFactory;
import org.myeslib.hazelcast.function.HzCommandHandlerInvoker;
import org.myeslib.hazelcast.storage.HzUnitOfWorkWriter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.TransactionalMap;
import com.hazelcast.transaction.TransactionContext;

public class ConsumeCommandsRoute extends RouteBuilder {

	final SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader;
	final HazelcastInstance hazelcastInstance;
	final HzStringTxMapFactory<UUID> hzStringTxMapFactory; 
	final HzUnitOfWorkWriterFactory hzUnitOfWorkWriterFactory ;
	
	@Inject
	public ConsumeCommandsRoute(SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader,
			HazelcastInstance hazelcastInstance,
			HzStringTxMapFactory<UUID> hzStringTxMapFactory, 
			HzUnitOfWorkWriterFactory hzUnitOfWorkWriterFactory) {
		this.snapshotReader = snapshotReader;
		this.hazelcastInstance = hazelcastInstance;
		this.hzStringTxMapFactory = hzStringTxMapFactory;
		this.hzUnitOfWorkWriterFactory = hzUnitOfWorkWriterFactory;
	}
	
	@Override
	public void configure() throws Exception {

		  errorHandler(deadLetterChannel("direct:dead-letter-channel").disableRedelivery());
			//    .maximumRedeliveries(3).redeliveryDelay(5000));
		
		 from("dataset:inventoryCommandsDataset")
		 	.routeId("dataset:inventoryCommandsDataset")
		    .to("direct:handle-inventory-item-command");
			
	     from("direct:handle-inventory-item-command")
	         .routeId("handle-inventory-item-command")
	      	 .log("received = ${body}")
	         .setHeader("id", simple("${body.getId()}"))
	         .process(new ItemInventoryProcessor())
	      	 .log("resulting body = ${body}")
	      	 .aggregate(header("id")).completionSize(Example.HOW_MANY_COMMANDS_TO_TEST).aggregationStrategy(new UseLatestAggregationStrategy())
	      	 .process(new Processor() {
				@Override
				public void process(Exchange e) throws Exception {
					UUID id = e.getIn().getHeader("id", UUID.class);
					Snapshot<InventoryItemAggregateRoot> snapshot = snapshotReader.get(id, new InventoryItemAggregateRoot());
					e.getOut().setBody(snapshot);
				}
			 })
			 .log("snapshot after all commands: ${body}");
	      
	      from("direct:dead-letter-channel")
	         .log("error !!");

	}

	
	private class ItemInventoryProcessor implements Processor {
		@Override
		public void process(Exchange e) throws Exception {

			UUID id = e.getIn().getHeader("id", UUID.class);
			Command command = e.getIn().getBody(Command.class);
			
			Snapshot<InventoryItemAggregateRoot> snapshot = snapshotReader.get(id, new InventoryItemAggregateRoot());
			InventoryItemCommandHandler commandHandler = new InventoryItemCommandHandler(snapshot.getAggregateInstance());
			
			TransactionContext context = hazelcastInstance.newTransactionContext();
			context.beginTransaction();
			
			TransactionalMap<UUID, String> pastTransactionsMap = hzStringTxMapFactory.get(context, HazelcastMaps.INVENTORY_ITEM_AGGREGATE_HISTORY.name());
			HzUnitOfWorkWriter<UUID> uowWriter = hzUnitOfWorkWriterFactory.create(pastTransactionsMap);
			CommandHandlerInvoker<UUID, InventoryItemAggregateRoot> cmdHandlerInvoker = new HzCommandHandlerInvoker<UUID, InventoryItemAggregateRoot>(uowWriter);
			
			UnitOfWork uow = null;
			try {
				uow = cmdHandlerInvoker.invoke(id, snapshot.getVersion(), command, commandHandler);
				context.commitTransaction();
			} catch (Throwable t) {
				context.rollbackTransaction();
				t.printStackTrace();
				throw new Exception(t);
			}
			
			e.getOut().setHeader("id", id);
			e.getOut().setBody(uow);
		}
	}
	
}