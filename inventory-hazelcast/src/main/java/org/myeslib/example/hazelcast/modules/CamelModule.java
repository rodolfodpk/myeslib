package org.myeslib.example.hazelcast.modules;

import java.util.UUID;

import javax.inject.Singleton;

import lombok.AllArgsConstructor;

import org.myeslib.core.storage.SnapshotReader;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.hazelcast.infra.HazelcastData;
import org.myeslib.example.hazelcast.routes.HzConsumeEventsRoute;
import org.myeslib.util.example.ReceiveCommandsAsJsonRoute;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;

@AllArgsConstructor
public class CamelModule extends AbstractModule {
	
	int jettyMinThreads;
	int jettyMaxThreads;
	int eventsQueueConsumers;
	
	@Provides
	@Singleton
	@Named("originUri")
	public String originUri() {
		return "direct:processCommand";
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
		String url = String.format("jetty:http://localhost:8080/inventory-item-command?minThreads=%d&maxThreads=%d", jettyMinThreads, jettyMaxThreads);
		return new ReceiveCommandsAsJsonRoute(url, originUri, gson);
	}
	
	
	@Provides
	@Singleton
	public HzConsumeEventsRoute hzConsumeEventsRoute(SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader) {
		return new HzConsumeEventsRoute(eventsQueueConsumers, snapshotReader);
	}
	
	@Override
	protected void configure() {
	}
}


