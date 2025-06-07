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

package org.openhab.io.homekit.protocol.method;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Defines the set of supported HomeKit protocol methods for device pairing and management.
 *
 * This enum represents the core HomeKit protocol methods that enable secure device pairing,
 * verification, and management operations. Each method is associated with a unique numeric key
 * that is used in the HomeKit protocol communication.
 *
 * The supported methods include:
 * - PAIR_SETUP: Initial pairing process
 * - PAIR_SETUP_WITH_AUTH: Pairing with additional authentication
 * - PAIR_VERIFY: Verification of existing pairing
 * - ADD_PAIRING: Adding a new pairing
 * - REMOVE_PAIRING: Removing an existing pairing
 * - LIST_PAIRINGS: Listing all current pairings
 *
 * The enum provides a bidirectional mapping between method keys and enum values,
 * allowing for efficient lookup and validation of protocol methods.
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
public enum HomekitMethod {
    /** Initial pairing process method */
    PAIR_SETUP(0),
    /** Pairing process with additional authentication */
    PAIR_SETUP_WITH_AUTH(1),
    /** Verification of existing pairing */
    PAIR_VERIFY(2),
    /** Adding a new pairing */
    ADD_PAIRING(3),
    /** Removing an existing pairing */
    REMOVE_PAIRING(4),
    /** Listing all current pairings */
    LIST_PAIRINGS(5);

    private final short key;

    /** Lookup map for efficient method retrieval by key */
    private static final Map<Short, HomekitMethod> lookup = new HashMap<Short, HomekitMethod>();

    static {
        for (HomekitMethod s : EnumSet.allOf(HomekitMethod.class)) {
            lookup.put(s.getKey(), s);
        }
    }

    /**
     * Creates a new HomeKit method with the specified key.
     *
     * @param key The numeric key associated with this method
     */
    HomekitMethod(short key) {
        this.key = key;
    }

    /**
     * Creates a new HomeKit method with the specified integer key.
     * The integer is automatically converted to a short value.
     *
     * @param key The numeric key associated with this method
     */
    HomekitMethod(int key) {
        this.key = (short) key;
    }

    /**
     * Gets the numeric key associated with this method.
     *
     * @return The short value representing this method's key
     */
    public short getKey() {
        return key;
    }

    /**
     * Retrieves a HomeKit method by its numeric key.
     *
     * @param code The numeric key to look up
     * @return The corresponding HomeKit method, or null if not found
     */
    public static HomekitMethod get(short code) {
        return lookup.get(code);
    }
}
