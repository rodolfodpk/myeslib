package org.myeslib.util.camel.example.dataset;

import org.apache.camel.builder.RouteBuilder;

public class DatasetsRoute extends RouteBuilder {

	@Override
	public void configure() throws Exception {

		 from("dataset:createCommandDataset?initialDelay=10000")
		 	.routeId("dataset:createCommandsDataset")
	        .to("hz:seda:inventory-item-command?transacted=true");

		 from("dataset:increaseCommandDataset?initialDelay=20000")
		 	.routeId("dataset:increaseCommandsDataset")
	        .to("hz:seda:inventory-item-command?transacted=true");

		 from("dataset:decreaseCommandDataset?initialDelay=30000")
		 	.routeId("dataset:decreaseCommandsDataset")
	        .to("hz:seda:inventory-item-command?transacted=true");

	}

}