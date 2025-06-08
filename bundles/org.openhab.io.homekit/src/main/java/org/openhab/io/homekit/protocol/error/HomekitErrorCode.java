/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.openhab.io.homekit.protocol.error;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Defines the error codes used in HomeKit protocol communication.
 *
 * This enum represents the standard error codes as defined by Apple's HomeKit Accessory Protocol (HAP).
 * Each error code is associated with a unique numeric value and a human-readable description that
 * indicates the nature of an error encountered during HomeKit operations.
 *
 * The error codes are used to:
 * - Indicate authentication failures
 * - Signal resource constraints
 * - Handle rate limiting
 * - Report availability issues
 * - Manage pairing states
 *
 * Key error codes include:
 * - UNKNOWN: Generic error for unexpected conditions
 * - AUTHENTICATION: Setup code or signature verification failure
 * - BACKOFF: Rate limiting indication
 * - MAX_PEERS: Maximum pairing limit reached
 * - MAX_TRIES: Maximum authentication attempts exceeded
 * - UNAVAILABLE: Pairing method unavailable
 * - BUSY: Server temporarily unavailable
 *
 * The enum provides utility methods for:
 * - Converting between error codes and enum values
 * - Checking for reserved error codes
 * - Accessing error descriptions
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@NonNullByDefault
public enum HomekitErrorCode {
    /** Reserved error code for future use */
    RESERVED_0(0x00, "Reserved"),
    /** Generic error to handle unexpected errors */
    UNKNOWN(0x01, "Generic error to handle unexpected errors"),
    /** Setup code or signature verification failed */
    AUTHENTICATION(0x02, "Setup code or signature verification failed"),
    /** Client must look at the retry delay TLV item and wait that many seconds before retrying */
    BACKOFF(0x03, "Client must look at the retry delay TLV item and wait that many seconds before retrying"),
    /** Server cannot accept any more pairings */
    MAX_PEERS(0x04, "Server cannot accept any more pairings"),
    /** Server reached its maximum number of authentication attempts */
    MAX_TRIES(0x05, "Server reached its maximum number of authentication attempts"),
    /** Server pairing method is unavailable */
    UNAVAILABLE(0x06, "Server pairing method is unavailable"),
    /** Server is busy and cannot accept a pairing request at this time */
    BUSY(0x07, "Server is busy and cannot accept a pairing request at this time");

    /** The numeric error code */
    private final int code;
    /** Human-readable description of the error */
    private final String description;

    /**
     * Creates a new error code with the specified code and description.
     *
     * @param code The numeric error code
     * @param description A human-readable description of the error
     */
    HomekitErrorCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Gets the numeric error code.
     *
     * @return The integer value representing this error code
     */
    public int getCode() {
        return code;
    }

    /**
     * Gets the human-readable description of this error code.
     *
     * @return A string describing the error
     */
    public String getDescription() {
        return description;
    }

    /**
     * Converts a numeric error code to the corresponding enum value.
     * If the code doesn't match any known error, returns UNKNOWN.
     *
     * @param code The numeric error code to convert
     * @return The corresponding error code, or UNKNOWN if not found
     */
    public static HomekitErrorCode fromCode(int code) {
        for (HomekitErrorCode errorCode : values()) {
            if (errorCode.code == code) {
                return errorCode;
            }
        }
        return UNKNOWN; // Default to UNKNOWN for unknown codes
    }

    /**
     * Checks if a numeric error code is reserved for future use.
     * A code is considered reserved if it is 0x00 or in the range 0x08-0xFF.
     *
     * @param code The numeric error code to check
     * @return true if the code is reserved, false otherwise
     */
    public static boolean isReserved(int code) {
        return code == 0x00 || (code >= 0x08 && code <= 0xFF);
    }
}
