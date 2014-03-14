package org.myeslib.example.jdbi.modules;

import javax.inject.Singleton;

import org.myeslib.util.hazelcast.HzCamelComponent;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastModule extends AbstractModule {

	@Provides
	@Singleton
	public HazelcastInstance hazelcastInstance(Config config) {
		return Hazelcast.newHazelcastInstance(config);
	}

	@Override
	protected void configure() {
		bind(HzCamelComponent.class);
	}
	
}


