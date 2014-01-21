package org.myeslib.example.infra;

import javax.sql.DataSource;

import lombok.Getter;

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

public class HazelcastConfigFactory {
	
	@Getter private final Config config;
	private final DataSource ds;
	private final Gson gson;

	public HazelcastConfigFactory(DataSource ds, Gson gson) {
		config = new Config();
		this.ds = ds;
		this.gson = gson;
		configPersistence();
		configSerialization();
	}

	private void configPersistence() {
		
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
		
	}

private void configSerialization() {
		
		SerializerConfig sc1 = new SerializerConfig().setImplementation(new GsonSerializer(gson, 1, AggregateRootHistory.class)).setTypeClass(AggregateRootHistory.class);
		SerializerConfig sc2 = new SerializerConfig().setImplementation(new GsonSerializer(gson, 2, UnitOfWork.class)).setTypeClass(UnitOfWork.class);

		SerializerConfig sc3 = new SerializerConfig().setImplementation(new GsonSerializer(gson, 3, CreateInventoryItem.class)).setTypeClass(CreateInventoryItem.class);
		SerializerConfig sc4 = new SerializerConfig().setImplementation(new GsonSerializer(gson, 4, InventoryItemCreated.class)).setTypeClass(InventoryItemCreated.class);
		
		SerializerConfig sc5 = new SerializerConfig().setImplementation(new GsonSerializer(gson, 5, IncreaseInventory.class)).setTypeClass(IncreaseInventory.class);
		SerializerConfig sc6 = new SerializerConfig().setImplementation(new GsonSerializer(gson, 6, InventoryIncreased.class)).setTypeClass(InventoryIncreased.class);

		SerializerConfig sc7 = new SerializerConfig().setImplementation(new GsonSerializer(gson, 7, DecreaseInventory.class)).setTypeClass(DecreaseInventory.class);
		SerializerConfig sc8 = new SerializerConfig().setImplementation(new GsonSerializer(gson, 8, InventoryDecreased.class)).setTypeClass(InventoryDecreased.class);

		config.getSerializationConfig().addSerializerConfig(sc1);
		config.getSerializationConfig().addSerializerConfig(sc2);
		config.getSerializationConfig().addSerializerConfig(sc3);
		config.getSerializationConfig().addSerializerConfig(sc4);
		config.getSerializationConfig().addSerializerConfig(sc5);
		config.getSerializationConfig().addSerializerConfig(sc6);
		config.getSerializationConfig().addSerializerConfig(sc7);
		config.getSerializationConfig().addSerializerConfig(sc8);
		
	}

}
