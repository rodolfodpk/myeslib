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
import org.myeslib.example.SampleDomain.IncreaseInventory;
import org.myeslib.example.SampleDomain.InventoryIncreased;
import org.myeslib.example.SampleDomainGsonFactory;
import org.myeslib.gson.FromStringFunction;
import org.myeslib.gson.ToStringFunction;

import com.google.gson.Gson;
import com.hazelcast.core.TransactionalMap;

@RunWith(MockitoJUnitRunner.class) 
public class HzEventStoreTest {
	
	@Mock
	TransactionalMap<UUID, String> mapWithUuidKey;
	
	@Mock
	TransactionalMap<Long, String> mapWithLongKey;
	
	final Gson gson = new SampleDomainGsonFactory().create();
	
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
		when(mapWithUuidKey.get(id)).thenReturn(null, gson.toJson(toStore));
		
		HzEventStore<UUID> store = new HzEventStore<>(mapWithUuidKey, new ToStringFunction(gson), new FromStringFunction(gson));
		store.store(id, t);

		ArgumentCaptor<UUID> argumentKey = ArgumentCaptor.forClass(UUID.class);
		ArgumentCaptor<String> argumentValue = ArgumentCaptor.forClass(String.class);

		verify(mapWithUuidKey, times(1)).set(argumentKey.capture(), argumentValue.capture());
		
		// checks for key
		assertThat(argumentKey.getValue(), sameInstance(id));

		// checks for all transactions fields except the timestamp
		String asString = argumentValue.getValue();
		AggregateRootHistory asObj = gson.fromJson(asString, AggregateRootHistory.class);
		UnitOfWork copy = asObj.getUnitsOfWork().get(0);
		assertThat(copy.getCommand(), is(command));
		assertThat(copy.getVersion(), is(1l));
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
		
		when(mapWithUuidKey.get(id)).thenReturn(gson.toJson(toStore));
		
		HzEventStore<UUID> store = new HzEventStore<>(mapWithUuidKey, new ToStringFunction(gson), new FromStringFunction(gson));
		UnitOfWork t2 = UnitOfWork.create(command, 1l, events);
		store.store(id, t2);

		ArgumentCaptor<UUID> argumentKey = ArgumentCaptor.forClass(UUID.class);
		ArgumentCaptor<String> argumentValue = ArgumentCaptor.forClass(String.class);

		verify(mapWithUuidKey, times(1)).set(argumentKey.capture(), argumentValue.capture());
		
		// checks for key
		assertThat(argumentKey.getValue(), sameInstance(id));

		String asString = argumentValue.getValue();
		AggregateRootHistory asObj = gson.fromJson(asString, AggregateRootHistory.class);

		// should have 2 transactions
		assertThat(asObj.getUnitsOfWork().size(), is(2));

		// checks for all transactions fields except the timestamp
		UnitOfWork copy = asObj.getUnitsOfWork().get(0);
		assertThat(copy.getCommand(), is(command));
		assertThat(copy.getVersion(), is(1l));
		assertThat("contains events", copy.getEvents().containsAll(events));

		// checks for all transactions fields except the timestamp
		UnitOfWork t2Candidate = asObj.getUnitsOfWork().get(1);
		assertThat(t2Candidate.getCommand(), is(command));
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
		
		when(mapWithUuidKey.get(id)).thenReturn(gson.toJson(toStore));

		HzEventStore<UUID> store = new HzEventStore<>(mapWithUuidKey, new ToStringFunction(gson), new FromStringFunction(gson));
		UnitOfWork t2 = UnitOfWork.create(command, 0l, events);
		store.store(id, t2);

	}
	
	@Test
	public void testUUID() {
		
		UUID id = UUID.randomUUID();
		
		System.out.println(id.toString().length());
		
		UUID id2 = UUID.fromString(id.toString());
		
		assertThat(id, equalTo(id2));
		
	}
	
}


