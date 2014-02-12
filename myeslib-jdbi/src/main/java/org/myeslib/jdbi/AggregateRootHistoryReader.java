package org.myeslib.jdbi;

import org.myeslib.core.data.AggregateRootHistory;

public interface AggregateRootHistoryReader<K> {

	AggregateRootHistory get(K id);
	
}
