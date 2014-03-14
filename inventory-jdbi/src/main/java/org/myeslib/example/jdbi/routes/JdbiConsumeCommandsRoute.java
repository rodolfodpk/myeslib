package org.myeslib.example.jdbi.routes;

import javax.inject.Inject;

import org.apache.camel.builder.RouteBuilder;

import com.google.inject.name.Named;

public class JdbiConsumeCommandsRoute extends RouteBuilder {

	final String originUri;
	final InventoryItemCmdProcessor inventoryItemCmdProcessor;
	
	@Inject
	public JdbiConsumeCommandsRoute(@Named("originUri") String originUri, InventoryItemCmdProcessor inventoryItemCmdProcessor) {
		this.originUri = originUri;
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
	      	  ;
       
	     from("direct:dead-letter-channel")
	      	.routeId("direct:dead-letter-channel")
	         .log("error !!");

	}

}