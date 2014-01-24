package org.myeslib.storage;

import org.myeslib.core.AggregateRoot;
import org.myeslib.core.Command;
import org.myeslib.core.CommandHandler;
import org.myeslib.data.UnitOfWork;

public interface TransactionalCommandHandler<K, A extends AggregateRoot> {

	public abstract UnitOfWork handle(K id, Long version, Command command,
			CommandHandler<A> commandHandler) throws Throwable;

}