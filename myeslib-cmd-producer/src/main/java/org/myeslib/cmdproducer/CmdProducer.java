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

	public final static int HOW_MANY_AGGREGATES = 1000;
	public final static List<UUID> ids = ids();
	
	public static void main(String[] args) throws Exception {

		log.info("starting...");
		
		Injector injector = Guice.createInjector(new CmdProducerModule());
	    CmdProducer example = injector.getInstance(CmdProducer.class);
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
	CmdProducer(DatasetsRoute datasetRoute) throws Exception  {
		
		main = new Main() ;
		main.enableHangupSupport();
		registry = new SimpleRegistry();
		context = new DefaultCamelContext(registry);
		
		registry.put("createCommandDataset", new CreateCommandDataSet(ids, HOW_MANY_AGGREGATES));
		registry.put("increaseCommandDataset", new IncreaseCommandDataSet(ids, HOW_MANY_AGGREGATES));
		registry.put("decreaseCommandDataset", new DecreaseCommandDataSet(ids, HOW_MANY_AGGREGATES));

		context.addRoutes(datasetRoute);
		
		main.getCamelContexts().clear();
		main.getCamelContexts().add(context);
		main.setDuration(-1);
		main.start();
		
	}

}

