package org.myeslib.util.gson;

import org.myeslib.core.data.AggregateRootHistory;

import com.google.common.base.Function;
import com.google.gson.Gson;
import com.google.inject.Inject;

public class ArhToStringFunction implements Function<AggregateRootHistory, String>{
	private final Gson gson;
	@Inject
	public ArhToStringFunction(Gson gson) {
		this.gson = gson;
	}
	@Override
	public String apply(AggregateRootHistory input) {
		return gson.toJson(input);
	}

}
