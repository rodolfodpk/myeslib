package org.myeslib.util;

import java.util.ConcurrentModificationException;

public class ValidationHelper {
    
    public static void ensureSameVersion(String id, Long targetVersion, Long snapshotVersion){
        if (!targetVersion.equals(snapshotVersion)) {
            String msg = String.format("** (%s) cmd version (%s) does not match snapshot version (%s)", 
                    id, targetVersion, snapshotVersion);
            throw new ConcurrentModificationException(msg);
        }
    }

}
