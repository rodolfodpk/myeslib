package org.myeslib.data;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.myeslib.core.Event;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.data.test.CommandJustForTest;
import org.myeslib.data.test.EventJustForTest;

public class UnitOfWorkTest {

	@Test
	public void versionShouldBeCommandVersionPlusOne() {
		List<Event> events = Arrays.asList((Event) new EventJustForTest(UUID.randomUUID(), 1));
		CommandJustForTest command = new CommandJustForTest(UUID.randomUUID(), 0L);
		UnitOfWork uow = UnitOfWork.create(command, events);
		assertThat(uow.getCommandVersion(), is(0L));
		assertThat(uow.getVersion(), is(1L));
	}

	@SuppressWarnings("unused")
	@Test(expected=NullPointerException.class)
	public void nullEvent() {
		List<Event> events = Arrays.asList((Event) null);
		CommandJustForTest command = new CommandJustForTest(UUID.randomUUID(), 1L);
		UnitOfWork uow = new UnitOfWork(command, 1L, events, System.currentTimeMillis());
	}
}
