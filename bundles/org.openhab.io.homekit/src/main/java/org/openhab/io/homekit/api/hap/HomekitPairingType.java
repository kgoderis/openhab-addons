package org.openhab.io.homekit.api.hap;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Enum representing HomeKit pairing type flags as defined by Apple's HomeKit HomekitAccessory Protocol.
 * Each flag is represented by a bit mask in a 32-bit integer.
 */
@NonNullByDefault
public enum HomekitPairingType {
    TRANSIENT(0x00000010, 4,
            "Transient Pair-Setup (kPairingFlag_Transient) - Pair Setup M1 – M4 without exchanging public keys"),
    SPLIT(0x01000000, 24,
            "Split-Pair Setup (kPairingFlag_Split) - When set with kPairingFlag_Transient, save the SRP Verifier used in this session. When only kPairingFlag_Split is set, use the saved SRP verifier from the previous session");

    private final int mask;
    private final int bit;
    private final String description;

    HomekitPairingType(int mask, int bit, String description) {
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

    public static boolean isSet(int flags, HomekitPairingType type) {
        return (flags & type.mask) != 0;
    }

    public static int setFlag(int flags, HomekitPairingType type) {
        return flags | type.mask;
    }

    public static int clearFlag(int flags, HomekitPairingType type) {
        return flags & ~type.mask;
    }

    public static boolean isReserved(int flags) {
        // Check if any bits other than the known flags are set
        int knownFlags = 0;
        for (HomekitPairingType type : values()) {
            knownFlags |= type.mask;
        }
        return (flags & ~knownFlags) != 0;
    }
}
