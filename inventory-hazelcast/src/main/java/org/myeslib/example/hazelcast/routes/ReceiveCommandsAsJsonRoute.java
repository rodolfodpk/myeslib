package org.myeslib.example.hazelcast.routes;

import lombok.RequiredArgsConstructor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.myeslib.core.Command;
import org.myeslib.util.gson.CommandFromStringFunction;

import com.google.inject.Inject;

@RequiredArgsConstructor(onConstructor=@__(@Inject))
public class ReceiveCommandsAsJsonRoute extends RouteBuilder {

    final String sourceUri;
    final String targetUri;
    final CommandFromStringFunction commandFromStringFunction;

    @Override
    public void configure() throws Exception {

        from(sourceUri)
            .streamCaching()
            .routeId("receive-commands-as-json")
            .process(new Processor() {
                @Override
                public void process(Exchange e) throws Exception {
                    String body = e.getIn().getBody(String.class);
                    e.getOut().setBody(commandFromStringFunction.apply(body), Command.class);
                }
            })
            .log("received id ${body.id} - targetVersion ${body.targetVersion}")
            .to(targetUri);

    }
}
