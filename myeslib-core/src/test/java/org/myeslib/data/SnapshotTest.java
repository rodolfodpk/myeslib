package org.myeslib.data;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Type;

import org.junit.Test;
import org.myeslib.core.data.Snapshot;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleDomainGsonFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class SnapshotTest {

	Gson gson = new SampleDomainGsonFactory().create();
	
	@Test
	public void serialize() {
		
		InventoryItemAggregateRoot ar = new InventoryItemAggregateRoot();
		ar.setAvaliable(1);
		
		Snapshot<InventoryItemAggregateRoot> s = new Snapshot<>(ar, 1L);
		
		String asString = gson.toJson(s) ;
		
		System.out.println(asString);
	
		Type snapshotInventoryItemType = new TypeToken<Snapshot<InventoryItemAggregateRoot>>() {}.getType();
			
		Snapshot<InventoryItemAggregateRoot> s2 = gson.fromJson(asString, snapshotInventoryItemType);
		
		assertEquals(s, s2);
		
	}

}
