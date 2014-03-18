package org.myeslib.example.hazelcast.modules;

import java.util.UUID;

import javax.inject.Singleton;

import lombok.AllArgsConstructor;

import org.myeslib.example.hazelcast.infra.HazelcastConfigFactory;
import org.myeslib.example.hazelcast.infra.InventoryItemMapConfigFactory;
import org.myeslib.example.hazelcast.infra.InventoryItemSerializersConfigFactory;
import org.myeslib.util.hazelcast.HzCamelComponent;
import org.myeslib.util.hazelcast.HzMapStore;
import org.myeslib.util.jdbi.AggregateRootHistoryReaderDao;
import org.myeslib.util.jdbi.UnitOfWorkWriterDao;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

@AllArgsConstructor
public class HazelcastModule extends AbstractModule {
	
    int writeDelaySeconds ;

    @Provides
    @Singleton
    @Named("writeDelaySeconds")
    public int writeDelaySeconds() {
        return writeDelaySeconds;
    }
    
    @Provides
	@Singleton
	public HzMapStore mapStore(AggregateRootHistoryReaderDao<UUID> reader, UnitOfWorkWriterDao<UUID> writer){
		return new HzMapStore(writer, reader);
	}
    
	@Provides
	@Singleton
	public Config config(InventoryItemMapConfigFactory mapConfigFactory, InventoryItemSerializersConfigFactory serializersFactory) {
		return new HazelcastConfigFactory(mapConfigFactory.create(), serializersFactory.create()).getConfig();
	}

	@Provides
	@Singleton
	public HazelcastInstance hazelcastInstance(Config config) {
		return Hazelcast.newHazelcastInstance(config);
	}
	
	@Override
	protected void configure() {
		bind(HzCamelComponent.class);
	}

}


