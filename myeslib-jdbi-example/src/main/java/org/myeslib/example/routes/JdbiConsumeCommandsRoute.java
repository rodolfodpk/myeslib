package org.myeslib.example.routes;

import java.util.UUID;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.myeslib.core.Command;
import org.myeslib.core.data.Snapshot;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.core.function.CommandHandlerInvoker;
import org.myeslib.core.storage.SnapshotReader;
import org.myeslib.example.JdbiExampleModule.AggregateRootHistoryWriterDaoFactory;
import org.myeslib.example.JdbiExampleModule.ServiceJustForTest;
import org.myeslib.example.SampleDomain.CreateInventoryItem;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleDomain.InventoryItemCommandHandler;
import org.myeslib.jdbi.storage.JdbiUnitOfWorkWriter;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.TransactionIsolationLevel;

import com.google.inject.name.Named;

@Slf4j
public class JdbiConsumeCommandsRoute extends RouteBuilder {

	final DBI dbi;
	final SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader;
	final CommandHandlerInvoker<UUID, InventoryItemAggregateRoot> cmdHandlerInvoker;
	final AggregateRootHistoryWriterDaoFactory aggregateRootHistoryWriterDaoFactory;
	final String originUri;
	final String destinationUri;
	
	@Inject
	public JdbiConsumeCommandsRoute(
			DBI dbi,
			SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader,
			CommandHandlerInvoker<UUID, InventoryItemAggregateRoot> cmdHandlerInvoker,
			AggregateRootHistoryWriterDaoFactory aggregateRootHistoryWriterDaoFactory, 
			@Named("originUri") String originUri,
			@Named("eventsDestinationUri") String destinationUri) {
		this.dbi = dbi;
		this.snapshotReader = snapshotReader;
		this.cmdHandlerInvoker = cmdHandlerInvoker;
		this.aggregateRootHistoryWriterDaoFactory = aggregateRootHistoryWriterDaoFactory;
		this.originUri = originUri;
		this.destinationUri = destinationUri;
	}
	
	@Override
	public void configure() throws Exception {

//		errorHandler(deadLetterChannel("direct:dead-letter-channel")
//			    .maximumRedeliveries(3).redeliveryDelay(5000));

		from(originUri) 	
		     .routeId("handle-inventory-item-command")
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
			final Command command = e.getIn().getBody(Command.class);
			
			final Handle handle = dbi.open() ;
			handle.getConnection().setAutoCommit(false);
			handle.begin();
			handle.setTransactionIsolation(TransactionIsolationLevel.SERIALIZABLE);
			
			Snapshot<InventoryItemAggregateRoot> snapshot = snapshotReader.get(id); 
			
			if (command.getVersion()<snapshot.getVersion()) {
				// NOW WHAT ???
			}
			
			InventoryItemCommandHandler commandHandler = new InventoryItemCommandHandler(snapshot.getAggregateInstance());
			
			try {

				if (command instanceof CreateInventoryItem){
					((CreateInventoryItem)command).setService(new ServiceJustForTest());
				}
				
				JdbiUnitOfWorkWriter<UUID> uowWriter = new JdbiUnitOfWorkWriter<>(aggregateRootHistoryWriterDaoFactory.create(handle));
				
				UnitOfWork uow = null;
				try {
					uow = cmdHandlerInvoker.invoke(id, command, commandHandler);
					uowWriter.insert(id, uow);
					log.debug("commited transaction {} {}", id, Thread.currentThread());
				} catch (Throwable t) {
					log.error("how to rollback transaction? {} {}", id, Thread.currentThread());
					t.printStackTrace();
					throw new Exception(t);
				}
				
				handle.commit();
				
				e.getOut().setHeader("id", id);
				e.getOut().setBody(uow);
				
			} catch (Throwable ex) {
				handle.rollback();
				log.error("*** Error - rolback tx");
				ex.printStackTrace();
				throw ex;
				
			} finally {
				handle.close();
			}
				
		}
	}
	
}