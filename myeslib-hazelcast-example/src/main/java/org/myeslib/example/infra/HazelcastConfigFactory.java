package org.myeslib.example.infra;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import lombok.Getter;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.SerializerConfig;

public class HazelcastConfigFactory {

	@Getter final Config config;
	
	public HazelcastConfigFactory(MapConfig inventoryMapConfig, Set<SerializerConfig> serializers, ConcurrentMap<String, Object> userContext) {
		
		this.config = new Config();
		
		config.addMapConfig(inventoryMapConfig);
		
		config.setUserContext(userContext);
		
		/** since now core impls are storing strings on map and handling gson serialization itself, this is not 
		 necessary anymore
		*/
//		for (SerializerConfig sc : serializers) {
//			config.getSerializationConfig().addSerializerConfig(sc); 
//		}
	}

}
