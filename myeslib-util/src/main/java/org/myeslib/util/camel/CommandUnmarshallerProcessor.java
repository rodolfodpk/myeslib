package org.myeslib.util.camel;

import lombok.AllArgsConstructor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.ValueBuilder;
import org.myeslib.core.Command;

import com.google.gson.Gson;

@AllArgsConstructor
public class CommandUnmarshallerProcessor implements Processor{
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
