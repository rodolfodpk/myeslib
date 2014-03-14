package org.myeslib.example.jdbi.modules;

import javax.inject.Singleton;

import org.myeslib.example.jdbi.routes.JdbiConsumeEventsRoute;
import org.myeslib.util.example.ReceiveCommandsAsJsonRoute;
import org.myeslib.util.hazelcast.HzJobLocker;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.hazelcast.core.HazelcastInstance;

public class CamelModule extends AbstractModule {

	@Provides
	@Singleton
	@Named("originUri")
	public String originUri() {
		return "direct:processCommand";
	}

	@Provides
	@Singleton
	public ReceiveCommandsAsJsonRoute receiveCommandsRoute(@Named("originUri") String originUri, Gson gson) {
		return new ReceiveCommandsAsJsonRoute("jetty:http://localhost:8080/inventory-item-command?minThreads=10&maxThreads=100", originUri, gson);
	}
	
	@Provides
	@Singleton
	public HzJobLocker jobLocker(HazelcastInstance hzInstance) {
		return new HzJobLocker(hzInstance, "consumeEventsJob", "10s");
	}
	
	@Override
	protected void configure() {
		bind(JdbiConsumeEventsRoute.class);
	}
	
}


