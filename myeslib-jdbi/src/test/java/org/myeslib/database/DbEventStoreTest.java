package org.myeslib.database;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.myeslib.data.AggregateRootHistory;
import org.myeslib.data.UnitOfWork;
import org.myeslib.example.SampleDomain.DecreaseInventory;
import org.myeslib.example.SampleDomain.IncreaseInventory;
import org.myeslib.example.SampleDomain.InventoryDecreased;
import org.myeslib.example.SampleDomain.InventoryIncreased;
import org.myeslib.storage.database.DefaultDbUnitOfWorkDao;
import org.myeslib.storage.database.jdbi.AggregateRootReaderRepository;
import org.myeslib.storage.database.jdbi.AggregateRootWriterRepository;
import org.skife.jdbi.v2.Handle;

@RunWith(MockitoJUnitRunner.class) 
public class DbEventStoreTest {
	
	@Mock
	Handle handle;
	
	@Mock
	AggregateRootReaderRepository<UUID> arReader;
	
	@Mock
	AggregateRootWriterRepository<UUID> arWriter;
	
	@Test
	public void firstTransactionOnEmptyHistory() {
		
		UUID id = UUID.randomUUID();

		UnitOfWork newUow = UnitOfWork.create(new IncreaseInventory(id, 1), 0l, Arrays.asList(new InventoryIncreased(id, 1)));
		
		when(arReader.get(id, handle)).thenReturn(null);
		
		DefaultDbUnitOfWorkDao<UUID> store = new DefaultDbUnitOfWorkDao<>(handle, arReader, arWriter);
		
		store.insert(id, newUow);
		
		verify(arReader, times(1)).get(id, handle);
		verify(arWriter, times(1)).insert(id, newUow, handle);
		
	}

	@Test
	public void baseVersionMatchingLastVersion() {
		
		UUID id = UUID.randomUUID();

		UnitOfWork existingUow = UnitOfWork.create(new IncreaseInventory(id, 1), 0l, Arrays.asList(new InventoryIncreased(id, 1)));

		AggregateRootHistory existing = new AggregateRootHistory();

		existing.add(existingUow);
		
		when(arReader.get(id, handle)).thenReturn(existing);
		
		UnitOfWork newUow = UnitOfWork.create(new DecreaseInventory(id, 1), 1L, Arrays.asList(new InventoryDecreased(id, 1)));
		
		DefaultDbUnitOfWorkDao<UUID> store = new DefaultDbUnitOfWorkDao<>(handle, arReader, arWriter);
		
		store.insert(id, newUow);
		
		verify(arReader, times(1)).get(id, handle);
		verify(arWriter, times(1)).insert(id, newUow, handle);
		
	}
	
	@Test(expected=ConcurrentModificationException.class)
	public void baseVersionDoestNotMatchLastVersion() {
		
		UUID id = UUID.randomUUID();

		UnitOfWork existingUow = UnitOfWork.create(new IncreaseInventory(id, 1), 0l, Arrays.asList(new InventoryIncreased(id, 1)));

		AggregateRootHistory existing = new AggregateRootHistory();

		existing.add(existingUow);
		
		when(arReader.get(id, handle)).thenReturn(existing);

		UnitOfWork newUow = UnitOfWork.create(new DecreaseInventory(id, 1), 0L, Arrays.asList(new InventoryDecreased(id, 1)));
		
		DefaultDbUnitOfWorkDao<UUID> store = new DefaultDbUnitOfWorkDao<>(handle, arReader, arWriter);
		
		store.insert(id, newUow);
		
		verify(arReader, times(1)).get(id, handle);
		verify(arWriter, times(0)).insert(any(UUID.class), any(UnitOfWork.class), any(Handle.class));
		
	}

	
}


