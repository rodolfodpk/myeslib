package org.myeslib.example;

import static org.myeslib.example.infra.HazelcastMaps.INVENTORY_ITEM_AGGREGATE_HISTORY;
import static org.myeslib.example.infra.HazelcastMaps.INVENTORY_ITEM_LAST_SNAPSHOT;

import java.util.UUID;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.main.Main;
import org.h2.jdbcx.JdbcConnectionPool;
import org.myeslib.example.SampleCoreDomain.InventoryItemAggregateRoot;
import org.myeslib.example.infra.GsonFactory;
import org.myeslib.example.infra.HazelcastFactory;
import org.myeslib.example.routes.ConsumeCommandsRoute;
import org.myeslib.hazelcast.AggregateRootHistoryMapFactory;
import org.myeslib.hazelcast.AggregateRootHistoryTxMapFactory;
import org.myeslib.hazelcast.AggregateRootSnapshotMapFactory;
import org.myeslib.hazelcast.JustAnotherHazelcastComponent;
import org.myeslib.hazelcast.TransactionalCommandProcessor;
import org.myeslib.util.KeyValueSnapshotReader;

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
		
		context.addRoutes(createRoute(hazelcastInstance));
				
		main.getCamelContexts().clear();
		
		main.getCamelContexts().add(context);
		
		main.setDuration(-1);
		
		log.info("starting...");
	
	}

	ConsumeCommandsRoute createRoute(HazelcastInstance hazelcastInstance ) {
		
		AggregateRootHistoryTxMapFactory<UUID, InventoryItemAggregateRoot> txMapFactory = new AggregateRootHistoryTxMapFactory<>();
		AggregateRootHistoryMapFactory<UUID, InventoryItemAggregateRoot> mapFactory = new AggregateRootHistoryMapFactory<>(hazelcastInstance);
		AggregateRootSnapshotMapFactory<UUID, InventoryItemAggregateRoot> snapshotMapFactory = new AggregateRootSnapshotMapFactory<>(hazelcastInstance);
		
		KeyValueSnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader = 
				new KeyValueSnapshotReader<>(mapFactory.get(INVENTORY_ITEM_AGGREGATE_HISTORY.name()), 
											 snapshotMapFactory.get(INVENTORY_ITEM_LAST_SNAPSHOT.name()));
	
		TransactionalCommandProcessor<UUID, InventoryItemAggregateRoot> txProcessor = 
				new TransactionalCommandProcessor<>(hazelcastInstance, txMapFactory, INVENTORY_ITEM_AGGREGATE_HISTORY.name());	

		return new ConsumeCommandsRoute(snapshotReader, txProcessor);

	}
	
	
}
