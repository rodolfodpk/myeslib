package org.myeslib.hazelcast;

import static org.myeslib.util.EventSourcingMagicHelper.applyCommandOn;

import java.util.List;

import lombok.AllArgsConstructor;

import org.myeslib.core.AggregateRoot;
import org.myeslib.core.Command;
import org.myeslib.core.CommandHandler;
import org.myeslib.core.Event;
import org.myeslib.data.UnitOfWork;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.transaction.TransactionContext;

@AllArgsConstructor
public class TransactionalCommandProcessor<K, A extends AggregateRoot> {

	private final HazelcastInstance hazelcastInstance;
	private final AggregateRootHistoryTxMapFactory<K, A> txMapFactory;
	private final String mapId;
	
	public UnitOfWork handle(final K id, final Long version, final Command command, final CommandHandler<A> commandHandler) throws Throwable {
		
		TransactionContext transactionContext = hazelcastInstance.newTransactionContext();
		
		transactionContext.beginTransaction(); 
		
		HazelcastEventStore<K> store = new HazelcastEventStore<>(txMapFactory.get(transactionContext, mapId));

		UnitOfWork uow = null;
		try {
			//List<? extends Event> newEvents = commandHandler.handle(command); 
			List<? extends Event> newEvents = applyCommandOn(command, commandHandler);
			uow = UnitOfWork.create(command, version, newEvents);
			store.store(id, uow);
			transactionContext.commitTransaction();
		} catch (Throwable t) {
			transactionContext.rollbackTransaction();
			throw t.getCause();
		}
		return uow;
	}

}
