package org.myeslib.example.jdbi.routes;

import java.util.ConcurrentModificationException;
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
import org.myeslib.example.SampleDomain.CreateInventoryItem;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleDomain.InventoryItemCommandHandler;
import org.myeslib.example.jdbi.modules.InventoryItemModule.AggregateRootHistoryWriterDaoFactory;
import org.myeslib.example.jdbi.modules.InventoryItemModule.ServiceJustForTest;
import org.myeslib.jdbi.storage.JdbiUnitOfWorkWriter;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.TransactionIsolationLevel;

import com.google.inject.name.Named;

@Slf4j
public class JdbiConsumeCommandsRoute extends RouteBuilder {

	final SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader;
	final CommandHandlerInvoker<UUID, InventoryItemAggregateRoot> cmdHandlerInvoker;
	final DBI dbi;
	final AggregateRootHistoryWriterDaoFactory aggregateRootHistoryWriterDaoFactory;
	final String originUri;
	
	@Inject
	public JdbiConsumeCommandsRoute(
			SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader,
			CommandHandlerInvoker<UUID, InventoryItemAggregateRoot> cmdHandlerInvoker,
			DBI dbi,
			AggregateRootHistoryWriterDaoFactory aggregateRootHistoryWriterDaoFactory, 
			@Named("originUri") String originUri) {
		this.snapshotReader = snapshotReader;
		this.cmdHandlerInvoker = cmdHandlerInvoker;
		this.dbi = dbi;
		this.aggregateRootHistoryWriterDaoFactory = aggregateRootHistoryWriterDaoFactory;
		this.originUri = originUri;
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
			if (!command.getVersion().equals(snapshot.getVersion())) {
				String msg = String.format("cmd version (%s) does not match snapshot version (%s)", command.getVersion(), snapshot.getVersion());
				throw new ConcurrentModificationException(msg);
			}
			
			InventoryItemCommandHandler commandHandler = new InventoryItemCommandHandler(snapshot.getAggregateInstance());

			if (command instanceof CreateInventoryItem){
				((CreateInventoryItem)command).setService(new ServiceJustForTest());
			}
			
			try {

				UnitOfWork uow = cmdHandlerInvoker.invoke(id, command, commandHandler);
				JdbiUnitOfWorkWriter<UUID> uowWriter = new JdbiUnitOfWorkWriter<>(aggregateRootHistoryWriterDaoFactory.create(handle));
				uowWriter.insert(id, uow);
				e.getOut().setHeader("id", id);
				e.getOut().setBody(uow);
				handle.commit();
				log.debug("commited transaction {} {}", id, Thread.currentThread());
			
			} catch (Throwable ex) {
				
				handle.rollback();
				log.error("*** Error - rolback tx");
				ex.printStackTrace();
				throw new RuntimeException(ex);
		
			} finally {
				handle.close();
			}
		}
	}
}