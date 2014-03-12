package org.myeslib.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class TimeUnitHelperTest {

	@Test
	public void tenseconds() {
		TimeUnitHelper t = new TimeUnitHelper("10s ");
		assertThat(t.getDurationAsNumber(), is(10L));
		assertThat(t.getDurationTime(), is(TimeUnit.SECONDS));
	}

	@Test(expected=IllegalArgumentException.class)
	public void badFormat() {
		TimeUnitHelper t = new TimeUnitHelper("s10segunds");
		assertThat(t.getDurationAsNumber(), is(10L));
		assertThat(t.getDurationTime(), is(TimeUnit.SECONDS));
	}


}
