package org.myeslib.example.hazelcast.routes;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import lombok.RequiredArgsConstructor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

import com.hazelcast.core.IQueue;

@RequiredArgsConstructor(onConstructor=@__(@Inject))
public class HzConsumeCommandsRoute extends RouteBuilder {

	final String commandsDestinationUri;
	final HzInventoryItemCmdProcessor inventoryItemCmdProcessor;
    final IQueue<UUID> eventsQueue;
	
	@Override
	public void configure() throws Exception {

         from(commandsDestinationUri)
			 .routeId("handle-inventory-item-command")
			 .setHeader("id", simple("${body.getId()}"))	    
	         .process(inventoryItemCmdProcessor) 
	      	 .wireTap("direct:enqueueId")
	      			.newExchangeBody(header("id"))
	      	 .end()
	      	 .setBody(constant(null)) 
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

	}
	
}