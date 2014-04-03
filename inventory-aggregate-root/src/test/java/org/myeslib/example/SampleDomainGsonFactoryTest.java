package org.myeslib.example;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.UUID;

import org.junit.Test;
import org.myeslib.core.Command;
import org.myeslib.core.Event;
import org.myeslib.core.data.AggregateRootHistory;
import org.myeslib.core.data.Snapshot;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.example.SampleDomain.IncreaseInventory;
import org.myeslib.example.SampleDomain.InventoryIncreased;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class SampleDomainGsonFactoryTest {
	
	final Gson gson = new SampleDomainGsonFactory().create();

	@Test
	public void aggregateRootHistory() {

		UUID id = UUID.randomUUID();
		
		Command command1 = new IncreaseInventory(id, 2, 0L);
		Event event11 = new InventoryIncreased(id, 1);
		Event event12 = new InventoryIncreased(id, 1);	
		UnitOfWork uow1 = UnitOfWork.create(command1, Arrays.asList(event11, event12));

		Command command2 = new IncreaseInventory(id, 10, 0L);
		Event event21 = new InventoryIncreased(id, 1);
		Event event22 = new InventoryIncreased(id, 1);	
		UnitOfWork uow2 = UnitOfWork.create(command2, Arrays.asList(event21, event22));

		AggregateRootHistory arh = new AggregateRootHistory();
		
		arh.add(uow1);
		arh.add(uow2);
		
		String asString = gson.toJson(arh);

		AggregateRootHistory arhFromJson = gson.fromJson(asString, AggregateRootHistory.class);
		
		assertEquals(arh, arhFromJson);
	
	}
	
	@Test
	public void snapshot() {
		
		InventoryItemAggregateRoot ar = new InventoryItemAggregateRoot();
		ar.setAvailable(1);
		
		Snapshot<InventoryItemAggregateRoot> s = new Snapshot<>(ar, 1L);
		
		String asString = gson.toJson(s) ;
		
		Type snapshotInventoryItemType = new TypeToken<Snapshot<InventoryItemAggregateRoot>>() {}.getType();
			
		Snapshot<InventoryItemAggregateRoot> s2 = gson.fromJson(asString, snapshotInventoryItemType);
		
		assertEquals(s, s2);
		
	}
	
	@Test
	public void commandPolimorfism() {
		// TODO
	}

}
