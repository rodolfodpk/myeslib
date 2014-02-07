package org.myeslib.hazelcast.storage;

import java.util.ConcurrentModificationException;

import lombok.extern.slf4j.Slf4j;

import org.myeslib.core.data.AggregateRootHistory;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.core.storage.UnitOfWorkWriter;

import com.google.common.base.Function;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.hazelcast.core.TransactionalMap;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class HzUnitOfWorkWriter<K> implements UnitOfWorkWriter<K>{

	private final Function<AggregateRootHistory, String> toStringFunction ;
	private final Function<String, AggregateRootHistory> fromStringFunction ;
	private final TransactionalMap<K, String> pastTransactionsMap ;
	
	@Inject
	public HzUnitOfWorkWriter(Function<AggregateRootHistory, String> toStringFunction,
			 Function<String, AggregateRootHistory> fromStringFunction,
			@Assisted TransactionalMap<K, String> pastTransactionsMap) {
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
		if (!history.getLastVersion().equals(uow.getBaseVersion())){
			throw new ConcurrentModificationException(String.format("version %s does not match the expected %s", 
																	history.getLastVersion().toString(), 
																	uow.getBaseVersion().toString())
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
		log.debug("found NULL value for {} on map {} ", id, pastTransactionsMap.getName());
		AggregateRootHistory history = fromStringFunction.apply(asString);
		return history == null ? new AggregateRootHistory() : history;
	}

}
