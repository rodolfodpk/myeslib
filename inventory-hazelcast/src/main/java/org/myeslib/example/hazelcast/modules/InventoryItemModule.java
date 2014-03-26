package org.myeslib.example.hazelcast.modules;

import java.util.UUID;

import javax.inject.Singleton;

import com.hazelcast.core.IQueue;
import org.myeslib.core.data.AggregateRootHistory;
import org.myeslib.core.data.Snapshot;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.core.function.CommandHandlerInvoker;
import org.myeslib.core.storage.SnapshotReader;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleDomain.InventoryItemInstanceFactory;
import org.myeslib.example.SampleDomain.ItemDescriptionGeneratorService;
import org.myeslib.example.SampleDomainGsonFactory;
import org.myeslib.example.hazelcast.infra.HazelcastData;
import org.myeslib.example.hazelcast.infra.InventoryItemMapConfigFactory;
import org.myeslib.example.hazelcast.infra.InventoryItemSerializersConfigFactory;
import org.myeslib.example.hazelcast.routes.HzInventoryItemCmdProcessor;
import org.myeslib.hazelcast.storage.HzSnapshotReader;
import org.myeslib.hazelcast.storage.HzUnitOfWorkJournal;
import org.myeslib.util.MultiMethodCommandHandlerInvoker;
import org.myeslib.util.gson.UowFromStringFunction;
import org.myeslib.util.gson.UowToStringFunction;
import org.myeslib.util.jdbi.*;
import org.myeslib.util.jdbi.UnitOfWorkJournalDao;
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
	@Named("aggregateRootName")
	public String aggregateRootName() {
		return "inventory_item" ;
	}
	
	@Provides
	@Singleton
	public AggregateRootHistoryReaderDao<UUID> arReader(ArTablesMetadata metadata, DBI dbi, Function<String, UnitOfWork> fromStringFunction) {
		return new JdbiAggregateRootHistoryReaderDao(metadata, dbi, fromStringFunction);
	}

	@Provides
	@Singleton
	public UnitOfWorkJournalDao<UUID> arWriter(ArTablesMetadata metadata, DBI dbi, Function<UnitOfWork, String> toStringFunction) {
		return new JdbiUnitOfWorkAutoCommitJournalDao(dbi, metadata, toStringFunction);
	}

	@Provides
	@Singleton
	public IMap<UUID, AggregateRootHistory> inventoryItemMap(HazelcastInstance hazelcastInstance) {
		return hazelcastInstance.getMap(HazelcastData.INVENTORY_ITEM_AGGREGATE_HISTORY.name());
	}
	
	@Provides
	@Singleton
	public IMap<UUID, Snapshot<InventoryItemAggregateRoot>> lastSnapshotMap(HazelcastInstance hazelcastInstance) {
		return hazelcastInstance.getMap(HazelcastData.INVENTORY_ITEM_LAST_SNAPSHOT.name());
	}

    @Provides
    @Singleton
    public IQueue<UUID> eventsQueue(HazelcastInstance hazelcastInstance) {
        return hazelcastInstance.getQueue(HazelcastData.INVENTORY_ITEM_EVENTS_QUEUE.name());
    }

	@Provides
	@Singleton
	public HzUnitOfWorkJournal<UUID> hzUnitOfWorkWriter(IMap<UUID, AggregateRootHistory> inventoryItemMap) {
		return new HzUnitOfWorkJournal<>(inventoryItemMap);
	}
	
	@Provides
	@Singleton
	public CommandHandlerInvoker<UUID, InventoryItemAggregateRoot> cmdInvoker() {
		return new MultiMethodCommandHandlerInvoker<UUID, InventoryItemAggregateRoot>();
	}
	
	@Provides
	@Singleton
	public SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader(IMap<UUID, AggregateRootHistory> inventoryItemMap, 
																		   IMap<UUID, Snapshot<InventoryItemAggregateRoot>> lastSnapshotMap, 
			                                                               Function<Void, InventoryItemAggregateRoot> newInstanceFactory) {
		return new HzSnapshotReader<UUID, InventoryItemAggregateRoot>(inventoryItemMap, lastSnapshotMap, newInstanceFactory);
	}

	@Provides
	@Singleton
	public Function<Void, InventoryItemAggregateRoot> factory() {
		return new InventoryItemInstanceFactory();
	}
	
	@Override
	protected void configure() {
		
		bind(HzInventoryItemCmdProcessor.class).asEagerSingleton();
		bind(ArTablesMetadata.class).asEagerSingleton();
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


