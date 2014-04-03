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
import org.myeslib.cmdproducer.routes.CommandsDataSetsRoute;

import com.google.inject.Guice;
import com.google.inject.Injector;

@Slf4j
public class CmdProducer {
	
    final Main main;
	final SimpleRegistry registry;
	final CamelContext context;
	
	static int dataSetSize;
	static int delayBetweenDataSets;
	static int initialDelay;
	
	final List<UUID> ids = new Vector<>();
	
	public static void main(String[] args) throws Exception {
		
		dataSetSize = args.length ==0 ? 1000 : new Integer(args[0]);  // default = 1000 aggregates
		delayBetweenDataSets = args.length <=1 ? 30000 : new Integer(args[1]); // default = 30 seconds
		initialDelay = args.length <=2 ? 30000 : new Integer(args[2]); // default = 30 seconds
			
		log.info("dataSetSize = {}", dataSetSize);
		log.info("delayBetweenDataSets = {}", delayBetweenDataSets);
		log.info("initialDelay = {}", initialDelay);
		
		Injector injector = Guice.createInjector(new CmdProducerModule(dataSetSize, delayBetweenDataSets, initialDelay));
	    CmdProducer example = injector.getInstance(CmdProducer.class);
		example.main.run();
		
	}
	
	public void populate() {
		for (int i=0; i< dataSetSize; i++){
			ids.add(UUID.randomUUID());
		}
	}
	
	@Inject
	CmdProducer(CommandsDataSetsRoute datasetRoute) throws Exception  {
		
		main = new Main() ;
		main.enableHangupSupport();
		registry = new SimpleRegistry();
		context = new DefaultCamelContext(registry);
		
		populate();
		
		registry.put("createCommandDataset", new CreateCommandDataSet(ids, dataSetSize));
		registry.put("increaseCommandDataset", new IncreaseCommandDataSet(ids, dataSetSize));
		registry.put("decreaseCommandDataset", new DecreaseCommandDataSet(ids, dataSetSize));

		context.addRoutes(datasetRoute);
		
		main.getCamelContexts().clear();
		main.getCamelContexts().add(context);
		main.setDuration(-1);
		main.start();
		
	}

}

