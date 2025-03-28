package org.openhab.io.homekit.util;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum Message {
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

    private static final Map<Short, Message> lookup = new HashMap<Short, Message>();

    static {
        for (Message s : EnumSet.allOf(Message.class)) {
            lookup.put(s.getKey(), s);
        }
    }

    Message(short key) {
        this.key = key;
    }

    Message(int key) {
        this.key = (short) key;
    }

    public short getKey() {
        return key;
    }

    public static Message get(short code) {
        return lookup.get(code);
    }
}
