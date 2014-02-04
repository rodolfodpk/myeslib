package org.myeslib.storage.database;

import static org.myeslib.util.EventSourcingMagicHelper.applyCommandOn;

import java.util.List;

import lombok.AllArgsConstructor;

import org.myeslib.core.AggregateRoot;
import org.myeslib.core.Command;
import org.myeslib.core.CommandHandler;
import org.myeslib.core.Event;
import org.myeslib.data.UnitOfWork;
import org.myeslib.storage.TransactionalCommandHandler;

@AllArgsConstructor
public class DbTransactionalCommandHandler<K, A extends AggregateRoot> implements TransactionalCommandHandler<K, A> {

	final DbEventStore<K> store;
	
	/* (non-Javadoc)
	 * @see org.myeslib.hazelcast.TransactionalCommandHandler#handle(K, java.lang.Long, org.myeslib.core.Command, org.myeslib.core.CommandHandler)
	 */
	@Override
	public UnitOfWork handle(final K id, final Long version, final Command command, final CommandHandler<A> commandHandler) throws Throwable {
		
		UnitOfWork uow = null;
		try {
			//List<? extends Event> newEvents = commandHandler.handle(command); 
			List<? extends Event> newEvents = applyCommandOn(command, commandHandler);
			uow = UnitOfWork.create(command, version, newEvents);
			store.store(id, uow);
			return uow;
		} catch (Throwable t) {
			throw t.getCause();
		}

	}

}
