package org.myeslib.example.jdbi.routes;

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
import org.myeslib.example.jdbi.modules.InventoryItemModule.AggregateRootHistoryWriterDaoFactory;
import org.myeslib.example.jdbi.modules.InventoryItemModule.ServiceJustForTest;
import org.myeslib.jdbi.storage.JdbiUnitOfWorkJournal;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.TransactionIsolationLevel;

import com.google.inject.Inject;

@Slf4j
public class InventoryItemCmdProcessor implements Processor {

    @Inject
    public InventoryItemCmdProcessor(
            SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader,
            CommandHandlerInvoker<UUID, InventoryItemAggregateRoot> cmdHandlerInvoker, DBI dbi,
            AggregateRootHistoryWriterDaoFactory aggregateRootHistoryWriterDaoFactory) {
        this.snapshotReader = snapshotReader;
        this.cmdHandlerInvoker = cmdHandlerInvoker;
        this.dbi = dbi;
        this.aggregateRootHistoryWriterDaoFactory = aggregateRootHistoryWriterDaoFactory;
    }

    final SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader;
    final CommandHandlerInvoker<UUID, InventoryItemAggregateRoot> cmdHandlerInvoker;
    final DBI dbi;
    final AggregateRootHistoryWriterDaoFactory aggregateRootHistoryWriterDaoFactory;

    @Override
    public void process(Exchange e) throws Exception {

        UUID id = e.getIn().getHeader("id", UUID.class);
        final Command command = e.getIn().getBody(Command.class);

        final Handle handle = dbi.open();
        handle.getConnection().setAutoCommit(false);
        handle.begin();
        handle.setTransactionIsolation(TransactionIsolationLevel.READ_COMMITTED);

        Snapshot<InventoryItemAggregateRoot> snapshot = snapshotReader.get(id);
        if (!command.getTargetVersion().equals(snapshot.getVersion())) {
            String msg = String.format("cmd version (%s) does not match snapshot version (%s)",
                    command.getTargetVersion(), snapshot.getVersion());
            throw new ConcurrentModificationException(msg);
        }

        InventoryItemCommandHandler commandHandler = new InventoryItemCommandHandler(snapshot.getAggregateInstance());

        if (command instanceof CreateInventoryItem) {
            ((CreateInventoryItem) command).setService(new ServiceJustForTest());
        }

        try {

            UnitOfWork uow = cmdHandlerInvoker.invoke(id, command, commandHandler);
            JdbiUnitOfWorkJournal<UUID> uowWriter = new JdbiUnitOfWorkJournal<>(
                    aggregateRootHistoryWriterDaoFactory.create(handle));
            uowWriter.append(id, uow);
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
