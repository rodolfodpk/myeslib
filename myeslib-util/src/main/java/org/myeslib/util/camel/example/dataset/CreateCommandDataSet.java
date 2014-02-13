package org.myeslib.util.camel.example.dataset;

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
		CreateInventoryItem command = new CreateInventoryItem(id, 0L, null);
		// command.setService(new ServiceJustForTest()); // doen't work since this service is transient and command will be stored within queue
		return command;
	}
	
}