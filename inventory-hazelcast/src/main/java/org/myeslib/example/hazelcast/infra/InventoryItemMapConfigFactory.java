package org.myeslib.example.hazelcast.infra;

import org.myeslib.util.hazelcast.HzMapStore;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapStoreConfig;

public class InventoryItemMapConfigFactory {
	
    @Inject
	public InventoryItemMapConfigFactory(HzMapStore mapStore, @Named("writeDelaySeconds") int writeDelaySeconds) {
        this.mapStore = mapStore;
        this.writeDelaySeconds = writeDelaySeconds;
    }

    final HzMapStore mapStore;
	final int writeDelaySeconds;

	public MapConfig create() {
		
		MapConfig mapConfig = new MapConfig();
		mapConfig.setName(HazelcastData.INVENTORY_ITEM_AGGREGATE_HISTORY.name());
		mapConfig.setInMemoryFormat(MapConfig.InMemoryFormat.OBJECT);
		
		MapStoreConfig mapStoreConfig = new MapStoreConfig();

		mapStoreConfig.setImplementation(mapStore);
		mapStoreConfig.setEnabled(true);
		mapStoreConfig.setWriteDelaySeconds(writeDelaySeconds); 
		/* writeDelaySeconds > 0 means write-behind. TODO make sure this is consistent*/
		mapConfig.setMapStoreConfig(mapStoreConfig);
		
		return mapConfig;
	}

}
