package org.myeslib.example.hazelcast.routes;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.myeslib.util.ValidationHelper.ensureSameVersion;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

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

@RequiredArgsConstructor(onConstructor=@__(@Inject))
public class HzInventoryItemCmdProcessor implements Processor {

    final static String ID = "id";

    final SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader;
    final HzUnitOfWorkJournal<UUID> uowJournal;
    final ItemDescriptionGeneratorService domainService;
    final UUIDGenerator uuidGenerator;

    @Override
    public void process(Exchange e) throws Exception {

        final UUID id = e.getIn().getHeader(ID, UUID.class);
        final Command command = e.getIn().getBody(Command.class);
        final Snapshot<InventoryItemAggregateRoot> snapshot = snapshotReader.get(id);

        checkNotNull(id);
        checkNotNull(command);
        checkNotNull(command.getTargetVersion());

        final List<? extends Event> events;
        final InventoryItemAggregateRoot instance = snapshot.getAggregateInstance();

        if (command instanceof CreateInventoryItem) {
            CreateCommandHandler commandHandler = new CreateCommandHandler(instance, domainService);
            events = commandHandler.handle(((CreateInventoryItem) command));
        } else if (command instanceof IncreaseInventory) {
            ensureSameVersion(id.toString(), command.getTargetVersion(), snapshot.getVersion());
            IncreaseCommandHandler commandHandler = new IncreaseCommandHandler(instance);
            events = commandHandler.handle(((IncreaseInventory) command));
        } else if (command instanceof DecreaseInventory) {
            ensureSameVersion(id.toString(), command.getTargetVersion(), snapshot.getVersion());
            DecreaseCommandHandler commandHandler = new DecreaseCommandHandler(instance);
            events = commandHandler.handle(((DecreaseInventory) command));
        } else {
            throw new IllegalArgumentException("Unknown command");
        }

        final UnitOfWork uow = UnitOfWork.create(uuidGenerator.generate(), command, events);

        uowJournal.append(id, uow);

        e.getOut().setHeader(ID, id);
        e.getOut().setBody(uow);

        // since the hazelcast map is configured to be write through and there is a db trigger to control optimistic locking and concurrency, 
        // this is a commited transaction

    }
}
