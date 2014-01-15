package org.myeslib.hazelcast;

import static org.myeslib.util.EventSourcingMagicHelper.applyCommandOn;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.myeslib.core.AggregateRoot;
import org.myeslib.core.Command;
import org.myeslib.core.CommandHandler;
import org.myeslib.core.Event;
import org.myeslib.data.UnitOfWork;

import com.hazelcast.transaction.TransactionContext;

@Slf4j
@AllArgsConstructor
public class TransactionalCommandProcessor<K, A extends AggregateRoot> {
	
	private final K id;
	private final Long version;
	private final CommandHandler<A> commandHandler ;
	private final HazelcastEventStore<K> store ;
	private final Command command;
	private final TransactionContext transactionContext;
	
	public UnitOfWork handle() {
		
		//transactionContext.beginTransaction();
		UnitOfWork uow = null;
		try {
			//List<? extends Event> newEvents = commandHandler.handle(command); 
			List<? extends Event> newEvents = applyCommandOn(command, commandHandler);
			uow = UnitOfWork.create(command, version, newEvents);
			store.store(id, uow);
			transactionContext.commitTransaction();
		} catch (Throwable t) {
			log.error(t.getMessage());
			t.printStackTrace();
			transactionContext.rollbackTransaction();
			throw t;
		}
		return uow;
	}

}
