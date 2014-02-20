package org.myeslib.core.data;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.myeslib.core.Command;
import org.myeslib.core.Event;

import lombok.Value;

@SuppressWarnings("serial")
@Value
public class UnitOfWork implements Comparable<UnitOfWork>, Serializable {

	final Command command;
	final List<? extends Event> events;
	final long version;
	final long timestamp;
	
	public UnitOfWork(Command command, Long version, List<? extends Event> events, long timestamp) {
		checkNotNull(command, "command cannot be null");
		checkArgument(command.getVersion()>=0, "command version must be >= 0");
		checkArgument(version>0, "invalid version");
		checkNotNull(events, "events cannot be null");
		for (Event e: events){
			checkNotNull(e, "event within events list cannot be null");
		}
		this.command = command;
		this.version = version;
		this.events = events;
		this.timestamp = timestamp;
	}
	
	public static UnitOfWork create(Command command, List<? extends Event> newEvents) {
		checkNotNull(command.getVersion(), "command version cannot be null");
		checkArgument(command.getVersion()>=0, "command version must be >= 0");
		return new UnitOfWork(command, command.getVersion()+1, newEvents, System.currentTimeMillis());
	}
	
	public List<Event> getEvents(){
		List<Event> result = new LinkedList<>();
		for (Event event : events) {
			result.add(event);
		}
		return result;
	}
	
	public int compareTo(UnitOfWork other) {
		if (version < other.version) {
			return -1;
		} else if (version > other.version) {
			return 1;
		}
		return 0;
	}

	public Long getCommandVersion() {
		return command.getVersion();
	}
}
