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
import org.myeslib.data.AggregateRootHistory;
import org.myeslib.data.UnitOfWork;
import org.myeslib.example.SampleCoreDomain.CreateInventoryItem;
import org.myeslib.example.SampleCoreDomain.DecreaseInventory;
import org.myeslib.example.SampleCoreDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleCoreDomain.InventoryItemCommandHandler;
import org.myeslib.example.SampleCoreDomain.InventoryItemCreated;
import org.myeslib.example.SampleCoreDomain.ItemDescriptionGeneratorService;

import com.google.common.base.Function;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.TransactionalMap;
import com.hazelcast.transaction.TransactionContext;

@RunWith(MockitoJUnitRunner.class)
public class TransactionalCommandHandlerTest {

	@Mock
	HazelcastInstance hazelcastInstance;
	
	@Mock
	AggregateRootHistoryTxMapFactory<UUID, InventoryItemAggregateRoot> txMapFactory ;
	
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
		TransactionalMap<UUID, AggregateRootHistory> txMapMock = Mockito.mock(TransactionalMap.class);
		when(txMapFactory.get(txContext, mapId)).thenReturn(txMapMock );
	
		TransactionalCommandHandler<UUID, InventoryItemAggregateRoot> tcp = 
				new TransactionalCommandHandler<>(hazelcastInstance, txMapFactory, mapId);
		
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
		TransactionalMap<UUID, AggregateRootHistory> txMapMock = Mockito.mock(TransactionalMap.class);
		when(txMapFactory.get(txContext, mapId)).thenReturn(txMapMock );
	
		TransactionalCommandHandler<UUID, InventoryItemAggregateRoot> tcp = 
				new TransactionalCommandHandler<>(hazelcastInstance, txMapFactory, mapId);
		
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
		TransactionalMap<UUID, AggregateRootHistory> txMapMock = Mockito.mock(TransactionalMap.class);
		when(txMapFactory.get(txContext, mapId)).thenReturn(txMapMock );
	
		TransactionalCommandHandler<UUID, InventoryItemAggregateRoot> tcp = 
				new TransactionalCommandHandler<>(hazelcastInstance, txMapFactory, mapId);
		
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
