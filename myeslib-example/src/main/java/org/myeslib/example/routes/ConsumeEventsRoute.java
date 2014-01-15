package org.myeslib.example.routes;

import java.util.UUID;

import lombok.AllArgsConstructor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.UseLatestAggregationStrategy;
import org.myeslib.data.Snapshot;
import org.myeslib.example.SampleCoreDomain.InventoryItemAggregateRoot;
import org.myeslib.example.infra.HazelcastMaps;
import org.myeslib.hazelcast.AggregateRootHistoryMapFactory;
import org.myeslib.hazelcast.AggregateRootHistoryTxMapFactory;
import org.myeslib.hazelcast.AggregateRootSnapshotMapFactory;
import org.myeslib.util.KeyValueSnapshotReader;

import com.hazelcast.core.HazelcastInstance;

@AllArgsConstructor
public class ConsumeEventsRoute extends RouteBuilder {
	
	final HazelcastInstance hz ;
	final AggregateRootHistoryTxMapFactory<UUID, InventoryItemAggregateRoot> txMapFactory;
	final AggregateRootHistoryMapFactory<UUID, InventoryItemAggregateRoot> mapFactory;
	final AggregateRootSnapshotMapFactory<UUID, InventoryItemAggregateRoot> snapshotMapFactory;
	
	@Override
	public void configure() throws Exception {

      from("direct:eventListener")
        .process(new Processor() {
			@Override
			public void process(Exchange e) throws Exception {
				
				UUID id = e.getIn().getBody(UUID.class);

				KeyValueSnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader = 
						new KeyValueSnapshotReader<>(mapFactory.get(HazelcastMaps.INVENTORY_ITEM_AGGREGATE_HISTORY.name()), 
													 snapshotMapFactory.get(HazelcastMaps.INVENTORY_ITEM_LAST_SNAPSHOT.name()));
				
				Snapshot<InventoryItemAggregateRoot> snapshot = snapshotReader.get(id, new InventoryItemAggregateRoot());
				
				InventoryItemAggregateRoot aggregateInstance = snapshot.getAggregateInstance();

				e.getOut().setBody(aggregateInstance);
			}
		})
        .multicast(new UseLatestAggregationStrategy())
        .to("direct:reflect-last-snapshot", "direct:reflect-query-model");
      
      from("direct:reflect-last-snapshot")
        .log("todo");
      
      from("direct:reflect-query-model")
        .log("todo");
      
	}
	
}
