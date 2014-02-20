package org.myeslib.example.routes;

import java.util.ConcurrentModificationException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
import org.myeslib.example.HzExampleModule.ServiceJustForTest;
import org.myeslib.example.SampleDomain.CreateInventoryItem;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleDomain.InventoryItemCommandHandler;
import org.myeslib.hazelcast.function.HzCommandHandlerInvoker;
import org.myeslib.hazelcast.storage.HzUnitOfWorkWriter;

import com.google.inject.name.Named;
import com.hazelcast.core.IMap;

@Slf4j
public class HzConsumeCommandsRoute extends RouteBuilder {

	final SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader;
	final HzUnitOfWorkWriter<UUID> hzUnitOfWorkWriter ;
	final IMap<UUID, AggregateRootHistory> inventoryItemMap ;
	final String originUri;
	final String destinationUri;

	@Inject
	public HzConsumeCommandsRoute(SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader,
			HzUnitOfWorkWriter<UUID> hzUnitOfWorkWriter,
			IMap<UUID, AggregateRootHistory> inventoryItemMap, 
			@Named("originUri") String originUri,
			@Named("eventsDestinationUri") String destinationUri) {
		this.snapshotReader = snapshotReader;
		this.hzUnitOfWorkWriter = hzUnitOfWorkWriter;
		this.inventoryItemMap = inventoryItemMap;
		this.originUri = originUri;
		this.destinationUri = destinationUri;
	}
	
	@Override
	public void configure() throws Exception {

//		errorHandler(deadLetterChannel("direct:dead-letter-channel")
//			    .maximumRedeliveries(3).redeliveryDelay(5000));
		
         from(originUri)
			 .routeId("handle-inventory-item-command")
	      	 // .log("received = ${body}")
	         .setHeader("id", simple("${body.getId()}"))
	         .process(new ItemInventoryProcessor()) 
	         // .log("resulting body = ${body}")
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
			
			if (command.getVersion()<snapshot.getVersion()) {
				// NOW WHAT ???
			}
			
			InventoryItemCommandHandler commandHandler = new InventoryItemCommandHandler(snapshot.getAggregateInstance());
			
			boolean locked = false;
			
			try {

				if (command instanceof CreateInventoryItem){
					((CreateInventoryItem)command).setService(new ServiceJustForTest());
					// how to lock since its not there yet ? (on map)
				} else {
					log.debug("will try to lock within {} {}", id, Thread.currentThread());
					if (!inventoryItemMap.tryLock(id, 1, TimeUnit.SECONDS)){
						log.error("{} NOT locked", id);
						throw new ConcurrentModificationException();
					} 
					locked = true;
					log.debug("locked{} {}", id, Thread.currentThread());
				}
				
				CommandHandlerInvoker<UUID, InventoryItemAggregateRoot> cmdHandlerInvoker = new HzCommandHandlerInvoker<UUID, InventoryItemAggregateRoot>(hzUnitOfWorkWriter);
				
				UnitOfWork uow = null;
				try {
					uow = cmdHandlerInvoker.invoke(id, command, commandHandler);
					log.debug("commited transaction {} {}", id, Thread.currentThread());
				} catch (Throwable t) {
					log.error("how to rollback transaction? {} {}", id, Thread.currentThread());
					t.printStackTrace();
					throw new Exception(t);
				}
				
				e.getOut().setHeader("id", id);
				e.getOut().setBody(uow);
				
			} finally {
				if (locked) {
					inventoryItemMap.unlock(id);
				}
			}
				
		}
	}
	
}