package org.myeslib.example.hz;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

public class MapLocking {
	
	final HazelcastInstance hz = Hazelcast.newHazelcastInstance();
	
	@Test
	public void testLock() {

		final IMap<String, String> map = hz.getMap("map1");
		final String key = "k1";
		
		Runnable r = new Runnable() {
			@Override
			public void run() {
				final boolean locked = map.tryLock(key);
				System.out.println("lock " + " " + Thread.currentThread() + " "+ locked);
			}};
			
		ExecutorService pool = Executors.newFixedThreadPool(10);	

		for (int i=0; i<10; i++) {
			pool.submit(r);
		}
			
		final boolean locked = map.tryLock(key);
		System.out.println("lock " + " " + Thread.currentThread() + " " + locked);
		
	}
	
}
