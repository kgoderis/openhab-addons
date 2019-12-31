package org.openhab.io.homekit.util;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum Error {
    RESERVED(0),
    UNKNOWN(1),
    AUTHENTICATION(2),
    BACKOFF(3),
    MAXPEERS(4),
    MAXTRIES(5),
    UNAVAILABLE(6),
    BUSY(7);

    private final short key;

    private static final Map<Short, Error> lookup = new HashMap<Short, Error>();

    static {
        for (Error s : EnumSet.allOf(Error.class)) {
            lookup.put(s.getKey(), s);
        }
    }

    Error(short key) {
        this.key = key;
    }

    Error(int key) {
        this.key = (short) key;
    }

    public short getKey() {
        return key;
    }

    public static Error get(short code) {
        return lookup.get(code);
    }
}
