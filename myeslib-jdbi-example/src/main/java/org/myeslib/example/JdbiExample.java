package org.myeslib.example;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.main.Main;
import org.myeslib.example.routes.JdbiConsumeCommandsRoute;
import org.myeslib.example.routes.JdbiConsumeEventsRoute;
import org.myeslib.util.camel.ReceiveCommandsAsJsonRoute;
import org.myeslib.util.hazelcast.HzCamelComponent;

import com.google.inject.Guice;
import com.google.inject.Injector;

@Slf4j
public class JdbiExample {
	
    final Main main;
	final SimpleRegistry registry;
	final CamelContext context;

	public static void main(String[] args) throws Exception {

		log.info("starting...");
		
		Injector injector = Guice.createInjector(new JdbiExampleModule());
	    JdbiExample example = injector.getInstance(JdbiExample.class);
		example.main.run();
		
	}
		
	@Inject
	JdbiExample(HzCamelComponent justAnotherHazelcastComponent, 
				ReceiveCommandsAsJsonRoute receiveCommandsRoute, 
				JdbiConsumeCommandsRoute consumeCommandsRoute, 
				JdbiConsumeEventsRoute consumeEventsRoute) throws Exception  {
		
		main = new Main() ;
		main.enableHangupSupport();
		registry = new SimpleRegistry();
		context = new DefaultCamelContext(registry);
		
		context.addComponent("hz", justAnotherHazelcastComponent);
		context.addRoutes(receiveCommandsRoute);
		context.addRoutes(consumeCommandsRoute);
		context.addRoutes(consumeEventsRoute);
		
		main.getCamelContexts().clear();
		main.getCamelContexts().add(context);
		main.setDuration(-1);
		main.start();
		
	}

}

