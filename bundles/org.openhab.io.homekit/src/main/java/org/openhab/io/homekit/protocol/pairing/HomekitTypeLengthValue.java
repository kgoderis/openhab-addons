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
 * Defines the Type-Length-Value (TLV) types used in HomeKit protocol.
 *
 * This enum represents the TLV types as defined by Apple's HomeKit Accessory Protocol (HAP).
 * Each TLV type is identified by a unique numeric code and is used to structure data
 * in the HomeKit protocol communication.
 *
 * The TLV types are used for:
 * - Authentication and security operations
 * - Key exchange and verification
 * - State management
 * - Error handling
 * - Data fragmentation
 *
 * Key TLV types include:
 * - METHOD: Specifies the pairing method to use
 * - IDENTIFIER: Authentication identifier
 * - PUBLIC_KEY: Cryptographic public keys
 * - PROOF: Authentication proofs
 * - ENCRYPTED_DATA: Encrypted payloads
 * - STATE: Pairing process state
 * - ERROR: Error codes and messages
 *
 * The enum provides utility methods for:
 * - Converting between type codes and enum values
 * - Validating type codes
 * - Accessing type metadata
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@NonNullByDefault
public enum HomekitTypeLengthValue {
    /** Specifies the HomeKit method to use for pairing */
    METHOD(0x00, "HomekitMethod", "HomekitMethod to use for pairing"),
    /** Identifier used for authentication */
    IDENTIFIER(0x01, "Identifier", "Identifier for authentication"),
    /** Random salt used in cryptographic operations */
    SALT(0x02, "Salt", "16+ bytes of random salt"),
    /** Public key used in cryptographic operations */
    PUBLIC_KEY(0x03, "PublicKey", "Curve25519, SRP public key, or signed Ed25519 key"),
    /** Cryptographic proof for authentication */
    PROOF(0x04, "Proof", "Ed25519 or SRP proof"),
    /** Encrypted data with authentication tag */
    ENCRYPTED_DATA(0x05, "EncryptedData", "Encrypted data with auth tag at end"),
    /** Current state in the pairing process */
    STATE(0x06, "State", "State of the pairing process. 1=M1, 2=M2, etc."),
    /** Error code for operation failures */
    ERROR(0x07, "Error", "Error code. Must only be present if error code is not 0"),
    /** Delay before retrying setup code */
    RETRY_DELAY(0x08, "RetryDelay", "Seconds to delay until retrying a setup code"),
    /** X.509 certificate for authentication */
    CERTIFICATE(0x09, "Certificate", "X.509 Certificate"),
    /** Cryptographic signature for verification */
    SIGNATURE(0x0A, "Signature", "Ed25519 or Apple Authentication Coprocessor signature"),
    /** Controller permissions bit field */
    PERMISSIONS(0x0B, "Permissions", "Bit value describing permissions of the controller being added"),
    /** Non-final data fragment */
    FRAGMENT_DATA(0x0C, "FragmentData", "Non-last fragment of data. If length is 0, it's an ACK"),
    /** Final data fragment */
    FRAGMENT_LAST(0x0D, "FragmentLast", "Last fragment of data"),
    /** HomeKit pairing type flags */
    FLAGS(0x13, "Flags", "HomekitPairing Type Flags (32 bit unsigned integer)"),
    /** Separator for TLV lists */
    SEPARATOR(0xFF, "Separator", "Zero-length TLV that separates different TLVs in a list");

    private final int type;
    private final String name;
    private final String description;

    /**
     * Creates a new TLV type with the specified type code, name, and description.
     *
     * @param type The numeric type code
     * @param name The short name of the type
     * @param description A detailed description of the type's purpose
     */
    HomekitTypeLengthValue(int type, String name, String description) {
        this.type = type;
        this.name = name;
        this.description = description;
    }

    /**
     * Gets the numeric type code for this TLV type.
     *
     * @return The integer value representing the type code
     */
    public int getType() {
        return type;
    }

    /**
     * Gets the short name of this TLV type.
     *
     * @return The string name of the type
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the detailed description of this TLV type.
     *
     * @return A string describing the type's purpose
     */
    public String getDescription() {
        return description;
    }

    /**
     * Converts a numeric type code to the corresponding TLV type.
     *
     * @param type The numeric type code to convert
     * @return The corresponding TLV type, or null if not found
     */
    public static HomekitTypeLengthValue fromType(int type) throws IllegalArgumentException {
        for (HomekitTypeLengthValue tlv : values()) {
            if (tlv.type == type) {
                return tlv;
            }
        }
        throw new IllegalArgumentException("Unknown type: " + type);
    }

    /**
     * Checks if a numeric type code represents a known TLV type.
     *
     * @param type The numeric type code to check
     * @return true if the type code is known, false otherwise
     */
    public static boolean isKnownType(int type) {
        try {
            fromType(type);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
