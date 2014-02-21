package org.myeslib.example;

import java.util.Map;
import java.util.UUID;

import javax.inject.Singleton;
import javax.sql.DataSource;

import org.h2.jdbcx.JdbcConnectionPool;
import org.myeslib.core.data.AggregateRootHistory;
import org.myeslib.core.data.Snapshot;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.core.function.CommandHandlerInvoker;
import org.myeslib.core.function.MultiMethodCommandHandlerInvoker;
import org.myeslib.core.storage.SnapshotReader;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleDomain.InventoryItemInstanceFactory;
import org.myeslib.example.SampleDomain.ItemDescriptionGeneratorService;
import org.myeslib.example.infra.HazelcastConfigFactory;
import org.myeslib.example.infra.HazelcastData;
import org.myeslib.example.infra.InventoryItemMapConfigFactory;
import org.myeslib.example.infra.SerializersConfigFactory;
import org.myeslib.example.routes.HzConsumeEventsRoute;
import org.myeslib.hazelcast.storage.HzSnapshotReader;
import org.myeslib.hazelcast.storage.HzUnitOfWorkWriter;
import org.myeslib.util.camel.ReceiveCommandsAsJsonRoute;
import org.myeslib.util.gson.UowFromStringFunction;
import org.myeslib.util.gson.UowToStringFunction;
import org.myeslib.util.h2.ArhCreateTablesHelper;
import org.myeslib.util.hazelcast.HzCamelComponent;
import org.myeslib.util.hazelcast.HzMapStore;
import org.myeslib.util.jdbi.AggregateRootHistoryReaderDao;
import org.myeslib.util.jdbi.AggregateRootHistoryWriterDao;
import org.myeslib.util.jdbi.ArTablesMetadata;
import org.myeslib.util.jdbi.JdbiAggregateRootHistoryAutoCommitWriterDao;
import org.myeslib.util.jdbi.JdbiAggregateRootHistoryReaderDao;
import org.skife.jdbi.v2.DBI;

import com.google.common.base.Function;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

public class HzExampleModule extends AbstractModule {
	
	@Provides
	@Singleton
	public DataSource datasource() {
		JdbcConnectionPool pool = JdbcConnectionPool.create("jdbc:h2:mem:test;MODE=Oracle", "scott", "tiger");
		pool.setMaxConnections(100);
		return pool;
	}

	@Provides
	@Singleton
	public DBI dbi(DataSource ds) {
		return new DBI(ds);
	}
	
	@Provides
	@Singleton
	public Gson gson() {
		return new SampleDomainGsonFactory().create();
	}

	@Provides
	@Singleton
	public Function<UnitOfWork, String> toStringFunction(Gson gson) {
		return new UowToStringFunction(gson);
	}

	@Provides
	@Singleton
	public Function<String, UnitOfWork> fromStringFunction(Gson gson) {
		return new UowFromStringFunction(gson);
	}
	
	@Provides
	@Singleton
	@Named("InventoryItem")
	public String inventoryTableName() {
		return "inventory_item" ;
	}
	
	@Provides
	@Singleton
	public ArTablesMetadata metadata(DBI dbi, @Named("InventoryItem") String tableName) {
		ArTablesMetadata metadata = new ArTablesMetadata(tableName);
		new ArhCreateTablesHelper(metadata, dbi).createTables();
		return metadata;
	}
	
	@Provides
	@Singleton
	public AggregateRootHistoryReaderDao<UUID> arReader(ArTablesMetadata metadata, DBI dbi, Function<String, UnitOfWork> fromStringFunction) {
		return new JdbiAggregateRootHistoryReaderDao(metadata, dbi, fromStringFunction);
	}

	@Provides
	@Singleton
	public AggregateRootHistoryWriterDao<UUID> arWriter(ArTablesMetadata metadata, DBI dbi, Function<UnitOfWork, String> toStringFunction) {
		return new JdbiAggregateRootHistoryAutoCommitWriterDao(dbi, metadata, toStringFunction);
	}
	
	@Provides
	@Singleton
	public HzMapStore mapStore(AggregateRootHistoryReaderDao<UUID> reader, AggregateRootHistoryWriterDao<UUID> writer){
		return new HzMapStore(writer, reader);
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
	public IMap<UUID, AggregateRootHistory> inventoryItemMap(HazelcastInstance hazelcastInstance) {
		return hazelcastInstance.getMap(HazelcastData.INVENTORY_ITEM_AGGREGATE_HISTORY.name());
	}
	
	@Provides
	@Singleton
	public HzUnitOfWorkWriter<UUID> hzUnitOfWorkWriter(IMap<UUID, AggregateRootHistory> inventoryItemMap) {
		return new HzUnitOfWorkWriter<>(inventoryItemMap);
	}
	
	@Provides
	@Singleton
	public CommandHandlerInvoker<UUID, InventoryItemAggregateRoot> cmdInvoker() {
		return new MultiMethodCommandHandlerInvoker<UUID, InventoryItemAggregateRoot>();
	}
	
	@Provides
	@Singleton
	public SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader(    
			              HazelcastInstance hazelcastInstance, 
			              Function<Void, InventoryItemAggregateRoot> factory) {
		Map<UUID, AggregateRootHistory> historyMap = hazelcastInstance.getMap(HazelcastData.INVENTORY_ITEM_AGGREGATE_HISTORY.name());
		Map<UUID, Snapshot<InventoryItemAggregateRoot>> snapshotMap = hazelcastInstance.getMap(HazelcastData.INVENTORY_ITEM_LAST_SNAPSHOT.name());
		return new HzSnapshotReader<UUID, InventoryItemAggregateRoot>(historyMap, snapshotMap, factory);
	}
	
	@Provides
	@Singleton
	@Named("originUri")
	public String originUri() {
		return "direct:processCommand";
		//return "hz:seda:inventory-item-command?transacted=true&concurrentConsumers=10";
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
	
	@Provides
	@Singleton
	public ReceiveCommandsAsJsonRoute receiveCommandsRoute(@Named("originUri") String originUri, Gson gson) {
		return new ReceiveCommandsAsJsonRoute("jetty:http://localhost:8080/inventory-item-command?minThreads=5&maxThreads=10", originUri, gson);
	}
	
	@Override
	protected void configure() {
		
		bind(HzCamelComponent.class);
		bind(HzConsumeEventsRoute.class);
		bind(ItemDescriptionGeneratorService.class).to(ServiceJustForTest.class).asEagerSingleton();;
		bind(InventoryItemMapConfigFactory.class).asEagerSingleton();;
		bind(SerializersConfigFactory.class).asEagerSingleton();;
		
	}
	
	public static class ServiceJustForTest implements ItemDescriptionGeneratorService {
		@Override
		public String generate(UUID id) {
			return "cool";
		}
	}
	
}


