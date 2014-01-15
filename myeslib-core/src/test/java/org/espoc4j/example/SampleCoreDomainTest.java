package org.espoc4j.example;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.myeslib.core.Event;
import org.myeslib.example.SampleCoreDomain.CreateInventoryItem;
import org.myeslib.example.SampleCoreDomain.DecreaseInventory;
import org.myeslib.example.SampleCoreDomain.IncreaseInventory;
import org.myeslib.example.SampleCoreDomain.InventoryDecreased;
import org.myeslib.example.SampleCoreDomain.InventoryIncreased;
import org.myeslib.example.SampleCoreDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleCoreDomain.InventoryItemCommandHandler;
import org.myeslib.example.SampleCoreDomain.InventoryItemCreated;
import org.myeslib.example.SampleCoreDomain.ItemDescriptionGeneratorService;

@RunWith(MockitoJUnitRunner.class)
public class SampleCoreDomainTest {

	@Mock
	ItemDescriptionGeneratorService uuidGeneratorService ;

	@Test(expected=NullPointerException.class)
	public void commandHandlerCreateWithNullService() {
		
		InventoryItemAggregateRoot aggregateRoot = new InventoryItemAggregateRoot();
		
		CreateInventoryItem command = new CreateInventoryItem(UUID.randomUUID());
		
		InventoryItemCommandHandler commandHandler = new InventoryItemCommandHandler(aggregateRoot);
		
		commandHandler.handle(command);
	}
	
	
	@Test(expected=IllegalArgumentException.class)
	public void commandHandlerCreateOnAnExistingInstance() {
		
		InventoryItemAggregateRoot aggregateRoot = new InventoryItemAggregateRoot();
		
		UUID id = UUID.randomUUID();
		
		aggregateRoot.setId(id);
		
		CreateInventoryItem command = new CreateInventoryItem(id);
		
		InventoryItemCommandHandler commandHandler = new InventoryItemCommandHandler(aggregateRoot);
	
		commandHandler.handle(command);
	
	}
	
	
	@Test(expected=IllegalArgumentException.class)
	public void commandHandlerIncreaseOnAnWrongInstance() {
		
		InventoryItemAggregateRoot aggregateRoot = new InventoryItemAggregateRoot();
		
		UUID id = UUID.randomUUID();
		
		aggregateRoot.setId(id);
		
		IncreaseInventory command = new IncreaseInventory(UUID.randomUUID(), 1);
		
		InventoryItemCommandHandler commandHandler = new InventoryItemCommandHandler(aggregateRoot);
	
		commandHandler.handle(command);
	
	}
	
	@Test
	public void commandHandlerCreateWithValidService() {
		
		InventoryItemAggregateRoot aggregateRoot = new InventoryItemAggregateRoot();
		
		UUID id = UUID.randomUUID();
		String desc = "item1";
		
		CreateInventoryItem command = new CreateInventoryItem(id);
		
		InventoryItemCommandHandler commandHandler = new InventoryItemCommandHandler(aggregateRoot);
	
		when(uuidGeneratorService.generate()).thenReturn(desc);
		
		command.setService(uuidGeneratorService);
		
		List<? extends Event> events = commandHandler.handle(command);
	
		verify(uuidGeneratorService).generate();
		
		Event expectedEvent = new InventoryItemCreated(id, desc);
		
		assertThat(events.get(0), equalTo(expectedEvent));
		
	}
	
	
	@Test
	public void commandHandlerIncrease() {
		
		InventoryItemAggregateRoot aggregateRoot = new InventoryItemAggregateRoot();
		
		UUID id = UUID.randomUUID();
		String desc = "item1";
		
		aggregateRoot.setAvaliable(0);
		aggregateRoot.setDescription(desc);
		aggregateRoot.setId(id);
		
		IncreaseInventory command = new IncreaseInventory(id, 3);
		
		InventoryItemCommandHandler commandHandler = new InventoryItemCommandHandler(aggregateRoot);
	
		List<? extends Event> events = commandHandler.handle(command);
	
		Event expectedEvent = new InventoryIncreased(id, 3);
		
		assertThat(events.get(0), is(expectedEvent));
		
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void commandHandlerDecreaseNotAvaliable() {
		
		InventoryItemAggregateRoot aggregateRoot = new InventoryItemAggregateRoot();
		
		UUID id = UUID.randomUUID();
		String desc = "item1";
		
		aggregateRoot.setAvaliable(2);
		aggregateRoot.setDescription(desc);
		aggregateRoot.setId(id);
		
		DecreaseInventory command = new DecreaseInventory(id, 3);
		
		InventoryItemCommandHandler commandHandler = new InventoryItemCommandHandler(aggregateRoot);
	
		commandHandler.handle(command);
		
	}
	
	@Test
	public void commandHandlerDecrease() {
		
		InventoryItemAggregateRoot aggregateRoot = new InventoryItemAggregateRoot();
		
		UUID id = UUID.randomUUID();
		String desc = "item1";
		
		aggregateRoot.setAvaliable(4);
		aggregateRoot.setDescription(desc);
		aggregateRoot.setId(id);
		
		DecreaseInventory command = new DecreaseInventory(id, 3);
		
		InventoryItemCommandHandler commandHandler = new InventoryItemCommandHandler(aggregateRoot);
	
		List<? extends Event> events = commandHandler.handle(command);
	
		Event expectedEvent = new InventoryDecreased(id, 3);
		
		assertThat(events.get(0), equalTo(expectedEvent));
		
	}
	
	
	@Test
	public void aggregateRootCreated() {
		
		InventoryItemAggregateRoot aggregateRoot = new InventoryItemAggregateRoot();
		
		UUID id = UUID.randomUUID();
		String desc = "item1";
		
		InventoryItemCreated event = new InventoryItemCreated(id, desc);
		
		aggregateRoot.on(event);
		
		assertThat(aggregateRoot.getId(), equalTo(id));
		assertThat(aggregateRoot.getDescription(), equalTo(desc));
		assertThat(aggregateRoot.getAvaliable(), equalTo(0));
		
	}
	
	@Test
	public void aggregateRootIncreased() {
		
		InventoryItemAggregateRoot aggregateRoot = new InventoryItemAggregateRoot();
		
		UUID id = UUID.randomUUID();
		String desc = "item1";
		
		aggregateRoot.setAvaliable(0);
		aggregateRoot.setDescription(desc);
		aggregateRoot.setId(id);
		
		InventoryIncreased event = new InventoryIncreased(id, 2);
		
		aggregateRoot.on(event);
		
		assertThat(aggregateRoot.getAvaliable(), equalTo(2));
		
	}
	
	@Test
	public void aggregateRootDecreased() {
		
		InventoryItemAggregateRoot aggregateRoot = new InventoryItemAggregateRoot();
		
		UUID id = UUID.randomUUID();
		String desc = "item1";
		
		aggregateRoot.setAvaliable(5);
		aggregateRoot.setDescription(desc);
		aggregateRoot.setId(id);
		
		InventoryDecreased event = new InventoryDecreased(id, 2);
		
		aggregateRoot.on(event);
		
		assertThat(aggregateRoot.getAvaliable(), equalTo(3));
		
	}
	

}
