package org.myeslib.cmdproducer.routes;

import java.lang.reflect.Type;

import lombok.AllArgsConstructor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.myeslib.core.Command;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@AllArgsConstructor
public class DatasetsRoute extends RouteBuilder {

	final Gson gson;
	final String targetEndpoint;
	final Type commandType = new TypeToken<Command>() {}.getType();

	final int delayBetweenDatasets;
	final int initialDelay;

	public void configure() throws Exception {

		fromF("dataset:createCommandDataset?initialDelay=%d", initialDelay)
				.routeId("dataset:createCommandsDataset")
				.process(new MarshalProcessor()).to(targetEndpoint);

		fromF("dataset:increaseCommandDataset?initialDelay=%d", initialDelay + delayBetweenDatasets)
				.routeId("dataset:increaseCommandsDataset")
				.process(new MarshalProcessor()).to(targetEndpoint);

		fromF("dataset:decreaseCommandDataset?initialDelay=%d", initialDelay + (delayBetweenDatasets *2))
				.routeId("dataset:decreaseCommandsDataset")
				.process(new MarshalProcessor()).to(targetEndpoint);

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
