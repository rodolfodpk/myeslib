package org.myeslib.example;

import static org.myeslib.example.infra.HazelcastMaps.INVENTORY_ITEM_AGGREGATE_HISTORY;

import java.util.Map;
import java.util.UUID;

import javax.inject.Singleton;
import javax.sql.DataSource;

import org.h2.jdbcx.JdbcConnectionPool;
import org.myeslib.core.data.Snapshot;
import org.myeslib.core.function.CommandHandlerInvoker;
import org.myeslib.core.storage.SnapshotReader;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleDomain.ItemDescriptionGeneratorService;
import org.myeslib.example.infra.HazelcastConfigFactory;
import org.myeslib.example.infra.HazelcastMaps;
import org.myeslib.example.infra.HzCamelComponent;
import org.myeslib.example.infra.InventoryItemMapConfigFactory;
import org.myeslib.example.infra.SerializersConfigFactory;
import org.myeslib.example.routes.ConsumeCommandsRoute;
import org.myeslib.hazelcast.HzStringTxMapFactory;
import org.myeslib.hazelcast.function.HzCommandHandlerInvoker;
import org.myeslib.hazelcast.gson.FromStringFunction;
import org.myeslib.hazelcast.gson.ToStringFunction;
import org.myeslib.hazelcast.storage.HzSnapshotReader;

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
		return new SampleDomainGsonFactory().create();
	}

	@Provides
	@Singleton
	public Config config(InventoryItemMapConfigFactory mapConfigFactory, SerializersConfigFactory serializersFactory) {
		return new HazelcastConfigFactory(mapConfigFactory.create(), serializersFactory.create()).getConfig();
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
	public SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader(HazelcastInstance hazelcastInstance, Gson gson) {
		Map<UUID, String> historyMap = hazelcastInstance.getMap(HazelcastMaps.INVENTORY_ITEM_AGGREGATE_HISTORY.name());
		Map<UUID, Snapshot<InventoryItemAggregateRoot>> snapshotMap = hazelcastInstance.getMap(HazelcastMaps.INVENTORY_ITEM_LAST_SNAPSHOT.name());
		return new HzSnapshotReader<UUID, InventoryItemAggregateRoot>(historyMap, snapshotMap, new FromStringFunction(gson));
	}

	@Provides
	@Singleton
	public CommandHandlerInvoker<UUID, InventoryItemAggregateRoot> txCommandHandler(HazelcastInstance hazelcastInstance, Gson gson) {
		HzStringTxMapFactory<UUID> txMapFactory = new HzStringTxMapFactory<>(); 
		return new HzCommandHandlerInvoker<>(hazelcastInstance, txMapFactory, INVENTORY_ITEM_AGGREGATE_HISTORY.name(), new FromStringFunction(gson), new ToStringFunction(gson));
	}

	@Provides
	@Singleton
	public ConsumeCommandsRoute route(SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader, 
			CommandHandlerInvoker<UUID, InventoryItemAggregateRoot> txCommandHandler) {
		return new ConsumeCommandsRoute(snapshotReader, txCommandHandler);
	}
	
	@Override
	protected void configure() {
		bind(ItemDescriptionGeneratorService.class).to(ServiceJustForTest.class);
		bind(InventoryItemMapConfigFactory.class);
		bind(SerializersConfigFactory.class);
	}
	

}

class ServiceJustForTest implements ItemDescriptionGeneratorService {
	@Override
	public String generate(UUID id) {
		return "a really nice description for this item from concrete service impl";
	}
}