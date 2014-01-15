package org.myeslib.example.infra;

import java.lang.reflect.Modifier;

import org.myeslib.core.Command;
import org.myeslib.core.Event;
import org.myeslib.example.SampleCoreDomain.CreateInventoryItem;
import org.myeslib.example.SampleCoreDomain.DecreaseInventory;
import org.myeslib.example.SampleCoreDomain.IncreaseInventory;
import org.myeslib.example.SampleCoreDomain.InventoryDecreased;
import org.myeslib.example.SampleCoreDomain.InventoryIncreased;
import org.myeslib.example.SampleCoreDomain.InventoryItemCreated;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;

/*
 * Produces a Gson instance able to ser/deserialize polymorfic types. 
 * Jackson is probably faster but it requires those @JsonCreator and @JsonProperty annotations which can be a bit verbose and error prone.
 */
public class GsonFactory {
	
	private final Gson gson;
	
	public GsonFactory() {
		
		final RuntimeTypeAdapterFactory<Command> commandAdapter = 
				RuntimeTypeAdapterFactory.of(Command.class)
				.registerSubtype(CreateInventoryItem.class, CreateInventoryItem.class.getSimpleName())
				.registerSubtype(IncreaseInventory.class, IncreaseInventory.class.getSimpleName())
				.registerSubtype(DecreaseInventory.class, DecreaseInventory.class.getSimpleName());

		final RuntimeTypeAdapterFactory<Event> eventAdapter = 
				RuntimeTypeAdapterFactory.of(Event.class)
				.registerSubtype(InventoryItemCreated.class, InventoryItemCreated.class.getSimpleName())
				.registerSubtype(InventoryIncreased.class, InventoryIncreased.class.getSimpleName())
				.registerSubtype(InventoryDecreased.class, InventoryDecreased.class.getSimpleName());	
		
		this.gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithModifiers(Modifier.TRANSIENT)
			.registerTypeAdapterFactory(commandAdapter)
			.registerTypeAdapterFactory(eventAdapter)
			.create();
	
	}

	public Gson create() {
		return gson;
	}

}
