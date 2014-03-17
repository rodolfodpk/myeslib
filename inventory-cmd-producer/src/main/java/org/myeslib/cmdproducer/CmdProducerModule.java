package org.myeslib.cmdproducer;

import javax.inject.Singleton;

import lombok.AllArgsConstructor;

import org.myeslib.cmdproducer.routes.DatasetsRoute;
import org.myeslib.example.SampleDomainGsonFactory;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

@AllArgsConstructor
public class CmdProducerModule extends AbstractModule {

    int datasetSize;
	int delayBetweenDatasets;
	int initialDelay;

	@Provides
	@Singleton
	public Gson gson() {
		return new SampleDomainGsonFactory().create();
	}

	@Provides
	@Singleton
	public DatasetsRoute datasetRoute(Gson gson) {
		return new DatasetsRoute(gson, "jetty://http://localhost:8080/inventory-item-command", datasetSize, delayBetweenDatasets, initialDelay);
	}

	@Override
	protected void configure() {
	}

}
