package org.myeslib.util.gson;

import org.myeslib.core.data.UnitOfWork;

import com.google.common.base.Function;
import com.google.gson.Gson;
import com.google.inject.Inject;

public class UowToStringFunction implements Function<UnitOfWork, String>{
	private final Gson gson;
	@Inject
	public UowToStringFunction(Gson gson) {
		this.gson = gson;
	}
	@Override
	public String apply(UnitOfWork input) {
		return gson.toJson(input, UnitOfWork.class);
	}

}
