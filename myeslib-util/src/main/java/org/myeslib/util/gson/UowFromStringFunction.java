package org.myeslib.util.gson;

import org.myeslib.core.data.UnitOfWork;

import com.google.common.base.Function;
import com.google.gson.Gson;
import com.google.inject.Inject;

public class UowFromStringFunction implements Function<String, UnitOfWork>{
	private final Gson gson;
	@Inject
	public UowFromStringFunction(Gson gson) {
		this.gson = gson;
	}
	@Override
	public UnitOfWork apply(String input) {
		return gson.fromJson(input, UnitOfWork.class);
	}

}
