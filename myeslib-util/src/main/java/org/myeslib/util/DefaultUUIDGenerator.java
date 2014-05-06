package org.myeslib.util;

import java.util.UUID;

public class DefaultUUIDGenerator implements UUIDGenerator {

    @Override
    public UUID generate() {
        return UUID.randomUUID();
    }

}
