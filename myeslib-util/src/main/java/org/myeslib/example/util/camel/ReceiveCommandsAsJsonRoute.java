package org.myeslib.example.util.camel;

import lombok.AllArgsConstructor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.ValueBuilder;
import org.myeslib.core.Command;
import org.myeslib.example.SampleDomain.CreateInventoryItem;
import org.myeslib.example.SampleDomain.DecreaseInventory;
import org.myeslib.example.SampleDomain.IncreaseInventory;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.jayway.jsonpath.JsonPath;

public class ReceiveCommandsAsJsonRoute  extends RouteBuilder{

	@Inject
	public ReceiveCommandsAsJsonRoute(String startUri, String endUri, Gson gson) {
		this.startUri = startUri;
		this.endUri = endUri;
		this.gson = gson;
	}

	final String startUri;
	final String endUri;
	final Gson gson;
	
	@Override
	public void configure() throws Exception {
		
	     from(startUri)
     	 .streamCaching()
     	 .routeId("receive-commands-as-json")
      	 //.log("received = ${body}")
      	 //.setHeader("commandType", jsonpath("$.type"))
      	 .process(new Processor() {
			@Override
			public void process(Exchange e) throws Exception {
				String body = e.getIn().getBody(String.class);
				e.getOut().setBody(body);
				String commandType = JsonPath.read(body, "$.type");
				e.getOut().setHeader("commandType", commandType);
			}
		})
      	 .choice()
      	 	.when(header("commandType").isEqualTo("CreateInventoryItem"))
      	 		.process(new CommandUnmarshallerProcessor(gson, body(String.class), CreateInventoryItem.class))
      	 		.to(endUri)
      	 	.when(header("commandType").isEqualTo("IncreaseInventory"))
      	 		.process(new CommandUnmarshallerProcessor(gson, body(String.class), IncreaseInventory.class))
      	 		.to(endUri)
      	 	.when(header("commandType").isEqualTo("DecreaseInventory"))
      	 		.process(new CommandUnmarshallerProcessor(gson, body(String.class), DecreaseInventory.class))
      	 		.to(endUri)
      	 	.otherwise()
      	 		.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
      	 .end()		
      	 //.log("resulting body = ${body}")
      	 ;

		
	}

}

@AllArgsConstructor
class CommandUnmarshallerProcessor implements Processor{
	private final Gson gson;
	private final ValueBuilder valueBuilder;
	private final Class<? extends Command> clazz;
	@Override
	public void process(Exchange e) throws Exception {
		String asJson = valueBuilder.evaluate(e, String.class);
		Command command = gson.fromJson(asJson, clazz);
		e.getOut().setBody(command, clazz);
	}
}

