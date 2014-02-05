package org.myeslib.example.infra;

import org.apache.camel.component.hazelcast.HazelcastComponent;

import com.hazelcast.core.HazelcastInstance;

public class HzCamelComponent extends HazelcastComponent {
	
	public HzCamelComponent(HazelcastInstance hazelcastInstance) {

		this.setHazelcastInstance(hazelcastInstance);
		
	}

}



