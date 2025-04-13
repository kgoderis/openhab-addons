package org.openhab.io.homekit.api.hap;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Enum representing HomeKit pairing status flags as defined by Apple's HomeKit Accessory Protocol.
 * Each flag is represented by a bit mask in a byte.
 */
@NonNullByDefault
public enum PairingStatusFlag {
    UNKNOWN(0x00, 1, "Accessory has an unknown pairing status"),
    NOT_PAIRED(0x01, 1, "Accessory has not been paired with any controllers"),
    NOT_CONFIGURED(0x02, 2, "Accessory has not been configured to join a Wi-Fi network"),
    PROBLEM_DETECTED(0x04, 3, "A problem has been detected on the accessory"),
    RESERVED_4(0x08, 4, "Reserved"),
    RESERVED_5(0x10, 5, "Reserved"),
    RESERVED_6(0x20, 6, "Reserved"),
    RESERVED_7(0x40, 7, "Reserved"),
    RESERVED_8(0x80, 8, "Reserved");

    private final int mask;
    private final int bit;
    private final String description;

    PairingStatusFlag(int mask, int bit, String description) {
        this.mask = mask;
        this.bit = bit;
        this.description = description;
    }

    public int getMask() {
        return mask;
    }

    public int getBit() {
        return bit;
    }

    public String getDescription() {
        return description;
    }

    public static boolean isSet(int flags, PairingStatusFlag flag) {
        return (flags & flag.mask) != 0;
    }

    public static int setFlag(int flags, PairingStatusFlag flag) {
        return flags | flag.mask;
    }

    public static int clearFlag(int flags, PairingStatusFlag flag) {
        return flags & ~flag.mask;
    }

    public static boolean isReserved(int flags) {
        // Check if any reserved bits (4-8) are set
        int reservedMask = 0x08 | 0x10 | 0x20 | 0x40 | 0x80;
        return (flags & reservedMask) != 0;
    }

    public static PairingStatusFlag fromValue(int value) {
        for (PairingStatusFlag pairingStatus : values()) {
            if (pairingStatus.mask == value) {
                return pairingStatus;
            }
        }
        return PROBLEM_DETECTED; // Default to PROBLEM_DETECTED for unknown values
    }
}
