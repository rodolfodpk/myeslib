package org.myeslib.cmdproducer.routes;

import java.lang.reflect.Type;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.myeslib.core.Command;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;

public class DatasetsRoute extends RouteBuilder {
	
	@Inject
	public DatasetsRoute(Gson gson, String targetEndpoint) {
		this.gson = gson;
		this.targetEndpoint = targetEndpoint;
	} 

	final Gson gson;
	final String targetEndpoint;
	final Type commandType = new TypeToken<Command>() {}.getType();
	
	public void configure() throws Exception {

		 from("dataset:createCommandDataset?initialDelay=10000")
		 	.routeId("dataset:createCommandsDataset")
		 	.process(new MarshalProcessor())
	        .to(targetEndpoint);

		 from("dataset:increaseCommandDataset?initialDelay=20000")
		 	.routeId("dataset:increaseCommandsDataset")
		 	.process(new MarshalProcessor())
		 	.to("jetty://http://localhost:8080/inventory-item-command");

		 from("dataset:decreaseCommandDataset?initialDelay=30000")
		 	.routeId("dataset:decreaseCommandsDataset")
		 	.process(new MarshalProcessor())
		 	.to("jetty://http://localhost:8080/inventory-item-command");

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

