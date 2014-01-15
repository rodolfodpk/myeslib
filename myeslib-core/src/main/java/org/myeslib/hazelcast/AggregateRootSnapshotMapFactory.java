package org.myeslib.hazelcast;

import java.util.Map;

import lombok.AllArgsConstructor;

import org.myeslib.core.AggregateRoot;
import org.myeslib.data.Snapshot;

import com.hazelcast.core.HazelcastInstance;

@AllArgsConstructor
public class AggregateRootSnapshotMapFactory<K, A extends AggregateRoot> {
	
	final HazelcastInstance hz;
	
	public Map<K, Snapshot<A>> get(final String mapId) {
		Map<K, Snapshot<A>> map = hz.getMap(mapId);
		return map;
	}

}
