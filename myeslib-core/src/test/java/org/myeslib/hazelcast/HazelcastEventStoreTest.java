package org.myeslib.hazelcast;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.myeslib.core.Command;
import org.myeslib.core.Event;
import org.myeslib.data.AggregateRootHistory;
import org.myeslib.data.UnitOfWork;
import org.myeslib.example.SampleCoreDomain.IncreaseInventory;
import org.myeslib.example.SampleCoreDomain.InventoryIncreased;
import org.myeslib.hazelcast.HazelcastEventStore;

import com.hazelcast.core.TransactionalMap;

@RunWith(MockitoJUnitRunner.class) 
public class HazelcastEventStoreTest {
	
	@Mock
	TransactionalMap<UUID, AggregateRootHistory> mapWithUuidKey;
	
	@Mock
	TransactionalMap<Long, AggregateRootHistory> mapWithLongKey;
	
	@Test
	public void oneTransaction() {
		
		UUID id = UUID.randomUUID();
		Command command = new IncreaseInventory(id, 1);
		Event event1 = new InventoryIncreased(id, 1);
		List<Event> events = Arrays.asList(event1);
		UnitOfWork t = UnitOfWork.create(command, 0l, events);
		AggregateRootHistory toStore = new AggregateRootHistory();
		toStore.add(t);
		
		// first get on map will returns null, second will returns toStore 
		when(mapWithUuidKey.get(id)).thenReturn(null, toStore);
		
		HazelcastEventStore<UUID> store = new HazelcastEventStore<>(mapWithUuidKey);
		store.store(id, t);

		ArgumentCaptor<UUID> argumentKey = ArgumentCaptor.forClass(UUID.class);
		ArgumentCaptor<AggregateRootHistory> argumentValue = ArgumentCaptor.forClass(AggregateRootHistory.class);

		verify(mapWithUuidKey, times(1)).set(argumentKey.capture(), argumentValue.capture());
		
		// checks for key
		assertThat(argumentKey.getValue(), sameInstance(id));

		// checks for all transactions fields except the timestamp
		UnitOfWork copy = argumentValue.getValue().getUnitsOfWork().get(0);
		assertThat(copy.getCommand(), sameInstance(command));
		assertThat(copy.getVersion(), sameInstance(1l));
		assertThat("contains events", copy.getEvents().containsAll(events));

	}

	@Test
	public void withoutConcurrencyException() {
		
		UUID id = UUID.randomUUID();
		Command command = new IncreaseInventory(id, 1);
		Event event1 = new InventoryIncreased(id, 1);
		List<Event> events = Arrays.asList(event1);
		UnitOfWork t = UnitOfWork.create(command, 0l, events);
		AggregateRootHistory toStore = new AggregateRootHistory();
		toStore.add(t);
		
		when(mapWithUuidKey.get(id)).thenReturn(toStore);
		
		HazelcastEventStore<UUID> store = new HazelcastEventStore<>(mapWithUuidKey);
		UnitOfWork t2 = UnitOfWork.create(command, 1l, events);
		store.store(id, t2);

		ArgumentCaptor<UUID> argumentKey = ArgumentCaptor.forClass(UUID.class);
		ArgumentCaptor<AggregateRootHistory> argumentValue = ArgumentCaptor.forClass(AggregateRootHistory.class);

		verify(mapWithUuidKey, times(1)).set(argumentKey.capture(), argumentValue.capture());
		
		// checks for key
		assertThat(argumentKey.getValue(), sameInstance(id));

		// should have 2 transactions
		assertThat(argumentValue.getValue().getUnitsOfWork().size(), is(2));

		// checks for all transactions fields except the timestamp
		UnitOfWork copy = argumentValue.getValue().getUnitsOfWork().get(0);
		assertThat(copy.getCommand(), sameInstance(command));
		assertThat(copy.getVersion(), is(1l));
		assertThat("contains events", copy.getEvents().containsAll(events));

		// checks for all transactions fields except the timestamp
		UnitOfWork t2Candidate = argumentValue.getValue().getUnitsOfWork().get(1);
		assertThat(t2Candidate.getCommand(), sameInstance(command));
		assertThat(t2Candidate.getVersion(), is(2l));
		assertThat("contains events", t2Candidate.getEvents().containsAll(events));

	}
	
	@Test(expected=ConcurrentModificationException.class)
	public void withConcurrencyException() {
		
		UUID id = UUID.randomUUID();
		Command command = new IncreaseInventory(id, 1);
		Event event1 = new InventoryIncreased(id, 1);
		List<Event> events = Arrays.asList(event1);
		UnitOfWork t = UnitOfWork.create(command, 0l, events);
		AggregateRootHistory toStore = new AggregateRootHistory();
		toStore.add(t);
		
		when(mapWithUuidKey.get(id)).thenReturn(toStore);

		HazelcastEventStore<UUID> store = new HazelcastEventStore<>(mapWithUuidKey);
		UnitOfWork t2 = UnitOfWork.create(command, 0l, events);
		store.store(id, t2);

	}
	
	@SuppressWarnings("serial")
	@Test
	public void oneTransactionWithLongKey() {
		
		Long id = new Long(1);
		Command command = new Command() {};
		Event event1 = new Event() {};
		List<Event> events = Arrays.asList(event1);
		UnitOfWork t = UnitOfWork.create(command, 0l, events);
		AggregateRootHistory toStore = new AggregateRootHistory();
		toStore.add(t);
		
		// first get on map will returns null, second will returns toStore 
		when(mapWithLongKey.get(id)).thenReturn(null, toStore);
		
		HazelcastEventStore<Long> store = new HazelcastEventStore<>(mapWithLongKey);
		store.store(id, t);

		ArgumentCaptor<Long> argumentKey = ArgumentCaptor.forClass(Long.class);
		ArgumentCaptor<AggregateRootHistory> argumentValue = ArgumentCaptor.forClass(AggregateRootHistory.class);

		verify(mapWithLongKey, times(1)).set(argumentKey.capture(), argumentValue.capture());
		
		// checks for key
		assertThat(argumentKey.getValue(), is(id));

		// checks for all transactions fields except the timestamp
		UnitOfWork copy = argumentValue.getValue().getUnitsOfWork().get(0);
		assertThat(copy.getCommand(), sameInstance(command));
		assertThat(copy.getVersion(), is(1l));
		assertThat("contains events", copy.getEvents().containsAll(events));

	}
	
	@Test
	public void testUUID() {
		
		UUID id = UUID.randomUUID();
		
		System.out.println(id.toString().length());
		
		UUID id2 = UUID.fromString(id.toString());
		
		assertThat(id, equalTo(id2));
		
	}
	
}


