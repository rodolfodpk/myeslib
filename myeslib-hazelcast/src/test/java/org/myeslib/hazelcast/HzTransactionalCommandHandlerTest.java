package org.myeslib.hazelcast;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.myeslib.data.UnitOfWork;
import org.myeslib.example.SampleDomain.CreateInventoryItem;
import org.myeslib.example.SampleDomain.DecreaseInventory;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleDomain.InventoryItemCommandHandler;
import org.myeslib.example.SampleDomain.InventoryItemCreated;
import org.myeslib.example.SampleDomain.ItemDescriptionGeneratorService;
import org.myeslib.example.SampleDomainGsonFactory;
import org.myeslib.gson.FromStringFunction;
import org.myeslib.gson.ToStringFunction;
import org.myeslib.storage.TransactionalCommandHandler;

import com.google.common.base.Function;
import com.google.gson.Gson;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.TransactionalMap;
import com.hazelcast.transaction.TransactionContext;

@RunWith(MockitoJUnitRunner.class)
public class HzTransactionalCommandHandlerTest {

	@Mock
	HazelcastInstance hazelcastInstance;
	
	@Mock
	HzStringTxMapFactory<UUID> txMapFactory ;
	

	final Gson gson = new SampleDomainGsonFactory().create();
	
	final FromStringFunction fromStringFunction = new FromStringFunction(gson);
	
	final ToStringFunction toStringFunction = new ToStringFunction(gson);
	
	String mapId = "map4test";
	
	Function<UUID, String> generateDescription = new Function<UUID, String>() {
		@Override
		public String apply(UUID id) {
			return String.format("description for item with id=", id.toString());
		}
	};
	
	@Test
	public void sucess() throws Throwable {

		UUID id = UUID.randomUUID();
		Long version = 0L;
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

		TransactionContext txContext = Mockito.mock(TransactionContext.class);
		when(hazelcastInstance.newTransactionContext()).thenReturn(txContext );
		
		@SuppressWarnings("unchecked")
		TransactionalMap<UUID, String> txMapMock = Mockito.mock(TransactionalMap.class);
		when(txMapFactory.get(txContext, mapId)).thenReturn(txMapMock);
	
		TransactionalCommandHandler<UUID, InventoryItemAggregateRoot> tcp = 
				new HzTransactionalCommandHandler<UUID, InventoryItemAggregateRoot>(hazelcastInstance, txMapFactory, mapId, fromStringFunction, toStringFunction);
		
		InventoryItemCreated expectedEvent = new InventoryItemCreated(id, generateDescription.apply(id))	;
			
		UnitOfWork uow = tcp.handle(id, version, command, commandHandler);
		
		verify(hazelcastInstance).newTransactionContext();
		verify(txContext).beginTransaction();
		verify(txContext).commitTransaction();	
		
		InventoryItemCreated resultingEvent = (InventoryItemCreated) uow.getEvents().get(0);
		
		assertThat(resultingEvent, equalTo(expectedEvent));
		
	}
	
	@Test(expected=NullPointerException.class)
	public void failSinceServiceIsNull() throws Throwable {

		UUID id = UUID.randomUUID();
		Long version = 0L;
		CreateInventoryItem command = new CreateInventoryItem(id);
	
		command.setService(null);
		
		InventoryItemAggregateRoot  instance = new InventoryItemAggregateRoot();
		InventoryItemCommandHandler commandHandler = new InventoryItemCommandHandler(instance);
		TransactionContext txContext = Mockito.mock(TransactionContext.class);
		when(hazelcastInstance.newTransactionContext()).thenReturn(txContext );
		@SuppressWarnings("unchecked")
		TransactionalMap<UUID, String> txMapMock = Mockito.mock(TransactionalMap.class);
		when(txMapFactory.get(txContext, mapId)).thenReturn(txMapMock );
	
		TransactionalCommandHandler<UUID, InventoryItemAggregateRoot> tcp = 
				new HzTransactionalCommandHandler<>(hazelcastInstance, txMapFactory, mapId, fromStringFunction, toStringFunction);
		
		try {
			tcp.handle(id, version, command, commandHandler);
		} catch (Throwable t) {
			verify(hazelcastInstance).newTransactionContext();
			verify(txContext).beginTransaction();
			verify(txContext).rollbackTransaction();	
			throw t;
		}

	}

	@Test(expected=IllegalArgumentException.class)
	public void failSinceThereIsntEnoughtItems() throws Throwable {

		UUID id = UUID.randomUUID();
		Long version = 0L;
		DecreaseInventory command = new DecreaseInventory(id, 5);
	
		InventoryItemAggregateRoot  instance = new InventoryItemAggregateRoot();
		instance.setId(id);
		
		InventoryItemCommandHandler commandHandler = new InventoryItemCommandHandler(instance);
		TransactionContext txContext = Mockito.mock(TransactionContext.class);
		when(hazelcastInstance.newTransactionContext()).thenReturn(txContext );
		@SuppressWarnings("unchecked")
		TransactionalMap<UUID, String> txMapMock = Mockito.mock(TransactionalMap.class);
		when(txMapFactory.get(txContext, mapId)).thenReturn(txMapMock );
	
		TransactionalCommandHandler<UUID, InventoryItemAggregateRoot> tcp = 
				new HzTransactionalCommandHandler<>(hazelcastInstance, txMapFactory, mapId, fromStringFunction, toStringFunction);
		
		try {
			tcp.handle(id, version, command, commandHandler);
		} catch (Throwable t) {
			verify(hazelcastInstance).newTransactionContext();
			verify(txContext).beginTransaction();
			verify(txContext).rollbackTransaction();	
			throw t;
		}

	}
}
