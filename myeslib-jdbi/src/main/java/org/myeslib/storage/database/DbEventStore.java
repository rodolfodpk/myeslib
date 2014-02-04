package org.myeslib.storage.database;

import java.util.ConcurrentModificationException;

import lombok.AllArgsConstructor;

import org.myeslib.data.AggregateRootHistory;
import org.myeslib.data.UnitOfWork;
import org.myeslib.storage.EventStore;
import org.myeslib.storage.database.jdbi.AggregateRootReaderRepository;
import org.myeslib.storage.database.jdbi.AggregateRootWriterRepository;
import org.skife.jdbi.v2.Handle;

@AllArgsConstructor
public class DbEventStore<K> implements EventStore<K>{

	private final Handle handle;
	private final AggregateRootReaderRepository<K> arReader ;
	private final AggregateRootWriterRepository<K> arWriter ;
	
	/*
	 * (non-Javadoc)
	 * @see org.myeslib.storage.EventStore#store(java.lang.Object, org.myeslib.data.UnitOfWork)
	 */
	public void store(final K id, final UnitOfWork uow) {
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
