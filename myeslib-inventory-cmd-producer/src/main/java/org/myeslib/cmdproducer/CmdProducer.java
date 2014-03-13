package org.myeslib.cmdproducer;

import java.util.List;
import java.util.UUID;
import java.util.Vector;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.main.Main;
import org.myeslib.cmdproducer.datasets.CreateCommandDataSet;
import org.myeslib.cmdproducer.datasets.DecreaseCommandDataSet;
import org.myeslib.cmdproducer.datasets.IncreaseCommandDataSet;
import org.myeslib.cmdproducer.routes.DatasetsRoute;

import com.google.inject.Guice;
import com.google.inject.Injector;

@Slf4j
public class CmdProducer {
	
    final Main main;
	final SimpleRegistry registry;
	final CamelContext context;
	static int howManyAggregates;

	final List<UUID> ids = new Vector<>();
	
	public static void main(String[] args) throws Exception {
		howManyAggregates = args.length ==0 ? 1000 : new Integer(args[0]);
		log.info("starting with howManyAggregates = {}", howManyAggregates);
		Injector injector = Guice.createInjector(new CmdProducerModule());
	    CmdProducer example = injector.getInstance(CmdProducer.class);
		example.main.run();
		
	}
	
	public void populate() {
		for (int i=0; i< howManyAggregates; i++){
			ids.add(UUID.randomUUID());
		}
	}
	
	@Inject
	CmdProducer(DatasetsRoute datasetRoute) throws Exception  {
		
		main = new Main() ;
		main.enableHangupSupport();
		registry = new SimpleRegistry();
		context = new DefaultCamelContext(registry);
		
		populate();
		
		registry.put("createCommandDataset", new CreateCommandDataSet(ids, howManyAggregates));
		registry.put("increaseCommandDataset", new IncreaseCommandDataSet(ids, howManyAggregates));
		registry.put("decreaseCommandDataset", new DecreaseCommandDataSet(ids, howManyAggregates));

		context.addRoutes(datasetRoute);
		
		main.getCamelContexts().clear();
		main.getCamelContexts().add(context);
		main.setDuration(-1);
		main.start();
		
	}

}

