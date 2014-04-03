package org.myeslib.cmdproducer.routes;

import java.lang.reflect.Type;

import lombok.AllArgsConstructor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.util.toolbox.AggregationStrategies;
import org.myeslib.core.Command;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@AllArgsConstructor
public class CommandsDataSetsRoute extends RouteBuilder {

	final Gson gson;
	final String targetEndpoint;
	final Type commandType = new TypeToken<Command>() {}.getType();

	final int datasetSize;
	final int delayBetweenDatasets;
	final int initialDelay;

	public void configure() throws Exception {

		fromF("dataset:createCommandDataset?initialDelay=%d", initialDelay)
				.routeId("dataset:createCommandsDataset")
				.startupOrder(1).autoStartup(true)
				.process(new MarshalProcessor()).to(targetEndpoint)
                .aggregate(constant(0), AggregationStrategies.useLatest()).completionSize(datasetSize)
    			    .log("finished")
    			    .log("will start next dataset")
    			    .to("controlbus:route?routeId=dataset:increaseCommandsDataset&action=start")
				.end()
    			;

		fromF("dataset:increaseCommandDataset?initialDelay=%d", delayBetweenDatasets)
				.routeId("dataset:increaseCommandsDataset")
                .startupOrder(2).autoStartup(false)
				.process(new MarshalProcessor()).to(targetEndpoint)
				.aggregate(constant(0), AggregationStrategies.useLatest()).completionSize(datasetSize)
                    .log("finished")
                    .log("will start next dataset")
                    .to("controlbus:route?routeId=dataset:decreaseCommandsDataset&action=start")
                .end();
				;

		fromF("dataset:decreaseCommandDataset?initialDelay=%d", delayBetweenDatasets)
		        .routeId("dataset:decreaseCommandsDataset")
                .startupOrder(3).autoStartup(false)
				.process(new MarshalProcessor()).to(targetEndpoint)
                .aggregate(constant(0), AggregationStrategies.useLatest()).completionSize(datasetSize)
                    .log("finished")
                .end();
		;

	}

	class MarshalProcessor implements Processor {
		@Override
		public void process(Exchange e) throws Exception {
			Command c = e.getIn().getBody(Command.class);
			String asJson = gson.toJson(c, commandType);
			e.getOut().setBody(asJson);
			e.getOut().setHeader(Exchange.CONTENT_TYPE, "application/json");
			e.getOut().setHeader(Exchange.HTTP_METHOD, constant("POST"));
		}
	}
}
