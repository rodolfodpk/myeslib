package org.myeslib.example.jdbi.modules;

import java.util.Map;
import java.util.UUID;

import javax.inject.Singleton;

import org.myeslib.core.data.Snapshot;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.core.function.CommandHandlerInvoker;
import org.myeslib.core.storage.SnapshotReader;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleDomain.InventoryItemInstanceFactory;
import org.myeslib.example.SampleDomain.ItemDescriptionGeneratorService;
import org.myeslib.example.SampleDomainGsonFactory;
import org.myeslib.example.jdbi.infra.HazelcastData;
import org.myeslib.example.jdbi.routes.InventoryItemCmdProcessor;
import org.myeslib.jdbi.storage.JdbiSnapshotReader;
import org.myeslib.util.MultiMethodCommandHandlerInvoker;
import org.myeslib.util.gson.UowFromStringFunction;
import org.myeslib.util.gson.UowToStringFunction;
import org.myeslib.util.jdbi.AggregateRootHistoryReaderDao;
import org.myeslib.util.jdbi.AggregateRootHistoryWriterDao;
import org.myeslib.util.jdbi.ArTablesMetadata;
import org.myeslib.util.jdbi.JdbiAggregateRootHistoryReaderDao;
import org.myeslib.util.jdbi.JdbiAggregateRootHistoryWriterDao;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

import com.google.common.base.Function;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Named;
import com.hazelcast.core.HazelcastInstance;

public class InventoryItemModule extends AbstractModule {

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
	@Named("inventory_item_table")
	public String inventoryTableName() {
		return "inventory_item" ;
	}

	@Provides
	@Singleton
	public ArTablesMetadata metadata(@Named("inventory_item_table") String tableName) {
		return new ArTablesMetadata(tableName);
	}
	
	@Provides
	@Singleton
	public AggregateRootHistoryReaderDao<UUID> arReader(ArTablesMetadata metadata, DBI dbi, Function<String, UnitOfWork> fromStringFunction) {
		return new JdbiAggregateRootHistoryReaderDao(metadata, dbi, fromStringFunction);
	}

	@Provides
	@Singleton
	public Map<UUID, Snapshot<InventoryItemAggregateRoot>> lastSnapshotMap(HazelcastInstance hazelcastInstance) {
		return hazelcastInstance.getMap(HazelcastData.INVENTORY_ITEM_LAST_SNAPSHOT.name());
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
			AggregateRootHistoryReaderDao<UUID> arReader, 
			Function<Void, InventoryItemAggregateRoot> newInstanceFactory) {
		return new JdbiSnapshotReader<>(lastSnapshotMap, arReader, newInstanceFactory);
	}
	
	@Provides
	@Singleton
	public CommandHandlerInvoker<UUID, InventoryItemAggregateRoot> invoker() {
		return new MultiMethodCommandHandlerInvoker<UUID, InventoryItemAggregateRoot>();
	}
	
	public interface AggregateRootHistoryWriterDaoFactory {
		JdbiAggregateRootHistoryWriterDao create(Handle handle);
	}
	
	@Override
	protected void configure() {

		bind(InventoryItemCmdProcessor.class).asEagerSingleton();
		
		install(new FactoryModuleBuilder()
        .implement(AggregateRootHistoryWriterDao.class, JdbiAggregateRootHistoryWriterDao.class)
        .build(AggregateRootHistoryWriterDaoFactory.class)) ;

		bind(ItemDescriptionGeneratorService.class).to(ServiceJustForTest.class).asEagerSingleton();

	}
	
	public static class ServiceJustForTest implements ItemDescriptionGeneratorService {
		@Override
		public String generate(UUID id) {
			return "cool";
		}
	}
	
}
