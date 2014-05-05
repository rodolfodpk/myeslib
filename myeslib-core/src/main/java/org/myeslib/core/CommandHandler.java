package org.myeslib.core;

import java.util.List;

public interface CommandHandler<C extends Command> {
    
    List<? extends Event> handle(C command);

}
