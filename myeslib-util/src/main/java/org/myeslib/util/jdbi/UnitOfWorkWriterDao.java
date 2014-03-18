package org.myeslib.util.jdbi;

import org.myeslib.core.data.UnitOfWork;

public interface UnitOfWorkWriterDao<K> {

	void insert(K id, UnitOfWork uow);
	
}
