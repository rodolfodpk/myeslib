package org.myeslib.util.camel.example.dataset;

import java.util.List;
import java.util.UUID;

import org.apache.camel.component.dataset.DataSetSupport;
import org.myeslib.example.SampleDomain.DecreaseInventory;

public class DecreaseCommandDataSet extends DataSetSupport {

	public final List<UUID> ids;
	
	public DecreaseCommandDataSet(List<UUID> ids, int howManyAggregates) {
		this.ids = ids;
		setSize(howManyAggregates);
		setReportCount(Math.min(100, howManyAggregates));
		for (int i=0; i< howManyAggregates; i++){
			ids.add(UUID.randomUUID());
		}
	}

	@Override
	protected Object createMessageBody(long messageIndex) {
		UUID id = ids.get((int) messageIndex);
		DecreaseInventory command = new DecreaseInventory(id, 1);
		return command;
	}
	
}