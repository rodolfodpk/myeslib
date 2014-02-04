package org.myeslib.example.routes;

import java.util.UUID;

import lombok.AllArgsConstructor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.UseLatestAggregationStrategy;
import org.myeslib.data.Snapshot;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.hazelcast.HzSnapshotReader;

@AllArgsConstructor
public class ConsumeEventsRoute extends RouteBuilder {
	
	final HzSnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader;
	
	@Override
	public void configure() throws Exception {

      from("direct:eventListener")
        .process(new Processor() {
			@Override
			public void process(Exchange e) throws Exception {
				UUID id = e.getIn().getBody(UUID.class);
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
