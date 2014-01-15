package org.myeslib.data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.myeslib.core.Event;

import lombok.Data;

@SuppressWarnings("serial")
@Data
public class AggregateRootHistory implements Serializable {

	private final List<UnitOfWork> unitsOfWork;

	public AggregateRootHistory() {
		this.unitsOfWork = new LinkedList<>();
	}

	public List<Event> getAllEvents() {
       return getEventsAfterUntil(0, Long.MAX_VALUE);
	}

	public List<Event> getEventsAfterUntil(long afterVersion, long untilVersion){
		List<Event> events = new LinkedList<>();
		for (UnitOfWork t : unitsOfWork) {
			if (t.getVersion() > afterVersion && t.getVersion() <= untilVersion){
				for (Event event : t.getEvents()) {
					events.add(event);
				}
			}
		}
		return events;
	}
	
	public List<Event> getEventsUntil(long version){
		List<Event> events = new LinkedList<>();
		for (UnitOfWork t : unitsOfWork) {
			if (t.getVersion() <= version){
				for (Event event : t.getEvents()) {
					events.add(event);
				}
			}
		}
		return events;
	}
	
	public Long getLastVersion() {
		return unitsOfWork.size()==0 ? 0 : unitsOfWork.get(unitsOfWork.size()-1).getVersion();
	}

	public void add(final UnitOfWork transaction) {
		checkNotNull(transaction);
		unitsOfWork.add(transaction);
	}

}
