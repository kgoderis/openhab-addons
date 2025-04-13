package org.openhab.io.homekit.api.hap;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Enum representing HomeKit error codes as defined by Apple's HomeKit Accessory Protocol.
 */
@NonNullByDefault
public enum ErrorCode {
    RESERVED_0(0x00, "Reserved"),
    UNKNOWN(0x01, "Generic error to handle unexpected errors"),
    AUTHENTICATION(0x02, "Setup code or signature verification failed"),
    BACKOFF(0x03, "Client must look at the retry delay TLV item and wait that many seconds before retrying"),
    MAX_PEERS(0x04, "Server cannot accept any more pairings"),
    MAX_TRIES(0x05, "Server reached its maximum number of authentication attempts"),
    UNAVAILABLE(0x06, "Server pairing method is unavailable"),
    BUSY(0x07, "Server is busy and cannot accept a pairing request at this time");

    private final int code;
    private final String description;

    ErrorCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ErrorCode fromCode(int code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.code == code) {
                return errorCode;
            }
        }
        return UNKNOWN; // Default to UNKNOWN for unknown codes
    }

    public static boolean isReserved(int code) {
        return code == 0x00 || (code >= 0x08 && code <= 0xFF);
    }
}
