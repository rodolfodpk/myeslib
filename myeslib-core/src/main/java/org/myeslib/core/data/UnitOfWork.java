package org.myeslib.core.data;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import lombok.Value;

import org.myeslib.core.Command;
import org.myeslib.core.Event;

@SuppressWarnings("serial")
@Value
public class UnitOfWork implements Comparable<UnitOfWork>, Serializable {

    final UUID id;
	final Command command;
	final List<? extends Event> events;
	final long version;
	
	public UnitOfWork(UUID id, Command command, Long version, List<? extends Event> events) {
	    checkNotNull(id, "id cannot be null");
	    checkNotNull(command, "command cannot be null");
		checkArgument(command.getTargetVersion()>=0, "target version must be >= 0");
		checkArgument(version>0, "invalid version");
		checkNotNull(events, "events cannot be null");
		for (Event e: events){
			checkNotNull(e, "event within events list cannot be null");
		}
		this.id = id;
		this.command = command;
		this.version = version;
		this.events = events;
	}
	
	public static UnitOfWork create(UUID id, Command command, List<? extends Event> newEvents) {
		checkNotNull(command.getTargetVersion(), "target version cannot be null");
		checkArgument(command.getTargetVersion()>=0, "target version must be >= 0");
		return new UnitOfWork(id, command, command.getTargetVersion()+1, newEvents);
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

	public Long getTargetVersion() {
		return command.getTargetVersion();
	}
}
