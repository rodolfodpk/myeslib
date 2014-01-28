package org.myeslib.example.infra;

import javax.sql.DataSource;

import org.myeslib.hazelcast.HzStringMapStore;

import com.google.inject.Inject;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapStoreConfig;

public class InventoryItemMapConfigFactory {
	
	private final DataSource ds;
	
	@Inject
	public InventoryItemMapConfigFactory(DataSource ds) {
		this.ds = ds;
	}

	public MapConfig create() {
		
		MapConfig mapConfig = new MapConfig();
		mapConfig.setName(HazelcastMaps.INVENTORY_ITEM_AGGREGATE_HISTORY.name());
		mapConfig.setInMemoryFormat(MapConfig.InMemoryFormat.OBJECT);
		MapStoreConfig mapStoreConfig = new MapStoreConfig();
		HzStringMapStore store = new HzStringMapStore(ds, HazelcastMaps.INVENTORY_ITEM_AGGREGATE_HISTORY.name());
		store.createTableForMap();
		mapStoreConfig.setImplementation(store);
		mapStoreConfig.setEnabled(true);
		mapStoreConfig.setWriteDelaySeconds(0);
		mapConfig.setMapStoreConfig(mapStoreConfig);
		
		return mapConfig;
	}

}
