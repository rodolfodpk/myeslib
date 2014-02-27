package org.myeslib.example.hazelcast.routes;

import java.util.ConcurrentModificationException;
import java.util.UUID;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.myeslib.core.Command;
import org.myeslib.core.data.AggregateRootHistory;
import org.myeslib.core.data.Snapshot;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.core.function.CommandHandlerInvoker;
import org.myeslib.core.storage.SnapshotReader;
import org.myeslib.example.SampleDomain.CreateInventoryItem;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleDomain.InventoryItemCommandHandler;
import org.myeslib.example.SampleDomain.ItemDescriptionGeneratorService;
import org.myeslib.hazelcast.storage.HzUnitOfWorkWriter;

import com.google.inject.name.Named;
import com.hazelcast.core.IMap;

@Slf4j
public class HzConsumeCommandsRoute extends RouteBuilder {

	final SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader;
	final HzUnitOfWorkWriter<UUID> hzUnitOfWorkWriter ;
	final CommandHandlerInvoker<UUID, InventoryItemAggregateRoot> cmdHandlerInvoker;
	final IMap<UUID, AggregateRootHistory> inventoryItemMap ;
	final ItemDescriptionGeneratorService service;
	final String originUri;
	final String destinationUri;


	@Inject
	public HzConsumeCommandsRoute(SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader,
			HzUnitOfWorkWriter<UUID> hzUnitOfWorkWriter,
			CommandHandlerInvoker<UUID, InventoryItemAggregateRoot> cmdHandlerInvoker,
			IMap<UUID, AggregateRootHistory> inventoryItemMap, 
			ItemDescriptionGeneratorService service,
			@Named("originUri") String originUri,
			@Named("eventsDestinationUri") String destinationUri) {
		this.snapshotReader = snapshotReader;
		this.hzUnitOfWorkWriter = hzUnitOfWorkWriter;
		this.cmdHandlerInvoker = cmdHandlerInvoker;
		this.inventoryItemMap = inventoryItemMap;
		this.service = service;
		this.originUri = originUri;
		this.destinationUri = destinationUri;
	}
	
	@Override
	public void configure() throws Exception {

//		errorHandler(deadLetterChannel("direct:dead-letter-channel")
//			    .maximumRedeliveries(3).redeliveryDelay(5000));
		
         from(originUri)
			 .routeId("handle-inventory-item-command")
	      	 .log("received from http ${body}")
	         .setHeader("id", simple("${body.getId()}"))
	         .process(new ItemInventoryProcessor()) 
	      	 .wireTap(destinationUri)
	      			.newExchangeBody(header("id"))
	      	 .end()		
	      	  ;
       
	     from("direct:dead-letter-channel")
	      	.routeId("direct:dead-letter-channel")
	         .log("error !!");

	}
	
	private class ItemInventoryProcessor implements Processor {
		
		@Override
		public void process(Exchange e) throws Exception {

			UUID id = e.getIn().getHeader("id", UUID.class);
			Command command = e.getIn().getBody(Command.class);
			
			Snapshot<InventoryItemAggregateRoot> snapshot = snapshotReader.get(id); 
			if (!command.getVersion().equals(snapshot.getVersion())) {
				String msg = String.format("cmd version (%s) does not match snapshot version (%s)", command.getVersion(), snapshot.getVersion());
				throw new ConcurrentModificationException(msg);
			}
			
			InventoryItemCommandHandler commandHandler = new InventoryItemCommandHandler(snapshot.getAggregateInstance());
			
			try {

				if (command instanceof CreateInventoryItem){
					((CreateInventoryItem)command).setService(service);
				}
				
				UnitOfWork uow = cmdHandlerInvoker.invoke(id, command, commandHandler);
				hzUnitOfWorkWriter.insert(id, uow);
				e.getOut().setHeader("id", id);
				e.getOut().setBody(uow);
				// log.debug("since this map is configured to be write through and there is a db trigger to control optimistic locking and concurrency, this is a commited transaction {} {}", id, Thread.currentThread());

			} catch (Throwable ex) {
				
				log.error("*** error");
				ex.printStackTrace();
				throw new RuntimeException(ex);
				
			} finally {
				
				
			}
				
		}
	}
	
}