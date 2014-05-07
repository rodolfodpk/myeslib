package org.myeslib.example.jdbi.modules;

import javax.inject.Singleton;

import lombok.AllArgsConstructor;

import org.myeslib.example.jdbi.routes.JdbiConsumeEventsRoute;
import org.myeslib.example.jdbi.routes.ReceiveCommandsAsJsonRoute;
import org.myeslib.util.gson.CommandFromStringFunction;
import org.myeslib.util.hazelcast.HzJobLocker;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.hazelcast.core.HazelcastInstance;

@AllArgsConstructor
public class CamelModule extends AbstractModule {
	
	int jettyMinThreads;
	int jettyMaxThreads;

	@Provides
	@Singleton
	@Named("originUri")
	public String originUri() {
		return "direct:processCommand";
	}

	@Provides
	@Singleton
	public ReceiveCommandsAsJsonRoute receiveCommandsRoute(@Named("originUri") String originUri, CommandFromStringFunction commandFromStringFunction) {
		String url = String.format("jetty:http://localhost:8080/inventory-item-command?minThreads=%d&maxThreads=%d", jettyMinThreads, jettyMaxThreads);
		return new ReceiveCommandsAsJsonRoute(url, originUri, commandFromStringFunction);
	}
	
	@Provides
	@Singleton
	public HzJobLocker jobLocker(HazelcastInstance hzInstance) {
		return new HzJobLocker(hzInstance, "consumeEventsJob", "10s");
	}
	
	@Override
	protected void configure() {
		bind(JdbiConsumeEventsRoute.class);
		bind(CommandFromStringFunction.class);
	}
	
}


