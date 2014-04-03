package org.myeslib.util.gson;

import com.google.gson.Gson;
import com.google.inject.Inject;
import org.myeslib.core.Command;

import com.google.common.base.Function;


public class CommandFromStringFunction implements Function<String, Command>{
    private final Gson gson;
    @Inject
    public CommandFromStringFunction(Gson gson){
        this.gson = gson;
    }
    @Override
    public Command apply(String asJson) {
        return gson.fromJson(asJson, Command.class);
    }
}
