package org.myeslib.example.jdbi.routes;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.myeslib.core.Command;
import org.myeslib.util.gson.CommandFromStringFunction;

import com.google.inject.Inject;

public class ReceiveCommandsAsJsonRoute  extends RouteBuilder{

    @Inject
    public ReceiveCommandsAsJsonRoute(String startUri, String endUri,
            CommandFromStringFunction commandFromStringFunction) {
        this.startUri = startUri;
        this.endUri = endUri;
        this.commandFromStringFunction = commandFromStringFunction;
    }

    final String startUri;
    final String endUri;
    final CommandFromStringFunction commandFromStringFunction;

    @Override
    public void configure() throws Exception {

        from(startUri)
                .streamCaching()
                .routeId("receive-commands-as-json")
                .process(new Processor() {
                    @Override
                    public void process(Exchange e) throws Exception {
                        String body = e.getIn().getBody(String.class);
                        e.getOut().setBody(commandFromStringFunction.apply(body), Command.class);
                    }
                })
                .log("received ${header.commandType} - id ${body.id} - targetVersion ${body.targetVersion}")
                .to(endUri);

	}

}
