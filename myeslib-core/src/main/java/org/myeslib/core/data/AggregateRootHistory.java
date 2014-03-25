package org.myeslib.core.data;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;
import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import lombok.Data;

import org.myeslib.core.Event;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@SuppressWarnings("serial")
@Data
public class AggregateRootHistory implements Serializable {

	private final List<UnitOfWork> unitsOfWork;
	private final Set<UnitOfWork> persisted;

	public AggregateRootHistory() {
		this.unitsOfWork = new LinkedList<>();
		this.persisted = new LinkedHashSet<>();
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

	public UnitOfWork getLastUnitOfWork() {
		return unitsOfWork.get(unitsOfWork.size()-1);
	}

    public List<UnitOfWork> getPendingOfPersistence() {
        return Lists.newLinkedList(Sets.difference(Sets.newLinkedHashSet(unitsOfWork), persisted));
    }

    public void markAsPersisted(UnitOfWork uow) {
        checkArgument(unitsOfWork.contains(uow), "unitOfWork must be part of this AggregateRootHistory in order to be marked as persisted");
        persisted.add(uow);
    }

}
