package org.myeslib.util.jdbi;

import org.myeslib.core.data.UnitOfWork;

public interface AggregateRootHistoryWriterDao<K> {

	void insert(K id, UnitOfWork uow);
	
}
