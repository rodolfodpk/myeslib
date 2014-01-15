package org.espoc4j.data;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.myeslib.core.Command;
import org.myeslib.core.Event;
import org.myeslib.data.UnitOfWork;
import org.myeslib.example.SampleCoreDomain.InventoryIncreased;

public class UnitOfWorkTest {

	@SuppressWarnings("serial")
	@Test
	public void baseVersionShouldBeFormerVersion() {
		List<Event> events = Arrays.asList((Event) new InventoryIncreased(UUID.randomUUID(), 1));
		UnitOfWork uow = new UnitOfWork(new Command() {}, 1L, events, System.currentTimeMillis());
		assertThat(uow.getBaseVersion(), is(0L));
	}

	@SuppressWarnings({ "unused", "serial" })
	@Test(expected=NullPointerException.class)
	public void nullEvent() {
		List<Event> events = Arrays.asList((Event) null);
		UnitOfWork uow = new UnitOfWork(new Command() {}, 1L, events, System.currentTimeMillis());
	}
}
