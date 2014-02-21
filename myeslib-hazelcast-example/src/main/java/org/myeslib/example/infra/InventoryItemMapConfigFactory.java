package org.myeslib.example.infra;

import org.myeslib.util.hazelcast.HzMapStore;

import com.google.inject.Inject;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapStoreConfig;

public class InventoryItemMapConfigFactory {
	
	final HzMapStore mapStore;
	
	@Inject
	public InventoryItemMapConfigFactory(HzMapStore mapStore) {
		this.mapStore = mapStore;
	}

	public MapConfig create() {
		
		MapConfig mapConfig = new MapConfig();
		mapConfig.setName(HazelcastData.INVENTORY_ITEM_AGGREGATE_HISTORY.name());
		mapConfig.setInMemoryFormat(MapConfig.InMemoryFormat.OBJECT);
	
		
		MapStoreConfig mapStoreConfig = new MapStoreConfig();

		//HzStringMapStore store = new HzStringMapStore(ds, "payment_aggregate_root");
		//HzStringMapStore store = new HzStringMapStore(ds, HazelcastData.INVENTORY_ITEM_AGGREGATE_HISTORY.name());
		//store.createTableForMap();

		mapStoreConfig.setImplementation(mapStore);
		mapStoreConfig.setEnabled(true);
		mapStoreConfig.setWriteDelaySeconds(0);
		mapConfig.setMapStoreConfig(mapStoreConfig);
		
		return mapConfig;
	}

}
