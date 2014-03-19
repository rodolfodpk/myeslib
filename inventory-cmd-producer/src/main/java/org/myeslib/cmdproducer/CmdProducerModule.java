package org.myeslib.cmdproducer;

import javax.inject.Singleton;

import lombok.AllArgsConstructor;

import org.myeslib.cmdproducer.routes.DataSetsRoute;
import org.myeslib.example.SampleDomainGsonFactory;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

@AllArgsConstructor
public class CmdProducerModule extends AbstractModule {

    int dataSetSize;
	int delayBetweenDataSets;
	int initialDelay;

	@Provides
	@Singleton
	public Gson gson() {
        return new SampleDomainGsonFactory().create();
	}

	@Provides
	@Singleton
	public DataSetsRoute dataSetsRouteRoute(Gson gson) {
		return new DataSetsRoute(gson, "jetty://http://localhost:8080/inventory-item-command", dataSetSize, delayBetweenDataSets, initialDelay);
	}

	@Override
	protected void configure() {
	}

}
