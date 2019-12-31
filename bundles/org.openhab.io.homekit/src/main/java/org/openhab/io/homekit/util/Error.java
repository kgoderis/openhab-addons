package org.openhab.io.homekit.util;

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

    Error(short key) {
        this.key = key;
    }

    Error(int key) {
        this.key = (short) key;
    }

    public short getKey() {
        return key;
    }
}
