package org.myeslib.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.core.function.CommandHandlerInvoker;
import org.myeslib.example.SampleDomain.CreateInventoryItem;
import org.myeslib.example.SampleDomain.DecreaseInventory;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleDomain.InventoryItemCommandHandler;
import org.myeslib.example.SampleDomain.InventoryItemCreated;
import org.myeslib.example.SampleDomain.ItemDescriptionGeneratorService;
import org.myeslib.util.MultiMethodCommandHandlerInvoker;

import com.google.common.base.Function;

@RunWith(MockitoJUnitRunner.class)
public class MultiMethodCommandHandlerInvokerTest {

	Function<UUID, String> generateDescription = new Function<UUID, String>() {
		@Override
		public String apply(UUID id) {
			return String.format("description for item with id=", id.toString());
		}
	};
	
	@Test
	public void sucess() throws Throwable {

		UUID id = UUID.randomUUID();
		CreateInventoryItem command = new CreateInventoryItem(id);
		ItemDescriptionGeneratorService service = Mockito.mock(ItemDescriptionGeneratorService.class);
		
		when(service.generate(id)).then(new Answer<String>() {
			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				UUID id = (UUID) invocation.getArguments()[0];
				return generateDescription.apply(id);
			}
		})
		;

		command.setService(service);
		
		InventoryItemAggregateRoot  instance = new InventoryItemAggregateRoot();
		
		InventoryItemCommandHandler commandHandler = new InventoryItemCommandHandler(instance);

		CommandHandlerInvoker<UUID, InventoryItemAggregateRoot> tcp = new MultiMethodCommandHandlerInvoker<UUID, InventoryItemAggregateRoot>();
		
		InventoryItemCreated expectedEvent = new InventoryItemCreated(id, generateDescription.apply(id))	;
			
		UnitOfWork uow = tcp.invoke(id, command, commandHandler);
		
		InventoryItemCreated resultingEvent = (InventoryItemCreated) uow.getEvents().get(0);
		
		assertThat(resultingEvent, equalTo(expectedEvent));
		
	}
	
	@Test(expected=NullPointerException.class)
	public void failSinceServiceIsNull() throws Throwable {

		UUID id = UUID.randomUUID();
		CreateInventoryItem command = new CreateInventoryItem(id);
	
		InventoryItemAggregateRoot  instance = new InventoryItemAggregateRoot();
		InventoryItemCommandHandler commandHandler = new InventoryItemCommandHandler(instance);
	
		CommandHandlerInvoker<UUID, InventoryItemAggregateRoot> tcp = new MultiMethodCommandHandlerInvoker<UUID, InventoryItemAggregateRoot>();

		try {
			tcp.invoke(id, command, commandHandler);
		} catch (Throwable t) {
			throw t;
		}

	}

	@Test(expected=IllegalArgumentException.class)
	public void failSinceThereIsntEnoughtItems() throws Throwable {

		UUID id = UUID.randomUUID();
		Long version = 0L;
		DecreaseInventory command = new DecreaseInventory(id, 5, version);
	
		InventoryItemAggregateRoot  instance = new InventoryItemAggregateRoot();
		instance.setId(id);
		
		InventoryItemCommandHandler commandHandler = new InventoryItemCommandHandler(instance);
	
		CommandHandlerInvoker<UUID, InventoryItemAggregateRoot> tcp = new MultiMethodCommandHandlerInvoker<UUID, InventoryItemAggregateRoot>();

		try {
			tcp.invoke(id, command, commandHandler);
		} catch (Throwable t) {
			throw t;
		}

	}
}
