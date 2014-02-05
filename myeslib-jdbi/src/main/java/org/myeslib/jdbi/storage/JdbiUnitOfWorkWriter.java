package org.myeslib.jdbi.storage;

import java.util.ConcurrentModificationException;

import lombok.AllArgsConstructor;

import org.myeslib.core.data.AggregateRootHistory;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.core.storage.UnitOfWorkWriter;
import org.myeslib.jdbi.AggregateRootHistoryReader;
import org.myeslib.jdbi.AggregateRootHistoryWriter;
import org.skife.jdbi.v2.Handle;

@AllArgsConstructor
public class JdbiUnitOfWorkWriter<K> implements UnitOfWorkWriter<K>{

	private final Handle handle;
	private final AggregateRootHistoryReader<K> arReader ;
	private final AggregateRootHistoryWriter<K> arWriter ;
	
	/*
	 * (non-Javadoc)
	 * @see org.myeslib.core.storage.UnitOfWorkWriter#insert(java.lang.Object, org.myeslib.core.data.UnitOfWork)
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
