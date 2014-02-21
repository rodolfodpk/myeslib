package org.myeslib.example.infra;

import java.util.Set;

import lombok.Getter;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.SerializerConfig;

public class HazelcastConfigFactory {

	@Getter final Config config;
	
	public HazelcastConfigFactory(MapConfig inventoryMapConfig, Set<SerializerConfig> serializers) {
		
		this.config = new Config();
		
		config.addMapConfig(inventoryMapConfig);
		
		//config.setUserContext(userContext);
		
		for (SerializerConfig sc : serializers) {
			config.getSerializationConfig().addSerializerConfig(sc); 
		}
	}

}
