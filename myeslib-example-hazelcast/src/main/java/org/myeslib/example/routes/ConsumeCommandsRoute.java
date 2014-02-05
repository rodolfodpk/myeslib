package org.myeslib.example.routes;

import java.util.UUID;

import javax.inject.Inject;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.myeslib.core.Command;
import org.myeslib.data.Snapshot;
import org.myeslib.data.UnitOfWork;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleDomain.InventoryItemCommandHandler;
import org.myeslib.storage.CommandHandlerInvoker;
import org.myeslib.storage.SnapshotReader;

public class ConsumeCommandsRoute extends RouteBuilder {

	final SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader;
	final CommandHandlerInvoker<UUID, InventoryItemAggregateRoot> cmdHandlerInvoker;
	
	@Inject
	public ConsumeCommandsRoute(SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader,
			CommandHandlerInvoker<UUID, InventoryItemAggregateRoot> cmdHandlerInvoker) {
		this.snapshotReader = snapshotReader;
		this.cmdHandlerInvoker = cmdHandlerInvoker;
	}
	
	@Override
	public void configure() throws Exception {

//		  errorHandler(deadLetterChannel("direct:dead-letter-channel")
//				    .maximumRedeliveries(3).redeliveryDelay(5000));
			
	      from("direct:handle-inventory-item-command")
	         .routeId("handle-inventory-item-command")
	      	 .log("received = ${body}")
	         .setHeader("id", simple("${body.getId()}"))
	         .process(new ItemInventoryProcessor())
	      	 .log("resulting body = ${body}");
	      
	      from("direct:dead-letter-channel")
	         .log("error !!");

	}

	
	private class ItemInventoryProcessor implements Processor {
		@Override
		public void process(Exchange e) throws Exception {
			Command command = e.getIn().getBody(Command.class);
			UUID id = e.getIn().getHeader("id", UUID.class);
			Snapshot<InventoryItemAggregateRoot> snapshot = snapshotReader.get(id, new InventoryItemAggregateRoot());
			InventoryItemCommandHandler commandHandler = new InventoryItemCommandHandler(snapshot.getAggregateInstance());
			UnitOfWork uow = null;
			try {
				uow = cmdHandlerInvoker.invoke(id, snapshot.getVersion(), command, commandHandler);
			} catch (Throwable t) {
				t.printStackTrace();
				throw new Exception(t);
			}
			e.getOut().setHeader("id", id);
			e.getOut().setBody(uow);
		}
	}

}
