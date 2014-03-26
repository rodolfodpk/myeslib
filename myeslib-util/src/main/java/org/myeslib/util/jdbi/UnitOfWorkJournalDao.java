package org.myeslib.util.jdbi;

import org.myeslib.core.data.UnitOfWork;

public interface UnitOfWorkJournalDao<K> {

	void insert(K id, UnitOfWork uow);
	
}
