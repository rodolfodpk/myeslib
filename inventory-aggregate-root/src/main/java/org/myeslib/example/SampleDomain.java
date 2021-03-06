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

    @Data
    public static class InventoryItemAggregateRoot implements AggregateRoot {

        UUID id;
        String description;
        Integer available = 0;

        public void on(InventoryItemCreated event) {
            this.id = event.id;
            this.description = event.description;
            this.available = 0;
        }

        public void on(InventoryIncreased event) {
            this.available = this.available + event.howMany;
        }

        public void on(InventoryDecreased event) {
            this.available = this.available - event.howMany;
        }
        
        public boolean isAvailable(int howMany) {
            return getAvailable() - howMany >= 0;
        }

    }
    
    // command handlers

    @AllArgsConstructor
    public static class CreateCommandHandler implements CommandHandler<CreateInventoryItem> {

        @Delegate @NonNull
        final InventoryItemAggregateRoot aggregateRoot;
        @NonNull
        final ItemDescriptionGeneratorService service;

        public List<? extends Event> handle(CreateInventoryItem command) {
            checkArgument(getId() == null, "item already exists");
            checkNotNull(service);
            String description = service.generate(command.getId());
            InventoryItemCreated event = new InventoryItemCreated(command.getId(), description);
            return Arrays.asList(event);
        }
    }
    
    @AllArgsConstructor
    public static class IncreaseCommandHandler implements CommandHandler<IncreaseInventory> {

        @Delegate @NonNull
        final InventoryItemAggregateRoot aggregateRoot;

        public List<? extends Event> handle(IncreaseInventory command) {
            checkArgument(getId() != null, "before increasing you must create an item");
            checkArgument(getId().equals(command.getId()), "item id does not match");
            InventoryIncreased event = new InventoryIncreased(command.getId(), command.getHowMany());
            return Arrays.asList(event);
        }
    }
    
    @AllArgsConstructor
    public static class DecreaseCommandHandler implements CommandHandler<DecreaseInventory> {

        @Delegate @NonNull
        final InventoryItemAggregateRoot aggregateRoot;

        public List<? extends Event> handle(DecreaseInventory command) {
            checkArgument(getId() != null, "before decreasing you must create an item");
            checkArgument(getId().equals(command.getId()), "item id does not match");
            checkArgument(isAvailable(command.howMany), "there are not enough items available");
            InventoryDecreased event = new InventoryDecreased(command.getId(), command.getHowMany());
            return Arrays.asList(event);
        }
    }
    
    // commands

    @Value
    public static class CreateInventoryItem implements Command {
        @NonNull
        UUID commandId;
        @NonNull
        UUID id;
        Long targetVersion = 0L;
    }

    @Value
    public static class IncreaseInventory implements Command {
        @NonNull
        UUID commandId;
        @NonNull
        UUID id;
        @NonNull
        Integer howMany;
        Long targetVersion;
    }

    @Value
    public static class DecreaseInventory implements Command {
        @NonNull
        UUID commandId;
        @NonNull
        UUID id;
        @NonNull
        Integer howMany;
        Long targetVersion;
    }

    // events

    @Value
    public static class InventoryItemCreated implements Event {
        @NonNull
        UUID id;
        @NonNull
        String description;
    }

    @Value
    public static class InventoryIncreased implements Event {
        @NonNull
        UUID id;
        @NonNull
        Integer howMany;
    }

    @Value
    public static class InventoryDecreased implements Event {
        @NonNull
        UUID id;
        @NonNull
        Integer howMany;
    }

    // a service just for the sake of the example

    public static interface ItemDescriptionGeneratorService {
        String generate(UUID id);
    }
    
    // a factory for the aggregate root

    public static class InventoryItemInstanceFactory implements
            Function<Void, InventoryItemAggregateRoot> {

        @Override
        public InventoryItemAggregateRoot apply(Void input) {
            return new InventoryItemAggregateRoot();
        }

    }

}
