package org.myeslib.example.hazelcast.infra;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.myeslib.core.AggregateRoot;
import org.myeslib.core.data.AggregateRootHistory;
import org.myeslib.core.data.Snapshot;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.example.SampleDomain.CreateInventoryItem;
import org.myeslib.example.SampleDomain.DecreaseInventory;
import org.myeslib.example.SampleDomain.IncreaseInventory;
import org.myeslib.example.SampleDomain.InventoryDecreased;
import org.myeslib.example.SampleDomain.InventoryIncreased;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleDomain.InventoryItemCreated;
import org.myeslib.util.hazelcast.HzGsonSerializer;
import org.myeslib.util.hazelcast.HzGsonTypedSerializer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.hazelcast.config.SerializerConfig;

public class InventoryItemSerializersConfigFactory {

	private final Gson gson;

	final List<Class<? extends Serializable>> classes = Arrays.asList(AggregateRootHistory.class, UnitOfWork.class, 
			AggregateRoot.class,
			InventoryItemAggregateRoot.class,
			CreateInventoryItem.class, InventoryItemCreated.class, 
			IncreaseInventory.class, InventoryIncreased.class,
			DecreaseInventory.class, InventoryDecreased.class
			);
	

	@Inject
	public InventoryItemSerializersConfigFactory(Gson gson) {
		this.gson = gson;
	}

	public Set<SerializerConfig> create() {
		Set<SerializerConfig> set = new HashSet<>();
		int id = 0;
		for (Class<?> c : classes) {
			id = id + 1;
			set.add(new SerializerConfig().setImplementation(new HzGsonSerializer(gson, id, c)).setTypeClass(c));
		}
		setupCustom(set, id);
		return set;
	}
	
	private void setupCustom(Set<SerializerConfig> set, int avaliableIdIndex) {
		
		 Type snapshotInventoryItemType = new TypeToken<Snapshot<InventoryItemAggregateRoot>>() {}.getType();
		
		 // so just one Snapshot serializer cfg in hazelcast ? TODO
		set.add(new SerializerConfig().setImplementation(new HzGsonTypedSerializer(gson, avaliableIdIndex+=1, snapshotInventoryItemType)).setTypeClass(Snapshot.class));

	}
}
