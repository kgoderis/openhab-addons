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

package org.openhab.io.homekit.protocol.message;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Defines the message types used in HomeKit protocol communication.
 *
 * This enum represents the standard message types as defined by Apple's HomeKit Accessory Protocol (HAP).
 * Each message type is associated with a unique numeric key that identifies the type of data being
 * transmitted in the HomeKit protocol.
 *
 * The message types are used for:
 * - Authentication and security operations
 * - Key exchange and verification
 * - State management
 * - Error handling
 * - Data encryption
 *
 * Key message types include:
 * - METHOD: Specifies the operation method
 * - IDENTIFIER: Authentication identifier
 * - PUBLIC_KEY: Cryptographic public keys
 * - PROOF: Authentication proofs
 * - ENCRYPTED_DATA: Encrypted payloads
 * - STATE: Operation state
 * - ERROR: Error information
 * - SIGNATURE: Digital signatures
 * - PERMISSIONS: Access control information
 *
 * The enum provides a bidirectional mapping between message type keys and enum values,
 * allowing for efficient lookup and validation of protocol messages.
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
public enum HomekitMessage {
    /** Specifies the operation method to use */
    METHOD(0),
    /** Authentication identifier */
    IDENTIFIER(1),
    /** Random salt for cryptographic operations */
    SALT(2),
    /** Cryptographic public key */
    PUBLIC_KEY(3),
    /** Authentication proof */
    PROOF(4),
    /** Encrypted data payload */
    ENCRYPTED_DATA(5),
    /** Current operation state */
    STATE(6),
    /** Error information */
    ERROR(7),
    /** Digital signature */
    SIGNATURE(10),
    /** Access control permissions */
    PERSMISSIONS(11);

    /** The numeric key associated with this message type */
    private final short key;

    /** Lookup map for efficient message type retrieval by key */
    private static final Map<Short, HomekitMessage> lookup = new HashMap<Short, HomekitMessage>();

    static {
        for (HomekitMessage s : EnumSet.allOf(HomekitMessage.class)) {
            lookup.put(s.getKey(), s);
        }
    }

    /**
     * Creates a new message type with the specified key.
     *
     * @param key The numeric key associated with this message type
     */
    HomekitMessage(short key) {
        this.key = key;
    }

    /**
     * Creates a new message type with the specified integer key.
     * The integer is automatically converted to a short value.
     *
     * @param key The numeric key associated with this message type
     */
    HomekitMessage(int key) {
        this.key = (short) key;
    }

    /**
     * Gets the numeric key associated with this message type.
     *
     * @return The short value representing this message type's key
     */
    public short getKey() {
        return key;
    }

    /**
     * Retrieves a message type by its numeric key.
     *
     * @param code The numeric key to look up
     * @return The corresponding message type, or null if not found
     */
    public static HomekitMessage get(short code) {
        return lookup.get(code);
    }
}
