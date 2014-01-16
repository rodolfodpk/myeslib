package org.myeslib.example.infra;

import javax.sql.DataSource;

import org.myeslib.data.AggregateRootHistory;
import org.myeslib.data.UnitOfWork;
import org.myeslib.example.SampleCoreDomain.CreateInventoryItem;
import org.myeslib.example.SampleCoreDomain.DecreaseInventory;
import org.myeslib.example.SampleCoreDomain.IncreaseInventory;
import org.myeslib.example.SampleCoreDomain.InventoryDecreased;
import org.myeslib.example.SampleCoreDomain.InventoryIncreased;
import org.myeslib.example.SampleCoreDomain.InventoryItemCreated;
import org.myeslib.hazelcast.AggregateRootHistoryMapStore;
import org.myeslib.hazelcast.GsonSerializer;

import com.google.gson.Gson;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapStoreConfig;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastFactory {
	
	// TODO entryProcessor with ServiceLocator or http://square.github.io/dagger/ ?

	private final HazelcastInstance hazelcastInstance;
	private final DataSource ds;
	private final Gson gson;

	public HazelcastFactory(DataSource ds, Gson gson) {
		this.ds = ds;
		this.gson = gson;
		this.hazelcastInstance = Hazelcast.newHazelcastInstance(config());
	}

	private Config config() {
		
		Config config = new Config();
		
		MapConfig mapConfig = new MapConfig();
		mapConfig.setName(HazelcastMaps.INVENTORY_ITEM_AGGREGATE_HISTORY.name());
		mapConfig.setInMemoryFormat(MapConfig.InMemoryFormat.OBJECT);

		MapStoreConfig mapStoreConfig = new MapStoreConfig();
		AggregateRootHistoryMapStore store = new AggregateRootHistoryMapStore(ds, HazelcastMaps.INVENTORY_ITEM_AGGREGATE_HISTORY.name(), gson, true);
		mapStoreConfig.setImplementation(store);
		mapStoreConfig.setEnabled(true);
		mapStoreConfig.setWriteDelaySeconds(0);
		
		mapConfig.setMapStoreConfig(mapStoreConfig);
		config.addMapConfig(mapConfig);

		gson(config);
		
		return config;
	}

private void gson(Config config) {
		
		SerializerConfig sc1 = new SerializerConfig().setImplementation(new GsonSerializer(gson, 1, AggregateRootHistory.class)).setTypeClass(AggregateRootHistory.class);
		SerializerConfig sc2 = new SerializerConfig().setImplementation(new GsonSerializer(gson, 2, UnitOfWork.class)).setTypeClass(UnitOfWork.class);

		SerializerConfig sc3 = new SerializerConfig().setImplementation(new GsonSerializer(gson, 3, CreateInventoryItem.class)).setTypeClass(CreateInventoryItem.class);
		SerializerConfig sc4 = new SerializerConfig().setImplementation(new GsonSerializer(gson, 4, InventoryItemCreated.class)).setTypeClass(InventoryItemCreated.class);
		
		SerializerConfig sc5 = new SerializerConfig().setImplementation(new GsonSerializer(gson, 5, IncreaseInventory.class)).setTypeClass(IncreaseInventory.class);
		SerializerConfig sc6 = new SerializerConfig().setImplementation(new GsonSerializer(gson, 6, InventoryIncreased.class)).setTypeClass(InventoryIncreased.class);

		SerializerConfig sc7 = new SerializerConfig().setImplementation(new GsonSerializer(gson, 5, DecreaseInventory.class)).setTypeClass(DecreaseInventory.class);
		SerializerConfig sc8 = new SerializerConfig().setImplementation(new GsonSerializer(gson, 6, InventoryDecreased.class)).setTypeClass(InventoryDecreased.class);

		config.getSerializationConfig().addSerializerConfig(sc1);
		config.getSerializationConfig().addSerializerConfig(sc2);
		config.getSerializationConfig().addSerializerConfig(sc3);
		config.getSerializationConfig().addSerializerConfig(sc4);
		config.getSerializationConfig().addSerializerConfig(sc5);
		config.getSerializationConfig().addSerializerConfig(sc6);
		config.getSerializationConfig().addSerializerConfig(sc7);
		config.getSerializationConfig().addSerializerConfig(sc8);
		
	}

	public HazelcastInstance get() {
		return hazelcastInstance;
	}

}
