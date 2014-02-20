package org.myeslib.hazelcast.storage;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ConcurrentModificationException;

import lombok.extern.slf4j.Slf4j;

import org.myeslib.core.data.AggregateRootHistory;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.core.storage.UnitOfWorkWriter;

import com.google.inject.Inject;
import com.hazelcast.core.IMap;

@Slf4j
public class HzUnitOfWorkWriter<K> implements UnitOfWorkWriter<K>{

	private final IMap<K, AggregateRootHistory> pastTransactionsMap ;
	
	@Inject
	public HzUnitOfWorkWriter(IMap<K, AggregateRootHistory> pastTransactionsMap) {
		checkNotNull(pastTransactionsMap);
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
		pastTransactionsMap.set(id, history); // hazelcast optimization --> set instead of put since is void
	}
		
	private AggregateRootHistory getHistoryFor(final K id) {
		// TODO WARNING https://github.com/hazelcast/hazelcast/issues/1593 
		log.debug("looking for {} on map {} ", id, pastTransactionsMap.getName());
		AggregateRootHistory history = pastTransactionsMap.get(id);
		if (history==null) {
			log.debug("found NULL value for {} on map {} ", id, pastTransactionsMap.getName());
		}
		return history == null ? new AggregateRootHistory() : history;
	}

}
