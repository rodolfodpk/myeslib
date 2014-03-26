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
import org.myeslib.example.hazelcast.routes.ReceiveCommandsAsJsonRoute;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.hazelcast.core.HazelcastInstance;

@Slf4j
public class HzExample {
	
    final Main main;
	final SimpleRegistry registry;
	final CamelContext context;
	
	static int jettyMinThreads;
	static int jettyMaxThreads;
	static int dbPoolMinConnections;
	static int dbPoolMaxConnections;
	static int eventsQueueConsumers;
	static int writeDelaySeconds;
	
	public static void main(String[] args) throws Exception {

		log.info("starting...");
		
		jettyMinThreads = args.length ==0 ? 10 : new Integer(args[0]);  
		jettyMaxThreads = args.length <=1 ? 100 : new Integer(args[1]);  
		dbPoolMinConnections = args.length <=2 ? 10 : new Integer(args[2]);  
		dbPoolMaxConnections = args.length <=3 ? 100 : new Integer(args[3]);  
		eventsQueueConsumers = args.length <=4 ? 50 : new Integer(args[4]);  
		writeDelaySeconds = args.length <=5 ? 0 : new Integer(args[5]) ;  
		
		log.info("jettyMinThreads = {}", jettyMinThreads);
		log.info("jettyMaxThreads = {}", jettyMaxThreads);
		log.info("dbPoolMinConnections = {}", dbPoolMinConnections);
		log.info("dbPoolMaxConnections = {}", dbPoolMaxConnections);
		log.info("eventsQueueConsumers = {}", eventsQueueConsumers);
        log.info("writeDelaySeconds = {} ({})", writeDelaySeconds, writeDelaySeconds>0 ? "write-behind" : "write-through");
        
		Injector injector = Guice.createInjector(new CamelModule(jettyMinThreads, jettyMaxThreads, eventsQueueConsumers),
												 new DatabaseModule(dbPoolMinConnections, dbPoolMaxConnections), 
												 new HazelcastModule(writeDelaySeconds), 
												 new InventoryItemModule());
	    HzExample example = injector.getInstance(HzExample.class);
		example.main.run();
		
	}
	
	@Inject
	HzExample(final HazelcastInstance hazelcastInstance,
              ReceiveCommandsAsJsonRoute receiveCommandsRoute,
			  HzConsumeCommandsRoute consumeCommandsRoute,
			  HzConsumeEventsRoute consumeEventsRoute
			) throws Exception  {
		
		this.main = new Main() ;
		this.main.enableHangupSupport();
		this.registry = new SimpleRegistry();
		this.context = new DefaultCamelContext(registry);

		context.addRoutes(receiveCommandsRoute);
		context.addRoutes(consumeCommandsRoute);
		context.addRoutes(consumeEventsRoute);
		
		main.getCamelContexts().clear();
		main.getCamelContexts().add(context);
		main.setDuration(-1);

 /*       Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                log.warn("stopping Camel...");
                try {
                    context.stop();
                } catch (Exception e) {
                    log.error(e.getMessage());
                } finally {
                    log.warn("stopping Hazelcast...");
                    hazelcastInstance.shutdown();
                }
            }
        });*/

        main.start();

        log.info("started...");

	}

}

