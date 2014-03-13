package org.myeslib.data.test;

import java.util.UUID;

import lombok.Value;

import org.myeslib.core.Event;

@SuppressWarnings("serial")
@Value
public class EventJustForTest implements Event {
	UUID id;
	int something;
}
