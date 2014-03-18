package org.myeslib.example.hazelcast.routes;

import javax.inject.Inject;

import org.apache.camel.builder.RouteBuilder;

import com.google.inject.name.Named;

public class HzConsumeCommandsRoute extends RouteBuilder {

	final String originUri;
	final String destinationUri;
	final InventoryItemCmdProcessor inventoryItemCmdProcessor;


	@Inject
	public HzConsumeCommandsRoute(
			@Named("originUri") String originUri,
			@Named("eventsDestinationUri") String destinationUri,
			InventoryItemCmdProcessor inventoryItemCmdProcessor) {

		this.originUri = originUri;
		this.destinationUri = destinationUri;
		this.inventoryItemCmdProcessor = inventoryItemCmdProcessor;
	}
	
	@Override
	public void configure() throws Exception {

//		errorHandler(deadLetterChannel("direct:dead-letter-channel")
//			    .maximumRedeliveries(3).redeliveryDelay(5000));
		
         from(originUri)
			 .routeId("handle-inventory-item-command")
			 .setHeader("id", simple("${body.getId()}"))	    
	         .process(inventoryItemCmdProcessor) 
	      	 .wireTap(destinationUri)
	      			.newExchangeBody(header("id"))
	      	 .end()		
	         ;
       
	     from("direct:dead-letter-channel")
	      	.routeId("direct:dead-letter-channel")
	         .log("error !!");

	}
	
}