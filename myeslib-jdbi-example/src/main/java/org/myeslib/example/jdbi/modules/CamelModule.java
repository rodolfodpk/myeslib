package org.myeslib.example.jdbi.modules;

import javax.inject.Singleton;

import org.myeslib.example.jdbi.routes.JdbiConsumeEventsRoute;
import org.myeslib.example.util.camel.ReceiveCommandsAsJsonRoute;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;

public class CamelModule extends AbstractModule {

	@Provides
	@Singleton
	@Named("originUri")
	public String originUri() {
		return "direct:processCommand";
	}

	@Provides
	@Singleton
	public ReceiveCommandsAsJsonRoute receiveCommandsRoute(@Named("originUri") String originUri, Gson gson) {
		return new ReceiveCommandsAsJsonRoute("jetty:http://localhost:8080/inventory-item-command?minThreads=5&maxThreads=10", originUri, gson);
	}
	
	@Override
	protected void configure() {
		bind(JdbiConsumeEventsRoute.class);
	}
	
}


