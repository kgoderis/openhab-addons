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

/**
 * Defines the status flags used in HomeKit pairing protocol.
 *
 * This enum represents the pairing status flags as defined by Apple's HomeKit Accessory Protocol (HAP).
 * Each flag is represented by a bit mask in a byte and indicates the current state of a HomeKit accessory
 * with respect to pairing and configuration.
 *
 * The flags are used to communicate:
 * - The current pairing state of the accessory
 * - Configuration status
 * - Error conditions
 * - Reserved states for future use
 *
 * Current supported flags:
 * - UNKNOWN: Initial or undetermined state
 * - NOT_PAIRED: No active pairing with controllers
 * - NOT_CONFIGURED: Wi-Fi network not configured
 * - PROBLEM_DETECTED: Error condition detected
 * - RESERVED_4 through RESERVED_8: Reserved for future use
 *
 * The enum provides utility methods for:
 * - Checking if a flag is set
 * - Setting or clearing flags
 * - Detecting reserved flag usage
 * - Converting between values and enum constants
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
public enum HomekitPairingStatusFlag {
    /** Indicates an unknown or undetermined pairing status */
    UNKNOWN(0x00, 1, "HomekitAccessory has an unknown pairing status"),
    /** Indicates the accessory has not been paired with any controllers */
    NOT_PAIRED(0x01, 1, "HomekitAccessory has not been paired with any controllers"),
    /** Indicates the accessory has not been configured for Wi-Fi */
    NOT_CONFIGURED(0x02, 2, "HomekitAccessory has not been configured to join a Wi-Fi network"),
    /** Indicates a problem has been detected on the accessory */
    PROBLEM_DETECTED(0x04, 3, "A problem has been detected on the accessory"),
    /** Reserved for future use */
    RESERVED_4(0x08, 4, "Reserved"),
    /** Reserved for future use */
    RESERVED_5(0x10, 5, "Reserved"),
    /** Reserved for future use */
    RESERVED_6(0x20, 6, "Reserved"),
    /** Reserved for future use */
    RESERVED_7(0x40, 7, "Reserved"),
    /** Reserved for future use */
    RESERVED_8(0x80, 8, "Reserved");

    private final int mask;
    private final int bit;
    private final String description;

    /**
     * Creates a new status flag with the specified mask, bit position, and description.
     *
     * @param mask The bit mask for this flag
     * @param bit The bit position (1-based)
     * @param description A human-readable description of the flag
     */
    HomekitPairingStatusFlag(int mask, int bit, String description) {
        this.mask = mask;
        this.bit = bit;
        this.description = description;
    }

    /**
     * Gets the bit mask for this status flag.
     *
     * @return The integer value representing the bit mask
     */
    public int getMask() {
        return mask;
    }

    /**
     * Gets the bit position for this status flag.
     *
     * @return The 1-based bit position
     */
    public int getBit() {
        return bit;
    }

    /**
     * Gets the human-readable description of this status flag.
     *
     * @return A string describing the status flag
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if a specific status flag is set in a flags value.
     *
     * @param flags The flags value to check
     * @param flag The status flag to check for
     * @return true if the flag is set, false otherwise
     */
    public static boolean isSet(int flags, HomekitPairingStatusFlag flag) {
        return (flags & flag.mask) != 0;
    }

    /**
     * Sets a specific status flag in a flags value.
     *
     * @param flags The current flags value
     * @param flag The status flag to set
     * @return The new flags value with the specified flag set
     */
    public static int setFlag(int flags, HomekitPairingStatusFlag flag) {
        return flags | flag.mask;
    }

    /**
     * Clears a specific status flag in a flags value.
     *
     * @param flags The current flags value
     * @param flag The status flag to clear
     * @return The new flags value with the specified flag cleared
     */
    public static int clearFlag(int flags, HomekitPairingStatusFlag flag) {
        return flags & ~flag.mask;
    }

    /**
     * Checks if any reserved flags are set in a flags value.
     *
     * @param flags The flags value to check
     * @return true if any reserved flags are set, false otherwise
     */
    public static boolean isReserved(int flags) {
        // Check if any reserved bits (4-8) are set
        int reservedMask = 0x08 | 0x10 | 0x20 | 0x40 | 0x80;
        return (flags & reservedMask) != 0;
    }

    /**
     * Converts a numeric value to the corresponding status flag.
     * If the value doesn't match any known flag, returns PROBLEM_DETECTED.
     *
     * @param value The numeric value to convert
     * @return The corresponding status flag, or PROBLEM_DETECTED if not found
     */
    public static HomekitPairingStatusFlag fromValue(int value) {
        for (HomekitPairingStatusFlag pairingStatus : values()) {
            if (pairingStatus.mask == value) {
                return pairingStatus;
            }
        }
        return PROBLEM_DETECTED; // Default to PROBLEM_DETECTED for unknown values
    }
}
