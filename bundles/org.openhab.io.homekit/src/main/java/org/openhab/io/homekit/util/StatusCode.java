package org.openhab.io.homekit.util;

public enum StatusCode {
    SUCCESS(0),
    REQUEST_DENIED(-70401),
    UNABLE_TO_PERFORM(-70402),
    BUSY(-70403),
    WRITE_TO_READ_ONLY(-70404),
    READ_TO_WRITE_ONLY(-70405),
    NOTIFICATION_NOT_SUPPORTED(-70406),
    OUT_OF_RESOURCES(-70407),
    TIME_OUT(-70408),
    NOT_EXIST(-70409),
    INVALID_WRITE(-70410),
    UNAUTHORIZED(-70411);

    private final int key;

    StatusCode(int key) {
        this.key = key;
    }

    public int getKey() {
        return key;
    }
}
