package org.myeslib.data;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.myeslib.core.Event;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.example.SampleDomain.CreateInventoryItem;
import org.myeslib.example.SampleDomain.IncreaseInventory;
import org.myeslib.example.SampleDomain.InventoryIncreased;

public class UnitOfWorkTest {

	@Test
	public void versionShouldBeCommandVersionPlusOne() {
		List<Event> events = Arrays.asList((Event) new InventoryIncreased(UUID.randomUUID(), 1));
		CreateInventoryItem command = new CreateInventoryItem(UUID.randomUUID(), 0L, null);
		UnitOfWork uow = UnitOfWork.create(command, events);
		assertThat(uow.getCommandVersion(), is(0L));
		assertThat(uow.getVersion(), is(1L));
	}

	@SuppressWarnings("unused")
	@Test(expected=NullPointerException.class)
	public void nullEvent() {
		List<Event> events = Arrays.asList((Event) null);
		IncreaseInventory command = new IncreaseInventory(UUID.randomUUID(), 1, 1L);
		UnitOfWork uow = new UnitOfWork(command, 1L, events, System.currentTimeMillis());
	}
}
