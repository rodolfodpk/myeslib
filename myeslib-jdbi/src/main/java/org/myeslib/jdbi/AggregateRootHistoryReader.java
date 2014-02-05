package org.myeslib.jdbi;

import org.myeslib.core.data.AggregateRootHistory;
import org.skife.jdbi.v2.Handle;

public interface AggregateRootHistoryReader<K> {

	AggregateRootHistory get(K id, Handle handle);
	
}
