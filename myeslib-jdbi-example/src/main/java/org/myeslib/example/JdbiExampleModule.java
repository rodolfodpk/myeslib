package org.myeslib.example;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Singleton;
import javax.sql.DataSource;

import org.h2.jdbcx.JdbcConnectionPool;
import org.myeslib.core.data.Snapshot;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.core.function.CommandHandlerInvoker;
import org.myeslib.core.storage.SnapshotReader;
import org.myeslib.core.storage.UnitOfWorkWriter;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleDomain.InventoryItemInstanceFactory;
import org.myeslib.example.SampleDomain.ItemDescriptionGeneratorService;
import org.myeslib.example.infra.HazelcastData;
import org.myeslib.example.routes.JdbiConsumeEventsRoute;
import org.myeslib.jdbi.AggregateRootHistoryReader;
import org.myeslib.jdbi.JdbiAggregateRootHistoryReader;
import org.myeslib.jdbi.function.JdbiCommandHandlerInvoker;
import org.myeslib.jdbi.storage.JdbiSnapshotReader;
import org.myeslib.jdbi.storage.JdbiUnitOfWorkWriter;
import org.myeslib.util.camel.example.dataset.DatasetsRoute;
import org.myeslib.util.gson.UowFromStringFunction;
import org.myeslib.util.gson.UowToStringFunction;
import org.myeslib.util.h2.ArhCreateTablesHelper;
import org.myeslib.util.hazelcast.HzCamelComponent;
import org.myeslib.util.jdbi.ArTablesMetadata;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

import com.google.common.base.Function;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Named;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class JdbiExampleModule extends AbstractModule {
	
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
	@Named("InventoryItemTable")
	public String inventoryTableName() {
		return "inventory_item" ;
	}

	@Provides
	@Singleton
	public ArTablesMetadata metadata(DBI dbi, @Named("InventoryItemTable") String tableName) {
		ArTablesMetadata metadata = new ArTablesMetadata(tableName);
		new ArhCreateTablesHelper(metadata, dbi).createTables();;
		return metadata;
	}
	
	@Provides
	@Singleton
	public Gson gson() {
		return new SampleDomainGsonFactory().create();
	}

	@Provides
	@Singleton
	public ConcurrentMap<String, Object> userContext() {
		ConcurrentMap<String, Object> userContext = new ConcurrentHashMap<>();
		return userContext;
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
	public Map<UUID, Snapshot<InventoryItemAggregateRoot>> inventoryItemMap(HazelcastInstance hazelcastInstance) {
		return hazelcastInstance.getMap(HazelcastData.INVENTORY_ITEM_LAST_SNAPSHOT.name());
	}
		
	@Provides
	@Singleton
	public AggregateRootHistoryReader<UUID> arReader(ArTablesMetadata metadata, DBI dbi, Function<String, UnitOfWork> fromStringFunction) {
		return new JdbiAggregateRootHistoryReader(metadata, dbi, fromStringFunction);
	}
	
	@Provides
	@Singleton
	public Function<Void, InventoryItemAggregateRoot> factory() {
		return new InventoryItemInstanceFactory();
	}
	
	@Provides
	@Singleton
	public SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader(
			Map<UUID, Snapshot<InventoryItemAggregateRoot>> lastSnapshotMap, 
			AggregateRootHistoryReader<UUID> arReader, 
			Function<Void, InventoryItemAggregateRoot> newInstanceFactory) {
		return new JdbiSnapshotReader<>(lastSnapshotMap, arReader, newInstanceFactory);
	}
	
	@Provides
	@Singleton
	public CommandHandlerInvoker<UUID, InventoryItemAggregateRoot> invoker() {
		return new JdbiCommandHandlerInvoker<UUID, InventoryItemAggregateRoot>();
	}
	
	public interface UnitOfWorkWriterFactory {
		JdbiUnitOfWorkWriter<UUID> create(Handle handle);
	}

	@Override
	protected void configure() {

		bind(DatasetsRoute.class);
		bind(JdbiConsumeEventsRoute.class);
		bind(ItemDescriptionGeneratorService.class).to(ServiceJustForTest.class).asEagerSingleton();;

		install(new FactoryModuleBuilder()
	        .implement(UnitOfWorkWriter.class, JdbiUnitOfWorkWriter.class)
	        .build(UnitOfWorkWriterFactory.class)) ;
		
	}
	
	public static class ServiceJustForTest implements ItemDescriptionGeneratorService {
		@Override
		public String generate(UUID id) {
			return "cool";
		}
	}
	
}


