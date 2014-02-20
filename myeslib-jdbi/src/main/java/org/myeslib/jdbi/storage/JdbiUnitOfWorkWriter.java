package org.myeslib.jdbi.storage;

import org.myeslib.core.data.UnitOfWork;
import org.myeslib.core.storage.UnitOfWorkWriter;
import org.myeslib.util.jdbi.AggregateRootHistoryWriterDao;

import com.google.inject.Inject;

public class JdbiUnitOfWorkWriter<K> implements UnitOfWorkWriter<K>{
	
	@Inject
	public JdbiUnitOfWorkWriter(AggregateRootHistoryWriterDao<K> dao) {
		this.dao = dao;
	}

	private final AggregateRootHistoryWriterDao<K> dao;
	
	/*
	 * (non-Javadoc)
	 * @see org.myeslib.core.storage.UnitOfWorkWriter#insert(java.lang.Object, org.myeslib.core.data.UnitOfWork)
	 */
	public void insert(final K id, final UnitOfWork uow) {
		
		dao.insert(id, uow);
		
	}
		
}
