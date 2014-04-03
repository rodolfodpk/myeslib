package org.myeslib.cmdproducer;

import javax.inject.Singleton;

import lombok.AllArgsConstructor;

import org.myeslib.cmdproducer.routes.CommandsDataSetsRoute;
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
	public CommandsDataSetsRoute dataSetsRouteRoute(Gson gson) {
		return new CommandsDataSetsRoute(gson, "jetty://http://localhost:8080/inventory-item-command", dataSetSize, delayBetweenDataSets, initialDelay);
	}

	@Override
	protected void configure() {
	}

}
