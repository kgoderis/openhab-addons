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

package org.openhab.io.homekit.server.servlet;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.agreement.srp.SRP6VerifierGenerator;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.http.HttpHeader;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;
import org.openhab.io.homekit.protocol.crypto.HomekitChachaDecoder;
import org.openhab.io.homekit.protocol.crypto.HomekitChachaEncoder;
import org.openhab.io.homekit.protocol.crypto.HomekitEdsaSigner;
import org.openhab.io.homekit.protocol.crypto.HomekitEdsaVerifier;
import org.openhab.io.homekit.protocol.crypto.HomekitEncryptionEngine;
import org.openhab.io.homekit.protocol.crypto.HomekitSRP6Server;
import org.openhab.io.homekit.protocol.error.HomekitErrorCode;
import org.openhab.io.homekit.protocol.message.HomekitMessage;
import org.openhab.io.homekit.util.HomekitByte;
import org.openhab.io.homekit.util.HomekitTypeLengthValueEncoderDecoder;
import org.openhab.io.homekit.util.HomekitTypeLengthValueEncoderDecoder.DecodeResult;
import org.openhab.io.homekit.util.HomekitTypeLengthValueEncoderDecoder.Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet that implements the HomeKit Secure Remote Password (SRP) pairing protocol.
 *
 * <p>
 * This servlet manages the secure pairing process between HomeKit accessories and controllers
 * using the SRP-6a protocol. The pairing process occurs in three distinct stages:
 * <ol>
 * <li>Initial handshake and salt exchange</li>
 * <li>SRP authentication and proof verification</li>
 * <li>Encrypted session key establishment and device verification</li>
 * </ol>
 *
 * <p>
 * The servlet maintains session state throughout the pairing process, storing SRP session
 * information in the HTTP session. It uses ChaCha20-Poly1305 for encrypted communication
 * and EdDSA for signature verification in the final stage.
 *
 * <p>
 * The class integrates with:
 * <ul>
 * <li>{@link HomekitBaseServlet} for base servlet functionality</li>
 * <li>{@link HomekitAccessoryServer} for server functionality</li>
 * <li>{@link HomekitServerSRP6Server} for SRP session management</li>
 * <li>{@link HomekitEncryptionEngine} for cryptographic operations</li>
 * <li>{@link HomekitTypeLengthValueEncoderDecoder} for TLV8 encoding/decoding</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@NonNullByDefault
public class HomekitPairSetupServlet extends HomekitBaseServlet {

    private static final long serialVersionUID = 1L;

    // ========== Log Message Prefixes ==========
    protected static final Logger logger = LoggerFactory.getLogger(HomekitPairSetupServlet.class);
    protected static final String LOG_PREFIX = "Homekit PairSetupServlet";
    protected static final String LOG_INIT = "Init";
    protected static final String LOG_ERROR = "Error";
    protected static final String LOG_WARN = "Warning";
    protected static final String LOG_SECURITY = "Security";
    protected static final String LOG_VERIFY = "Verify";
    protected static final String LOG_CRYPTO = "Crypto";

    protected byte[] sessionKey = new byte[32];

    public HomekitPairSetupServlet() {
        super();
        logger.debug("{} : {} - Creating new pair setup servlet", LOG_PREFIX, LOG_INIT);
    }

    public HomekitPairSetupServlet(HomekitAccessoryServer server) {
        super(server);
        logger.debug("{} : {} - Creating new pair setup servlet with server", LOG_PREFIX, LOG_INIT);
    }

    /**
     * Handles POST requests for the pairing setup process.
     *
     * <p>
     * This method orchestrates the three-stage pairing process by:
     * <ul>
     * <li>Reading and decoding the TLV8-encoded request body</li>
     * <li>Determining the current pairing stage from the state value</li>
     * <li>Routing to the appropriate stage handler:
     * <ul>
     * <li>Stage 1: Initial handshake and salt exchange</li>
     * <li>Stage 2: SRP authentication and proof verification</li>
     * <li>Stage 3: Session key establishment and device verification</li>
     * </ul>
     * </li>
     * </ul>
     *
     * <p>
     * The method maintains session state throughout the process, ensuring that
     * each stage builds upon the previous one's cryptographic material.
     *
     * <p>
     * Error handling ensures:
     * <ul>
     * <li>Proper validation of request data</li>
     * <li>Graceful handling of invalid stages</li>
     * <li>Detailed error logging for debugging</li>
     * <li>Appropriate HTTP status codes for different error conditions</li>
     * </ul>
     *
     * @param request The HTTP request containing the pairing data
     * @param response The HTTP response for the pairing result
     * @throws IOException if an I/O error occurs during request/response handling
     * @throws ServletException if the request cannot be processed
     */
    @Override
    protected void doPost(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response)
            throws ServletException, IOException {

        logger.debug("{} [{}] : {} : Handling pair setup request", LOG_PREFIX,
                server != null ? server.getUID() : "UNKNOWN", LOG_SECURITY);
        if (request == null || response == null) {
            logger.error("{} [{}] : {} : Request or response is null", LOG_PREFIX,
                    server != null ? server.getUID() : "UNKNOWN", LOG_ERROR);
            if (response != null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
            return;
        }

        try {
            // Read the request body as bytes using standard Java
            byte[] body;
            try (var inputStream = request.getInputStream()) {
                body = inputStream.readAllBytes();
            }
            short state = getState(body);
            logger.trace("{} [{}] : {} : Processing setup stage {}", LOG_PREFIX,
                    server != null ? server.getUID() : "UNKNOWN", LOG_SECURITY, state);

            switch (state) {
                case 1: {
                    doStage1(request, response, body);
                    break;
                }
                case 3: {
                    doStage2(request, response, body);
                    break;
                }
                case 5: {
                    doStage3(request, response, body);
                    break;
                }
                default: {
                    logger.warn("{} [{}] : {} : Invalid setup stage: {}", LOG_PREFIX,
                            server != null ? server.getUID() : "UNKNOWN", LOG_WARN, state);
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("{} [{}] : {} : Error processing setup request: {}", LOG_PREFIX,
                    server != null ? server.getUID() : "UNKNOWN", LOG_ERROR, e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Handles Stage 1 of the pairing process - Initial handshake and salt exchange.
     *
     * <p>
     * This method initiates the SRP-6a protocol by:
     * <ul>
     * <li>Creating or retrieving the SRP session from the HTTP session</li>
     * <li>Generating a random salt value</li>
     * <li>Computing the SRP verifier using the setup code</li>
     * <li>Generating the server's public key</li>
     * <li>Returning the salt and public key to the client</li>
     * </ul>
     *
     * <p>
     * The method ensures that the session is in the correct state (INIT) before
     * proceeding and maintains the SRP session in the HTTP session for subsequent stages.
     *
     * <p>
     * Security considerations:
     * <ul>
     * <li>Uses cryptographically secure random number generation for salt</li>
     * <li>Implements proper SRP-6a protocol initialization</li>
     * <li>Maintains session state securely</li>
     * <li>Protects against replay attacks</li>
     * </ul>
     *
     * @param request The HTTP request containing the client's initial message
     * @param response The HTTP response for the server's response
     * @param body The raw TLV8-encoded request body
     * @throws IOException if an I/O error occurs during response writing
     * @throws ServletException if the request cannot be processed
     */
    public void doStage1(HttpServletRequest request, HttpServletResponse response, byte[] body)
            throws ServletException, IOException {
        logger.debug("{} [{}] : {} : Stage {} : Starting pair setup stage", LOG_PREFIX,
                server != null ? server.getUID() : "UNKNOWN", LOG_SECURITY, 1);
        logger.trace("{} [{}] : {} : Stage {} : Received request body: {}", LOG_PREFIX,
                server != null ? server.getUID() : "UNKNOWN", LOG_SECURITY, 1, HomekitByte.toHexString(body));

        if (server == null) {
            logger.error("{} [{}] : {} : Stage {} : Server instance is null", LOG_PREFIX, "UNKNOWN", LOG_ERROR);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        HttpSession httpSession = request.getSession();
        HomekitSRP6Server SRP6Server = (HomekitSRP6Server) httpSession.getAttribute("SRP6Server");

        if (SRP6Server == null) {
            SRP6Server = new HomekitSRP6Server();
            httpSession.setAttribute("SRP6Server", SRP6Server);
            logger.debug("{} [{}] : {} : Stage {} : Created new SRP session", LOG_PREFIX, server.getUID(),
                    LOG_SECURITY);
        }

        // Generate random salt for non-deterministic operation
        logger.trace("{} [{}] : {} : Stage {} : Generating random salt", LOG_PREFIX, server.getUID(), LOG_CRYPTO, 1);
        byte[] saltArray = new byte[16];
        HomekitEncryptionEngine.getSecureRandom().nextBytes(saltArray);
        BigInteger salt = new BigInteger(1, saltArray);
        logger.debug("{} [{}] : {} : Stage {} : Salt = {}", LOG_PREFIX, server.getUID(), LOG_VERIFY, 1,
                HomekitByte.toHexString(HomekitByte.toByteArray(salt)));

        @SuppressWarnings("null") // server null check performed at method start
        String setupCode = server.getSetupCode();

        // Use default identity for non-deterministic operation
        byte[] identityBytes = "Pair-Setup".getBytes(StandardCharsets.UTF_8);

        // Generate verifier using the new API
        SRP6VerifierGenerator verifierGenerator = new SRP6VerifierGenerator();
        verifierGenerator.init(HomekitEncryptionEngine.N_3072, HomekitEncryptionEngine.G, new SHA512Digest());
        BigInteger verifier = verifierGenerator.generateVerifier(HomekitByte.toByteArray(salt), identityBytes,
                setupCode.getBytes(StandardCharsets.UTF_8));
        SRP6Server.init(verifier);
        logger.trace("{} [{}] : {} : Stage {} : Generated verifier", LOG_PREFIX, server.getUID(), LOG_CRYPTO, 1);
        logger.debug("{} [{}] : {} : Stage {} : Verifier = {}", LOG_PREFIX, server.getUID(), LOG_VERIFY, 1,
                HomekitByte.toHexString(HomekitByte.toByteArray(verifier)));

        // Store salt in session for use in Stage 2 HAP parameter injection
        httpSession.setAttribute("SRPSalt", salt);
        logger.trace("{} [{}] : {} : Stage {} : Stored salt in session for Stage 2", LOG_PREFIX, server.getUID(),
                LOG_CRYPTO, 1);

        // Generate random server public key (no deterministic override)
        BigInteger serverPublicKey = SRP6Server.generateSRP6aServerCredentials();
        logger.debug("{} [{}] : {} : Stage {} : Server public key = {}", LOG_PREFIX, server.getUID(), LOG_VERIFY, 1,
                HomekitByte.toHexString(HomekitByte.toByteArray(serverPublicKey)));

        Encoder encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();
        encoder.add(HomekitMessage.STATE, (short) 0x02);
        encoder.add(HomekitMessage.SALT, salt);
        encoder.add(HomekitMessage.PUBLIC_KEY, serverPublicKey);

        logger.debug("{} [{}] : {} : Stage {} : Completing pair setup stage", LOG_PREFIX, server.getUID(),
                LOG_SECURITY);
        response.setContentType("application/pairing+tlv8");
        response.setContentLengthLong(encoder.toByteArray().length);
        response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
        response.setStatus(HttpServletResponse.SC_OK);
        response.getOutputStream().write(encoder.toByteArray());
        response.getOutputStream().flush();
        logger.debug("{} [{}] : {} : Stage {} : Pair setup stage completed successfully", LOG_PREFIX, server.getUID(),
                LOG_SECURITY);
    }

    /**
     * Handles Stage 2 of the pairing process - SRP authentication and proof verification.
     *
     * <p>
     * This method performs the core SRP authentication by:
     * <ul>
     * <li>Retrieving the SRP session from the HTTP session</li>
     * <li>Extracting the client's public key and proof from the request</li>
     * <li>Computing the server's proof using the SRP session key</li>
     * <li>Verifying the client's proof</li>
     * <li>Returning the server's proof to the client</li>
     * </ul>
     *
     * <p>
     * The method ensures that the session is in the correct state (STEP_1) before
     * proceeding and handles SRP exceptions by clearing the session and returning
     * an unauthorized response.
     *
     * <p>
     * Security considerations:
     * <ul>
     * <li>Validates client proof using SRP-6a protocol</li>
     * <li>Maintains session state securely</li>
     * <li>Protects against man-in-the-middle attacks</li>
     * <li>Handles authentication failures gracefully</li>
     * </ul>
     *
     * @param request The HTTP request containing the client's authentication data
     * @param response The HTTP response for the server's proof
     * @param body The raw TLV8-encoded request body
     * @throws IOException if an I/O error occurs during response writing
     * @throws ServletException if the request cannot be processed
     */
    public void doStage2(HttpServletRequest request, HttpServletResponse response, byte[] body)
            throws ServletException, IOException {
        logger.debug("{} [{}] : {} : Stage {} : Executing pair setup stage", LOG_PREFIX,
                server != null ? server.getUID() : "UNKNOWN", LOG_SECURITY);
        logger.trace("{} [{}] : {} : Stage {} : Received request body: {}", LOG_PREFIX,
                server != null ? server.getUID() : "UNKNOWN", LOG_SECURITY, HomekitByte.toHexString(body));

        HttpSession httpSession = request.getSession();
        HomekitSRP6Server SRP6Server = (HomekitSRP6Server) httpSession.getAttribute("SRP6Server");

        if (SRP6Server == null) {
            logger.error("{} [{}] : {} : Stage {} : No SRP session found", LOG_PREFIX,
                    server != null ? server.getUID() : "UNKNOWN", LOG_ERROR);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        logger.trace("{} [{}] : {} : Stage {} : Retrieved SRP session", LOG_PREFIX,
                server != null ? server.getUID() : "UNKNOWN", LOG_SECURITY);

        try {
            DecodeResult d = HomekitTypeLengthValueEncoderDecoder.decode(body);

            logger.trace("{} [{}] : {} : Stage {} : Extracting client public key from TLV8 content", LOG_PREFIX,
                    server != null ? server.getUID() : "UNKNOWN", LOG_CRYPTO, 2);
            BigInteger clientPublicKey = d.getBigInt(HomekitMessage.PUBLIC_KEY);
            logger.debug("{} [{}] : {} : Stage {} : Client public key = {}", LOG_PREFIX,
                    server != null ? server.getUID() : "UNKNOWN", LOG_VERIFY, 2,
                    HomekitByte.toHexString(HomekitByte.toByteArray(clientPublicKey)));

            logger.trace("{} [{}] : {} : Stage {} : Extracting client proof from TLV8 content", LOG_PREFIX,
                    server != null ? server.getUID() : "UNKNOWN", LOG_CRYPTO, 2);
            BigInteger clientProof = d.getBigInt(HomekitMessage.PROOF);
            logger.debug("{} [{}] : {} : Stage {} : Client proof = {}", LOG_PREFIX,
                    server != null ? server.getUID() : "UNKNOWN", LOG_VERIFY, 2,
                    HomekitByte.toHexString(HomekitByte.toByteArray(clientProof)));

            // **HAP PARAMETER INJECTION**: Set identity and salt on server for HAP-compliant M1 verification
            BigInteger salt = (BigInteger) httpSession.getAttribute("SRPSalt");
            if (salt == null) {
                logger.error("{} [{}] : {} : Stage {} : Salt not found in session", LOG_PREFIX,
                        server != null ? server.getUID() : "UNKNOWN", LOG_ERROR, 2);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            SRP6Server.setIdentity("Pair-Setup".getBytes(StandardCharsets.UTF_8));
            SRP6Server.setSalt(HomekitByte.toByteArray(salt));
            logger.trace("{} [{}] : {} : Stage {} : Injected HAP parameters (identity and salt) for M1 verification",
                    LOG_PREFIX, server != null ? server.getUID() : "UNKNOWN", LOG_CRYPTO, 2);

            // Calculate secret and verify client proof using HAP-compliant method
            SRP6Server.calculateSecret(clientPublicKey);

            // **HAP-COMPLIANT M1 VERIFICATION**: Use the HAP-specific verification method
            boolean m1VerificationResult = SRP6Server.verifyClientEvidenceMessage(clientProof);
            if (!m1VerificationResult) {
                logger.error("{} [{}] : {} : Stage {} : M1 verification failed - HAP evidence mismatch", LOG_PREFIX,
                        server != null ? server.getUID() : "UNKNOWN", LOG_ERROR, 2);
                httpSession.removeAttribute("SRP6Server");
                httpSession.removeAttribute("SRPSalt");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            logger.debug("{} [{}] : {} : Stage {} : HAP M1 verification successful", LOG_PREFIX,
                    server != null ? server.getUID() : "UNKNOWN", LOG_VERIFY, 2);

            // Generate server proof M2
            BigInteger serverProof = SRP6Server.calculateServerEvidenceMessage();
            logger.debug("{} [{}] : {} : Stage {} : Server proof = {}", LOG_PREFIX,
                    server != null ? server.getUID() : "UNKNOWN", LOG_VERIFY, 2,
                    HomekitByte.toHexString(HomekitByte.toByteArray(serverProof)));

            Encoder encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();
            encoder.add(HomekitMessage.STATE, (short) 0x04);
            encoder.add(HomekitMessage.PROOF, serverProof);

            logger.debug("{} [{}] : {} : Stage {} : Completing pair setup stage", LOG_PREFIX,
                    server != null ? server.getUID() : "UNKNOWN", LOG_SECURITY);
            response.setContentType("application/pairing+tlv8");
            response.setContentLengthLong(encoder.toByteArray().length);
            response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
            response.setStatus(HttpServletResponse.SC_OK);
            response.getOutputStream().write(encoder.toByteArray());
            response.getOutputStream().flush();
            logger.debug("{} [{}] : {} : Stage {} : Pair setup stage completed successfully", LOG_PREFIX,
                    server != null ? server.getUID() : "UNKNOWN", LOG_SECURITY);

        } catch (CryptoException e) {
            logger.error("{} [{}] : {} : Stage {} : SRP authentication failed: {}", LOG_PREFIX,
                    server != null ? server.getUID() : "UNKNOWN", LOG_ERROR, e.getMessage(), e);
            httpSession.removeAttribute("SRP6Server");
            httpSession.removeAttribute("SRPSalt");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getOutputStream().flush();
        }
    }

    /**
     * Handles Stage 3 of the pairing process - Session key establishment and device verification.
     *
     * <p>
     * This method finalizes the pairing process by:
     * <ul>
     * <li>Retrieving the SRP session from the HTTP session</li>
     * <li>Deriving the shared secret from the SRP session key</li>
     * <li>Generating the session key using HKDF</li>
     * <li>Decrypting and verifying the client's device information</li>
     * <li>Verifying the client's signature using EdDSA</li>
     * <li>Generating and encrypting the server's response</li>
     * </ul>
     *
     * <p>
     * The method uses ChaCha20-Poly1305 for encrypted communication and EdDSA for
     * signature verification, ensuring a secure and authenticated pairing process.
     *
     * <p>
     * Security considerations:
     * <ul>
     * <li>Uses HKDF for key derivation</li>
     * <li>Implements ChaCha20-Poly1305 for encryption</li>
     * <li>Uses EdDSA for signature verification</li>
     * <li>Protects against replay attacks</li>
     * <li>Maintains session security</li>
     * </ul>
     *
     * @param request The HTTP request containing the client's encrypted device info
     * @param response The HTTP response for the server's encrypted response
     * @param body The raw TLV8-encoded request body
     * @throws IOException if an I/O error occurs during response writing
     * @throws ServletException if the request cannot be processed
     */
    public void doStage3(HttpServletRequest request, HttpServletResponse response, byte[] body)
            throws ServletException, IOException {
        logger.debug("{} [{}] : {} : Stage {} : Executing pair setup stage", LOG_PREFIX,
                server != null ? server.getUID() : "UNKNOWN", LOG_SECURITY);
        logger.trace("{} [{}] : {} : Stage {} : Received request body: {}", LOG_PREFIX,
                server != null ? server.getUID() : "UNKNOWN", LOG_SECURITY, HomekitByte.toHexString(body));

        if (server == null) {
            logger.error("{} [{}] : {} : Stage {} : Server instance is null", LOG_PREFIX, "UNKNOWN", LOG_ERROR);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        HttpSession httpSession = request.getSession();
        HomekitSRP6Server SRP6Server = (HomekitSRP6Server) httpSession.getAttribute("SRP6Server");

        if (SRP6Server == null) {
            logger.error("{} [{}] : {} : Stage {} : No SRP session found", LOG_PREFIX, server.getUID(), LOG_ERROR);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        logger.trace("{} [{}] : {} : Stage {} : Retrieved SRP session", LOG_PREFIX, server.getUID(), LOG_CRYPTO, 3);
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            logger.error("{} [{}] : {} : Stage {} : SHA-512 not available", LOG_PREFIX, server.getUID(), LOG_ERROR, 3);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        try {
            // Ensure the session key is calculated before retrieving it
            SRP6Server.calculateSessionKey();
        } catch (CryptoException e) {
            logger.error("{} [{}] : {} : Stage {} : Failed to calculate SRP session key: {}", LOG_PREFIX,
                    server.getUID(), LOG_ERROR, 3, e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        BigInteger sessionKey = SRP6Server.getSessionKey();
        if (sessionKey == null) {
            logger.error("{} [{}] : {} : Stage {} : SRP session key is null after SRP6Server.getSessionKey()",
                    LOG_PREFIX, server.getUID(), LOG_ERROR, 3);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        logger.trace("{} [{}] : {} : Stage {} : Retrieved SRP session key", LOG_PREFIX, server.getUID(), LOG_CRYPTO, 3);
        logger.debug("{} [{}] : {} : Stage {} : SRP session key = {}", LOG_PREFIX, server.getUID(), LOG_VERIFY, 3,
                HomekitByte.toHexString(HomekitByte.toByteArray(sessionKey)));

        byte[] sharedSecret = HomekitByte.toByteArray(sessionKey);
        logger.trace("{} [{}] : {} : Stage {} : Generated shared secret", LOG_PREFIX, server.getUID(), LOG_CRYPTO, 3);
        logger.debug("{} [{}] : {} : Stage {} : Shared secret = {}", LOG_PREFIX, server.getUID(), LOG_VERIFY, 3,
                HomekitByte.toHexString(sharedSecret));

        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Setup-Encrypt-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-Encrypt-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] derivedSessionKey = new byte[32];
        hkdf.generateBytes(derivedSessionKey, 0, 32);
        logger.trace("{} [{}] : {} : Stage {} : Generated session key", LOG_PREFIX, server.getUID(), LOG_CRYPTO, 3);
        logger.debug("{} [{}] : {} : Stage {} : Session key = {}", LOG_PREFIX, server.getUID(), LOG_VERIFY, 3,
                HomekitByte.toHexString(derivedSessionKey));

        DecodeResult d = HomekitTypeLengthValueEncoderDecoder.decode(body);

        byte[] encryptedData = new byte[d.getLength(HomekitMessage.ENCRYPTED_DATA) - 16];
        d.getBytes(HomekitMessage.ENCRYPTED_DATA, encryptedData, 0);
        logger.debug("{} [{}] : {} : Stage {} : Extracted {} bytes of encrypted data", LOG_PREFIX, server.getUID(),
                LOG_SECURITY, encryptedData.length);
        assert encryptedData != null : "Encrypted data should not be null";

        byte[] tag = new byte[16];
        d.getBytes(HomekitMessage.ENCRYPTED_DATA, tag, encryptedData.length);
        logger.debug("{} [{}] : {} : Stage {} : Extracted 16-byte authentication tag", LOG_PREFIX, server.getUID(),
                LOG_SECURITY);
        assert tag != null : "Authentication tag should not be null";

        HomekitChachaDecoder chachaDecoder = new HomekitChachaDecoder(derivedSessionKey,
                "PS-Msg05".getBytes(StandardCharsets.UTF_8));
        byte[] plaintext = chachaDecoder.decodeCiphertext(tag, encryptedData);
        logger.trace("{} [{}] : {} : Stage {} : Decrypted client data", LOG_PREFIX, server.getUID(), LOG_SECURITY);

        d = HomekitTypeLengthValueEncoderDecoder.decode(plaintext);
        byte[] clientPairingIdentifier = d.getBytes(HomekitMessage.IDENTIFIER);
        byte[] clientLongtermPublicKey = d.getBytes(HomekitMessage.PUBLIC_KEY);
        byte[] clientSignature = d.getBytes(HomekitMessage.SIGNATURE);

        // Defensive validation of client pairing data
        // Static analysis indicates these cannot be null at this point, but we maintain validation logic
        // in commented form for documentation and code clarity
        assert clientPairingIdentifier != null : "Client pairing identifier should not be null";
        assert clientLongtermPublicKey != null : "Client longterm public key should not be null";
        assert clientSignature != null : "Client signature should not be null";

        logger.trace("{} [{}] : {} : Stage {} : Validating client pairing data - all required fields present",
                LOG_PREFIX, server.getUID(), LOG_CRYPTO, 3);
        logger.debug("{} [{}] : {} : Stage {} : Client pairing identifier = {}", LOG_PREFIX, server.getUID(),
                LOG_VERIFY, 3, HomekitByte.toHexString(clientPairingIdentifier));
        logger.debug("{} [{}] : {} : Stage {} : Client longterm public key = {}", LOG_PREFIX, server.getUID(),
                LOG_VERIFY, 3, HomekitByte.toHexString(clientLongtermPublicKey));
        logger.debug("{} [{}] : {} : Stage {} : Client signature = {}", LOG_PREFIX, server.getUID(), LOG_VERIFY, 3,
                HomekitByte.toHexString(clientSignature));
        logger.trace("{} [{}] : {} : Stage {} : Retrieved client pairing ID and keys", LOG_PREFIX, server.getUID(),
                LOG_CRYPTO, 3);

        hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Setup-Controller-Sign-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-Controller-Sign-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] clientDeviceX = new byte[32];
        hkdf.generateBytes(clientDeviceX, 0, 32);
        logger.debug("{} [{}] : {} : Stage {} : Client device X = {}", LOG_PREFIX, server.getUID(), LOG_VERIFY, 3,
                HomekitByte.toHexString(clientDeviceX));

        byte[] clientDeviceInfo = HomekitByte.joinBytes(clientDeviceX, clientPairingIdentifier,
                clientLongtermPublicKey);
        logger.trace("{} [{}] : {} : Stage {} : Generated client device info", LOG_PREFIX, server.getUID(), LOG_CRYPTO,
                3);
        logger.debug("{} [{}] : {} : Stage {} : Client device info = {}", LOG_PREFIX, server.getUID(), LOG_VERIFY, 3,
                HomekitByte.toHexString(clientDeviceInfo));

        boolean isError = false;

        try {
            if (!new HomekitEdsaVerifier(clientLongtermPublicKey).verify(clientDeviceInfo, clientSignature)) {
                logger.warn("{} [{}] : {} : Stage {} : Client signature verification failed", LOG_PREFIX,
                        server.getUID(), LOG_WARN);
                isError = true;
            }
        } catch (Exception e) {
            logger.error("{} [{}] : {} : Stage {} : Error during signature verification: {}", LOG_PREFIX,
                    server.getUID(), LOG_ERROR, e.getMessage(), e);
            isError = true;
        }

        Encoder encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();

        if (isError) {
            logger.warn("{} [{}] : {} : Stage {} : Setup failed, sending error response", LOG_PREFIX, server.getUID(),
                    LOG_WARN);
            encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();
            encoder.add(HomekitMessage.STATE, (short) 6);
            encoder.add(HomekitMessage.ERROR, HomekitErrorCode.AUTHENTICATION);

            logger.debug("{} [{}] : {} : Stage {} : Removing SRP session", LOG_PREFIX, server.getUID(), LOG_SECURITY);
            httpSession.removeAttribute("SRP6Server");

            response.setContentType("application/pairing+tlv8");
            response.setContentLengthLong(encoder.toByteArray().length);
            response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
            response.setStatus(HttpServletResponse.SC_OK);
            response.getOutputStream().write(encoder.toByteArray());
            response.getOutputStream().flush();
            logger.debug("{} [{}] : {} : Stage {} : Error response sent", LOG_PREFIX, server.getUID(), LOG_SECURITY);
            return;
        }

        logger.debug("{} [{}] : {} : Stage {} : Adding pairing for server", LOG_PREFIX, server.getUID(), LOG_SECURITY);
        try {
            // Null Pointer Access Warning Checked
            // At this point, we've already verified that clientPairingIdentifier and clientLongtermPublicKey are not
            // null in the verification check above, so it's safe to call addPairing with these parameters
            server.addPairing(clientPairingIdentifier, clientLongtermPublicKey);
        } catch (Exception e) {
            logger.error("{} [{}] : {} : Stage {} : Failed to add pairing: {}", LOG_PREFIX, server.getUID(), LOG_ERROR,
                    e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret,
                "Pair-Setup-HomekitAccessory-Sign-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-HomekitAccessory-Sign-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] serverDeviceX = new byte[32];
        hkdf.generateBytes(serverDeviceX, 0, 32);
        logger.trace("{} [{}] : {} : Stage {} : Generated accessory device X", LOG_PREFIX, server.getUID(), LOG_CRYPTO,
                3);
        logger.debug("{} [{}] : {} : Stage {} : Server device X = {}", LOG_PREFIX, server.getUID(), LOG_VERIFY, 3,
                HomekitByte.toHexString(serverDeviceX));

        HomekitEdsaSigner signer = new HomekitEdsaSigner(server.getSecretKey());
        byte[] serverInfo = HomekitByte.joinBytes(serverDeviceX, server.getPairingId(), signer.getPublicKey());
        logger.trace("{} [{}] : {} : Stage {} : Generated accessory info", LOG_PREFIX, server.getUID(), LOG_CRYPTO, 3);
        logger.debug("{} [{}] : {} : Stage {} : Server pairing identifier = {}", LOG_PREFIX, server.getUID(),
                LOG_VERIFY, 3, HomekitByte.toHexString(server.getPairingId()));
        logger.debug("{} [{}] : {} : Stage {} : Server longterm public key = {}", LOG_PREFIX, server.getUID(),
                LOG_VERIFY, 3, HomekitByte.toHexString(signer.getPublicKey()));
        logger.debug("{} [{}] : {} : Stage {} : Server device info = {}", LOG_PREFIX, server.getUID(), LOG_VERIFY, 3,
                HomekitByte.toHexString(serverInfo));

        byte[] serverSignature;
        try {
            serverSignature = signer.sign(serverInfo);
            logger.trace("{} [{}] : {} : Stage {} : Generated accessory signature", LOG_PREFIX, server.getUID(),
                    LOG_CRYPTO, 3);
            logger.debug("{} [{}] : {} : Stage {} : Server signature = {}", LOG_PREFIX, server.getUID(), LOG_VERIFY, 3,
                    HomekitByte.toHexString(serverSignature));
        } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException e) {
            logger.error("{} [{}] : {} : Stage {} : Failed to create accessory signature: {}", LOG_PREFIX,
                    server.getUID(), LOG_ERROR, e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        // Null Pointer Access Warning Checked
        // getPairingId() and getPublicKey() methods are guaranteed to not return null by their implementation
        // This is enforced at the component initialization time, so we can safely use these values
        encoder.add(HomekitMessage.IDENTIFIER, server.getPairingId());
        encoder.add(HomekitMessage.PUBLIC_KEY, signer.getPublicKey());
        encoder.add(HomekitMessage.SIGNATURE, serverSignature);

        byte[] plainText = encoder.toByteArray();
        // Null Pointer Access Warning Checked
        // Static analysis indicates encoder.toByteArray() cannot return null, but we maintain validation
        // in commented form for documentation and code clarity
        assert plainText != null : "Encoded plaintext should not be null";

        logger.trace("{} [{}] : {} : Stage {} : Successfully encoded plaintext data", LOG_PREFIX, server.getUID(),
                LOG_SECURITY);

        HomekitChachaEncoder chachaEncoder = new HomekitChachaEncoder(derivedSessionKey,
                "PS-Msg06".getBytes(StandardCharsets.UTF_8));
        byte[] encryptedDataWithTag = chachaEncoder.encodeCiphertext(plainText);

        encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();
        encoder.add(HomekitMessage.STATE, (short) 6);
        encoder.add(HomekitMessage.ENCRYPTED_DATA, encryptedDataWithTag);

        logger.debug("{} [{}] : {} : Stage {} : Removing SRP session", LOG_PREFIX, server.getUID(), LOG_SECURITY);
        httpSession.removeAttribute("SRP6Server");

        response.setContentType("application/pairing+tlv8");
        response.setContentLengthLong(encoder.toByteArray().length);
        response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
        response.setStatus(HttpServletResponse.SC_OK);
        response.getOutputStream().write(encoder.toByteArray());
        response.getOutputStream().flush();
        logger.debug("{} [{}] : {} : Stage {} : Pair setup stage completed successfully", LOG_PREFIX, server.getUID(),
                LOG_SECURITY);
    }
}
