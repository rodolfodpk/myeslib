package org.myeslib.example.jdbi.routes;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.jayway.jsonpath.JsonPath;
import lombok.AllArgsConstructor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.ValueBuilder;
import org.myeslib.core.Command;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.example.SampleDomain.CreateInventoryItem;
import org.myeslib.example.SampleDomain.DecreaseInventory;
import org.myeslib.example.SampleDomain.IncreaseInventory;

public class ReceiveCommandsAsJsonRoute  extends RouteBuilder{

    public static final String DIRECT_SEND_TO_AGGREGATE_ROOT_PROCESSOR = "direct:send-to-aggregate-root-processor";
    public static final String COMMAND_TYPE = "commandType";

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
      	 //.setHeader(COMMAND_TYPE, jsonpath("$.type"))
      	 .process(new Processor() {
             @Override
             public void process(Exchange e) throws Exception {
                 String body = e.getIn().getBody(String.class);
                 e.getOut().setBody(body);
                 String commandType = JsonPath.read(body, "$.type");
                 e.getOut().setHeader(COMMAND_TYPE, commandType);
             }
         })
      	 .choice()
      	 	.when(header(COMMAND_TYPE).isEqualTo("CreateInventoryItem"))
      	 		.process(new CommandUnmarshallerProcessor(gson, body(String.class), CreateInventoryItem.class))
      	 		.to(DIRECT_SEND_TO_AGGREGATE_ROOT_PROCESSOR)
      	 	.when(header(COMMAND_TYPE).isEqualTo("IncreaseInventory"))
      	 		.process(new CommandUnmarshallerProcessor(gson, body(String.class), IncreaseInventory.class))
      	 		.to(DIRECT_SEND_TO_AGGREGATE_ROOT_PROCESSOR)
      	 	.when(header(COMMAND_TYPE).isEqualTo("DecreaseInventory"))
      	 		.process(new CommandUnmarshallerProcessor(gson, body(String.class), DecreaseInventory.class))
      	 		.to(DIRECT_SEND_TO_AGGREGATE_ROOT_PROCESSOR)
      	 	.otherwise()
      	 		.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
      	 .end()		
      	 ;

	   from(DIRECT_SEND_TO_AGGREGATE_ROOT_PROCESSOR)
  	      .log("received ${header.commandType} - id ${body.id} - version ${body.version}")
	      .to(endUri)
	      .process(new Processor() {
			@Override
			public void process(Exchange e) throws Exception {
				UnitOfWork uow = e.getIn().getBody(UnitOfWork.class);
				String asJson = gson.toJson(uow);
				e.getOut().setBody(asJson);
			}
		});
	   
	}

}

@AllArgsConstructor
class CommandUnmarshallerProcessor implements Processor{
	private final Gson gson;
	private final ValueBuilder valueBuilder;
	private final Class<? extends Command> clazz;
	@Override
	public void process(Exchange e) throws Exception {
		e.getOut().setHeader(ReceiveCommandsAsJsonRoute.COMMAND_TYPE, e.getIn().getHeader(ReceiveCommandsAsJsonRoute.COMMAND_TYPE));
		String asJson = valueBuilder.evaluate(e, String.class);
		Command command = gson.fromJson(asJson, clazz);
		e.getOut().setBody(command, clazz);
	}
}

