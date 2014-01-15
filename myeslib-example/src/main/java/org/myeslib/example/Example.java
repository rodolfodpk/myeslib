package org.myeslib.example;

import static org.myeslib.example.SampleCoreDomain.*;

import java.util.UUID;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.main.Main;
import org.h2.jdbcx.JdbcConnectionPool;
import org.myeslib.example.infra.GsonFactory;
import org.myeslib.example.infra.HazelcastFactory;
import org.myeslib.example.routes.ConsumeCommandsRoute;
import org.myeslib.hazelcast.AggregateRootHistoryMapFactory;
import org.myeslib.hazelcast.AggregateRootHistoryTxMapFactory;
import org.myeslib.hazelcast.AggregateRootSnapshotMapFactory;
import org.myeslib.hazelcast.JustAnotherHazelcastComponent;

import com.google.gson.Gson;
import com.hazelcast.core.HazelcastInstance;

@Slf4j
public class Example {
	
	protected final Main main;
	protected final SimpleRegistry registry;
	protected final CamelContext context;
	
	public static void main(String[] args) throws Exception {
		
		Example example = new Example() ;
		
		example.main.run();
		
	}
	
	Example() throws Exception {
		
		this.main = new Main() ;
		
		this.main.enableHangupSupport();
		
		this.registry = new SimpleRegistry();
		
		this.context = new DefaultCamelContext(registry);
		
		CamelContext context = new DefaultCamelContext(registry);
		
		DataSource ds = JdbcConnectionPool.create("jdbc:h2:mem:test;MODE=Oracle", "scott", "tiger");

		Gson gson = new GsonFactory().create();
		
		HazelcastInstance hazelcastInstance = new HazelcastFactory(ds, gson).get();

		JustAnotherHazelcastComponent hz = new JustAnotherHazelcastComponent(hazelcastInstance);
		
		context.addComponent("hz", hz);
		
		context.addRoutes(new ConsumeCommandsRoute(hazelcastInstance, 
											new AggregateRootHistoryTxMapFactory<UUID, InventoryItemAggregateRoot>(),
											new AggregateRootHistoryMapFactory<UUID, InventoryItemAggregateRoot>(hazelcastInstance), 
											new AggregateRootSnapshotMapFactory<UUID, InventoryItemAggregateRoot>(hazelcastInstance)));
		
		main.getCamelContexts().clear();
		
		main.getCamelContexts().add(context);
		
		main.setDuration(-1);
		
		log.info("starting...");
	
	}

}
