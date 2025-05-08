package org.openhab.io.homekit.api.hap;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum HomekitMethod {
    PAIR_SETUP(0),
    PAIR_SETUP_WITH_AUTH(1),
    PAIR_VERIFY(2),
    ADD_PAIRING(3),
    REMOVE_PAIRING(4),
    LIST_PAIRINGS(5);

    private final short key;

    private static final Map<Short, HomekitMethod> lookup = new HashMap<Short, HomekitMethod>();

    static {
        for (HomekitMethod s : EnumSet.allOf(HomekitMethod.class)) {
            lookup.put(s.getKey(), s);
        }
    }

    HomekitMethod(short key) {
        this.key = key;
    }

    HomekitMethod(int key) {
        this.key = (short) key;
    }

    public short getKey() {
        return key;
    }

    public static HomekitMethod get(short code) {
        return lookup.get(code);
    }
}
