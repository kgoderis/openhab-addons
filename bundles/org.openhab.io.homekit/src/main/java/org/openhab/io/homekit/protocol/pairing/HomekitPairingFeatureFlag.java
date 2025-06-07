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
 * Defines the feature flags used in HomeKit pairing protocol.
 *
 * This enum represents the feature flags as defined by Apple's HomeKit Accessory Protocol (HAP).
 * Each flag is represented by a bit mask in a byte and indicates specific capabilities or
 * authentication methods supported by a HomeKit accessory.
 *
 * The flags are used during the pairing process to:
 * - Indicate supported authentication methods
 * - Specify hardware capabilities
 * - Enable or disable specific features
 * - Reserve bits for future use
 *
 * Current supported flags:
 * - NOT_SUPPORTED: No special features supported
 * - APPLE_AUTH_COPROCESSOR: Hardware-based authentication support
 * - SOFTWARE_AUTH: Software-based authentication support
 * - RESERVED_3 through RESERVED_8: Reserved for future use
 *
 * The enum provides utility methods for:
 * - Checking if a flag is set
 * - Setting or clearing flags
 * - Converting between values and enum constants
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
public enum HomekitPairingFeatureFlag {
    /** Indicates no special features are supported */
    NOT_SUPPORTED(0x00, "Not Supported"),
    /** Indicates support for Apple Authentication Coprocessor */
    APPLE_AUTH_COPROCESSOR(0x01, "Supports Apple Authentication Coprocessor"),
    /** Indicates support for Software Authentication */
    SOFTWARE_AUTH(0x02, "Supports Software Authentication"),
    /** Reserved for future use */
    RESERVED_3(0x04, "Reserved"),
    /** Reserved for future use */
    RESERVED_4(0x08, "Reserved"),
    /** Reserved for future use */
    RESERVED_5(0x10, "Reserved"),
    /** Reserved for future use */
    RESERVED_6(0x20, "Reserved"),
    /** Reserved for future use */
    RESERVED_7(0x40, "Reserved"),
    /** Reserved for future use */
    RESERVED_8(0x80, "Reserved");

    private final int mask;
    private final String description;

    /**
     * Creates a new feature flag with the specified mask and description.
     *
     * @param mask The bit mask for this flag
     * @param description A human-readable description of the flag
     */
    HomekitPairingFeatureFlag(int mask, String description) {
        this.mask = mask;
        this.description = description;
    }

    /**
     * Gets the bit mask for this feature flag.
     *
     * @return The integer value representing the bit mask
     */
    public int getMask() {
        return mask;
    }

    /**
     * Gets the human-readable description of this feature flag.
     *
     * @return A string describing the feature flag
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if a specific feature flag is set in a flags value.
     *
     * @param flags The flags value to check
     * @param flag The feature flag to check for
     * @return true if the flag is set, false otherwise
     */
    public static boolean isSet(int flags, HomekitPairingFeatureFlag flag) {
        return (flags & flag.mask) != 0;
    }

    /**
     * Sets a specific feature flag in a flags value.
     *
     * @param flags The current flags value
     * @param flag The feature flag to set
     * @return The new flags value with the specified flag set
     */
    public static int setFlag(int flags, HomekitPairingFeatureFlag flag) {
        return flags | flag.mask;
    }

    /**
     * Clears a specific feature flag in a flags value.
     *
     * @param flags The current flags value
     * @param flag The feature flag to clear
     * @return The new flags value with the specified flag cleared
     */
    public static int clearFlag(int flags, HomekitPairingFeatureFlag flag) {
        return flags & ~flag.mask;
    }

    /**
     * Converts a numeric value to the corresponding feature flag.
     * If the value doesn't match any known flag, returns RESERVED_3.
     *
     * @param value The numeric value to convert
     * @return The corresponding feature flag, or RESERVED_3 if not found
     */
    public static HomekitPairingFeatureFlag fromValue(int value) {
        for (HomekitPairingFeatureFlag featureFlag : values()) {
            if (featureFlag.mask == value) {
                return featureFlag;
            }
        }
        return RESERVED_3; // Default to RESERVED_3 for unknown values
    }
}
