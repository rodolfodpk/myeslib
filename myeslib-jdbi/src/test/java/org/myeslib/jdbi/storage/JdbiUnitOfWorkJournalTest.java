package org.myeslib.jdbi.storage;

import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.myeslib.core.Command;
import org.myeslib.core.Event;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.example.SampleDomain.IncreaseInventory;
import org.myeslib.example.SampleDomain.InventoryIncreased;
import org.myeslib.util.jdbi.UnitOfWorkJournalDao;

@RunWith(MockitoJUnitRunner.class) 
public class JdbiUnitOfWorkJournalTest {

	@Mock
    UnitOfWorkJournalDao<UUID> dao;
	
	@Test
	public void insert() {
		
		JdbiUnitOfWorkJournal<UUID> writer = new JdbiUnitOfWorkJournal<>(dao);

	    UUID id = UUID.randomUUID();
		
		Command command1 = new IncreaseInventory(id, 2, 0l);
		Event event11 = new InventoryIncreased(id, 1);
		Event event12 = new InventoryIncreased(id, 1);	
		UnitOfWork uow1 = UnitOfWork.create(command1, Arrays.asList(event11, event12));

		writer.append(id, uow1);
		
		verify(dao).append(id, uow1);
		
	}

}


