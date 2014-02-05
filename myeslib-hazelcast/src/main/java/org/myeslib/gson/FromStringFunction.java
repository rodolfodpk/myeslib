package org.myeslib.gson;

import lombok.AllArgsConstructor;

import org.myeslib.data.AggregateRootHistory;

import com.google.common.base.Function;
import com.google.gson.Gson;

@AllArgsConstructor
public class FromStringFunction implements Function<String, AggregateRootHistory>{
	private final Gson gson;
	@Override
	public AggregateRootHistory apply(String input) {
		return gson.fromJson(input, AggregateRootHistory.class);
	}

}
