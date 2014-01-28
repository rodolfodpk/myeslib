package org.myeslib.hazelcast;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.myeslib.core.Event;
import org.myeslib.data.AggregateRootHistory;
import org.myeslib.data.Snapshot;
import org.myeslib.example.SampleDomain.InventoryDecreased;
import org.myeslib.example.SampleDomain.InventoryIncreased;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleDomainGsonFactory;
import org.myeslib.gson.FromStringFunction;

import com.google.common.base.Function;
import com.google.gson.Gson;

@RunWith(MockitoJUnitRunner.class) 
public class HzSnapshotReaderTest {
	
	final Gson gson = new SampleDomainGsonFactory().create();
	
	@SuppressWarnings("unchecked")
	@Test 
	public void lastSnapshotNullNoTransactionHistory() {

		Map<Long, String> eventsMap = Mockito.mock(Map.class);
		Map<Long, Snapshot<InventoryItemAggregateRoot>> lastSnapshotMap = Mockito.mock(Map.class);
		Long id = 1l;
		
		InventoryItemAggregateRoot freshInstance = new InventoryItemAggregateRoot();
		
		HzSnapshotReader<Long, InventoryItemAggregateRoot> st = new HzSnapshotReader<Long, InventoryItemAggregateRoot>(eventsMap, lastSnapshotMap, new FromStringFunction(gson));
		
		when(eventsMap.get(id)).thenReturn(null);
		when(lastSnapshotMap.get(id)).thenReturn(null);

		assertThat(st.get(id, freshInstance).getAggregateInstance(), sameInstance(freshInstance));

		verify(eventsMap).get(id);
		verify(lastSnapshotMap).get(id);
		
	}
	
	@SuppressWarnings({"unchecked" })
	@Test
	public void lastSnapshotNullWithTransactionHistory() {

		UUID id = UUID.randomUUID();
		
		Map<UUID, String> eventsMap = Mockito.mock(Map.class);
		Map<UUID, Snapshot<InventoryItemAggregateRoot>> lastSnapshotMap = Mockito.mock(Map.class);
		Function<String, AggregateRootHistory> fromStringFunction = Mockito.mock(Function.class);
		AggregateRootHistory transactionHistory = Mockito.mock(AggregateRootHistory.class);
	
		long originalVersion = 1;
		
		List<Event> events = Arrays.asList((Event)new InventoryIncreased(id, 2));
		
		when(eventsMap.get(id)).thenReturn("");
		when(fromStringFunction.apply("")).thenReturn(transactionHistory);
		when(transactionHistory.getLastVersion()).thenReturn(originalVersion);
		when(lastSnapshotMap.get(id)).thenReturn(null);
		when(transactionHistory.getEventsUntil(originalVersion)).thenReturn(events);
		
		InventoryItemAggregateRoot freshInstance = new InventoryItemAggregateRoot();

		HzSnapshotReader<UUID, InventoryItemAggregateRoot> st = new HzSnapshotReader<>(eventsMap, lastSnapshotMap, fromStringFunction);

		Snapshot<InventoryItemAggregateRoot> resultingSnapshot = st.get(id, freshInstance);
		
		verify(eventsMap).get(id);
		verify(fromStringFunction).apply("");
		verify(lastSnapshotMap).get(id);
		verify(transactionHistory, times(2)).getLastVersion(); 
		verify(transactionHistory).getEventsUntil(originalVersion); 
		
		assertThat(resultingSnapshot.getVersion(), is(originalVersion));
		
		InventoryItemAggregateRoot fromSnapshot = resultingSnapshot.getAggregateInstance();

		assertThat(fromSnapshot.getAvaliable(), is(2));

	}
	
	@SuppressWarnings({"unchecked" })
	@Test
	public void snapshotNotUpToDateWithTransactionHistory() {

		UUID id = UUID.randomUUID();
		
		Map<UUID, String> eventsMap = Mockito.mock(Map.class);
		Map<UUID, Snapshot<InventoryItemAggregateRoot>> lastSnapshotMap = Mockito.mock(Map.class);
		Function<String, AggregateRootHistory> fromStringFunction = Mockito.mock(Function.class);
		AggregateRootHistory transactionHistory = Mockito.mock(AggregateRootHistory.class);
	
		long firstVersion = 1;
		long versionNotYetOnLastSnapshot = 2;
		
		List<Event> events = Arrays.asList((Event)new InventoryDecreased(id, 2));
		
		InventoryItemAggregateRoot aggregateInstance = new InventoryItemAggregateRoot();
		aggregateInstance.setAvaliable(3);
		Snapshot<InventoryItemAggregateRoot> snapshotInstance = new Snapshot<>(aggregateInstance, firstVersion);
	
		when(eventsMap.get(id)).thenReturn("");
		when(fromStringFunction.apply("")).thenReturn(transactionHistory);
		when(transactionHistory.getLastVersion()).thenReturn(versionNotYetOnLastSnapshot);
		when(lastSnapshotMap.get(id)).thenReturn(snapshotInstance);
		when(transactionHistory.getEventsAfterUntil(firstVersion, versionNotYetOnLastSnapshot)).thenReturn(events);
	
		InventoryItemAggregateRoot freshInstance = new InventoryItemAggregateRoot();

		HzSnapshotReader<UUID, InventoryItemAggregateRoot> st = new HzSnapshotReader<>(eventsMap, lastSnapshotMap, fromStringFunction);

		Snapshot<InventoryItemAggregateRoot> resultingSnapshot = st.get(id, freshInstance);
		
		verify(eventsMap).get(id);
		verify(fromStringFunction).apply("");
		verify(lastSnapshotMap).get(id);
		verify(transactionHistory, times(2)).getLastVersion(); 
		verify(transactionHistory).getEventsAfterUntil(firstVersion, versionNotYetOnLastSnapshot);
		
		assertThat(resultingSnapshot.getVersion(), is(versionNotYetOnLastSnapshot));
		
		InventoryItemAggregateRoot fromSnapshot = resultingSnapshot.getAggregateInstance();

		assertThat(fromSnapshot.getAvaliable(), is(1));

	}

}

