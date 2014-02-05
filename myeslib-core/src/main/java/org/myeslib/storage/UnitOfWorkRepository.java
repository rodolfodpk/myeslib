package org.myeslib.storage;

import org.myeslib.data.UnitOfWork;

public interface UnitOfWorkRepository<K> {

	public void insert(final K id, final UnitOfWork uow) ;
	
}