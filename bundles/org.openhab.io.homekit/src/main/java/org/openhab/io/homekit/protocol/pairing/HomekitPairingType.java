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

package org.openhab.io.homekit.protocol.pairing;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Defines the pairing types used in HomeKit protocol.
 *
 * This enum represents the pairing type flags as defined by Apple's HomeKit Accessory Protocol (HAP).
 * Each flag is represented by a bit mask in a 32-bit integer and indicates the specific type of
 * pairing operation being performed.
 *
 * The pairing types control:
 * - The pairing process flow
 * - Key exchange behavior
 * - Session persistence
 * - Security requirements
 *
 * Current supported types:
 * - TRANSIENT: Temporary pairing without key exchange
 * - SPLIT: Split-pair setup with SRP verifier persistence
 *
 * The enum provides utility methods for:
 * - Checking if a type is set
 * - Setting or clearing type flags
 * - Detecting reserved flag usage
 * - Managing pairing session state
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@NonNullByDefault
public enum HomekitPairingType {
    /**
     * Transient pairing type that performs Pair Setup M1-M4 without exchanging public keys.
     * This type is used for temporary or one-time pairing operations.
     */
    TRANSIENT(0x00000010, 4,
            "Transient Pair-Setup (kPairingFlag_Transient) - Pair Setup M1 – M4 without exchanging public keys"),
    /**
     * Split-pair setup type that manages SRP verifier persistence.
     * When combined with TRANSIENT, saves the SRP verifier for the current session.
     * When used alone, uses a previously saved SRP verifier.
     */
    SPLIT(0x01000000, 24,
            "Split-Pair Setup (kPairingFlag_Split) - When set with kPairingFlag_Transient, save the SRP Verifier used in this session. When only kPairingFlag_Split is set, use the saved SRP verifier from the previous session");

    private final int mask;
    private final int bit;
    private final String description;

    /**
     * Creates a new pairing type with the specified mask, bit position, and description.
     *
     * @param mask The bit mask for this type
     * @param bit The bit position (1-based)
     * @param description A human-readable description of the type
     */
    HomekitPairingType(int mask, int bit, String description) {
        this.mask = mask;
        this.bit = bit;
        this.description = description;
    }

    /**
     * Gets the bit mask for this pairing type.
     *
     * @return The integer value representing the bit mask
     */
    public int getMask() {
        return mask;
    }

    /**
     * Gets the bit position for this pairing type.
     *
     * @return The 1-based bit position
     */
    public int getBit() {
        return bit;
    }

    /**
     * Gets the human-readable description of this pairing type.
     *
     * @return A string describing the pairing type
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if a specific pairing type is set in a flags value.
     *
     * @param flags The flags value to check
     * @param type The pairing type to check for
     * @return true if the type is set, false otherwise
     */
    public static boolean isSet(int flags, HomekitPairingType type) {
        return (flags & type.mask) != 0;
    }

    /**
     * Sets a specific pairing type in a flags value.
     *
     * @param flags The current flags value
     * @param type The pairing type to set
     * @return The new flags value with the specified type set
     */
    public static int setFlag(int flags, HomekitPairingType type) {
        return flags | type.mask;
    }

    /**
     * Clears a specific pairing type in a flags value.
     *
     * @param flags The current flags value
     * @param type The pairing type to clear
     * @return The new flags value with the specified type cleared
     */
    public static int clearFlag(int flags, HomekitPairingType type) {
        return flags & ~type.mask;
    }

    /**
     * Checks if any reserved flags are set in a flags value.
     * A flag is considered reserved if it's not one of the known pairing types.
     *
     * @param flags The flags value to check
     * @return true if any reserved flags are set, false otherwise
     */
    public static boolean isReserved(int flags) {
        // Check if any bits other than the known flags are set
        int knownFlags = 0;
        for (HomekitPairingType type : values()) {
            knownFlags |= type.mask;
        }
        return (flags & ~knownFlags) != 0;
    }
}
