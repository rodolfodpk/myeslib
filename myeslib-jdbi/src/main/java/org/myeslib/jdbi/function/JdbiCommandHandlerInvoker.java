package org.myeslib.jdbi.function;

import static org.myeslib.util.EventSourcingMagicHelper.applyCommandOn;

import java.util.List;

import lombok.AllArgsConstructor;

import org.myeslib.core.AggregateRoot;
import org.myeslib.core.Command;
import org.myeslib.core.CommandHandler;
import org.myeslib.core.Event;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.core.function.CommandHandlerInvoker;

@AllArgsConstructor
public class JdbiCommandHandlerInvoker<K, A extends AggregateRoot> implements CommandHandlerInvoker<K, A> {

	/*
	 * (non-Javadoc)
	 * @see org.myeslib.core.function.CommandHandlerInvoker#invoke(java.lang.Object, java.lang.Long, org.myeslib.core.Command, org.myeslib.core.CommandHandler)
	 */
	@Override
	public UnitOfWork invoke(final K id, final Long version, final Command command, final CommandHandler<A> commandHandler) throws Throwable {
		
		UnitOfWork uow = null;
		try {
			//List<? extends Event> newEvents = commandHandler.handle(command); 
			List<? extends Event> newEvents = applyCommandOn(command, commandHandler);
			uow = UnitOfWork.create(command, version, newEvents);
			return uow;
		} catch (Throwable t) {
			throw t.getCause();
		}

	}

}
