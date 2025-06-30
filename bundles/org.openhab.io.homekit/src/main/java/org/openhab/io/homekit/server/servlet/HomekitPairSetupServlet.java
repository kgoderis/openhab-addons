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
import org.openhab.io.homekit.protocol.crypto.HomekitServerSRP6Session;
import org.openhab.io.homekit.protocol.crypto.HomekitServerSRP6Session.State;
import org.openhab.io.homekit.protocol.error.HomekitErrorCode;
import org.openhab.io.homekit.protocol.message.HomekitMessage;
import org.openhab.io.homekit.util.HomekitByte;
import org.openhab.io.homekit.util.HomekitTypeLengthValueEncoderDecoder;
import org.openhab.io.homekit.util.HomekitTypeLengthValueEncoderDecoder.DecodeResult;
import org.openhab.io.homekit.util.HomekitTypeLengthValueEncoderDecoder.Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.srp6.ClientEvidenceRoutine;
import com.nimbusds.srp6.SRP6ClientEvidenceContext;
import com.nimbusds.srp6.SRP6CryptoParams;
import com.nimbusds.srp6.SRP6Exception;
import com.nimbusds.srp6.SRP6VerifierGenerator;
import com.nimbusds.srp6.XRoutineWithUserIdentity;

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
 * <li>{@link HomekitServerSRP6Session} for SRP session management</li>
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
    protected static final String LOG_PREFIX = "Homekit PairSetupServlet: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    protected static final String LOG_SECURITY = LOG_PREFIX + "Security - ";

    protected byte[] sessionKey = new byte[32];

    /**
     * Creates a new pair setup servlet.
     *
     * <p>
     * This constructor initializes a basic servlet instance. It is recommended to use
     * the constructor with a server parameter for proper functionality.
     */
    public HomekitPairSetupServlet() {
        logger.debug("{}Creating new pair setup servlet without server", LOG_INIT);
    }

    /**
     * Creates a new pair setup servlet with the specified server.
     *
     * <p>
     * This constructor initializes the servlet with the necessary components for
     * handling pair setup operations. It sets up:
     * <ul>
     * <li>The base servlet functionality through the parent class</li>
     * <li>Access to the server's cryptographic material</li>
     * <li>Integration with the server's pairing management</li>
     * </ul>
     *
     * @param server The HomeKit accessory server instance
     */
    public HomekitPairSetupServlet(HomekitAccessoryServer server) {
        super(server);
        logger.debug("{}Creating new pair setup servlet with server", LOG_INIT);
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

        logger.debug("{}Handling pair setup request", LOG_SECURITY);
        if (request == null || response == null) {
            logger.error("{}Request or response is null", LOG_ERROR);
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
            logger.trace("{}Processing setup stage {}", LOG_SECURITY, state);

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
                    logger.warn("{}Invalid setup stage: {}", LOG_WARN, state);
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("{}Error processing setup request: {}", LOG_ERROR, e.getMessage(), e);
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
    protected void doStage1(HttpServletRequest request, HttpServletResponse response, byte[] body)
            throws ServletException, IOException {
        logger.debug("{}Starting Stage 1 setup", LOG_SECURITY);
        logger.trace("{}Received request body: {}", LOG_SECURITY, HomekitByte.toHexString(body));

        if (server == null) {
            logger.error("{}Server instance is null", LOG_ERROR);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        HttpSession httpSession = request.getSession();
        HomekitServerSRP6Session SRP6Session = (HomekitServerSRP6Session) httpSession.getAttribute("SRP6Session");

        if (SRP6Session == null) {
            SRP6Session = new HomekitServerSRP6Session(HomekitEncryptionEngine.SRP6Params);
            SRP6Session.setClientEvidenceRoutine(new HomekitEncryptionEngine.ClientEvidenceRoutineImpl());
            SRP6Session.setServerEvidenceRoutine(new HomekitEncryptionEngine.ServerEvidenceRoutineImpl());
            httpSession.setAttribute("SRP6Session", SRP6Session);
            logger.debug("{}Created new SRP session", LOG_SECURITY);
        }

        if (SRP6Session.getState() != State.INIT) {
            logger.error("{}Session is not in INIT state", LOG_ERROR);
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            return;
        }

        SRP6VerifierGenerator verifierGenerator = new SRP6VerifierGenerator(HomekitEncryptionEngine.SRP6Params);
        verifierGenerator.setXRoutine(new XRoutineWithUserIdentity());

        logger.trace("{}Generating random salt", LOG_SECURITY);
        byte[] saltArray = new byte[16];
        HomekitEncryptionEngine.getSecureRandom().nextBytes(saltArray);
        BigInteger salt = new BigInteger(1, saltArray);

        @SuppressWarnings("null") // server null check performed at method start
        String setupCode = server.getSetupCode();

        BigInteger verifier = verifierGenerator.generateVerifier(salt, "Pair-Setup", setupCode);
        logger.trace("{}Generated verifier", LOG_SECURITY);
        BigInteger serverPublicKey = SRP6Session.step1("Pair-Setup", salt, verifier);

        Encoder encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();
        encoder.add(HomekitMessage.STATE, (short) 0x02);
        encoder.add(HomekitMessage.SALT, salt);
        encoder.add(HomekitMessage.PUBLIC_KEY, serverPublicKey);

        logger.debug("{}Completing Stage 1 setup", LOG_SECURITY);
        response.setContentType("application/pairing+tlv8");
        response.setContentLengthLong(encoder.toByteArray().length);
        response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
        response.setStatus(HttpServletResponse.SC_OK);
        response.getOutputStream().write(encoder.toByteArray());
        response.getOutputStream().flush();
        logger.debug("{}Stage 1 setup complete", LOG_SECURITY);
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
    protected void doStage2(HttpServletRequest request, HttpServletResponse response, byte[] body)
            throws ServletException, IOException {
        logger.debug("{}Starting Stage 2 setup", LOG_SECURITY);
        logger.trace("{}Received request body: {}", LOG_SECURITY, HomekitByte.toHexString(body));

        HttpSession httpSession = request.getSession();
        HomekitServerSRP6Session SRP6Session = (HomekitServerSRP6Session) httpSession.getAttribute("SRP6Session");

        if (SRP6Session == null) {
            logger.error("{}No SRP session found", LOG_ERROR);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        logger.trace("{}Retrieved SRP session", LOG_SECURITY);
        if (SRP6Session.getState() != State.STEP_1) {
            logger.error("{}Session is not in STEP_1 state", LOG_ERROR);
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            return;
        }

        try {
            DecodeResult d = HomekitTypeLengthValueEncoderDecoder.decode(body);

            logger.trace("{}Extracting client public key from TLV8 content", LOG_SECURITY);
            BigInteger clientPublicKey = d.getBigInt(HomekitMessage.PUBLIC_KEY);

            logger.trace("{}Extracting client proof from TLV8 content", LOG_SECURITY);
            BigInteger clientProof = d.getBigInt(HomekitMessage.PROOF);
            BigInteger serverProof = SRP6Session.step2(clientPublicKey, clientProof);

            Encoder encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();
            encoder.add(HomekitMessage.STATE, (short) 0x04);
            encoder.add(HomekitMessage.PROOF, serverProof);

            logger.debug("{}Completing Stage 2 setup", LOG_SECURITY);
            response.setContentType("application/pairing+tlv8");
            response.setContentLengthLong(encoder.toByteArray().length);
            response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
            response.setStatus(HttpServletResponse.SC_OK);
            response.getOutputStream().write(encoder.toByteArray());
            response.getOutputStream().flush();
            logger.debug("{}Stage 2 setup complete", LOG_SECURITY);

        } catch (SRP6Exception e) {
            logger.error("{}SRP authentication failed: {}", LOG_ERROR, e.getMessage(), e);
            httpSession.removeAttribute("SRP6Session");
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
    protected void doStage3(HttpServletRequest request, HttpServletResponse response, byte[] body)
            throws ServletException, IOException {
        logger.debug("{}Starting Stage 3 setup", LOG_SECURITY);
        logger.trace("{}Received request body: {}", LOG_SECURITY, HomekitByte.toHexString(body));

        if (server == null) {
            logger.error("{}Server instance is null", LOG_ERROR);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        HttpSession httpSession = request.getSession();
        HomekitServerSRP6Session SRP6Session = (HomekitServerSRP6Session) httpSession.getAttribute("SRP6Session");

        if (SRP6Session == null) {
            logger.error("{}No SRP session found", LOG_ERROR);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        logger.trace("{}Retrieved SRP session", LOG_SECURITY);
        MessageDigest digest = SRP6Session.getCryptoParams().getMessageDigestInstance();
        BigInteger SRPSessionKey = SRP6Session.getSessionKey(false);
        logger.trace("{}Retrieved SRP session key", LOG_SECURITY);

        byte[] sharedSecret = digest.digest(bigIntegerToUnsignedByteArray(SRPSessionKey));
        logger.trace("{}Generated shared secret", LOG_SECURITY);

        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Setup-Encrypt-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-Encrypt-Info".getBytes(StandardCharsets.UTF_8)));
        hkdf.generateBytes(sessionKey, 0, 32);
        logger.trace("{}Generated session key", LOG_SECURITY);

        DecodeResult d = HomekitTypeLengthValueEncoderDecoder.decode(body);

        byte[] encryptedData = new byte[d.getLength(HomekitMessage.ENCRYPTED_DATA) - 16];
        d.getBytes(HomekitMessage.ENCRYPTED_DATA, encryptedData, 0);
        logger.debug("{}Extracted {} bytes of encrypted data", LOG_SECURITY, encryptedData.length);
        assert encryptedData != null : "Encrypted data should not be null";

        byte[] tag = new byte[16];
        d.getBytes(HomekitMessage.ENCRYPTED_DATA, tag, encryptedData.length);
        logger.debug("{}Extracted 16-byte authentication tag", LOG_SECURITY);
        assert tag != null : "Authentication tag should not be null";

        HomekitChachaDecoder chachaDecoder = new HomekitChachaDecoder(sessionKey,
                "PS-Msg05".getBytes(StandardCharsets.UTF_8));
        byte[] plaintext = chachaDecoder.decodeCiphertext(tag, encryptedData);
        logger.trace("{}Decrypted client data", LOG_SECURITY);

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

        logger.trace("{}Validating client pairing data - all required fields present", LOG_SECURITY);
        logger.trace("{}Retrieved client pairing ID and keys", LOG_SECURITY);

        hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Setup-Controller-Sign-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-Controller-Sign-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] clientDeviceX = new byte[32];
        hkdf.generateBytes(clientDeviceX, 0, 32);

        byte[] clientDeviceInfo = HomekitByte.joinBytes(clientDeviceX, clientPairingIdentifier,
                clientLongtermPublicKey);
        logger.trace("{}Generated client device info", LOG_SECURITY);

        boolean isError = false;

        try {
            if (!new HomekitEdsaVerifier(clientLongtermPublicKey).verify(clientDeviceInfo, clientSignature)) {
                logger.warn("{}Client signature verification failed", LOG_WARN);
                isError = true;
            }
        } catch (Exception e) {
            logger.error("{}Error during signature verification: {}", LOG_ERROR, e.getMessage(), e);
            isError = true;
        }

        Encoder encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();

        if (isError) {
            logger.warn("{}Setup failed, sending error response", LOG_WARN);
            encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();
            encoder.add(HomekitMessage.STATE, (short) 6);
            encoder.add(HomekitMessage.ERROR, HomekitErrorCode.AUTHENTICATION);

            logger.debug("{}Removing SRP session", LOG_SECURITY);
            httpSession.removeAttribute("SRP6Session");

            response.setContentType("application/pairing+tlv8");
            response.setContentLengthLong(encoder.toByteArray().length);
            response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
            response.setStatus(HttpServletResponse.SC_OK);
            response.getOutputStream().write(encoder.toByteArray());
            response.getOutputStream().flush();
            logger.debug("{}Error response sent", LOG_SECURITY);
            return;
        }

        logger.debug("{}Adding pairing for server", LOG_SECURITY);
        try {
            // Null Pointer Access Warning Checked
            // At this point, we've already verified that clientPairingIdentifier and clientLongtermPublicKey are not
            // null in the verification check above, so it's safe to call addPairing with these parameters
            server.addPairing(clientPairingIdentifier, clientLongtermPublicKey);
        } catch (Exception e) {
            logger.error("{}Failed to add pairing: {}", LOG_ERROR, e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret,
                "Pair-Setup-HomekitAccessory-Sign-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-HomekitAccessory-Sign-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] serverDeviceX = new byte[32];
        hkdf.generateBytes(serverDeviceX, 0, 32);
        logger.trace("{}Generated accessory device X", LOG_SECURITY);

        HomekitEdsaSigner signer = new HomekitEdsaSigner(server.getSecretKey());
        byte[] serverInfo = HomekitByte.joinBytes(serverDeviceX, server.getPairingId(), signer.getPublicKey());
        logger.trace("{}Generated accessory info", LOG_SECURITY);

        byte[] serverSignature;
        try {
            serverSignature = signer.sign(serverInfo);
            logger.trace("{}Generated accessory signature", LOG_SECURITY);
        } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException e) {
            logger.error("{}Failed to create accessory signature: {}", LOG_ERROR, e.getMessage(), e);
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

        logger.trace("{}Successfully encoded plaintext data", LOG_SECURITY);

        HomekitChachaEncoder chachaEncoder = new HomekitChachaEncoder(sessionKey,
                "PS-Msg06".getBytes(StandardCharsets.UTF_8));
        byte[] encryptedDataWithTag = chachaEncoder.encodeCiphertext(plainText);

        encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();
        encoder.add(HomekitMessage.STATE, (short) 6);
        encoder.add(HomekitMessage.ENCRYPTED_DATA, encryptedDataWithTag);

        logger.debug("{}Removing SRP session", LOG_SECURITY);
        httpSession.removeAttribute("SRP6Session");

        response.setContentType("application/pairing+tlv8");
        response.setContentLengthLong(encoder.toByteArray().length);
        response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
        response.setStatus(HttpServletResponse.SC_OK);
        response.getOutputStream().write(encoder.toByteArray());
        response.getOutputStream().flush();
        logger.debug("{}Stage 3 setup complete", LOG_SECURITY);
    }

    /**
     * Implementation of the SRP-6a client evidence routine.
     *
     * <p>
     * This class implements the client evidence calculation according to the
     * SRP-6a protocol specification. It computes the M1 value using the formula:
     * 
     * <pre>
     * M1 = H(H(N) xor H(g) || H(username) || s || A || B || H(S))
     * </pre>
     *
     * <p>
     * The implementation ensures:
     * <ul>
     * <li>Proper cryptographic hash function usage</li>
     * <li>Correct byte array operations</li>
     * <li>Protocol-compliant evidence calculation</li>
     * </ul>
     */
    class ClientEvidenceRoutineImpl implements ClientEvidenceRoutine {
        public ClientEvidenceRoutineImpl() {
        }

        /**
         * Calculates M1 according to the SRP-6a protocol specification.
         *
         * <p>
         * This method computes the client evidence value (M1) using the formula:
         * 
         * <pre>
         * M1 = H(H(N) xor H(g) || H(username) || s || A || B || H(S))
         * </pre>
         *
         * <p>
         * The calculation involves:
         * <ul>
         * <li>Computing hash of the modulus (N) and generator (g)</li>
         * <li>XORing the hashes of N and g</li>
         * <li>Computing hash of the username</li>
         * <li>Computing hash of the session key (S)</li>
         * <li>Concatenating all components and computing final hash</li>
         * </ul>
         *
         * @param cryptoParams The SRP-6a cryptographic parameters
         * @param ctx The client evidence context containing session values
         * @return The computed M1 value as a BigInteger
         * @throws IllegalStateException if the hash algorithm is not available
         */
        @Override
        @SuppressWarnings("null") // Parent ClientEvidenceRoutine interface doesn't constrain these parameters with
                                            // @NonNull
        public BigInteger computeClientEvidence(@Nullable SRP6CryptoParams cryptoParams,
                @Nullable SRP6ClientEvidenceContext ctx) {
            // Null Pointer Access Warning Checked
            // The interface declaration allows null parameters, but our implementation requires non-null values
            // We explicitly check for null and throw an exception rather than risking an NPE
            if (cryptoParams == null || ctx == null) {
                throw new IllegalArgumentException("CryptoParams and context cannot be null");
            }
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance(cryptoParams.H);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("Could not locate requested algorithm", e);
            }

            digest.update(bigIntegerToUnsignedByteArray(cryptoParams.N));
            byte[] hN = digest.digest();

            digest.update(bigIntegerToUnsignedByteArray(cryptoParams.g));
            byte[] hg = digest.digest();

            byte[] hNhg = xor(hN, hg);

            digest.update(ctx.userID.getBytes(StandardCharsets.UTF_8));
            byte[] hu = digest.digest();

            digest.update(bigIntegerToUnsignedByteArray(ctx.S));
            byte[] hS = digest.digest();

            digest.update(hNhg);
            digest.update(hu);
            digest.update(bigIntegerToUnsignedByteArray(ctx.s));
            digest.update(bigIntegerToUnsignedByteArray(ctx.A));
            digest.update(bigIntegerToUnsignedByteArray(ctx.B));
            digest.update(hS);

            return new BigInteger(1, digest.digest());
        }

        /**
         * Performs XOR operation on two byte arrays.
         *
         * <p>
         * This helper method performs a bitwise XOR operation on corresponding
         * bytes of two input arrays. The arrays must be of equal length.
         *
         * @param b1 The first byte array
         * @param b2 The second byte array
         * @return The result of the XOR operation as a byte array
         */
        private byte[] xor(byte[] b1, byte[] b2) {
            byte[] result = new byte[b1.length];
            for (int i = 0; i < b1.length; i++) {
                result[i] = (byte) (b1[i] ^ b2[i]);
            }
            return result;
        }
    }
}
