package org.myeslib.util.hazelcast;

import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.myeslib.util.TimeUnitHelper;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

@Slf4j
public class HzJobLocker {

	public static final String LOCK_FAILED_MSG = "Cannot lock. May be there is another job instance working concurrently";
	private static final String TARGET_MAP = "distributedLockMap";

	@Inject
	public HzJobLocker(HazelcastInstance instance, @Named("job.id") String jobId, @Named("job.lock.duration") String jobDuration) {
		this.instance = instance;
		this.jobId = jobId;
		this.jobDuration = jobDuration;
	}

	private final HazelcastInstance instance;
	private final String jobId;
	private final String jobDuration;
	
	public boolean lock() {
		boolean success ;
		try {
			TimeUnitHelper helper = new TimeUnitHelper(jobDuration);
			Long durationInNumber = helper.getDurationAsNumber();
			TimeUnit durationTime = helper.getDurationTime();
			log.info(String.format("Will try to lock for jobId %s for %s %s...", jobId, durationInNumber.toString(), durationTime.toString()));
			IMap<String, ?> map = instance.getMap(TARGET_MAP);
			map.tryLock(jobId, durationInNumber, durationTime);
			success = map.isLocked(jobId);
		} catch (Throwable t) {
			t.printStackTrace();
			success = false;
		}
		return success;
	}
	
}
