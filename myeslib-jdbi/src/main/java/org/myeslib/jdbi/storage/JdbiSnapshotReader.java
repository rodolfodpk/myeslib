package org.myeslib.jdbi.storage;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.myeslib.util.EventSourcingMagicHelper.applyEventsOn;

import java.util.List;
import java.util.Map;

import org.myeslib.core.AggregateRoot;
import org.myeslib.core.Event;
import org.myeslib.core.data.AggregateRootHistory;
import org.myeslib.core.data.Snapshot;
import org.myeslib.core.storage.SnapshotReader;
import org.myeslib.util.jdbi.AggregateRootHistoryReaderDao;

import com.google.common.base.Function;
import com.google.inject.Inject;

public class JdbiSnapshotReader<K, A extends AggregateRoot> implements SnapshotReader<K, A> {
    
	@Inject
	public JdbiSnapshotReader(
			Map<K, Snapshot<A>> lastSnapshotMap,
			AggregateRootHistoryReaderDao<K> arReader,
			Function<Void, A> newInstanceFactory) {
		checkNotNull(lastSnapshotMap);
		checkNotNull(arReader);
		checkNotNull(newInstanceFactory);
		this.lastSnapshotMap = lastSnapshotMap;
		this.arReader = arReader;
		this.newInstanceFactory = newInstanceFactory;
	}

	private final Map<K, Snapshot<A>> lastSnapshotMap ; 
	private final AggregateRootHistoryReaderDao<K> arReader ;
	private final Function<Void, A> newInstanceFactory ;
	
	/*
	 * (non-Javadoc)
	 * @see org.myeslib.core.storage.SnapshotReader#get(java.lang.Object)
	 */
	public Snapshot<A> get(final K id) {
		checkNotNull(id);
		final A aggregateRootFreshInstance = newInstanceFactory.apply(null);
		final AggregateRootHistory transactionHistory = getEventsOrEmptyIfNull(id);
		final Long lastVersion = transactionHistory.getLastVersion();
		final Snapshot<A> lastSnapshot = lastSnapshotMap.get(id);
		final Snapshot<A> resultingSnapshot;
		if (lastSnapshot==null){
			resultingSnapshot = applyAllEventsOnFreshInstance(transactionHistory, aggregateRootFreshInstance);
		} else {
			if (lastSnapshot.getVersion() < lastVersion) {
				resultingSnapshot = applyEventsSinceLastSnapshot(transactionHistory, lastSnapshot);
			} else {
				resultingSnapshot = lastSnapshot;
			}
		}
		return resultingSnapshot;
	}

	private AggregateRootHistory getEventsOrEmptyIfNull(final K id) {
		final AggregateRootHistory events = arReader.get(id);
		return events == null ? new AggregateRootHistory() : events;
	}
	
	private Snapshot<A> applyAllEventsOnFreshInstance(final AggregateRootHistory transactionHistory, 
													  final A aggregateRootFreshInstance) {
		final Long lastVersion = transactionHistory.getLastVersion();
		final List<? extends Event> eventsToApply = transactionHistory.getEventsUntil(lastVersion);
		applyEventsOn(eventsToApply, aggregateRootFreshInstance);
		return new Snapshot<A>(aggregateRootFreshInstance, lastVersion);
	}
	
	private Snapshot<A> applyEventsSinceLastSnapshot(final AggregateRootHistory transactionHistory,
													 final Snapshot<A> lastSnapshot) {
		final Long lastVersion = transactionHistory.getLastVersion();
		final List<? extends Event> eventsToApply = transactionHistory.getEventsAfterUntil(lastSnapshot.getVersion(), lastVersion);
		applyEventsOn(eventsToApply, lastSnapshot.getAggregateInstance());
		return new Snapshot<A>(lastSnapshot.getAggregateInstance(), lastVersion);
	}

}
