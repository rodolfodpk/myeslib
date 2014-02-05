package org.myeslib.jdbi;

import org.myeslib.core.data.UnitOfWork;
import org.skife.jdbi.v2.Handle;

public interface AggregateRootHistoryWriter<K> {
	
	void insert(K id, UnitOfWork uow, Handle handle);

}
