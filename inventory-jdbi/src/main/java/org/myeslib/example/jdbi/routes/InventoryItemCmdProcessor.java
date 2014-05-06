package org.myeslib.example.jdbi.routes;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.UUID;

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
public class InventoryItemCmdProcessor implements Processor {
    
    final static String ID = "id";
    
    @Inject
    public InventoryItemCmdProcessor(
            SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader,
            DBI dbi,
            AggregateRootHistoryWriterDaoFactory aggregateRootHistoryWriterDaoFactory,
            ItemDescriptionGeneratorService service,
            UUIDGenerator uuidGenerator) {
        this.snapshotReader = snapshotReader;
        this.dbi = dbi;
        this.aggregateRootHistoryWriterDaoFactory = aggregateRootHistoryWriterDaoFactory;
        this.service = service;
        this.uuidGenerator = uuidGenerator;
    }

    final SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader;
    final DBI dbi;
    final AggregateRootHistoryWriterDaoFactory aggregateRootHistoryWriterDaoFactory;
    final ItemDescriptionGeneratorService service;
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
            
            if (!command.getTargetVersion().equals(snapshot.getVersion())) {
                String msg = String.format("cmd version (%s) does not match snapshot version (%s)", command.getTargetVersion(), snapshot.getVersion());
                throw new ConcurrentModificationException(msg);
            }

            checkNotNull(id);
            checkNotNull(command);
            checkNotNull(command.getTargetVersion());

            final List<? extends Event> events;

            if (command instanceof CreateInventoryItem){
                CreateCommandHandler commandHandler = new CreateCommandHandler(snapshot.getAggregateInstance(), service);
                events = commandHandler.handle(((CreateInventoryItem)command));
            } else if (command instanceof IncreaseInventory) {
                IncreaseCommandHandler commandHandler = new IncreaseCommandHandler(snapshot.getAggregateInstance());
                events = commandHandler.handle(((IncreaseInventory)command));
            } else if (command instanceof DecreaseInventory) {
                DecreaseCommandHandler commandHandler = new DecreaseCommandHandler(snapshot.getAggregateInstance());
                events = commandHandler.handle(((DecreaseInventory)command));
            } else {
                events = new ArrayList<>();
            }

            final JdbiUnitOfWorkJournal<UUID> uowJournal = new JdbiUnitOfWorkJournal<>(aggregateRootHistoryWriterDaoFactory.create(handle));
            final UnitOfWork uow = UnitOfWork.create(uuidGenerator.generate(), command, events);
            
            uowJournal.append(id, uow);
            e.getOut().setHeader(ID, id);
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
