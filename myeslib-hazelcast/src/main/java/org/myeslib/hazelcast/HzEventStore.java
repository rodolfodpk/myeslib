package org.myeslib.hazelcast;

import java.util.ConcurrentModificationException;

import lombok.AllArgsConstructor;

import org.myeslib.data.AggregateRootHistory;
import org.myeslib.data.UnitOfWork;
import org.myeslib.storage.EventStore;

import com.google.common.base.Function;
import com.hazelcast.core.TransactionalMap;

@AllArgsConstructor
public class HzEventStore<K> implements EventStore<K>{

	private final TransactionalMap<K, String> pastTransactionsMap ;
	private final Function<AggregateRootHistory, String> toStringFunction ;
	private final Function<String, AggregateRootHistory> fromStringFunction ;
	
	public void store(final K id, final UnitOfWork uow) {
		final AggregateRootHistory history = getHistoryFor(id);
		if (history.getLastVersion() != uow.getBaseVersion()){
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
		String asString = pastTransactionsMap.get(id);
		AggregateRootHistory history = fromStringFunction.apply(asString);
		return history == null ? new AggregateRootHistory() : history;
	}

}
