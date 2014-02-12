package org.myeslib.example;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Singleton;
import javax.sql.DataSource;

import org.h2.jdbcx.JdbcConnectionPool;
import org.myeslib.core.data.AggregateRootHistory;
import org.myeslib.core.data.Snapshot;
import org.myeslib.core.storage.SnapshotReader;
import org.myeslib.core.storage.UnitOfWorkWriter;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleDomain.InventoryItemInstanceFactory;
import org.myeslib.example.SampleDomain.ItemDescriptionGeneratorService;
import org.myeslib.example.infra.HazelcastConfigFactory;
import org.myeslib.example.infra.HazelcastData;
import org.myeslib.example.infra.InventoryItemMapConfigFactory;
import org.myeslib.example.infra.SerializersConfigFactory;
import org.myeslib.example.routes.HzConsumeEventsRoute;
import org.myeslib.hazelcast.HzStringTxMapFactory;
import org.myeslib.hazelcast.storage.HzSnapshotReader;
import org.myeslib.hazelcast.storage.HzUnitOfWorkWriter;
import org.myeslib.util.camel.example.dataset.DatasetsRoute;
import org.myeslib.util.gson.ArhFromStringFunction;
import org.myeslib.util.gson.ArhToStringFunction;
import org.myeslib.util.hazelcast.HzCamelComponent;

import com.google.common.base.Function;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Named;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

public class HzExampleModule extends AbstractModule {
	
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
	public Function<AggregateRootHistory, String> toStringFunction(Gson gson) {
		return new ArhToStringFunction(gson);
	}

	@Provides
	@Singleton
	public Function<String, AggregateRootHistory> fromStringFunction(Gson gson) {
		return new ArhFromStringFunction(gson);
	}

	@Provides
	@Singleton
	public ConcurrentMap<String, Object> userContext() {
		ConcurrentMap<String, Object> userContext = new ConcurrentHashMap<>();
		return userContext;
	}
	
	@Provides
	@Singleton
	public Config config(InventoryItemMapConfigFactory mapConfigFactory, SerializersConfigFactory serializersFactory, ConcurrentMap<String, Object> userContext) {
		return new HazelcastConfigFactory(mapConfigFactory.create(), serializersFactory.create(), userContext).getConfig();
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
	public IMap<UUID, String> inventoryItemMap(HazelcastInstance hazelcastInstance) {
		return hazelcastInstance.getMap(HazelcastData.INVENTORY_ITEM_AGGREGATE_HISTORY.name());
	}
	
	@Provides
	@Singleton
	public SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader(    
			              HazelcastInstance hazelcastInstance, 
			              Function<String, AggregateRootHistory> fromStringFunction,
			              Function<Void, InventoryItemAggregateRoot> factory) {
		Map<UUID, String> historyMap = hazelcastInstance.getMap(HazelcastData.INVENTORY_ITEM_AGGREGATE_HISTORY.name());
		Map<UUID, Snapshot<InventoryItemAggregateRoot>> snapshotMap = hazelcastInstance.getMap(HazelcastData.INVENTORY_ITEM_LAST_SNAPSHOT.name());
		return new HzSnapshotReader<UUID, InventoryItemAggregateRoot>(historyMap, snapshotMap, fromStringFunction, factory);
	}
	
	@Provides
	@Singleton
	@Named("originUri")
	public String originUri() {
		return "hz:seda:inventory-item-command?transacted=true&concurrentConsumers=10";
	}

	@Provides
	@Singleton
	@Named("eventsDestinationUri")
	public String destinationUri() {
		return String.format("hz:seda:%s", HazelcastData.INVENTORY_ITEM_EVENTS_QUEUE.name());
	}

	@Provides
	@Singleton
	public Function<Void, InventoryItemAggregateRoot> factory() {
		return new InventoryItemInstanceFactory();
	}
	
	public interface HzUnitOfWorkWriterFactory {
		HzUnitOfWorkWriter<UUID> create(IMap<UUID, String> pastTransactionsMap);
	}
	
	@Override
	protected void configure() {
		
		bind(DatasetsRoute.class);
		bind(HzConsumeEventsRoute.class);
		bind(HzStringTxMapFactory.class).asEagerSingleton();
		
		bind(ItemDescriptionGeneratorService.class).to(ServiceJustForTest.class).asEagerSingleton();;
		bind(InventoryItemMapConfigFactory.class).asEagerSingleton();;
		bind(SerializersConfigFactory.class).asEagerSingleton();;
		
		install(new FactoryModuleBuilder()
	        .implement(UnitOfWorkWriter.class, HzUnitOfWorkWriter.class)
	        .build(HzUnitOfWorkWriterFactory.class)) ;
		
	}
	
	public static class ServiceJustForTest implements ItemDescriptionGeneratorService {
		@Override
		public String generate(UUID id) {
			return "cool";
		}
	}
	
}


