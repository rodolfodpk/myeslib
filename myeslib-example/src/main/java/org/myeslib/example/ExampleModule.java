package org.myeslib.example;

import static org.myeslib.example.infra.HazelcastMaps.INVENTORY_ITEM_AGGREGATE_HISTORY;

import java.util.Map;
import java.util.UUID;

import javax.inject.Singleton;
import javax.sql.DataSource;

import org.h2.jdbcx.JdbcConnectionPool;
import org.myeslib.data.AggregateRootHistory;
import org.myeslib.data.Snapshot;
import org.myeslib.example.SampleCoreDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleCoreDomain.ItemDescriptionGeneratorService;
import org.myeslib.example.infra.GsonFactory;
import org.myeslib.example.infra.HazelcastConfigFactory;
import org.myeslib.example.infra.HazelcastMaps;
import org.myeslib.example.routes.ConsumeCommandsRoute;
import org.myeslib.hazelcast.HzAggregateRootHistoryTxMapFactory;
import org.myeslib.hazelcast.HzCamelComponent;
import org.myeslib.hazelcast.HzSnapshotReader;
import org.myeslib.hazelcast.HzTransactionalCommandHandler;
import org.myeslib.storage.SnapshotReader;
import org.myeslib.storage.TransactionalCommandHandler;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class ExampleModule extends AbstractModule {
	
	@Provides
	@Singleton
	public DataSource datasource() {
		return JdbcConnectionPool.create("jdbc:h2:mem:test;MODE=Oracle", "scott", "tiger");
	}

	@Provides
	@Singleton
	public Gson gson() {
		return new GsonFactory().create();
	}

	@Provides
	@Singleton
	public Config config(DataSource datasource, Gson gson) {
		return new HazelcastConfigFactory(datasource, gson).getConfig();
	}

	@Provides
	@Singleton
	public HazelcastInstance hazelcastInstance(Config config) {
		return Hazelcast.newHazelcastInstance(config);
	}

	@Provides
	@Singleton
	public HzCamelComponent create(HazelcastInstance hazelcastInstance){
		return new HzCamelComponent(hazelcastInstance);
	}
	
	@Provides
	@Singleton
	public SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader(HazelcastInstance hazelcastInstance) {
		Map<UUID, AggregateRootHistory> historyMap = hazelcastInstance.getMap(HazelcastMaps.INVENTORY_ITEM_AGGREGATE_HISTORY.name());
		Map<UUID, Snapshot<InventoryItemAggregateRoot>> snapshotMap = hazelcastInstance.getMap(HazelcastMaps.INVENTORY_ITEM_LAST_SNAPSHOT.name());
		return new HzSnapshotReader<UUID, InventoryItemAggregateRoot>(historyMap, snapshotMap);
	}

	@Provides
	@Singleton
	public TransactionalCommandHandler<UUID, InventoryItemAggregateRoot> txCommandHandler(HazelcastInstance hazelcastInstance) {
		HzAggregateRootHistoryTxMapFactory<UUID, InventoryItemAggregateRoot> txMapFactory = new HzAggregateRootHistoryTxMapFactory<UUID, InventoryItemAggregateRoot>(); 
		return new HzTransactionalCommandHandler<>(hazelcastInstance, txMapFactory, INVENTORY_ITEM_AGGREGATE_HISTORY.name());
	}

	@Provides
	@Singleton
	public ConsumeCommandsRoute route(SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader, 
			TransactionalCommandHandler<UUID, InventoryItemAggregateRoot> txCommandHandler) {
		return new ConsumeCommandsRoute(snapshotReader, txCommandHandler);
	}
	
	@Override
	protected void configure() {
		bind(ItemDescriptionGeneratorService.class).to(ServiceJustForTest.class);
	}
	

}

class ServiceJustForTest implements ItemDescriptionGeneratorService {
	@Override
	public String generate(UUID id) {
		return "a really nice description for this item from concrete service impl";
	}
}