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

import java.math.BigInteger;

import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.agreement.srp.SRP6Client;
import org.bouncycastle.crypto.agreement.srp.SRP6Util;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * HAP-compatible SRP-6a client implementation extending BouncyCastle's SRP6Client.
 *
 * This class provides a client-side implementation of the SRP-6a protocol specifically
 * designed for HomeKit Accessory Protocol (HAP) compatibility. It extends BouncyCastle's
 * SRP6Client and adds custom evidence calculation routines that match the HAP specification.
 *
 * Key features:
 * - Extends BouncyCastle's SRP6Client for robust cryptographic operations
 * - Implements HAP-specific evidence calculation (M1 and M2)
 * - Uses SHA-512 as the hash function as required by HAP
 * - Supports the 3072-bit group from RFC5054
 * - Provides state management for the authentication process
 *
 * The evidence calculation follows the HAP specification:
 * - M1 = H(H(N) xor H(g) | H(I) | s | A | B | H(S))
 * - M2 = H(A | M1 | H(S))
 *
 * @author Karel Goderis - HomeKit adaptation
 */
@NonNullByDefault
public class HomekitSRP6Client extends SRP6Client {

    private byte[] salt = new byte[0];
    private byte[] identity = new byte[0];
    private byte[] password = new byte[0];

    /**
     * Initialises the client to begin new authentication attempt with the HAP SRP-6a parameters
     */
    public void init() {
        this.N = HomekitEncryptionEngine.N_3072;
        this.g = HomekitEncryptionEngine.G;
        this.random = HomekitEncryptionEngine.getSecureRandom();
        this.digest = new SHA512Digest();
    }

    /**
     * Gets the session key 'S'.
     *
     * @return The session key 'S', or null if not yet computed
     */
    public @Nullable BigInteger getPremasterSecret() {
        return S;
    }

    public @Nullable BigInteger getSessionKey() {
        return Key;
    }

    /**
     * Gets the client evidence message 'M1'.
     *
     * @return The client evidence message 'M1', or null if not yet computed
     */
    public @Nullable BigInteger getClientEvidence() {
        return M1;
    }

    /**
     * Gets the server evidence message 'M2'.
     *
     * @return The server evidence message 'M2', or null if not yet computed
     */
    public @Nullable BigInteger getServerEvidence() {
        return M2;
    }

    /**
     * Gets the client public key 'A'.
     *
     * @return The client public key 'A', or null if not yet computed
     */
    public @Nullable BigInteger getClientPublicKey() {
        return A;
    }

    /**
     * Gets the server public key 'B'.
     *
     * @return The server public key 'B', or null if not yet computed
     */
    public @Nullable BigInteger getServerPublicKey() {
        return B;
    }

    // add getters for all fields
    public @Nullable BigInteger getSafePrime() {
        return N;
    }

    public @Nullable BigInteger getGroupParameter() {
        return g;
    }

    public byte[] getIdentity() {
        return identity;
    }

    public byte[] getPassword() {
        return password;
    }

    public byte[] getSalt() {
        return salt;
    }

    /**
     * Sets the private value 'a' for deterministic testing.
     * 
     * @param privateValue The private value to use, or null to use random generation
     */
    public void setPrivateValue(@Nullable BigInteger privateValue) {
        this.a = privateValue;
    }

    /**
     * Gets the private value 'a'.
     *
     * @return The private value 'a', or null if not yet computed
     */
    public @Nullable BigInteger getPrivateValue() {
        return a;
    }

    /**
     * Generates client's credentials given the client's salt, identity and password
     * 
     * @param salt The salt used in the client's verifier.
     * @param identity The user's identity (eg. username)
     * @param password The user's password
     * @return Client's public value to send to server
     * @throws IllegalArgumentException if any required parameter is null or empty
     * @throws IllegalStateException if internal parameters are not initialized
     */
    @SuppressWarnings("null")
    public BigInteger generateSRP6aClientCredentials(byte[] salt, byte[] identity, byte[] password) {
        // Validate input parameters
        StringBuilder missingParams = new StringBuilder();
        if (salt == null || salt.length == 0) {
            missingParams.append("salt, ");
        }
        if (identity == null || identity.length == 0) {
            missingParams.append("identity, ");
        }
        if (password == null || password.length == 0) {
            missingParams.append("password, ");
        }

        if (missingParams.length() > 0) {
            // Remove trailing comma and space
            missingParams.setLength(missingParams.length() - 2);
            throw new IllegalArgumentException(
                    "Missing required parameters for client credentials: " + missingParams.toString());
        }

        // Validate internal parameters
        StringBuilder missingInternalParams = new StringBuilder();
        if (this.digest == null) {
            missingInternalParams.append("digest, ");
        }
        if (this.N == null) {
            missingInternalParams.append("N (safe prime), ");
        }
        if (this.g == null) {
            missingInternalParams.append("g (group parameter), ");
        }

        if (missingInternalParams.length() > 0) {
            // Remove trailing comma and space
            missingInternalParams.setLength(missingInternalParams.length() - 2);
            throw new IllegalStateException("Internal parameters not initialized: " + missingInternalParams.toString()
                    + ". Call init() first.");
        }

        // Store the credentials for later use in evidence calculation
        this.salt = salt;
        this.identity = identity;
        this.password = password;

        // If a private value was set, use it; otherwise use random generation
        if (a != null) {
            // Compute the public value using the set private value
            this.x = SRP6Util.calculateX(digest, N, salt, identity, password);
            this.A = g.modPow(a, N);
            return A;
        } else {
            // Call the parent method to generate credentials with random private value
            return super.generateClientCredentials(salt, identity, password);
        }
    }

    /**
     * Computes the client evidence message M1 using the previously received values.
     * To be called after calculating the secret S.
     * 
     * @return M1: the client side generated evidence message
     * @throws CryptoException
     */
    @Override
    public BigInteger calculateClientEvidenceMessage() throws CryptoException {
        // Verify pre-requirements
        StringBuilder missingParams = new StringBuilder();
        if (this.A == null) {
            missingParams.append("A (client public key), ");
        }
        if (this.B == null) {
            missingParams.append("B (server public key), ");
        }
        if (this.S == null) {
            missingParams.append("S (session key), ");
        }
        if (this.digest == null) {
            missingParams.append("digest, ");
        }
        if (this.N == null) {
            missingParams.append("N (safe prime), ");
        }
        if (this.g == null) {
            missingParams.append("g (group parameter), ");
        }
        if (this.identity == null || this.identity.length == 0) {
            missingParams.append("identity, ");
        }
        if (this.salt == null || this.salt.length == 0) {
            missingParams.append("salt, ");
        }

        if (missingParams.length() > 0) {
            // Remove trailing comma and space
            missingParams.setLength(missingParams.length() - 2);
            throw new CryptoException(
                    "Impossible to compute M1: missing required parameters: " + missingParams.toString());
        }

        // compute the client evidence message 'M1'
        this.M1 = calculateM1(digest, N, A, B, S, g, identity, salt);
        return M1;
    }

    /**
     * Authenticates the server evidence message M2 received and saves it only if correct.
     * 
     * @param serverM2 the server side generated evidence message
     * @return A boolean indicating if the server message M2 was the expected one.
     * @throws CryptoException
     */
    @Override
    public boolean verifyServerEvidenceMessage(@Nullable BigInteger serverM2) throws CryptoException {
        // Verify pre-requirements
        StringBuilder missingParams = new StringBuilder();
        if (this.A == null) {
            missingParams.append("A (client public key), ");
        }
        if (this.M1 == null) {
            missingParams.append("M1 (client evidence message), ");
        }
        if (this.S == null) {
            missingParams.append("S (session key), ");
        }
        if (this.digest == null) {
            missingParams.append("digest, ");
        }
        if (this.N == null) {
            missingParams.append("N (safe prime), ");
        }

        if (missingParams.length() > 0) {
            // Remove trailing comma and space
            missingParams.setLength(missingParams.length() - 2);
            throw new CryptoException(
                    "Impossible to compute and verify M2: missing required parameters: " + missingParams.toString());
        }

        if (serverM2 == null) {
            return false;
        }

        // Compute the own server evidence message 'M2'
        BigInteger computedM2 = calculateM2(digest, N, A, M1, S);
        if (computedM2.equals(serverM2)) {
            this.M2 = serverM2;
            return true;
        }
        return false;
    }

    /**
     * Computes the client evidence message 'M1' using HAP-specific formula.
     *
     * @param digest The message digest instance
     * @param N The modulus
     * @param g The generator
     * @param identity The user identity
     * @param salt The salt
     * @param A The client public key
     * @param B The server public key
     * @param S The session key
     * @return The client evidence message 'M1'
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    protected BigInteger calculateM1(Digest digest, BigInteger N, BigInteger A, BigInteger B, BigInteger S,
            BigInteger g, byte[] identity, byte[] salt) {

        // Validate all parameters
        StringBuilder missingParams = new StringBuilder();
        if (digest == null) {
            missingParams.append("digest, ");
        }
        if (N == null) {
            missingParams.append("N (modulus), ");
        }
        if (A == null) {
            missingParams.append("A (client public key), ");
        }
        if (B == null) {
            missingParams.append("B (server public key), ");
        }
        if (S == null) {
            missingParams.append("S (session key), ");
        }
        if (g == null) {
            missingParams.append("g (generator), ");
        }
        if (identity == null || identity.length == 0) {
            missingParams.append("identity, ");
        }
        if (salt == null || salt.length == 0) {
            missingParams.append("salt, ");
        }

        if (missingParams.length() > 0) {
            // Remove trailing comma and space
            missingParams.setLength(missingParams.length() - 2);
            throw new IllegalArgumentException(
                    "Missing required parameters for M1 calculation: " + missingParams.toString());
        }

        // M1 = H(H(N) xor H(g) | H(I) | s | A | B | H(S))
        digest.reset();
        digest.update(N.toByteArray(), 0, N.toByteArray().length);
        byte[] hN = new byte[digest.getDigestSize()];
        digest.doFinal(hN, 0);

        digest.reset();
        digest.update(g.toByteArray(), 0, g.toByteArray().length);
        byte[] hg = new byte[digest.getDigestSize()];
        digest.doFinal(hg, 0);

        // H(N) xor H(g)
        byte[] hNxorHg = new byte[hN.length];
        for (int i = 0; i < hN.length; i++) {
            hNxorHg[i] = (byte) (hN[i] ^ hg[i]);
        }

        digest.reset();
        digest.update(identity, 0, identity.length);
        byte[] hu = new byte[digest.getDigestSize()];
        digest.doFinal(hu, 0);

        digest.reset();
        digest.update(S.toByteArray(), 0, S.toByteArray().length);
        byte[] hS = new byte[digest.getDigestSize()];
        digest.doFinal(hS, 0);

        digest.reset();
        digest.update(hNxorHg, 0, hNxorHg.length);
        digest.update(hu, 0, hu.length);
        digest.update(salt, 0, salt.length);
        digest.update(A.toByteArray(), 0, A.toByteArray().length);
        digest.update(B.toByteArray(), 0, B.toByteArray().length);
        digest.update(hS, 0, hS.length);

        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);
        return new BigInteger(1, result);
    }

    /**
     * Computes the server evidence message 'M2' using HAP-specific formula.
     *
     * @param digest The message digest instance
     * @param N The modulus
     * @param A The client public key
     * @param M1 The client evidence message
     * @param S The session key
     * @return The server evidence message 'M2'
     * @throws IllegalArgumentException if any parameter is null
     */
    private BigInteger calculateM2(Digest digest, BigInteger N, BigInteger A, BigInteger M1, BigInteger S) {

        // Validate all parameters
        StringBuilder missingParams = new StringBuilder();
        if (digest == null) {
            missingParams.append("digest, ");
        }
        if (N == null) {
            missingParams.append("N (modulus), ");
        }
        if (A == null) {
            missingParams.append("A (client public key), ");
        }
        if (M1 == null) {
            missingParams.append("M1 (client evidence message), ");
        }
        if (S == null) {
            missingParams.append("S (session key), ");
        }

        if (missingParams.length() > 0) {
            // Remove trailing comma and space
            missingParams.setLength(missingParams.length() - 2);
            throw new IllegalArgumentException(
                    "Missing required parameters for M2 calculation: " + missingParams.toString());
        }

        // M2 = H(A | M1 | H(S))
        digest.reset();
        digest.update(S.toByteArray(), 0, S.toByteArray().length);
        byte[] hS = new byte[digest.getDigestSize()];
        digest.doFinal(hS, 0);

        digest.reset();
        digest.update(A.toByteArray(), 0, A.toByteArray().length);
        digest.update(M1.toByteArray(), 0, M1.toByteArray().length);
        digest.update(hS, 0, hS.length);

        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);
        return new BigInteger(1, result);
    }
}
