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

package org.openhab.io.homekit.protocol.status;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Defines the status codes used in HomeKit protocol communication.
 *
 * This enum represents the standard status codes as defined by Apple's HomeKit Accessory Protocol (HAP).
 * Each status code is associated with a unique numeric value that indicates the result of a HomeKit
 * operation or the current state of an accessory.
 *
 * The status codes are used to:
 * - Indicate success or failure of operations
 * - Report error conditions
 * - Signal resource constraints
 * - Communicate authorization states
 * - Handle timing and availability issues
 *
 * Key status codes include:
 * - SUCCESS: Operation completed successfully
 * - REQUEST_DENIED: Operation was denied
 * - UNABLE_TO_PERFORM: Operation could not be performed
 * - BUSY: Accessory is currently busy
 * - WRITE_TO_READ_ONLY: Attempted to write to read-only characteristic
 * - READ_TO_WRITE_ONLY: Attempted to read from write-only characteristic
 * - NOTIFICATION_NOT_SUPPORTED: Characteristic does not support notifications
 * - OUT_OF_RESOURCES: Accessory has insufficient resources
 * - TIME_OUT: Operation timed out
 * - NOT_EXIST: Requested resource does not exist
 * - INVALID_WRITE: Invalid write operation attempted
 * - UNAUTHORIZED: Operation requires authorization
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@NonNullByDefault
public enum HomekitStatusCode {
    /** Operation completed successfully */
    SUCCESS(0),
    /** Operation was denied */
    REQUEST_DENIED(-70401),
    /** Operation could not be performed */
    UNABLE_TO_PERFORM(-70402),
    /** Accessory is currently busy */
    BUSY(-70403),
    /** Attempted to write to read-only characteristic */
    WRITE_TO_READ_ONLY(-70404),
    /** Attempted to read from write-only characteristic */
    READ_TO_WRITE_ONLY(-70405),
    /** Characteristic does not support notifications */
    NOTIFICATION_NOT_SUPPORTED(-70406),
    /** Accessory has insufficient resources */
    OUT_OF_RESOURCES(-70407),
    /** Operation timed out */
    TIME_OUT(-70408),
    /** Requested resource does not exist */
    NOT_EXIST(-70409),
    /** Invalid write operation attempted */
    INVALID_WRITE(-70410),
    /** Operation requires authorization */
    UNAUTHORIZED(-70411);

    private final int key;

    /**
     * Creates a new status code with the specified numeric value.
     *
     * @param key The numeric value associated with this status code
     */
    HomekitStatusCode(int key) {
        this.key = key;
    }

    /**
     * Gets the numeric value associated with this status code.
     *
     * @return The integer value representing this status code
     */
    public int getKey() {
        return key;
    }
}
