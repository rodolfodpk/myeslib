package org.myeslib.hazelcast;

import lombok.AllArgsConstructor;

import org.myeslib.core.AggregateRoot;
import org.myeslib.data.AggregateRootHistory;

import com.hazelcast.core.TransactionalMap;
import com.hazelcast.transaction.TransactionContext;

@AllArgsConstructor
public class AggregateRootHistoryTxMapFactory<K, A extends AggregateRoot> {
	
	public TransactionalMap<K, AggregateRootHistory> get(final TransactionContext context, final String mapId) {
		TransactionalMap<K, AggregateRootHistory> aggregateRootHistoryMap = context.getMap(mapId);
		return aggregateRootHistoryMap;
	}

}
