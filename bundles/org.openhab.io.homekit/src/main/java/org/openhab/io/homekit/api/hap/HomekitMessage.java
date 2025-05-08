package org.openhab.io.homekit.api.hap;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum HomekitMessage {
    METHOD(0),
    IDENTIFIER(1),
    SALT(2),
    PUBLIC_KEY(3),
    PROOF(4),
    ENCRYPTED_DATA(5),
    STATE(6),
    ERROR(7),
    SIGNATURE(10),
    PERSMISSIONS(11);

    private final short key;

    private static final Map<Short, HomekitMessage> lookup = new HashMap<Short, HomekitMessage>();

    static {
        for (HomekitMessage s : EnumSet.allOf(HomekitMessage.class)) {
            lookup.put(s.getKey(), s);
        }
    }

    HomekitMessage(short key) {
        this.key = key;
    }

    HomekitMessage(int key) {
        this.key = (short) key;
    }

    public short getKey() {
        return key;
    }

    public static HomekitMessage get(short code) {
        return lookup.get(code);
    }
}
