package org.myeslib.hazelcast.gson;

import org.myeslib.core.data.AggregateRootHistory;

import com.google.common.base.Function;
import com.google.gson.Gson;
import com.google.inject.Inject;

public class FromStringFunction implements Function<String, AggregateRootHistory>{
	private final Gson gson;
	@Inject
	public FromStringFunction(Gson gson) {
		this.gson = gson;
	}
	@Override
	public AggregateRootHistory apply(String input) {
		return gson.fromJson(input, AggregateRootHistory.class);
	}

}
