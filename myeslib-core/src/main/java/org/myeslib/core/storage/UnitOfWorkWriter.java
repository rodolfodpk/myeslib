package org.myeslib.core.storage;

import org.myeslib.core.data.UnitOfWork;

public interface UnitOfWorkWriter<K> {

	public void insert(final K id, final UnitOfWork uow) ;
	
}