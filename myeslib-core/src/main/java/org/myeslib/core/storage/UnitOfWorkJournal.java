package org.myeslib.core.storage;

import org.myeslib.core.data.UnitOfWork;

public interface UnitOfWorkJournal<K> {

	public void append(final K id, final UnitOfWork uow) ;
	
}