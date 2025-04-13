package org.openhab.io.homekit.api.hap;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Enum representing HomeKit feature flags as defined by Apple's HomeKit Accessory Protocol.
 * Each flag is represented by a bit mask in a byte.
 */
@NonNullByDefault
public enum PairingFeatureFlag {
    NOT_SUPPORTED(0x00, "Not Supported"),
    APPLE_AUTH_COPROCESSOR(0x01, "Supports Apple Authentication Coprocessor"),
    SOFTWARE_AUTH(0x02, "Supports Software Authentication"),
    RESERVED_3(0x04, "Reserved"),
    RESERVED_4(0x08, "Reserved"),
    RESERVED_5(0x10, "Reserved"),
    RESERVED_6(0x20, "Reserved"),
    RESERVED_7(0x40, "Reserved"),
    RESERVED_8(0x80, "Reserved");

    private final int mask;
    private final String description;

    PairingFeatureFlag(int mask, String description) {
        this.mask = mask;
        this.description = description;
    }

    public int getMask() {
        return mask;
    }

    public String getDescription() {
        return description;
    }

    public static boolean isSet(int flags, PairingFeatureFlag flag) {
        return (flags & flag.mask) != 0;
    }

    public static int setFlag(int flags, PairingFeatureFlag flag) {
        return flags | flag.mask;
    }

    public static int clearFlag(int flags, PairingFeatureFlag flag) {
        return flags & ~flag.mask;
    }

    public static PairingFeatureFlag fromValue(int value) {
        for (PairingFeatureFlag featureFlag : values()) {
            if (featureFlag.mask == value) {
                return featureFlag;
            }
        }
        return RESERVED_3; // Default to RESERVED_3 for unknown values
    }
}
