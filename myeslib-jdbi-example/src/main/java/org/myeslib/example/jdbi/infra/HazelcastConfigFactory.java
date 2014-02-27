package org.myeslib.example.jdbi.infra;

import java.util.concurrent.ConcurrentMap;

import lombok.Getter;

import com.google.inject.Inject;
import com.hazelcast.config.Config;

public class HazelcastConfigFactory {

	@Getter final Config config;
	
	@Inject
	public HazelcastConfigFactory(ConcurrentMap<String, Object> userContext) {
		
		this.config = new Config();
		
		config.setUserContext(userContext);
		
	}

}
