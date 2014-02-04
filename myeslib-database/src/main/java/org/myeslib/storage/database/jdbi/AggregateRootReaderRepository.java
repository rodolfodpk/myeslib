package org.myeslib.storage.database.jdbi;

import org.myeslib.data.AggregateRootHistory;
import org.skife.jdbi.v2.Handle;

public interface AggregateRootReaderRepository<K> {

	AggregateRootHistory get(K id, Handle handle);
	
}
