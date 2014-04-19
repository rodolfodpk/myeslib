package org.myeslib.cmdproducer.datasets;

import java.util.List;
import java.util.UUID;

import org.apache.camel.component.dataset.DataSetSupport;
import org.myeslib.example.SampleDomain.CreateInventoryItem;

public class CreateCommandDataSet extends DataSetSupport {

	private final List<UUID> ids ;
	
	public CreateCommandDataSet(List<UUID> ids, int howManyAggregates) {
		this.ids = ids;
		setSize(howManyAggregates);
		setReportCount(Math.min(100, howManyAggregates));
	}

	@Override
	protected Object createMessageBody(long messageIndex) {
		UUID id = ids.get((int) messageIndex);
		CreateInventoryItem command = new CreateInventoryItem(UUID.randomUUID(), id);
		return command;
	}
	
}