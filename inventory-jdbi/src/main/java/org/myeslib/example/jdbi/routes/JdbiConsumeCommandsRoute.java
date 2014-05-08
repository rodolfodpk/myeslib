package org.myeslib.example.jdbi.routes;

import javax.inject.Inject;

import org.apache.camel.builder.RouteBuilder;

import com.google.inject.name.Named;

public class JdbiConsumeCommandsRoute extends RouteBuilder {

    final String commandsDestinationUri;
    final InventoryItemCmdProcessor inventoryItemCmdProcessor;

    @Inject
    public JdbiConsumeCommandsRoute(@Named("commandsDestinationUri") String commandsDestinationUri,
            InventoryItemCmdProcessor inventoryItemCmdProcessor) {
        this.commandsDestinationUri = commandsDestinationUri;
        this.inventoryItemCmdProcessor = inventoryItemCmdProcessor;
    }

    @Override
    public void configure() throws Exception {

        // errorHandler(deadLetterChannel("direct:dead-letter-channel")
        // .maximumRedeliveries(3).redeliveryDelay(5000));
        
        // TODO idempotent repo for commands

        from(commandsDestinationUri)
            .routeId("handle-inventory-item-command")
            .setHeader("id", simple("${body.getId()}"))
            .process(inventoryItemCmdProcessor);

        from("direct:dead-letter-channel")
            .routeId("direct:dead-letter-channel")
            .log("error !!");

    }

}