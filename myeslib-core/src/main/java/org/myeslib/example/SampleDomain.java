package org.myeslib.example;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Delegate;
import lombok.NonNull;
import lombok.Value;

import org.myeslib.core.AggregateRoot;
import org.myeslib.core.Command;
import org.myeslib.core.CommandHandler;
import org.myeslib.core.Event;

import com.google.common.base.Function;

@SuppressWarnings("serial")
public class SampleDomain {

	@AllArgsConstructor
	public static class InventoryItemCommandHandler implements CommandHandler<InventoryItemAggregateRoot> {
		
		@Delegate
		final InventoryItemAggregateRoot aggregateRoot;
		
		public List<? extends Event> handle(CreateInventoryItem command) {
	    	checkArgument(getId() == null, "item already exists");
			checkNotNull(command.getService());
			String description = command.getService().generate(command.getId());
			InventoryItemCreated event = new InventoryItemCreated(command.getId(), description);
			return Arrays.asList(event);		
		}

		public List<? extends Event> handle(IncreaseInventory command) {
	    	checkArgument(getId() != null, "before increasing you must create an item");
	     	checkArgument(getId().equals(command.getId()), "item id does not match");
			InventoryIncreased event = new InventoryIncreased(command.getId(), command.getHowMany());
			return Arrays.asList(event);		
		}

		public List<? extends Event> handle(DecreaseInventory command) {
	    	checkArgument(getId() != null, "before decreasing you must create an item");
	     	checkArgument(getId().equals(command.getId()), "item id does not match");
	    	if (getAvaliable() - command.getHowMany() < 0){
	    		throw new IllegalArgumentException("there are not enough items avaliable");
	    	}
			InventoryDecreased event = new InventoryDecreased(command.getId(), command.getHowMany());
			return Arrays.asList(event);		
		}		
	}
	
	@Data
	public static class InventoryItemAggregateRoot implements AggregateRoot {
		
		UUID id;
		String description;
		Integer avaliable = 0; // needs a default value 

		public void on(InventoryItemCreated event) {
			this.id = event.id;
			this.description = event.description;
			this.avaliable = 0;
		}
		
		public void on(InventoryIncreased event) {
			this.avaliable = this.avaliable + event.howMany;
		}

		public void on(InventoryDecreased event) {
			this.avaliable = this.avaliable - event.howMany;
		}
		
	}
	
	// commands
	
	@Data
	public static class CreateInventoryItem implements Command {
		transient ItemDescriptionGeneratorService service; 
		@NonNull UUID id;
	}
	
	@Value
	public static class IncreaseInventory implements Command {
		@NonNull UUID id;
		@NonNull Integer howMany;
	}
	
	@Value
	public static class DecreaseInventory implements Command {
		@NonNull UUID id;
		@NonNull Integer howMany;
	}
	
	// events 
	
	@Value
	public static class InventoryItemCreated implements Event {
		@NonNull UUID id;
		@NonNull String description;
	}
	
	@Value
	public static class InventoryIncreased implements Event {
		@NonNull UUID id;
		@NonNull Integer howMany;
	}

	@Value
	public static class InventoryDecreased implements Event {
		@NonNull UUID id;
		@NonNull Integer howMany;
	}
	
	// a service just for sake of example
	
	public static interface ItemDescriptionGeneratorService {
		String generate(UUID id);
	}
	
	public static class InventoryItemInstanceFactory implements Function<Void, InventoryItemAggregateRoot> {

		@Override
		public InventoryItemAggregateRoot apply(Void input) {
			return new InventoryItemAggregateRoot();
		}

	}

}
