package org.myeslib.storage.jdbi.impl;

import org.myeslib.data.UnitOfWork;
import org.skife.jdbi.v2.Handle;

public interface AggregateRootWriterRepository<K> {
	
	void insert(K id, UnitOfWork uow, Handle handle);

}
