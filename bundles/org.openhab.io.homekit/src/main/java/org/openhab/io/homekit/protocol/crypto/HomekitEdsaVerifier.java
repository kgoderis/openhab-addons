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

package org.openhab.io.homekit.protocol.crypto;

import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

/**
 * Provides EdDSA (Edwards-curve Digital Signature Algorithm) signature verification for HomeKit protocol.
 *
 * This class implements signature verification using the Ed25519 curve with SHA-512 as specified
 * in the HomeKit Accessory Protocol (HAP). It is used to verify the authenticity of messages
 * and data exchanged during the HomeKit pairing and communication process.
 *
 * Key features:
 * - Uses Ed25519 curve for efficient signature verification
 * - Implements SHA-512 for message hashing
 * - Supports verification of digital signatures
 * - Handles public key management
 *
 * Security considerations:
 * - Uses cryptographically secure EdDSA implementation
 * - Implements proper key specification and parameter handling
 * - Provides secure signature verification
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
public class HomekitEdsaVerifier {

    /** The public key used for signature verification */
    private final PublicKey publicKey;

    /**
     * Creates a new EdDSA verifier with the specified public key.
     *
     * This constructor initializes the verifier with an Ed25519 public key that will be used
     * for signature verification. The public key is converted from its raw byte representation
     * to a proper EdDSAPublicKey instance.
     *
     * @param publicKey The raw public key bytes to use for verification
     * @throws IllegalArgumentException if the public key is invalid
     */
    public HomekitEdsaVerifier(byte[] publicKey) {
        EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("ed25519-sha-512");
        EdDSAPublicKeySpec pubKey = new EdDSAPublicKeySpec(publicKey, spec);
        this.publicKey = new EdDSAPublicKey(pubKey);
    }

    /**
     * Verifies a digital signature against the provided data.
     *
     * This method verifies that the signature was created using the private key corresponding
     * to the public key held by this verifier. The verification process uses EdDSA with SHA-512
     * as specified in the HomeKit protocol.
     *
     * @param data The data that was signed
     * @param signature The signature to verify
     * @return true if the signature is valid, false otherwise
     * @throws Exception if the verification process fails
     */
    public boolean verify(byte[] data, byte[] signature) throws Exception {
        Signature sgr = new EdDSAEngine(MessageDigest.getInstance("SHA-512"));
        sgr.initVerify(publicKey);
        sgr.update(data);

        return sgr.verify(signature);
    }
}
