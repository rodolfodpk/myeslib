package org.myeslib.util.hazelcast;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

import org.myeslib.core.data.AggregateRootHistory;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.util.jdbi.AggregateRootHistoryReaderDao;
import org.myeslib.util.jdbi.AggregateRootHistoryWriterDao;

import com.google.inject.Inject;
import com.hazelcast.core.MapStore;

@Slf4j
public class HzMapStore implements MapStore<UUID, AggregateRootHistory>{

	private final AggregateRootHistoryWriterDao<UUID> writer;
	private final AggregateRootHistoryReaderDao<UUID> reader;
	
	@Inject
	public HzMapStore(AggregateRootHistoryWriterDao<UUID> writer, AggregateRootHistoryReaderDao<UUID> reader){
		this.writer = writer;
		this.reader = reader;
	}

	@Override
	public AggregateRootHistory load(UUID key) {
		log.info("load {}", key);
		return reader.get(key);
	}

	@Override
	public Map<UUID, AggregateRootHistory> loadAll(Collection<UUID> keys) {
		log.debug("load all");
		Map<UUID, AggregateRootHistory> result = new HashMap<>();
		for (UUID id : keys) {
			result.put(id, reader.get(id));
		}
		return result;
	}

	@Override
	public Set<UUID> loadAllKeys() {
		// TODO check is possible to avoid this initial load
		log.debug("load all keys -- empty");
		Set<UUID> keys = new HashSet<>();
		return keys;
	}

	@Override
	public void store(UUID key, AggregateRootHistory value) {
		// To set timestamp from db onto it ?
		// log.info("store {} {}", key, value);
		log.info("storing {}", key);
		UnitOfWork uow = value.getLastUnitOfWork();
		log.debug("store uow {}", uow);
		writer.insert(key, uow);
	}

	@Override
	public void storeAll(Map<UUID, AggregateRootHistory> map) {
		log.debug("store all");
		for (Map.Entry<UUID, AggregateRootHistory> entry : map.entrySet()) {
			store(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public void delete(UUID key) {
		// never will delete
	}

	@Override
	public void deleteAll(Collection<UUID> keys) {
		// never will delete
	}

}
