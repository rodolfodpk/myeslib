package org.myeslib.example.jdbi.routes;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.myeslib.util.ValidationHelper.ensureSameVersion;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
import org.myeslib.example.jdbi.modules.InventoryItemModule.AggregateRootHistoryWriterDaoFactory;
import org.myeslib.jdbi.storage.JdbiUnitOfWorkJournal;
import org.myeslib.util.UUIDGenerator;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.TransactionIsolationLevel;

import com.google.inject.Inject;

@Slf4j
@RequiredArgsConstructor(onConstructor=@__(@Inject))
public class InventoryItemCmdProcessor implements Processor {
    
    final static String ID = "id";
    
    final SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader;
    final DBI dbi;
    final AggregateRootHistoryWriterDaoFactory aggregateRootHistoryWriterDaoFactory;
    final ItemDescriptionGeneratorService domainService;
    final UUIDGenerator uuidGenerator;
    
    @Override
    public void process(Exchange e) throws Exception {

        final Handle handle = dbi.open();

        try {
            
            handle.getConnection().setAutoCommit(false);
            handle.begin();
            handle.setTransactionIsolation(TransactionIsolationLevel.READ_COMMITTED);

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

            final JdbiUnitOfWorkJournal<UUID> uowJournal = new JdbiUnitOfWorkJournal<>(aggregateRootHistoryWriterDaoFactory.create(handle));
            final UnitOfWork uow = UnitOfWork.create(uuidGenerator.generate(), command, events);
            
            uowJournal.append(id, uow);
            
            handle.commit();

            e.getOut().setHeader(ID, id);
            e.getOut().setBody(uow);
            
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
