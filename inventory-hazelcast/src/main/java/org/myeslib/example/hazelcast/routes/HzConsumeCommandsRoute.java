package org.myeslib.example.hazelcast.routes;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

import com.google.inject.name.Named;
import com.hazelcast.core.IQueue;

public class HzConsumeCommandsRoute extends RouteBuilder {

	final String originUri;
	final HzInventoryItemCmdProcessor inventoryItemCmdProcessor;
    final IQueue<UUID> eventsQueue;

	@Inject
	public HzConsumeCommandsRoute(
			@Named("originUri") String originUri,
			HzInventoryItemCmdProcessor inventoryItemCmdProcessor,
            IQueue<UUID> eventsQueue) {

		this.originUri = originUri;
		this.inventoryItemCmdProcessor = inventoryItemCmdProcessor;
        this.eventsQueue = eventsQueue;
	}
	
	@Override
	public void configure() throws Exception {

//		errorHandler(deadLetterChannel("direct:dead-letter-channel")
//			    .maximumRedeliveries(3).redeliveryDelay(5000));
		
         from(originUri)
			 .routeId("handle-inventory-item-command")
			 .setHeader("id", simple("${body.getId()}"))	    
	         .process(inventoryItemCmdProcessor) 
	      	 .wireTap("direct:enqueueId")
	      			.newExchangeBody(header("id"))
	      	 .end()		
	         ;

         from("direct:enqueueId")
            .routeId("enqueueId")
            .process(new Processor() {
                @Override
                public void process(Exchange e) throws Exception {
                    if (!eventsQueue.offer(body().evaluate(e, UUID.class), 100, TimeUnit.MILLISECONDS)){
                        log.error("error while enqueuing {}", body().evaluate(e, UUID.class));
                    }
                }
            })
            ;

	     from("direct:dead-letter-channel")
	      	.routeId("direct:dead-letter-channel")
	         .log("error !!");

	}
	
}