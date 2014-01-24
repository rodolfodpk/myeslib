package org.myeslib.storage;

import org.myeslib.data.UnitOfWork;

public interface EventStore<K> {

	public void store(final K id, final UnitOfWork uow) ;
	
}