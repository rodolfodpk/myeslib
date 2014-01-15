package org.myeslib.example.routes;

import java.util.UUID;

import lombok.AllArgsConstructor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.myeslib.core.Command;
import org.myeslib.data.Snapshot;
import org.myeslib.data.UnitOfWork;
import org.myeslib.example.SampleCoreDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleCoreDomain.InventoryItemCommandHandler;
import org.myeslib.example.infra.HazelcastMaps;
import org.myeslib.hazelcast.AggregateRootHistoryMapFactory;
import org.myeslib.hazelcast.AggregateRootHistoryTxMapFactory;
import org.myeslib.hazelcast.AggregateRootSnapshotMapFactory;
import org.myeslib.hazelcast.HazelcastEventStore;
import org.myeslib.hazelcast.TransactionalCommandProcessor;
import org.myeslib.util.KeyValueSnapshotReader;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.transaction.TransactionContext;

@AllArgsConstructor
public class ConsumeCommandsRoute extends RouteBuilder {
	
	final HazelcastInstance hazelcastInstance ;
	final AggregateRootHistoryTxMapFactory<UUID, InventoryItemAggregateRoot> txMapFactory;
	final AggregateRootHistoryMapFactory<UUID, InventoryItemAggregateRoot> mapFactory;
	final AggregateRootSnapshotMapFactory<UUID, InventoryItemAggregateRoot> snapshotMapFactory;
	
	@Override
	public void configure() throws Exception {

//		  errorHandler(deadLetterChannel("direct:dead-letter-channel")
//				    .maximumRedeliveries(3).redeliveryDelay(5000));
			
	      from("direct:handle-create")
	         .routeId("handle-create")
	      	 .log("received = ${body}")
	         .setHeader("id", simple("${body.getId()}"))
	      	 .process(new ProcessInventoryItemCommand())
	      	 .log("resulting body = ${body}");
	      
	      from("direct:handle-increase")
	      	 .routeId("handle-increase")
	      	 .setHeader("id", simple("${body.getId()}"))
	      	 .process(new ProcessInventoryItemCommand())
	      	 .log("resulting body = ${body}");
	      
	      from("direct:handle-decrease")
	     	 .routeId("handle-decrease") 
	      	 .setHeader("id", simple("${body.getId()}"))
	      	 .process(new ProcessInventoryItemCommand())
	      	 .log("resulting body = ${body}");
	      
	      from("direct:dead-letter-channel")
	         .log("error !!");

	}
	
	class ProcessInventoryItemCommand implements Processor {

		@Override
		public void process(Exchange e) throws Exception {
			
			Command command = e.getIn().getBody(Command.class);
			
			UUID id = e.getIn().getHeader("id", UUID.class);
			
			TransactionContext context = hazelcastInstance.newTransactionContext();
			
			KeyValueSnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader = 
					new KeyValueSnapshotReader<>(mapFactory.get(HazelcastMaps.INVENTORY_ITEM_AGGREGATE_HISTORY.name()), 
												 snapshotMapFactory.get(HazelcastMaps.INVENTORY_ITEM_LAST_SNAPSHOT.name()));
			
			Snapshot<InventoryItemAggregateRoot> snapshot = snapshotReader.get(id, new InventoryItemAggregateRoot());
			
			InventoryItemCommandHandler commandHandler = new InventoryItemCommandHandler(snapshot.getAggregateInstance());

			context.beginTransaction();
			
			HazelcastEventStore<UUID> store = new HazelcastEventStore<UUID>(txMapFactory.get(context, HazelcastMaps.INVENTORY_ITEM_AGGREGATE_HISTORY.name()));
			
			TransactionalCommandProcessor<UUID, InventoryItemAggregateRoot> txProcessor = 
					new TransactionalCommandProcessor<>(id, snapshot.getVersion(), commandHandler, store, command, context);	

			UnitOfWork uow = txProcessor.handle();
			
			e.getOut().setBody(uow);

		}
		
	}

}
