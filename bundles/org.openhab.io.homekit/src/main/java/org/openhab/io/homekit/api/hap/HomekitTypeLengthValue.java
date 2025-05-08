package org.openhab.io.homekit.api.hap;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Enum representing Homekit TLV (Type-Length-Value) types as defined by Apple's Homekit HomekitAccessory Protocol.
 */
@NonNullByDefault
public enum HomekitTypeLengthValue {
    METHOD(0x00, "HomekitMethod", "HomekitMethod to use for pairing"),
    IDENTIFIER(0x01, "Identifier", "Identifier for authentication"),
    SALT(0x02, "Salt", "16+ bytes of random salt"),
    PUBLIC_KEY(0x03, "PublicKey", "Curve25519, SRP public key, or signed Ed25519 key"),
    PROOF(0x04, "Proof", "Ed25519 or SRP proof"),
    ENCRYPTED_DATA(0x05, "EncryptedData", "Encrypted data with auth tag at end"),
    STATE(0x06, "State", "State of the pairing process. 1=M1, 2=M2, etc."),
    ERROR(0x07, "Error", "Error code. Must only be present if error code is not 0"),
    RETRY_DELAY(0x08, "RetryDelay", "Seconds to delay until retrying a setup code"),
    CERTIFICATE(0x09, "Certificate", "X.509 Certificate"),
    SIGNATURE(0x0A, "Signature", "Ed25519 or Apple Authentication Coprocessor signature"),
    PERMISSIONS(0x0B, "Permissions", "Bit value describing permissions of the controller being added"),
    FRAGMENT_DATA(0x0C, "FragmentData", "Non-last fragment of data. If length is 0, it's an ACK"),
    FRAGMENT_LAST(0x0D, "FragmentLast", "Last fragment of data"),
    FLAGS(0x13, "Flags", "HomekitPairing Type Flags (32 bit unsigned integer)"),
    SEPARATOR(0xFF, "Separator", "Zero-length TLV that separates different TLVs in a list");

    private final int type;
    private final String name;
    private final String description;

    HomekitTypeLengthValue(int type, String name, String description) {
        this.type = type;
        this.name = name;
        this.description = description;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public static HomekitTypeLengthValue fromType(int type) {
        for (HomekitTypeLengthValue tlv : values()) {
            if (tlv.type == type) {
                return tlv;
            }
        }
        return null;
    }

    public static boolean isKnownType(int type) {
        return fromType(type) != null;
    }
}
