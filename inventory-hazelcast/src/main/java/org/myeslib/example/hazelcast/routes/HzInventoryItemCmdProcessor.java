package org.myeslib.example.hazelcast.routes;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.UUID;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.myeslib.core.Command;
import org.myeslib.core.Event;
import org.myeslib.core.data.Snapshot;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.core.storage.SnapshotReader;
import org.myeslib.example.SampleDomain.CreateCommandHandler;
import org.myeslib.example.SampleDomain.CreateInventoryItem;
import org.myeslib.example.SampleDomain.DecreaseCommandHandler;
import org.myeslib.example.SampleDomain.DecreaseInventory;
import org.myeslib.example.SampleDomain.IncreaseCommandHandler;
import org.myeslib.example.SampleDomain.IncreaseInventory;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleDomain.ItemDescriptionGeneratorService;
import org.myeslib.hazelcast.storage.HzUnitOfWorkJournal;
import org.myeslib.util.UUIDGenerator;

import com.google.inject.Inject;

public class HzInventoryItemCmdProcessor implements Processor {

    final static String ID = "id";

    @Inject
    public HzInventoryItemCmdProcessor(
            SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader,
            HzUnitOfWorkJournal<UUID> uowJournal, ItemDescriptionGeneratorService service,
            UUIDGenerator uuidGenerator) {
        this.snapshotReader = snapshotReader;
        this.uowJournal = uowJournal;
        this.service = service;
        this.uuidGenerator = uuidGenerator;
    }

    final SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader;
    final HzUnitOfWorkJournal<UUID> uowJournal;
    final ItemDescriptionGeneratorService service;
    final UUIDGenerator uuidGenerator;
    
    @Override
    public void process(Exchange e) throws Exception {

        final UUID id = e.getIn().getHeader(ID, UUID.class);
        final Command command = e.getIn().getBody(Command.class);
        final Snapshot<InventoryItemAggregateRoot> snapshot = snapshotReader.get(id);

        checkNotNull(id);
        checkNotNull(command);
        checkNotNull(command.getTargetVersion());

        if (!command.getTargetVersion().equals(snapshot.getVersion())) {
            String msg = String.format(
                    "** (%s) cmd version (%s) does not match snapshot version (%s)", id,
                    command.getTargetVersion(), snapshot.getVersion());
            throw new ConcurrentModificationException(msg);
        }

        final List<? extends Event> events;

        if (command instanceof CreateInventoryItem) {
            CreateCommandHandler commandHandler = new CreateCommandHandler(
                    snapshot.getAggregateInstance(), service);
            events = commandHandler.handle(((CreateInventoryItem) command));
        } else if (command instanceof IncreaseInventory) {
            IncreaseCommandHandler commandHandler = new IncreaseCommandHandler(
                    snapshot.getAggregateInstance());
            events = commandHandler.handle(((IncreaseInventory) command));
        } else if (command instanceof DecreaseInventory) {
            DecreaseCommandHandler commandHandler = new DecreaseCommandHandler(
                    snapshot.getAggregateInstance());
            events = commandHandler.handle(((DecreaseInventory) command));
        } else {
            events = new ArrayList<>();
        }

        final UnitOfWork uow = UnitOfWork.create(uuidGenerator.generate(), command, events);

        uowJournal.append(id, uow);

        e.getOut().setHeader(ID, id);
        e.getOut().setBody(uow);

        // log.debug("since this map is configured to be write through and there is a db trigger to control optimistic locking and concurrency, this is a commited transaction {} {}",
        // id, Thread.currentThread());

    }
}
