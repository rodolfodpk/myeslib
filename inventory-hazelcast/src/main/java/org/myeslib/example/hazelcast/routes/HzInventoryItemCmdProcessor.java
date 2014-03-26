package org.myeslib.example.hazelcast.routes;

import java.util.ConcurrentModificationException;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.myeslib.core.Command;
import org.myeslib.core.data.Snapshot;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.core.function.CommandHandlerInvoker;
import org.myeslib.core.storage.SnapshotReader;
import org.myeslib.example.SampleDomain.CreateInventoryItem;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleDomain.InventoryItemCommandHandler;
import org.myeslib.example.SampleDomain.ItemDescriptionGeneratorService;
import org.myeslib.hazelcast.storage.HzUnitOfWorkJournal;

import com.google.inject.Inject;

@Slf4j
public class HzInventoryItemCmdProcessor  implements Processor {
	
	@Inject
	public HzInventoryItemCmdProcessor(
			SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader,
			CommandHandlerInvoker<UUID, InventoryItemAggregateRoot> cmdHandlerInvoker,
			HzUnitOfWorkJournal<UUID> hzUnitOfWorkWriter,
			ItemDescriptionGeneratorService service) {
		this.snapshotReader = snapshotReader;
		this.cmdHandlerInvoker = cmdHandlerInvoker;
		this.hzUnitOfWorkWriter = hzUnitOfWorkWriter;
		this.service = service;
	}

	final SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader;
	final CommandHandlerInvoker<UUID, InventoryItemAggregateRoot> cmdHandlerInvoker;
	final HzUnitOfWorkJournal<UUID> hzUnitOfWorkWriter ;
	final ItemDescriptionGeneratorService service;

	@Override
	public void process(Exchange e) throws Exception {

		UUID id = e.getIn().getHeader("id", UUID.class);
		Command command = e.getIn().getBody(Command.class);
		
		Snapshot<InventoryItemAggregateRoot> snapshot = snapshotReader.get(id); 
		if (!command.getVersion().equals(snapshot.getVersion())) {
			String msg = String.format("** (%s) cmd version (%s) does not match snapshot version (%s)", id, command.getVersion(), snapshot.getVersion());
			throw new ConcurrentModificationException(msg);
		}
		
		InventoryItemCommandHandler commandHandler = new InventoryItemCommandHandler(snapshot.getAggregateInstance());
		
		try {

			if (command instanceof CreateInventoryItem){
				((CreateInventoryItem)command).setService(service);
			}
			
			UnitOfWork uow = cmdHandlerInvoker.invoke(id, command, commandHandler);
			hzUnitOfWorkWriter.append(id, uow);
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
