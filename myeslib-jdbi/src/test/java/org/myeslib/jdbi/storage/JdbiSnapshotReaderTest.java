package org.myeslib.jdbi.storage;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.myeslib.core.Event;
import org.myeslib.core.data.AggregateRootHistory;
import org.myeslib.core.data.Snapshot;
import org.myeslib.example.SampleDomain.InventoryDecreased;
import org.myeslib.example.SampleDomain.InventoryIncreased;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.util.jdbi.AggregateRootHistoryReaderDao;

import com.google.common.base.Function;

@RunWith(MockitoJUnitRunner.class) 
public class JdbiSnapshotReaderTest {
	
	@Mock
	Map<UUID, Snapshot<InventoryItemAggregateRoot>> lastSnapshotMap ; 

	@Mock
	Function<Void, InventoryItemAggregateRoot> newInstanceFunction;
	
	@Mock
	AggregateRootHistoryReaderDao<UUID> arReader ;
	
	@Test 
	public void lastSnapshotNullNoTransactionHistory() {

		UUID id = UUID.randomUUID();
		
		InventoryItemAggregateRoot freshInstance = new InventoryItemAggregateRoot();
		
		JdbiSnapshotReader<UUID, InventoryItemAggregateRoot> st = new JdbiSnapshotReader<UUID, InventoryItemAggregateRoot>(lastSnapshotMap, arReader, newInstanceFunction);
		
		when(newInstanceFunction.apply(any(Void.class))).thenReturn(freshInstance);
		when(arReader.get(id)).thenReturn(null);
		when(lastSnapshotMap.get(id)).thenReturn(null);

		assertThat(st.get(id).getAggregateInstance(), sameInstance(freshInstance));

		verify(arReader).get(id);
		verify(lastSnapshotMap).get(id);
		
	}
	
	@Test
	public void lastSnapshotNullWithTransactionHistory() {

		UUID id = UUID.randomUUID();
		
		AggregateRootHistory transactionHistory = Mockito.mock(AggregateRootHistory.class);
	
		long originalVersion = 1;
		List<Event> events = Arrays.asList((Event)new InventoryIncreased(id, 2));
		
		when(transactionHistory.getLastVersion()).thenReturn(originalVersion);
		when(transactionHistory.getEventsUntil(originalVersion)).thenReturn(events);

		when(lastSnapshotMap.get(id)).thenReturn(null);
		
		InventoryItemAggregateRoot freshInstance = new InventoryItemAggregateRoot();

		JdbiSnapshotReader<UUID, InventoryItemAggregateRoot> st = new JdbiSnapshotReader<UUID, InventoryItemAggregateRoot>(lastSnapshotMap, arReader, newInstanceFunction);
		
		when(newInstanceFunction.apply(any(Void.class))).thenReturn(freshInstance);
		when(arReader.get(id)).thenReturn(transactionHistory);
		
		Snapshot<InventoryItemAggregateRoot> resultingSnapshot = st.get(id);
		
		verify(arReader).get(id);
		verify(lastSnapshotMap).get(id);
		verify(transactionHistory, times(2)).getLastVersion(); 
		verify(transactionHistory).getEventsUntil(originalVersion); 
		
		assertThat(resultingSnapshot.getVersion(), is(originalVersion));
		
		InventoryItemAggregateRoot fromSnapshot = resultingSnapshot.getAggregateInstance();

		assertThat(fromSnapshot.getAvailable(), is(2));

	}
	
	@Test
	public void snapshotNotUpToDateWithTransactionHistory() {

		UUID id = UUID.randomUUID();
		
		AggregateRootHistory transactionHistory = Mockito.mock(AggregateRootHistory.class);
	
		long firstVersion = 1;
		long versionNotYetOnLastSnapshot = 2;
		
		List<Event> events = Arrays.asList((Event)new InventoryDecreased(id, 2));
		
		InventoryItemAggregateRoot aggregateInstance = new InventoryItemAggregateRoot();
		aggregateInstance.setAvailable(3);
		Snapshot<InventoryItemAggregateRoot> snapshotInstance = new Snapshot<>(aggregateInstance, firstVersion);
	
		when(transactionHistory.getLastVersion()).thenReturn(versionNotYetOnLastSnapshot);
		when(lastSnapshotMap.get(id)).thenReturn(snapshotInstance);
		when(transactionHistory.getEventsAfterUntil(firstVersion, versionNotYetOnLastSnapshot)).thenReturn(events);
	
		InventoryItemAggregateRoot freshInstance = new InventoryItemAggregateRoot();

		JdbiSnapshotReader<UUID, InventoryItemAggregateRoot> st = new JdbiSnapshotReader<UUID, InventoryItemAggregateRoot>(lastSnapshotMap, arReader, newInstanceFunction);
		
		when(newInstanceFunction.apply(any(Void.class))).thenReturn(freshInstance);
		when(arReader.get(id)).thenReturn(transactionHistory);

		Snapshot<InventoryItemAggregateRoot> resultingSnapshot = st.get(id);
		
		verify(arReader).get(id);
		verify(lastSnapshotMap).get(id);
		verify(transactionHistory, times(2)).getLastVersion(); 
		verify(transactionHistory).getEventsAfterUntil(firstVersion, versionNotYetOnLastSnapshot);
		
		assertThat(resultingSnapshot.getVersion(), is(versionNotYetOnLastSnapshot));
		
		InventoryItemAggregateRoot fromSnapshot = resultingSnapshot.getAggregateInstance();

		assertThat(fromSnapshot.getAvailable(), is(1));

	}

}

