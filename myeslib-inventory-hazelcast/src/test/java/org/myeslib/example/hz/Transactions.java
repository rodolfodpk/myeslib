package org.myeslib.example.hz;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.transaction.TransactionContext;

public class Transactions {
	
	final HazelcastInstance hz = Hazelcast.newHazelcastInstance();
	
	@Test @Ignore
	public void testLock() throws InterruptedException {

		final IMap<UUID, String> map = hz.getMap("map1");
		
		Runnable r = new Runnable() {
			@Override
			public void run() {
				TransactionContext tc = hz.newTransactionContext();
				try {
					System.out.println("tx " + " " + tc.getTxnId() + " " + Thread.currentThread()) ;
					tc.beginTransaction();
					map.set(UUID.randomUUID(), "value");
				} catch (Exception e) {
					tc.commitTransaction();
				}
			}};
			
		ExecutorService pool = Executors.newFixedThreadPool(10);	

		for (int i=0; i<10; i++) {
			pool.submit(r);
		}
		
		pool.awaitTermination(1, TimeUnit.SECONDS);

		TransactionContext tc = hz.newTransactionContext();
		try {
			System.out.println("tx " + " " + tc.getTxnId() + " " + Thread.currentThread()) ;
			tc.beginTransaction();
			map.set(UUID.randomUUID(), "value");
		} catch (Exception e) {
			tc.commitTransaction();
		}

		
	}
	
}
