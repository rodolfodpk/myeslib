package org.myeslib.hazelcast;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.myeslib.data.AggregateRootHistory;
import org.myeslib.data.UnitOfWork;
import org.myeslib.example.SampleCoreDomain.CreateInventoryItem;
import org.myeslib.example.SampleCoreDomain.DecreaseInventory;
import org.myeslib.example.SampleCoreDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleCoreDomain.InventoryItemCommandHandler;
import org.myeslib.example.SampleCoreDomain.InventoryItemCreated;
import org.myeslib.example.SampleCoreDomain.ItemDescriptionGeneratorService;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.TransactionalMap;
import com.hazelcast.transaction.TransactionContext;

@RunWith(MockitoJUnitRunner.class)
public class TransactionalCommandProcessorTest {

	@Mock
	HazelcastInstance hazelcastInstance;
	
	@Mock
	AggregateRootHistoryTxMapFactory<UUID, InventoryItemAggregateRoot> txMapFactory ;
	
	String mapId = "map4test";
	String itemDescription = "ok, here you are a description";
	
	@Test
	public void sucess() throws Throwable {

		UUID id = UUID.randomUUID();
		Long version = 0L;
		CreateInventoryItem command = new CreateInventoryItem(id);
		ItemDescriptionGeneratorService service = Mockito.mock(ItemDescriptionGeneratorService.class);
		when(service.generate()).thenReturn(itemDescription);
		command.setService(service);
		InventoryItemAggregateRoot  instance = new InventoryItemAggregateRoot();
		
		InventoryItemCommandHandler commandHandler = new InventoryItemCommandHandler(instance);

		TransactionContext txContext = Mockito.mock(TransactionContext.class);
		when(hazelcastInstance.newTransactionContext()).thenReturn(txContext );
		
		@SuppressWarnings("unchecked")
		TransactionalMap<UUID, AggregateRootHistory> txMapMock = Mockito.mock(TransactionalMap.class);
		when(txMapFactory.get(txContext, mapId)).thenReturn(txMapMock );
	
		TransactionalCommandProcessor<UUID, InventoryItemAggregateRoot> tcp = 
				new TransactionalCommandProcessor<>(hazelcastInstance, txMapFactory, mapId);
		
		InventoryItemCreated expectedEvent = new InventoryItemCreated(id, itemDescription)	;
			
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
	
		TransactionalCommandProcessor<UUID, InventoryItemAggregateRoot> tcp = 
				new TransactionalCommandProcessor<>(hazelcastInstance, txMapFactory, mapId);
		
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
	
		TransactionalCommandProcessor<UUID, InventoryItemAggregateRoot> tcp = 
				new TransactionalCommandProcessor<>(hazelcastInstance, txMapFactory, mapId);
		
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
