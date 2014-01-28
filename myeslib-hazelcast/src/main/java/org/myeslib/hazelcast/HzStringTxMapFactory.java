package org.myeslib.hazelcast;

import lombok.AllArgsConstructor;

import com.hazelcast.core.TransactionalMap;
import com.hazelcast.transaction.TransactionContext;

@AllArgsConstructor
public class HzStringTxMapFactory<K> {
	
	public TransactionalMap<K, String> get(final TransactionContext context, final String mapId) {
		TransactionalMap<K, String> aggregateRootHistoryMap = context.getMap(mapId);
		return aggregateRootHistoryMap;
	}

}
