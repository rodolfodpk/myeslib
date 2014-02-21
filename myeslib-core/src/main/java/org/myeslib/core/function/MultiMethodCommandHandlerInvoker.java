package org.myeslib.core.function;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.myeslib.util.EventSourcingMagicHelper.applyCommandOn;

import java.util.List;

import org.myeslib.core.AggregateRoot;
import org.myeslib.core.Command;
import org.myeslib.core.CommandHandler;
import org.myeslib.core.Event;
import org.myeslib.core.data.UnitOfWork;

public class MultiMethodCommandHandlerInvoker<K, A extends AggregateRoot> implements CommandHandlerInvoker<K, A> {

	/*
	 * (non-Javadoc)
	 * @see org.myeslib.core.function.CommandHandlerInvoker#invoke(java.lang.Object, org.myeslib.core.Command, org.myeslib.core.CommandHandler)
	 */
	@Override
	public UnitOfWork invoke(K id, Command command, CommandHandler<A> commandHandler) throws Throwable {
		
		checkNotNull(id);
		checkNotNull(command);
		checkNotNull(command.getVersion());
		checkNotNull(commandHandler);
		
		UnitOfWork uow = null;
		try {
			//List<? extends Event> newEvents = commandHandler.handle(command); 
			List<? extends Event> newEvents = applyCommandOn(command, commandHandler);
			uow = UnitOfWork.create(command, newEvents);
		} catch (Throwable t) {
			throw t.getCause();
		}

		return uow;
		
	}

}

