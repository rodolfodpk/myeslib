package org.myeslib.util.gson;

import com.google.gson.Gson;
import com.google.inject.Inject;
import org.myeslib.core.Command;

import com.google.common.base.Function;


public class CommandToStringFunction implements Function<Command, String> {
    private final Gson gson;
    @Inject
    public CommandToStringFunction(Gson gson){
        this.gson = gson;
    }
    @Override
    public String apply(Command command) {
        return gson.toJson(command);
    }
}
