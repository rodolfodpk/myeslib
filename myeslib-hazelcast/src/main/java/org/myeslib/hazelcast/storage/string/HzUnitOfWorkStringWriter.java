package org.myeslib.hazelcast.storage.string;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ConcurrentModificationException;

import lombok.extern.slf4j.Slf4j;

import org.myeslib.core.data.AggregateRootHistory;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.core.storage.UnitOfWorkWriter;

import com.google.common.base.Function;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.hazelcast.core.IMap;

@Deprecated
@Slf4j
public class HzUnitOfWorkStringWriter<K> implements UnitOfWorkWriter<K>{

	private final Function<AggregateRootHistory, String> toStringFunction ;
	private final Function<String, AggregateRootHistory> fromStringFunction ;
	private final IMap<K, String> pastTransactionsMap ;
	
	@Inject
	public HzUnitOfWorkStringWriter(Function<AggregateRootHistory, String> toStringFunction,
			 Function<String, AggregateRootHistory> fromStringFunction,
			@Assisted IMap<K, String> pastTransactionsMap) {
		checkNotNull(toStringFunction);
		checkNotNull(fromStringFunction);
		checkNotNull(pastTransactionsMap);
		this.toStringFunction = toStringFunction;
		this.fromStringFunction = fromStringFunction;
		this.pastTransactionsMap = pastTransactionsMap;
	}

	/*
	 * (non-Javadoc)
	 * @see org.myeslib.core.storage.UnitOfWorkWriter#insert(java.lang.Object, org.myeslib.core.data.UnitOfWork)
	 */
	public void insert(final K id, final UnitOfWork uow) {
		checkNotNull(id);
		checkNotNull(uow);
		final AggregateRootHistory history = getHistoryFor(id);
		if (!history.getLastVersion().equals(uow.getCommandVersion())){
			throw new ConcurrentModificationException(String.format("version %s does not match the expected %s", 
																	history.getLastVersion().toString(), 
																	uow.getCommandVersion().toString())
													 );
														
		} 
		history.add(uow);
		String asString = toStringFunction.apply(history);
		pastTransactionsMap.set(id, asString); // hazelcast optimization --> set instead of put since is void
	}
		
	private AggregateRootHistory getHistoryFor(final K id) {
		// TODO WARNING https://github.com/hazelcast/hazelcast/issues/1593 
		log.debug("looking for {} on map {} ", id, pastTransactionsMap.getName());
		String asString = pastTransactionsMap.get(id);
		if (asString==null) {
			log.debug("found NULL value for {} on map {} ", id, pastTransactionsMap.getName());
		}
		AggregateRootHistory history = fromStringFunction.apply(asString);
		return history == null ? new AggregateRootHistory() : history;
	}

}
