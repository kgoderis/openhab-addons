package org.openhab.io.homekit.util;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum Method {
    PAIR_SETUP(0),
    PAIR_SETUP_WITH_AUTH(1),
    PAIR_VERIFY(2),
    ADD_PAIRING(3),
    REMOVE_PAIRING(4),
    LIST_PAIRINGS(5);

    private final short key;

    private static final Map<Short, Method> lookup = new HashMap<Short, Method>();

    static {
        for (Method s : EnumSet.allOf(Method.class)) {
            lookup.put(s.getKey(), s);
        }
    }

    Method(short key) {
        this.key = key;
    }

    Method(int key) {
        this.key = (short) key;
    }

    public short getKey() {
        return key;
    }

    public static Method get(short code) {
        return lookup.get(code);
    }
}
