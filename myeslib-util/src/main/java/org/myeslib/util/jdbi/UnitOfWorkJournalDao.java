package org.myeslib.util.jdbi;

import org.myeslib.core.data.UnitOfWork;

public interface UnitOfWorkJournalDao<K> {

	void append(K id, UnitOfWork uow);
	
}
