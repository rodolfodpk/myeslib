package org.myeslib.jdbi.storage;

import org.myeslib.core.data.UnitOfWork;
import org.myeslib.core.storage.UnitOfWorkJournal;
import org.myeslib.util.jdbi.UnitOfWorkJournalDao;

import com.google.inject.Inject;

public class JdbiUnitOfWorkJournal<K> implements UnitOfWorkJournal<K> {
	
	@Inject
	public JdbiUnitOfWorkJournal(UnitOfWorkJournalDao<K> dao) {
		this.dao = dao;
	}

	private final UnitOfWorkJournalDao<K> dao;
	
	/*
	 * (non-Javadoc)
	 * @see org.myeslib.core.storage.UnitOfWorkJournal#append(java.lang.Object, org.myeslib.core.data.UnitOfWork)
	 */
	public void append(final K id, final UnitOfWork uow) {
		
		dao.append(id, uow);
		
	}
		
}
