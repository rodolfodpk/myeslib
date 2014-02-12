package org.myeslib.hazelcast.function;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.myeslib.util.EventSourcingMagicHelper.applyCommandOn;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.myeslib.core.AggregateRoot;
import org.myeslib.core.Command;
import org.myeslib.core.CommandHandler;
import org.myeslib.core.Event;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.core.function.CommandHandlerInvoker;
import org.myeslib.hazelcast.storage.HzUnitOfWorkWriter;

import com.google.inject.Inject;

@Slf4j
public class HzCommandHandlerInvoker<K, A extends AggregateRoot> implements CommandHandlerInvoker<K, A> {

	private final HzUnitOfWorkWriter<K> writer ;

	@Inject
	public HzCommandHandlerInvoker(HzUnitOfWorkWriter<K> writer) {
		this.writer = writer;
	}

	/*
	 * (non-Javadoc)
	 * @see org.myeslib.core.function.CommandHandlerInvoker#invoke(java.lang.Object, java.lang.Long, org.myeslib.core.Command, org.myeslib.core.CommandHandler)
	 */
	@Override
	public UnitOfWork invoke(final K id, final Long version, final Command command, final CommandHandler<A> commandHandler) throws Throwable {
			
			checkNotNull(id);
			checkNotNull(version);
			checkNotNull(command);
			checkNotNull(commandHandler);

			try {
			//List<? extends Event> newEvents = commandHandler.handle(command); 
			final List<? extends Event> newEvents = applyCommandOn(command, commandHandler);
			final UnitOfWork uow = UnitOfWork.create(command, version, newEvents);
			log.debug("got UnitOfWork");
			writer.insert(id, uow);
			log.debug("inserted UnitOfWork");
			return uow;
		} catch (Throwable t) {
			// t.printStackTrace();
			throw t.getCause();
		}

	}

}
