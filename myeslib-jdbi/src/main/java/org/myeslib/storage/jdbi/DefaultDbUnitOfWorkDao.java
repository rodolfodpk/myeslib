package org.myeslib.storage.jdbi;

import java.util.ConcurrentModificationException;

import lombok.AllArgsConstructor;

import org.myeslib.data.AggregateRootHistory;
import org.myeslib.data.UnitOfWork;
import org.myeslib.storage.UnitOfWorkRepository;
import org.myeslib.storage.jdbi.impl.AggregateRootReaderRepository;
import org.myeslib.storage.jdbi.impl.AggregateRootWriterRepository;
import org.skife.jdbi.v2.Handle;

@AllArgsConstructor
public class DefaultDbUnitOfWorkDao<K> implements UnitOfWorkRepository<K>{

	private final Handle handle;
	private final AggregateRootReaderRepository<K> arReader ;
	private final AggregateRootWriterRepository<K> arWriter ;
	
	/*
	 * (non-Javadoc)
	 * @see org.myeslib.storage.UnitOfWorkDao#insert(java.lang.Object, org.myeslib.data.UnitOfWork)
	 */
	public void insert(final K id, final UnitOfWork uow) {
		final AggregateRootHistory history = getHistoryFor(id);
		if (history.getLastVersion() != uow.getBaseVersion()){
			throw new ConcurrentModificationException(String.format("base version ( %s ) does not match the last version ( %s )", 
																	uow.getBaseVersion().toString(),
																	history.getLastVersion().toString()												
																   )
													 );
														
		} 
		arWriter.insert(id, uow, handle);
	}
		
	private AggregateRootHistory getHistoryFor(final K id) {
		AggregateRootHistory history = arReader.get(id, handle);
		return history == null ? new AggregateRootHistory() : history;
	}

}
