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
import java.security.SecureRandom;

import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.agreement.srp.SRP6Client;
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
public class SRP6aClient extends SRP6Client {

    private byte[] salt;
    private byte[] identity;
    private byte[] password;

    /**
     * Initialises the client to begin new authentication attempt with the HAP SRP-6a parameters
     */
    public void init() {
        this.N = HomekitEncryptionEngine.N_3072;
        this.g = HomekitEncryptionEngine.G;
        this.random = new SecureRandom();
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
     * Generates client's credentials given the client's salt, identity and password
     * 
     * @param salt The salt used in the client's verifier.
     * @param identity The user's identity (eg. username)
     * @param password The user's password
     * @return Client's public value to send to server
     */
    @Override
    @SuppressWarnings("null")
    public BigInteger generateClientCredentials(byte[] salt, byte[] identity, byte[] password) {

        this.salt = salt;
        this.identity = identity;
        this.password = password;

        return super.generateClientCredentials(salt, identity, password);
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
        if (this.A == null || this.B == null || this.S == null) {
            throw new CryptoException(
                    "Impossible to compute M1: " + "some data are missing from the previous operations (A,B,S)");
        }
        // compute the client evidence message 'M1'
        this.M1 = calculateM1(digest, N, A, B, S, g, identity, salt);
        return M1;
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
     */
    protected BigInteger calculateM1(Digest digest, BigInteger N, BigInteger A, BigInteger B, BigInteger S,
            BigInteger g, byte[] identity, byte[] salt) {

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
}
