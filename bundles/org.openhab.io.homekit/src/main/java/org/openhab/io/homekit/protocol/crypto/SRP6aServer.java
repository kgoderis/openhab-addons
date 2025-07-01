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
import org.bouncycastle.crypto.agreement.srp.SRP6Server;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * HAP-compatible SRP-6a server implementation extending BouncyCastle's SRP6Server.
 *
 * This class provides a server-side implementation of the SRP-6a protocol specifically
 * designed for HomeKit Accessory Protocol (HAP) compatibility. It extends BouncyCastle's
 * SRP6Server and adds custom evidence calculation routines that match the HAP specification.
 *
 * Key features:
 * - Extends BouncyCastle's SRP6Server for robust cryptographic operations
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
public class SRP6aServer extends SRP6Server {

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

    /**
     * Computes the server evidence message M2 using the previously verified values.
     * To be called after successfully verifying the client evidence message M1.
     * @return M2: the server side generated evidence message
     * @throws CryptoException
     */
    @Override
    public BigInteger calculateServerEvidenceMessage() throws CryptoException {
        // Verify pre-requirements
        if (this.A == null || this.M1 == null || this.S == null) {
            throw new CryptoException("Impossible to compute M2: " +
                    "some data are missing from the previous operations (A,M1,S)");
        }

        // Compute the server evidence message 'M2'
        this.M2 = calculateM2(digest, N, A, M1, S);
        return M2;
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
     */
    private BigInteger calculateM2(Digest digest, BigInteger N, BigInteger A, BigInteger M1, BigInteger S) {

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
