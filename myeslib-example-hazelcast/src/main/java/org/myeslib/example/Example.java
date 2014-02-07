package org.myeslib.example;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.main.Main;
import org.myeslib.example.infra.HzCamelComponent;
import org.myeslib.example.routes.ConsumeCommandsRoute;

import com.google.inject.Guice;
import com.google.inject.Injector;

@Slf4j
public class Example {
	
    final Main main;
	final SimpleRegistry registry;
	final CamelContext context;
	
	public final static int HOW_MANY_COMMANDS_TO_TEST = 1000;
	
	public static void main(String[] args) throws Exception {

		log.info("starting...");
		
		Injector injector = Guice.createInjector(new ExampleModule());
	    Example example = injector.getInstance(Example.class);
		example.main.run();
		
	}
	
	@Inject
	Example(HzCamelComponent justAnotherHazelcastComponent, ConsumeCommandsRoute consumeCommandsRoute) throws Exception  {
		
		this.main = new Main() ;
		this.main.enableHangupSupport();
		this.registry = new SimpleRegistry();
		this.context = new DefaultCamelContext(registry);
		
		CamelContext context = new DefaultCamelContext(registry);
		context.addComponent("hz", justAnotherHazelcastComponent);

		registry.put("inventoryCommandsDataset", new CommandsDataSet(HOW_MANY_COMMANDS_TO_TEST));
		context.addRoutes(consumeCommandsRoute);
				
		main.getCamelContexts().clear();
		main.getCamelContexts().add(context);
		main.setDuration(-1);
		main.start();
		
	}

}

