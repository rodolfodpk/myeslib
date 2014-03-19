package org.myeslib.hazelcast.storage;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.myeslib.core.Event;
import org.myeslib.core.data.AggregateRootHistory;
import org.myeslib.core.data.Snapshot;
import org.myeslib.example.SampleDomain.InventoryDecreased;
import org.myeslib.example.SampleDomain.InventoryIncreased;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleDomain.InventoryItemCreated;
import org.myeslib.example.SampleDomainGsonFactory;

import com.google.common.base.Function;
import com.google.gson.Gson;

@RunWith(MockitoJUnitRunner.class) 
public class HzSnapshotReaderTest {
	
	final Gson gson = new SampleDomainGsonFactory().create();
	
	@SuppressWarnings("unchecked")
	@Test 
	public void lastSnapshotNullNoTransactionHistory() {

		Map<UUID, AggregateRootHistory> eventsMap = Mockito.mock(Map.class);
		Map<UUID, Snapshot<InventoryItemAggregateRoot>> lastSnapshotMap = Mockito.mock(Map.class);
		Function<Void, InventoryItemAggregateRoot> inventoryItemInstanceFactory = Mockito.mock(Function.class);
		
		UUID id = UUID.randomUUID();
		
		InventoryItemAggregateRoot freshInstance = new InventoryItemAggregateRoot();
		
		HzSnapshotReader<UUID, InventoryItemAggregateRoot> st = new HzSnapshotReader<UUID, InventoryItemAggregateRoot>(eventsMap, lastSnapshotMap, inventoryItemInstanceFactory);
		
		when(inventoryItemInstanceFactory.apply(any(Void.class))).thenReturn(freshInstance);
		when(eventsMap.get(id)).thenReturn(null);
		when(lastSnapshotMap.get(id)).thenReturn(null);

		assertThat(st.get(id).getAggregateInstance(), sameInstance(freshInstance));

		verify(inventoryItemInstanceFactory).apply(any(Void.class));
		verify(eventsMap).get(id);
		verify(lastSnapshotMap).get(id);
		
	}
	
	@SuppressWarnings({"unchecked" })
	@Test
	public void lastSnapshotNullWithTransactionHistory() {

		UUID id = UUID.randomUUID();
		
		Map<UUID, AggregateRootHistory> eventsMap = Mockito.mock(Map.class);
		Map<UUID, Snapshot<InventoryItemAggregateRoot>> lastSnapshotMap = Mockito.mock(Map.class);
		Function<Void, InventoryItemAggregateRoot> inventoryItemInstanceFactory = Mockito.mock(Function.class);
		AggregateRootHistory transactionHistoryWithOneUnitOfWork = Mockito.mock(AggregateRootHistory.class);

		InventoryItemAggregateRoot freshInstance = new InventoryItemAggregateRoot();
		long originalVersion = 1;
		
		List<Event> events = new ArrayList<>();
		events.add((Event)new InventoryItemCreated(id, "desc"));
		events.add((Event)new InventoryIncreased(id, 4));
		events.add((Event)new InventoryDecreased(id, 2));
	
		when(eventsMap.get(id)).thenReturn(transactionHistoryWithOneUnitOfWork);
		when(transactionHistoryWithOneUnitOfWork.getLastVersion()).thenReturn(originalVersion);
		when(transactionHistoryWithOneUnitOfWork.getEventsUntil(originalVersion)).thenReturn(events);
		when(lastSnapshotMap.get(id)).thenReturn(null);
		when(inventoryItemInstanceFactory.apply(any(Void.class))).thenReturn(freshInstance);
		
		HzSnapshotReader<UUID, InventoryItemAggregateRoot> st = new HzSnapshotReader<UUID, InventoryItemAggregateRoot>(eventsMap, lastSnapshotMap, inventoryItemInstanceFactory);

		Snapshot<InventoryItemAggregateRoot> resultingSnapshot = st.get(id);
		
		verify(eventsMap).get(id);
		verify(lastSnapshotMap).get(id);
		verify(transactionHistoryWithOneUnitOfWork, times(2)).getLastVersion(); 
		verify(transactionHistoryWithOneUnitOfWork).getEventsUntil(originalVersion);
		verify(inventoryItemInstanceFactory).apply(any(Void.class));
		verify(transactionHistoryWithOneUnitOfWork).getEventsUntil(1); 
		
		assertThat(resultingSnapshot.getVersion(), is(originalVersion));
		
		assertThat(resultingSnapshot.getAggregateInstance().getAvailable(), is(2));

	}
	
	@SuppressWarnings({"unchecked" })
	@Test
	public void snapshotNotUpToDateWithTransactionHistory() {

		UUID id = UUID.randomUUID();
		
		Map<UUID, AggregateRootHistory> eventsMap = Mockito.mock(Map.class);
		Map<UUID, Snapshot<InventoryItemAggregateRoot>> lastSnapshotMap = Mockito.mock(Map.class);
		Function<Void, InventoryItemAggregateRoot> inventoryItemInstanceFactory = Mockito.mock(Function.class);
		AggregateRootHistory transactionHistoryWithOneUnitOfWork = Mockito.mock(AggregateRootHistory.class);
	
		long firstVersion = 1;
		long versionNotYetOnLastSnapshot = 2;
		
		List<Event> events = new ArrayList<>();
		events.add((Event)new InventoryItemCreated(id, "desc"));
		events.add((Event)new InventoryIncreased(id, 4));
		events.add((Event)new InventoryDecreased(id, 2));
		
		InventoryItemAggregateRoot aggregateInstance = new InventoryItemAggregateRoot();
		aggregateInstance.setAvailable(4);
		Snapshot<InventoryItemAggregateRoot> snapshotInstance = new Snapshot<>(aggregateInstance, firstVersion);

		InventoryItemAggregateRoot freshInstance = new InventoryItemAggregateRoot();

		when(eventsMap.get(id)).thenReturn(transactionHistoryWithOneUnitOfWork);
		when(transactionHistoryWithOneUnitOfWork.getLastVersion()).thenReturn(versionNotYetOnLastSnapshot);
		when(lastSnapshotMap.get(id)).thenReturn(snapshotInstance);
		when(transactionHistoryWithOneUnitOfWork.getEventsAfterUntil(firstVersion, versionNotYetOnLastSnapshot)).thenReturn(events);
	
		HzSnapshotReader<UUID, InventoryItemAggregateRoot> st = new HzSnapshotReader<UUID, InventoryItemAggregateRoot>(eventsMap, lastSnapshotMap, inventoryItemInstanceFactory);
		
		Snapshot<InventoryItemAggregateRoot> resultingSnapshot = st.get(id);
		
		verify(eventsMap).get(id);
		verify(lastSnapshotMap).get(id);
		verify(transactionHistoryWithOneUnitOfWork, times(2)).getLastVersion(); 
		when(inventoryItemInstanceFactory.apply(any(Void.class))).thenReturn(freshInstance);
		verify(inventoryItemInstanceFactory).apply(any(Void.class));
		verify(transactionHistoryWithOneUnitOfWork).getEventsAfterUntil(firstVersion, versionNotYetOnLastSnapshot);
		
		assertThat(resultingSnapshot.getVersion(), is(versionNotYetOnLastSnapshot));
		
		InventoryItemAggregateRoot fromSnapshot = resultingSnapshot.getAggregateInstance();

		assertThat(fromSnapshot.getAvailable(), is(2));

	}

}

