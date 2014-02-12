package org.myeslib.util.hazelcast;

import lombok.extern.slf4j.Slf4j;

import org.apache.camel.component.hazelcast.HazelcastComponent;

import com.hazelcast.core.HazelcastInstance;

@Slf4j
public class HzCamelComponent extends HazelcastComponent {
	
	protected HazelcastInstance hz ;
	
	public HzCamelComponent(HazelcastInstance hazelcastInstance) {
		this.hz = hazelcastInstance;
		this.setHazelcastInstance(hazelcastInstance);
		
	}
	
    @Override
    public void doStop() throws Exception {
    	if (hz.getLifecycleService().isRunning()) {
    		log.info("Will shutdown hazelcast...");
    	 	hz.getLifecycleService().shutdown();
    	}
        super.doStop();
    }

}



