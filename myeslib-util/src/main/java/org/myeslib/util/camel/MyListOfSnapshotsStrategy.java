package org.myeslib.util.camel;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AbstractListAggregationStrategy;
import org.myeslib.core.data.Snapshot;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;

public class MyListOfSnapshotsStrategy extends AbstractListAggregationStrategy<Snapshot<InventoryItemAggregateRoot>> {
    @SuppressWarnings("unchecked")
	@Override
    public Snapshot<InventoryItemAggregateRoot> getValue(Exchange exchange) {
        return exchange.getIn().getBody(Snapshot.class);
    }
}