package org.myeslib.example.hazelcast.modules;

import java.util.UUID;

import javax.inject.Singleton;

import lombok.AllArgsConstructor;

import org.myeslib.core.data.Snapshot;
import org.myeslib.core.storage.SnapshotReader;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.hazelcast.infra.HazelcastData;
import org.myeslib.example.hazelcast.routes.HzConsumeEventsRoute;
import org.myeslib.example.hazelcast.routes.ReceiveCommandsAsJsonRoute;
import org.myeslib.util.gson.CommandFromStringFunction;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;

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
    public ReceiveCommandsAsJsonRoute receiveCommandsRoute(@Named("originUri") String originUri, CommandFromStringFunction commandFromStringFunction) {
        String url = String.format("jetty:http://localhost:8080/inventory-item-command?minThreads=%d&maxThreads=%d", jettyMinThreads, jettyMaxThreads);
        return new ReceiveCommandsAsJsonRoute(url, originUri, commandFromStringFunction);
    }
	   
    @Provides
	@Singleton
	public HzConsumeEventsRoute hzConsumeEventsRoute(SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader,
                                                     IMap<UUID, Snapshot<InventoryItemAggregateRoot>> lastSnapshotMap,
                                                     IQueue<UUID> eventsQueue) {
		return new HzConsumeEventsRoute(eventsQueueConsumers, snapshotReader, lastSnapshotMap, eventsQueue);
	}
	
	@Override
	protected void configure() {
	}
}


