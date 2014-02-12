package org.myeslib.util.camel.example.dataset;

import java.util.List;
import java.util.UUID;

import org.apache.camel.component.dataset.DataSetSupport;
import org.myeslib.example.SampleDomain.IncreaseInventory;

public class IncreaseCommandDataSet extends DataSetSupport {

	public final List<UUID> ids ;
	
	public IncreaseCommandDataSet(List<UUID> ids, int howManyAggregates) {
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
		IncreaseInventory command = new IncreaseInventory(id, 2);
		return command;
	}
	
}