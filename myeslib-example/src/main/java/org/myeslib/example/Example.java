package org.myeslib.example;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.main.Main;
import org.myeslib.example.routes.ConsumeCommandsRoute;
import org.myeslib.hazelcast.JustAnotherHazelcastComponent;

import com.google.inject.Guice;
import com.google.inject.Injector;

@Slf4j
public class Example {
	
    final Main main;
	final SimpleRegistry registry;
	final CamelContext context;
	
	public static void main(String[] args) throws Exception {

		Injector injector = Guice.createInjector(new ExampleModule());
	    Example example = injector.getInstance(Example.class);
		example.main.run();
		
	}
	
	@Inject
	Example(JustAnotherHazelcastComponent justAnotherHazelcastComponent, ConsumeCommandsRoute consumeCommandsRoute)  {
		
		this.main = new Main() ;
		this.main.enableHangupSupport();
		this.registry = new SimpleRegistry();
		this.context = new DefaultCamelContext(registry);
		
		CamelContext context = new DefaultCamelContext(registry);
		context.addComponent("hz", justAnotherHazelcastComponent);
		try {
			context.addRoutes(consumeCommandsRoute);
		} catch (Exception e) {
			e.printStackTrace();
		}
				
		main.getCamelContexts().clear();
		main.getCamelContexts().add(context);
		main.setDuration(-1);
		
		log.info("starting...");
	
	}

}
