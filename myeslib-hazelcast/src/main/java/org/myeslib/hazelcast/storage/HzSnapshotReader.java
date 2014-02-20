package org.myeslib.hazelcast.storage;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.myeslib.util.EventSourcingMagicHelper.applyEventsOn;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.myeslib.core.AggregateRoot;
import org.myeslib.core.Event;
import org.myeslib.core.data.AggregateRootHistory;
import org.myeslib.core.data.Snapshot;
import org.myeslib.core.storage.SnapshotReader;

import com.google.common.base.Function;

public class HzSnapshotReader<K, A extends AggregateRoot> implements SnapshotReader<K, A> {

	private final Map<K, AggregateRootHistory> eventsMap ;
	private final Map<K, Snapshot<A>> lastSnapshotMap ; 
	private final Function<Void, A> newInstanceFactory ;
    
	@Inject
	public HzSnapshotReader(Map<K, AggregateRootHistory> eventsMap,
			Map<K, Snapshot<A>> lastSnapshotMap,
			Function<Void, A> newInstanceFactory) {
		checkNotNull(eventsMap);
		checkNotNull(lastSnapshotMap);
		this.eventsMap = eventsMap;
		this.lastSnapshotMap = lastSnapshotMap;
		this.newInstanceFactory = newInstanceFactory;
	}

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
		final AggregateRootHistory arh = eventsMap.get(id);
		return arh == null ? new AggregateRootHistory() : arh;
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
