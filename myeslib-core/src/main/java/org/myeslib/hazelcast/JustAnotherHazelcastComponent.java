package org.myeslib.hazelcast;

import org.apache.camel.component.hazelcast.HazelcastComponent;

import com.hazelcast.core.HazelcastInstance;

public class JustAnotherHazelcastComponent extends HazelcastComponent {
	
	public JustAnotherHazelcastComponent(HazelcastInstance hazelcastInstance) {

		this.setHazelcastInstance(hazelcastInstance);
		
	}

}



