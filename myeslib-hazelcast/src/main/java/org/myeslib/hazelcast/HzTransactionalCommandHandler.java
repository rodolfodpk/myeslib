package org.myeslib.hazelcast;

import static org.myeslib.util.EventSourcingMagicHelper.applyCommandOn;

import java.util.List;

import lombok.AllArgsConstructor;

import org.myeslib.core.AggregateRoot;
import org.myeslib.core.Command;
import org.myeslib.core.CommandHandler;
import org.myeslib.core.Event;
import org.myeslib.data.AggregateRootHistory;
import org.myeslib.data.UnitOfWork;
import org.myeslib.storage.TransactionalCommandHandler;

import com.google.common.base.Function;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.transaction.TransactionContext;

@AllArgsConstructor
public class HzTransactionalCommandHandler<K, A extends AggregateRoot> implements TransactionalCommandHandler<K, A> {

	private final HazelcastInstance hazelcastInstance;
	private final HzStringTxMapFactory<K> txMapFactory;
	private final String mapId;
	private final Function<String, AggregateRootHistory> fromStringFunction ;
	private final Function<AggregateRootHistory, String> toStringFunction ;
	
	/* (non-Javadoc)
	 * @see org.myeslib.hazelcast.ITransactionalCommandHandler#handle(K, java.lang.Long, org.myeslib.core.Command, org.myeslib.core.CommandHandler)
	 */
	@Override
	public UnitOfWork handle(final K id, final Long version, final Command command, final CommandHandler<A> commandHandler) throws Throwable {
		
		TransactionContext transactionContext = hazelcastInstance.newTransactionContext();
		transactionContext.beginTransaction(); 
		HzEventStore<K> store = new HzEventStore<>(txMapFactory.get(transactionContext, mapId), toStringFunction, fromStringFunction);

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
