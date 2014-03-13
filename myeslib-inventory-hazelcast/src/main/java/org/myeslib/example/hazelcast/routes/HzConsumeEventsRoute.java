package org.myeslib.example.hazelcast.routes;

import java.util.UUID;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.hazelcast.HazelcastConstants;
import org.apache.camel.processor.aggregate.AbstractListAggregationStrategy;
import org.apache.camel.processor.aggregate.UseLatestAggregationStrategy;
import org.apache.camel.util.toolbox.AggregationStrategies;
import org.myeslib.core.data.Snapshot;
import org.myeslib.core.storage.SnapshotReader;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.hazelcast.infra.HazelcastData;

import com.google.inject.Inject;

public class HzConsumeEventsRoute extends RouteBuilder {

	final SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader;
	
	@Inject
	public HzConsumeEventsRoute(
			SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader) {
		this.snapshotReader = snapshotReader;
	}

	@Override
	public void configure() throws Exception {

      fromF("hz:seda:%s?transacted=true&concurrentConsumers=10", HazelcastData.INVENTORY_ITEM_EVENTS_QUEUE.name())
        .routeId("seda:eventsQueue")
        .log("received ${body}")
        .process(new Processor() {
			@Override
			public void process(Exchange e) throws Exception {
				UUID id = e.getIn().getBody(UUID.class);
				Snapshot<InventoryItemAggregateRoot> snapshot = snapshotReader.get(id);
				e.getOut().setHeader("id", id);
				e.getOut().setBody(snapshot);
			}
		})
        //.log("produced ${body}")
        .multicast(new UseLatestAggregationStrategy())
        .to("direct:reflect-last-snapshot", "direct:reflect-query-model")
        .aggregate(header("id"), AggregationStrategies.useLatest()).completionSize(3) // 3 commands per aggregate
        .aggregate(constant(true), new MyListOfSnapshotsStrategy()).completionInterval(60000) // print result every 1 minute
        .split(body()) 
		.log("*** resulting snapshot after all commands: ${body}");
      
      from("direct:reflect-last-snapshot")
         .routeId("direct:reflect-last-snapshot")
 	 	 .setHeader(HazelcastConstants.OPERATION, constant(HazelcastConstants.PUT_OPERATION))
 	 	 .setHeader(HazelcastConstants.OBJECT_ID, header("id"))
 	 	 .toF("hz:%s%s", HazelcastConstants.MAP_PREFIX, HazelcastData.INVENTORY_ITEM_LAST_SNAPSHOT.name());
      
      from("direct:reflect-query-model")
      	.routeId("direct:reflect-query-model")
        .process(new Processor() {
			@Override
			public void process(Exchange exchange) throws Exception {
			 // do nothing 
			}
		});
      
	}
	
}


class MyListOfSnapshotsStrategy extends AbstractListAggregationStrategy<Snapshot<InventoryItemAggregateRoot>> {
    @SuppressWarnings("unchecked")
	@Override
    public Snapshot<InventoryItemAggregateRoot> getValue(Exchange exchange) {
        return exchange.getIn().getBody(Snapshot.class);
    }
}