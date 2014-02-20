package org.myeslib.util.jdbi;

import org.myeslib.core.data.AggregateRootHistory;

public interface AggregateRootHistoryReaderDao<K> {

	AggregateRootHistory get(K id);
	
}
