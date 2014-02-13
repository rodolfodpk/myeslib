package org.myeslib.data;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.myeslib.core.Command;
import org.myeslib.core.Event;
import org.myeslib.core.data.AggregateRootHistory;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.example.SampleDomain.IncreaseInventory;
import org.myeslib.example.SampleDomain.InventoryIncreased;

@RunWith(MockitoJUnitRunner.class) 
public class AggregateRootHistoryTest {

	@Test
	public void empty() {
		AggregateRootHistory transactions = new AggregateRootHistory();
		assertThat(transactions.getLastVersion(), is(new Long(0)));
		assertThat(transactions.getAllEvents().size(), is(0));
	}
	
	@Test(expected=NullPointerException.class)
	public void nullCommand() {
		AggregateRootHistory transactions = new AggregateRootHistory();
		transactions.add(UnitOfWork.create(null, Arrays.asList(Mockito.mock(Event.class))));
	}
	
	@Test(expected=NullPointerException.class)
	public void nullEventsList() {
		AggregateRootHistory transactions = new AggregateRootHistory();
		transactions.add(UnitOfWork.create(Mockito.mock(Command.class), null));
	}	
	
	@Test(expected=IllegalArgumentException.class)
	public void invalidVersion() {
		AggregateRootHistory transactions = new AggregateRootHistory();
		Command command = Mockito.mock(Command.class);
		when(command.getVersion()).thenReturn(-1L);
		transactions.add(UnitOfWork.create(command, Arrays.asList(Mockito.mock(Event.class))));
	}	
	
	@Test 
	public void firstTransaction() {
		UUID id = UUID.randomUUID();
		AggregateRootHistory transactions = new AggregateRootHistory();
		Command command = new IncreaseInventory(id, 1, 0L);
		Event event1 = new InventoryIncreased(id, 1);
		transactions.add(UnitOfWork.create(command, Arrays.asList(event1)));

		assertThat(transactions.getUnitsOfWork().size(), is(1));
		assertThat(transactions.getUnitsOfWork().get(0).getCommand(), sameInstance(command));

		assertThat(transactions.getAllEvents().size(), is(1));
		assertThat(transactions.getAllEvents().get(0), sameInstance(event1));
	}	

	@Test 
	public void twoEvents() {
		
		AggregateRootHistory transactions = new AggregateRootHistory();
		
		UUID id = UUID.randomUUID();
		Command command = new IncreaseInventory(id, 2, 0l);
		Event event1 = new InventoryIncreased(id, 1);
		Event event2 = new InventoryIncreased(id, 1);	
		
		transactions.add(UnitOfWork.create(command, Arrays.asList(event1, event2)));
		
		assertThat(transactions.getLastVersion(), is(1L));
		assertThat(transactions.getUnitsOfWork().size(), is(1));
		assertThat(transactions.getUnitsOfWork().get(0).getCommand(), sameInstance(command));

		assertThat(transactions.getAllEvents().size(), is(2));
		
		assertThat("all events are within history", transactions.getAllEvents().containsAll(Arrays.asList(event1, event2)));
		
	}	
	
	@Test(expected=NullPointerException.class)
	public void nullEvent() {
		
		UUID id = UUID.randomUUID();
		AggregateRootHistory transactions = new AggregateRootHistory();
		Command command = new IncreaseInventory(id, 2, 1L);
		Event event1 = (Event) null;
		transactions.add(UnitOfWork.create(command, Arrays.asList(event1)));

	}	
	
}
