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

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.SecureRandom;

import com.nimbusds.srp6.SRP6ClientCredentials;
import com.nimbusds.srp6.SRP6ClientEvidenceContext;
import com.nimbusds.srp6.SRP6CryptoParams;
import com.nimbusds.srp6.SRP6Exception;
import com.nimbusds.srp6.SRP6Routines;
import com.nimbusds.srp6.SRP6ServerEvidenceContext;
import com.nimbusds.srp6.SRP6Session;
import com.nimbusds.srp6.URoutineContext;
import com.nimbusds.srp6.XRoutine;

/**
 * Implements a client-side Secure Remote Password (SRP-6a) authentication session for HomeKit.
 *
 * This class extends the SRP6Session from Nimbus to provide a stateful client-side implementation
 * of the SRP-6a protocol, which is used for secure password-based authentication in HomeKit.
 * It handles the computation and storage of SRP-6a variables between protocol steps, as well as
 * session timeouts.
 *
 * Key features:
 * - Implements the SRP-6a protocol for secure password authentication
 * - Maintains session state and handles timeouts
 * - Supports custom routines for password key computation
 * - Provides step-by-step authentication process
 * - Handles cryptographic parameter validation
 *
 * Security considerations:
 * - Uses cryptographically secure random number generation
 * - Implements proper session state management
 * - Validates all cryptographic parameters
 * - Handles session timeouts to prevent replay attacks
 * - Supports custom password key computation routines
 *
 * Authentication process:
 * 1. Initialize session (INIT state)
 * 2. Record user identity and password (STEP_1)
 * 3. Process server response with salt and public value (STEP_2)
 * 4. Verify server evidence message (STEP_3)
 *
 * @author Vladimir Dzhuvinov
 * @author Bernard Wittwer
 * @author Karel Goderis - HomeKit adaptation
 */
public class HomekitClientSRP6Session extends SRP6Session implements Serializable {

    /** Serializable class version number */
    private static final long serialVersionUID = -479060216624675478L;

    /**
     * Enumerates the states of a client-side SRP-6a authentication session.
     * Each state represents a specific step in the authentication process.
     */
    public static enum State {
        /** The session is initialized and ready to begin authentication */
        INIT,

        /** User identity and password have been recorded */
        STEP_1,

        /** Server response with salt and public value has been processed */
        STEP_2,

        /** Server evidence message has been verified, authentication complete */
        STEP_3
    }

    /** The user password 'P' */
    private String password;

    /** The password key 'x' */
    private BigInteger x = null;

    /** The client private value 'a' */
    private BigInteger a = null;

    /** The current SRP-6a authentication state */
    private State state;

    /** Custom routine for password key 'x' computation */
    private XRoutine xRoutine = null;

    /**
     * Creates a new client-side SRP-6a authentication session.
     *
     * This constructor initializes a new session with the specified timeout
     * and sets its state to INIT. The timeout determines how long the session
     * will wait for responses from the server before expiring.
     *
     * @param timeout The session timeout in seconds, or 0 to disable timeouts
     */
    public HomekitClientSRP6Session(final int timeout) {
        super(timeout);
        state = State.INIT;
        updateLastActivityTime();
    }

    /**
     * Creates a new client-side SRP-6a authentication session with timeouts disabled.
     */
    public HomekitClientSRP6Session() {
        this(0);
    }

    /**
     * Sets a custom routine for password key computation.
     *
     * This method allows customization of the password key computation process.
     * The custom routine must be set before STEP_2 of the authentication process.
     *
     * @param routine The custom password key routine, or null to use the default
     */
    public void setXRoutine(final XRoutine routine) {
        xRoutine = routine;
    }

    /**
     * Gets the current password key computation routine.
     *
     * @return The custom routine instance, or null if using the default
     */
    public XRoutine getXRoutine() {
        return xRoutine;
    }

    /**
     * Records the user's identity and password to begin authentication.
     *
     * This method initiates the authentication process by recording the user's
     * credentials. It validates the input parameters and updates the session state.
     *
     * @param userID The user's identity (username), UTF-8 encoded
     * @param password The user's password, UTF-8 encoded
     * @throws IllegalArgumentException if userID is null/empty or password is null
     * @throws IllegalStateException if called in a state other than INIT
     */
    public void step1(final String userID, final String password) {
        if (userID == null || userID.trim().isEmpty()) {
            throw new IllegalArgumentException("The user identity 'I' must not be null or empty");
        }

        this.userID = userID;

        if (password == null) {
            throw new IllegalArgumentException("The user password 'P' must not be null");
        }

        this.password = password;

        if (state != State.INIT) {
            throw new IllegalStateException("State must be INIT");
        }

        state = State.STEP_1;
        updateLastActivityTime();
    }

    /**
     * Receives the password salt 's' and public value 'B' from the server.
     * The SRP-6a crypto parameters are also set. The session is incremented
     * to {@link State#STEP_2}.
     *
     * <p>
     * Argument origin:
     *
     * <ul>
     * <li>From server: password salt 's', public value 'B'.
     * <li>From server or pre-agreed: crypto parameters prime 'N',
     * generator 'g' and hash function 'H'.
     * </ul>
     *
     * @param config The SRP-6a crypto parameters. Must not be {@code null}.
     * @param s The password salt 's'. Must not be {@code null}.
     * @param B The public server value 'B'. Must not be {@code null}.
     *
     * @return The client credentials consisting of the client public key
     *         'A' and the client evidence message 'M1'.
     *
     * @throws IllegalStateException If the method is invoked in a state
     *             other than {@link State#STEP_1}.
     * @throws SRP6Exception If the session has timed out or the
     *             public server value 'B' is invalid.
     */
    public SRP6ClientCredentials step2(final SRP6CryptoParams config, final BigInteger s, final BigInteger B)
            throws SRP6Exception {

        // Check arguments
        if (config == null) {
            throw new IllegalArgumentException("The SRP-6a crypto parameters must not be null");
        }

        this.config = config;

        MessageDigest digest = config.getMessageDigestInstance();

        if (digest == null) {
            throw new IllegalArgumentException("Unsupported hash algorithm 'H': " + config.H);
        }

        if (s == null) {
            throw new IllegalArgumentException("The salt 's' must not be null");
        }

        this.s = s;

        if (B == null) {
            throw new IllegalArgumentException("The public server value 'B' must not be null");
        }

        this.B = B;

        // Check current state
        if (state != State.STEP_1) {
            throw new IllegalStateException("State violation: Session must be in STEP_1 state");
        }

        // Check timeout
        if (hasTimedOut()) {
            throw new SRP6Exception("Session timeout", SRP6Exception.CauseType.TIMEOUT);
        }

        // Check B validity
        if (!SRP6Routines.isValidPublicValue(config.N, B)) {
            throw new SRP6Exception("Bad server public value 'B'", SRP6Exception.CauseType.BAD_PUBLIC_VALUE);
        }

        // Compute the password key 'x'
        if (xRoutine != null) {

            // With custom routine
            x = xRoutine.computeX(config.getMessageDigestInstance(), s.toByteArray(),
                    userID.getBytes(Charset.forName("UTF-8")), password.getBytes(Charset.forName("UTF-8")));

        } else {
            // With default routine
            x = SRP6Routines.computeX(digest, s.toByteArray(), password.getBytes(Charset.forName("UTF-8")));
            digest.reset();
        }

        // Generate client private and public values
        a = generatePrivateValue(config.N, random);
        digest.reset();

        A = SRP6Routines.computePublicClientValue(config.N, config.g, a);

        // Compute the session key
        k = SRP6Routines.computeK(digest, config.N, config.g);
        digest.reset();

        if (hashedKeysRoutine != null) {
            URoutineContext hashedKeysContext = new URoutineContext(A, B);
            u = hashedKeysRoutine.computeU(config, hashedKeysContext);
        } else {
            u = SRP6Routines.computeU(digest, config.N, A, B);
            digest.reset();
        }

        S = SRP6Routines.computeSessionKey(config.N, config.g, k, x, u, a, B);

        // Compute the client evidence message
        if (clientEvidenceRoutine != null) {

            // With custom routine
            SRP6ClientEvidenceContext ctx = new SRP6ClientEvidenceContext(userID, s, A, B, S);
            M1 = clientEvidenceRoutine.computeClientEvidence(config, ctx);

        } else {
            // With default routine
            M1 = SRP6Routines.computeClientEvidence(digest, A, B, S);
            digest.reset();
        }

        state = State.STEP_2;

        updateLastActivityTime();

        return new SRP6ClientCredentials(A, M1);
    }

    /**
     * Receives the server evidence message 'M1'. The session is
     * incremented to {@link State#STEP_3}.
     *
     * <p>
     * Argument origin:
     *
     * <ul>
     * <li>From server: evidence message 'M2'.
     * </ul>
     *
     * @param M2 The server evidence message 'M2'. Must not be
     *            {@code null}.
     *
     * @throws IllegalStateException If the method is invoked in a state
     *             other than {@link State#STEP_2}.
     * @throws SRP6Exception If the session has timed out or the
     *             server evidence message 'M2' is
     *             invalid.
     */
    public void step3(final BigInteger M2) throws SRP6Exception {

        // Check argument

        if (M2 == null) {
            throw new IllegalArgumentException("The server evidence message 'M2' must not be null");
        }

        this.M2 = M2;

        // Check current state
        if (state != State.STEP_2) {
            throw new IllegalStateException("State violation: Session must be in STEP_2 state");
        }

        // Check timeout
        if (hasTimedOut()) {
            throw new SRP6Exception("Session timeout", SRP6Exception.CauseType.TIMEOUT);
        }

        // Compute the own server evidence message 'M2'
        BigInteger computedM2;

        if (serverEvidenceRoutine != null) {

            // With custom routine
            SRP6ServerEvidenceContext ctx = new SRP6ServerEvidenceContext(A, M1, S);

            computedM2 = serverEvidenceRoutine.computeServerEvidence(config, ctx);

        } else {
            // With default routine
            MessageDigest digest = config.getMessageDigestInstance();
            computedM2 = computeServerEvidence(digest, A, M1, S);
        }

        if (!computedM2.equals(M2)) {
            throw new SRP6Exception("Bad server credentials", SRP6Exception.CauseType.BAD_CREDENTIALS);
        }

        state = State.STEP_3;

        updateLastActivityTime();
    }

    /**
     * Returns the current state of this SRP-6a authentication session.
     *
     * @return The current state.
     */
    public State getState() {

        return state;
    }

    public BigInteger generatePrivateValue(BigInteger N, SecureRandom random) {
        final int minBits = Math.min(3072, N.bitLength() / 2);

        BigInteger min = BigInteger.ONE.shiftLeft(minBits - 1);
        BigInteger max = N.subtract(BigInteger.ONE);

        return createRandomBigIntegerInRange(min, max, random);
    }

    /**
     * Returns a random big integer in the specified range [min, max].
     *
     * @param min The least value that may be generated. Must not be {@code null}.
     * @param max The greatest value that may be generated. Must not be {@code null}.
     * @param random Source of randomness. Must not be {@code null}.
     * @return A random big integer in the range [min, max].
     */
    protected BigInteger createRandomBigIntegerInRange(final BigInteger min, final BigInteger max,
            final SecureRandom random) {

        final int cmp = min.compareTo(max);

        if (cmp >= 0) {

            if (cmp > 0) {
                throw new IllegalArgumentException("'min' may not be greater than 'max'");
            }

            return min;
        }

        if (min.bitLength() > max.bitLength() / 2) {
            return createRandomBigIntegerInRange(BigInteger.ZERO, max.subtract(min), random).add(min);
        }

        final int MAX_ITERATIONS = 1000;

        for (int i = 0; i < MAX_ITERATIONS; ++i) {

            BigInteger x = new BigInteger(max.bitLength(), random);

            if (x.compareTo(min) >= 0 && x.compareTo(max) <= 0) {
                return x;
            }
        }

        // fall back to a faster (restricted) method
        return new BigInteger(max.subtract(min).bitLength() - 1, random).add(min);
    }

    /**
     * Computes the server evidence message M2 = H(A | M1 | S)
     *
     * <p>
     * Specification: Tom Wu's paper "SRP-6: Improvements and
     * refinements to the Secure Remote Password protocol", table 5, from
     * 2002.
     *
     * @param digest The hash function 'H'. Must not be {@code null}.
     * @param A The public client value 'A'. Must not be {@code null}.
     * @param M1 The client evidence message 'M1'. Must not be
     *            {@code null}.
     * @param S The session key 'S'. Must not be {@code null}.
     *
     * @return The resulting server evidence message 'M2'.
     */
    protected static BigInteger computeServerEvidence(final MessageDigest digest, final BigInteger A,
            final BigInteger M1, final BigInteger S) {

        digest.update(A.toByteArray());
        digest.update(M1.toByteArray());
        digest.update(S.toByteArray());

        return new BigInteger(1, digest.digest());
    }
}
