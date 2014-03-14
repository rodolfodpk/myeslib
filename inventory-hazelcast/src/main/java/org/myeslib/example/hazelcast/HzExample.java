package org.myeslib.example.hazelcast;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.main.Main;
import org.myeslib.example.hazelcast.modules.CamelModule;
import org.myeslib.example.hazelcast.modules.DatabaseModule;
import org.myeslib.example.hazelcast.modules.HazelcastModule;
import org.myeslib.example.hazelcast.modules.InventoryItemModule;
import org.myeslib.example.hazelcast.routes.HzConsumeCommandsRoute;
import org.myeslib.example.hazelcast.routes.HzConsumeEventsRoute;
import org.myeslib.util.example.ReceiveCommandsAsJsonRoute;
import org.myeslib.util.hazelcast.HzCamelComponent;

import com.google.inject.Guice;
import com.google.inject.Injector;

@Slf4j
public class HzExample {
	
    final Main main;
	final SimpleRegistry registry;
	final CamelContext context;
	
	static int jettyMinThreads;
	static int jettyMaxThreads;
	static int dbPoolMinThreads;
	static int dbPoolMaxThreads;
	static int eventsQueueConsumers;
	
	public static void main(String[] args) throws Exception {

		log.info("starting...");
		
		jettyMinThreads = args.length ==0 ? 10 : new Integer(args[0]);  
		jettyMaxThreads = args.length <=1 ? 100 : new Integer(args[1]);  
		dbPoolMinThreads = args.length <=2 ? 10 : new Integer(args[2]);  
		dbPoolMaxThreads = args.length <=3 ? 100 : new Integer(args[3]);  
		eventsQueueConsumers = args.length <=4 ? 50 : new Integer(args[4]);  
		
		log.info("jettyMinThreads = {}", jettyMinThreads);
		log.info("jettyMaxThreads = {}", jettyMaxThreads);
		log.info("dbPoolMinThreads = {}", dbPoolMinThreads);
		log.info("dbPoolMaxThreads = {}", dbPoolMaxThreads);
		log.info("eventsQueueConsumers = {}", eventsQueueConsumers);

		Injector injector = Guice.createInjector(new CamelModule(jettyMinThreads, jettyMaxThreads, eventsQueueConsumers),
												 new DatabaseModule(dbPoolMinThreads, dbPoolMaxThreads), 
												 new HazelcastModule(), 
												 new InventoryItemModule());
	    HzExample example = injector.getInstance(HzExample.class);
		example.main.run();
		
	}
	
	@Inject
	HzExample(HzCamelComponent justAnotherHazelcastComponent, 
			  ReceiveCommandsAsJsonRoute receiveCommandsRoute, 
			  HzConsumeCommandsRoute consumeCommandsRoute, 
			  HzConsumeEventsRoute consumeEventsRoute
			) throws Exception  {
		
		this.main = new Main() ;
		this.main.enableHangupSupport();
		this.registry = new SimpleRegistry();
		this.context = new DefaultCamelContext(registry);
		
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

