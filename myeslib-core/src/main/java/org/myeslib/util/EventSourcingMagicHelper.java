package org.myeslib.util;

import java.util.List;

import lombok.SneakyThrows;
import mm4j.MultiMethod;

import org.myeslib.core.AggregateRoot;
import org.myeslib.core.Command;
import org.myeslib.core.CommandHandler;
import org.myeslib.core.Event;

/**
 * Both CommandHandler and AggregateRoot ideally should have handle(Command command) and on(Event event), respectively.
 * But then we would need a big if statement to examine the parameter and to dispatch to appropriate method. 
 * This would be very boilerplate (if command instanceof IncreaseInventory....), so here we are using some magic.
 */
public class EventSourcingMagicHelper {
	
	@SuppressWarnings("unchecked")
	@SneakyThrows
	static public List<? extends Event> applyCommandOn(Command command, CommandHandler<? extends AggregateRoot> instance)  {
	   MultiMethod mm = MultiMethod.getMultiMethod(instance.getClass(), "handle");
	   List<? extends Event> events = (List<? extends Event>) mm.invoke(instance, command);
	   return events;
	}
	
	@SneakyThrows
	static public void applyEventsOn(List<? extends Event> events, AggregateRoot instance)  {
	   MultiMethod mm = MultiMethod.getMultiMethod(instance.getClass(), "on");
	   for (Event event : events) {
		  mm.invoke(instance, event);
	   }
	}

}
