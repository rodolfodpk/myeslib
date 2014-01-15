package org.myeslib.hazelcast;

import java.util.Map;

import lombok.AllArgsConstructor;

import org.myeslib.core.AggregateRoot;
import org.myeslib.data.AggregateRootHistory;

import com.hazelcast.core.HazelcastInstance;

@AllArgsConstructor
public class AggregateRootHistoryMapFactory<K, A extends AggregateRoot> {
	
	final HazelcastInstance hz;
	
	public Map<K, AggregateRootHistory> get(final String mapId) {
		Map<K, AggregateRootHistory> map = hz.getMap(mapId);
		return map;
	}

}
