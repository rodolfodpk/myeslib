package org.myeslib.example.hazelcast.modules;

import java.util.Map;
import java.util.UUID;

import javax.inject.Singleton;

import org.myeslib.core.data.AggregateRootHistory;
import org.myeslib.core.data.Snapshot;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.core.function.CommandHandlerInvoker;
import org.myeslib.core.function.MultiMethodCommandHandlerInvoker;
import org.myeslib.core.storage.SnapshotReader;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleDomain.InventoryItemInstanceFactory;
import org.myeslib.example.SampleDomain.ItemDescriptionGeneratorService;
import org.myeslib.example.SampleDomainGsonFactory;
import org.myeslib.example.hazelcast.infra.HazelcastData;
import org.myeslib.example.hazelcast.infra.InventoryItemMapConfigFactory;
import org.myeslib.example.hazelcast.infra.InventoryItemSerializersConfigFactory;
import org.myeslib.hazelcast.storage.HzSnapshotReader;
import org.myeslib.hazelcast.storage.HzUnitOfWorkWriter;
import org.myeslib.util.gson.UowFromStringFunction;
import org.myeslib.util.gson.UowToStringFunction;
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
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

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
	public ArTablesMetadata metadata(DBI dbi, @Named("inventory_item_table") String tableName) {
		return new ArTablesMetadata(tableName);
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
	public Function<Void, InventoryItemAggregateRoot> factory() {
		return new InventoryItemInstanceFactory();
	}
	
	@Override
	protected void configure() {
		
		bind(InventoryItemMapConfigFactory.class).asEagerSingleton();;
		bind(InventoryItemSerializersConfigFactory.class).asEagerSingleton();;
		bind(ItemDescriptionGeneratorService.class).to(ServiceJustForTest.class).asEagerSingleton();;
		
	}
	
	
	public static class ServiceJustForTest implements ItemDescriptionGeneratorService {
		@Override
		public String generate(UUID id) {
			return "cool";
		}
	}
	
}


