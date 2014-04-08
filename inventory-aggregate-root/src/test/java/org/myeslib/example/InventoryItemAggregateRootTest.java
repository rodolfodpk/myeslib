package org.myeslib.example;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.myeslib.example.SampleDomain.InventoryDecreased;
import org.myeslib.example.SampleDomain.InventoryIncreased;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleDomain.InventoryItemCreated;

@RunWith(MockitoJUnitRunner.class)
public class InventoryItemAggregateRootTest {

    @Test
	public void created() {
		
		InventoryItemAggregateRoot aggregateRoot = new InventoryItemAggregateRoot();
		
		UUID id = UUID.randomUUID();
		String desc = "item1";
		
		InventoryItemCreated event = new InventoryItemCreated(id, desc);
		
		aggregateRoot.on(event);
		
		assertThat(aggregateRoot.getId(), equalTo(id));
		assertThat(aggregateRoot.getDescription(), equalTo(desc));
		assertThat(aggregateRoot.getAvailable(), equalTo(0));
		
	}
	
	@Test
	public void increased() {
		
		InventoryItemAggregateRoot aggregateRoot = new InventoryItemAggregateRoot();
		
		UUID id = UUID.randomUUID();
		String desc = "item1";
		
		aggregateRoot.setAvailable(0);
		aggregateRoot.setDescription(desc);
		aggregateRoot.setId(id);
		
		InventoryIncreased event = new InventoryIncreased(id, 2);
		
		aggregateRoot.on(event);
		
		assertThat(aggregateRoot.getAvailable(), equalTo(2));
		
	}
	
	@Test
	public void decreased() {
		
		InventoryItemAggregateRoot aggregateRoot = new InventoryItemAggregateRoot();
		
		UUID id = UUID.randomUUID();
		String desc = "item1";
		
		aggregateRoot.setAvailable(5);
		aggregateRoot.setDescription(desc);
		aggregateRoot.setId(id);
		
		InventoryDecreased event = new InventoryDecreased(id, 2);
		
		aggregateRoot.on(event);
		
		assertThat(aggregateRoot.getAvailable(), equalTo(3));
		
	}
	

}
