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
import org.bouncycastle.crypto.agreement.srp.SRP6Server;
import org.bouncycastle.crypto.agreement.srp.SRP6Util;
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
public class HomekitSRP6Server extends SRP6Server {

    // HAP-specific fields for evidence calculation
    private byte[] identity = new byte[0];
    private byte[] salt = new byte[0];

    /**
     * Initializes the server with the given verifier.
     * 
     * @param v The verifier value
     * @throws IllegalArgumentException if the verifier is null
     */
    public void init(BigInteger v) {
        if (v == null) {
            throw new IllegalArgumentException("Verifier parameter cannot be null");
        }
        super.init(HomekitEncryptionEngine.N_3072, HomekitEncryptionEngine.G, v, new SHA512Digest(),
                HomekitEncryptionEngine.getSecureRandom());
    }

    /**
     * Sets the private value 'b' for deterministic testing.
     * 
     * @param privateValue The private value to use, or null to use random generation
     */
    public void setPrivateValue(@Nullable BigInteger privateValue) {
        this.b = privateValue;
    }

    /**
     * Gets the private value 'b'.
     *
     * @return The private value 'b', or null if not yet computed
     */
    public @Nullable BigInteger getPrivateValue() {
        return b;
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
     * Sets the user identity for the session (e.g., "Pair-Setup")
     * 
     * @param identity the user identity as a byte array
     */
    public void setIdentity(byte[] identity) {
        this.identity = identity;
    }

    /**
     * Sets the salt for the session
     * 
     * @param salt the salt as a byte array
     */
    public void setSalt(byte[] salt) {
        this.salt = salt;
    }

    /**
     * Gets the user identity for the session
     * 
     * @return the user identity as a byte array
     */
    public byte[] getIdentity() {
        return identity;
    }

    /**
     * Gets the salt for the session
     * 
     * @return the salt as a byte array
     */
    public byte[] getSalt() {
        return salt;
    }

    /**
     * Generates server's credentials.
     * 
     * @return Server's public value to send to client
     * @throws IllegalStateException if internal parameters are not initialized
     */
    public BigInteger generateSRP6aServerCredentials() {
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
        if (this.v == null) {
            missingInternalParams.append("v (verifier), ");
        }

        if (missingInternalParams.length() > 0) {
            // Remove trailing comma and space
            missingInternalParams.setLength(missingInternalParams.length() - 2);
            throw new IllegalStateException("Internal parameters not initialized: " + missingInternalParams.toString()
                    + ". Call init() first.");
        }

        // If a private value was set, use it; otherwise use random generation
        if (b != null) {
            // BigInteger k = calculateKRFC(digest, N, g); // ✅ RFC-compliant k calculation
            BigInteger k = SRP6Util.calculateK(digest, N, g);
            // Compute the public value using the set private value
            this.B = k.multiply(v).mod(N).add(g.modPow(b, N)).mod(N);
            return B;
        } else {
            // Call the parent method to generate credentials with random private value
            return super.generateServerCredentials();
        }
    }

    /**
     * Computes the server evidence message M2 using the previously verified values.
     * To be called after successfully verifying the client evidence message M1.
     * 
     * @return M2: the server side generated evidence message
     * @throws CryptoException
     */
    @Override
    public BigInteger calculateServerEvidenceMessage() throws CryptoException {
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
                    "Impossible to compute M2: missing required parameters: " + missingParams.toString());
        }

        // Compute the server evidence message 'M2'
        this.M2 = HomekitSRP6Util.calculateM2(digest, N, A, M1, S);
        return M2;
    }

    /**
     * Verifies the client evidence message M1 using the HAP-specific calculation.
     *
     * @param clientM1 The client-provided evidence message
     * @return true if the evidence matches, false otherwise
     * @throws CryptoException if required parameters are missing
     */
    @Override
    public boolean verifyClientEvidenceMessage(@Nullable BigInteger clientM1) throws CryptoException {
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
                    "Impossible to verify M1: missing required parameters: " + missingParams.toString());
        }

        if (clientM1 == null) {
            return false;
        }

        // Calculate expected M1 using HAP-specific formula
        BigInteger expectedM1 = HomekitSRP6Util.calculateM1(digest, N, A, B, S, g, identity, salt);

        // Compare with provided client M1
        if (expectedM1.equals(clientM1)) {
            this.M1 = clientM1;
            return true;
        }
        return false;
    }

    public BigInteger calculateClientEvidenceMessage() throws CryptoException {
        if (S == null) {
            throw new CryptoException("Session key S not calculated");
        }
        if (A == null) {
            throw new CryptoException("Client public key A not set");
        }
        if (B == null) {
            throw new CryptoException("Server public key B not set");
        }
        if (N == null) {
            throw new CryptoException("Modulus N not set");
        }
        if (g == null) {
            throw new CryptoException("Generator g not set");
        }
        if (digest == null) {
            throw new CryptoException("Digest not set");
        }
        if (identity.length == 0) {
            throw new CryptoException("Identity not set");
        }
        if (salt.length == 0) {
            throw new CryptoException("Salt not set");
        }

        // Use our own calculateM1HAP implementation that handles large inputs
        return HomekitSRP6Util.calculateM1(digest, N, A, B, S, g, identity, salt);
    }

    /**
     * Calculate the premaster secret S using HAP-compliant logic and large-key calculateU.
     *
     * @param clientPublicKey The client's public key A
     * @return The premaster secret S
     */
    public BigInteger calculateServerSecret(BigInteger clientPublicKey) {
        if (clientPublicKey == null) {
            throw new IllegalStateException("Client public key cannot be null");
        }
        if (this.B == null) {
            throw new IllegalStateException("Server public key B not set");
        }
        if (this.N == null) {
            throw new IllegalStateException("Modulus N not set");
        }
        if (this.b == null) {
            throw new IllegalStateException("Server private key b not set");
        }
        if (this.digest == null) {
            throw new IllegalStateException("Digest not set");
        }
        if (this.v == null) {
            throw new IllegalStateException("Verifier v not set");
        }

        this.A = clientPublicKey;

        // Calculate u using our own implementation that handles large keys
        BigInteger u = HomekitSRP6Util.calculateU(digest, N, A, B);

        // Calculate the premaster secret S using the standard SRP6 formula
        // For server: S = (A * v^u)^b mod N
        BigInteger base = A.multiply(v.modPow(u, N)).mod(N);
        this.S = base.modPow(b, N);

        return this.S;
    }
}
