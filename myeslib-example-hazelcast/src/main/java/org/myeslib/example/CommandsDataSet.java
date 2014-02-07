package org.myeslib.example;

import java.util.UUID;

import org.apache.camel.component.dataset.DataSetSupport;
import org.myeslib.core.Command;
import org.myeslib.example.ExampleModule.ServiceJustForTest;
import org.myeslib.example.SampleDomain.CreateInventoryItem;
import org.myeslib.example.SampleDomain.DecreaseInventory;
import org.myeslib.example.SampleDomain.IncreaseInventory;

class CommandsDataSet extends DataSetSupport {

	final UUID id = UUID.randomUUID();
	
	CommandsDataSet() {
		setSize(1000);
		setReportCount(100);
	}

	@Override
	protected Object createMessageBody(long messageIndex) {

		boolean isEven = (messageIndex % 2 == 0);
		Command cmd = null ;
		
		if (messageIndex==0){
			CreateInventoryItem command = new CreateInventoryItem(id);
			command.setService(new ServiceJustForTest());
			cmd = command;
		} else {
			if (isEven){
				DecreaseInventory command = new DecreaseInventory(id, 1);
				cmd = command;
			} else {
				IncreaseInventory command = new IncreaseInventory(id, 2);
				cmd = command;
			}
		}

		return cmd;
		
	}
	
	
}