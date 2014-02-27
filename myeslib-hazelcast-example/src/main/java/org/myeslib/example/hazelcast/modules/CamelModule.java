package org.myeslib.example.hazelcast.modules;

import javax.inject.Singleton;

import org.myeslib.example.hazelcast.infra.HazelcastData;
import org.myeslib.example.hazelcast.routes.HzConsumeEventsRoute;
import org.myeslib.example.util.camel.ReceiveCommandsAsJsonRoute;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;

public class CamelModule extends AbstractModule {
	
	@Provides
	@Singleton
	@Named("originUri")
	public String originUri() {
		return "direct:processCommand";
		//return "hz:seda:inventory-item-command?transacted=true&concurrentConsumers=10";
	}

	@Provides
	@Singleton
	@Named("eventsDestinationUri")
	public String destinationUri() {
		return String.format("hz:seda:%s", HazelcastData.INVENTORY_ITEM_EVENTS_QUEUE.name());
	}


	@Provides
	@Singleton
	public ReceiveCommandsAsJsonRoute receiveCommandsRoute(@Named("originUri") String originUri, Gson gson) {
		return new ReceiveCommandsAsJsonRoute("jetty:http://localhost:8080/inventory-item-command?minThreads=5&maxThreads=10", originUri, gson);
	}
	
	@Override
	protected void configure() {
		
		bind(HzConsumeEventsRoute.class);
	}
}


