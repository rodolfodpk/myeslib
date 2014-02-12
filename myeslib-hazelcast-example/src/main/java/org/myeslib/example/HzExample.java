package org.myeslib.example;

import java.util.List;
import java.util.UUID;
import java.util.Vector;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.main.Main;
import org.myeslib.example.routes.HzConsumeCommandsRoute;
import org.myeslib.example.routes.HzConsumeEventsRoute;
import org.myeslib.util.camel.example.dataset.CreateCommandDataSet;
import org.myeslib.util.camel.example.dataset.DatasetsRoute;
import org.myeslib.util.camel.example.dataset.DecreaseCommandDataSet;
import org.myeslib.util.camel.example.dataset.IncreaseCommandDataSet;
import org.myeslib.util.hazelcast.HzCamelComponent;

import com.google.inject.Guice;
import com.google.inject.Injector;

@Slf4j
public class HzExample {
	
    final Main main;
	final SimpleRegistry registry;
	final CamelContext context;

	public final static int HOW_MANY_AGGREGATES = 1000;
	public final static List<UUID> ids = ids();
	
	public static void main(String[] args) throws Exception {

		log.info("starting...");
		
		Injector injector = Guice.createInjector(new HzExampleModule());
	    HzExample example = injector.getInstance(HzExample.class);
		example.main.run();
		
	}
	
	public static List<UUID> ids() {
		Vector<UUID> ids = new Vector<>();
		for (int i=0; i< HOW_MANY_AGGREGATES; i++){
			ids.add(UUID.randomUUID());
		}
		return ids;
	}
	
	@Inject
	HzExample(HzCamelComponent justAnotherHazelcastComponent, 
			  DatasetsRoute datasetRoute, 
			  HzConsumeCommandsRoute consumeCommandsRoute, 
			  HzConsumeEventsRoute consumeEventsRoute) throws Exception  {
		
		this.main = new Main() ;
		this.main.enableHangupSupport();
		this.registry = new SimpleRegistry();
		this.context = new DefaultCamelContext(registry);
		
		CamelContext context = new DefaultCamelContext(registry);
		context.addComponent("hz", justAnotherHazelcastComponent);

		registry.put("createCommandDataset", new CreateCommandDataSet(ids, HOW_MANY_AGGREGATES));
		registry.put("increaseCommandDataset", new IncreaseCommandDataSet(ids, HOW_MANY_AGGREGATES));
		registry.put("decreaseCommandDataset", new DecreaseCommandDataSet(ids, HOW_MANY_AGGREGATES));

		context.addRoutes(datasetRoute);
		context.addRoutes(consumeCommandsRoute);
		context.addRoutes(consumeEventsRoute);
		
		main.getCamelContexts().clear();
		main.getCamelContexts().add(context);
		main.setDuration(-1);
		main.start();
		
	}

}

