package org.myeslib.cmdproducer;

import javax.inject.Singleton;

import org.myeslib.cmdproducer.routes.DatasetsRoute;
import org.myeslib.example.SampleDomainGsonFactory;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class CmdProducerModule extends AbstractModule {
	
	@Provides
	@Singleton
	public Gson gson() {
		return new SampleDomainGsonFactory().create();
	}

	@Provides
	@Singleton
	public DatasetsRoute datasetRoute(Gson gson) {
		return new DatasetsRoute(gson, "jetty://http://localhost:8080/inventory-item-command");
	}
	
	@Override
	protected void configure() {
	}
	
}


