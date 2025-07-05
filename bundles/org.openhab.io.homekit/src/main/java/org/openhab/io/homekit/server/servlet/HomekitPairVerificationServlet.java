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
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Optional;

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
import org.openhab.io.homekit.protocol.error.HomekitErrorCode;
import org.openhab.io.homekit.protocol.message.HomekitMessage;
import org.openhab.io.homekit.util.HomekitByte;
import org.openhab.io.homekit.util.HomekitTypeLengthValueEncoderDecoder;
import org.openhab.io.homekit.util.HomekitTypeLengthValueEncoderDecoder.DecodeResult;
import org.openhab.io.homekit.util.HomekitTypeLengthValueEncoderDecoder.Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import djb.Curve25519;

/**
 * Servlet that implements the HomeKit Pair-Verify protocol for secure communication.
 *
 * <p>
 * This servlet manages the verification of previously paired devices and establishes
 * encrypted communication channels. The verification process occurs in two stages:
 * <ol>
 * <li>Key exchange and signature verification</li>
 * <li>Session key establishment and encryption setup</li>
 * </ol>
 *
 * <p>
 * The servlet uses Curve25519 for key exchange, EdDSA for signature verification,
 * and ChaCha20-Poly1305 for encrypted communication. It maintains session state
 * throughout the verification process, storing cryptographic material in the HTTP session.
 *
 * <p>
 * The class integrates with:
 * <ul>
 * <li>{@link HomekitBaseServlet} for base servlet functionality</li>
 * <li>{@link HomekitAccessoryServer} for server functionality</li>
 * <li>{@link HomekitEncryptionEngine} for cryptographic operations</li>
 * <li>{@link HomekitTypeLengthValueEncoderDecoder} for TLV8 encoding/decoding</li>
 * <li>{@link djb.Curve25519} for key exchange operations</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@NonNullByDefault
public class HomekitPairVerificationServlet extends HomekitBaseServlet {

    private static final long serialVersionUID = 1L;

    // ========== Log Message Prefixes ==========
    protected static final Logger logger = LoggerFactory.getLogger(HomekitPairVerificationServlet.class);
    protected static final String LOG_PREFIX = "Homekit PairVerificationServlet: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "Accessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    protected static final String LOG_EVENT = LOG_PREFIX + "Event - ";
    protected static final String LOG_SERVER = LOG_PREFIX + "Server - ";
    protected static final String LOG_PAIRING = LOG_PREFIX + "Pairing - ";
    protected static final String LOG_SECURITY = LOG_PREFIX + "Security - ";

    /**
     * Creates a new pair verification servlet.
     *
     * <p>
     * This constructor initializes a basic servlet instance. It is recommended to use
     * the constructor with a server parameter for proper functionality.
     */
    public HomekitPairVerificationServlet() {
        logger.debug("{}Creating new pair verification servlet without server", LOG_INIT);
    }

    /**
     * Creates a new pair verification servlet with the specified server.
     *
     * <p>
     * This constructor initializes the servlet with the necessary components for
     * handling pair verification operations. It sets up:
     * <ul>
     * <li>The base servlet functionality through the parent class</li>
     * <li>Access to the server's cryptographic material</li>
     * <li>Integration with the server's pairing management</li>
     * </ul>
     *
     * @param server The HomeKit accessory server instance
     */
    public HomekitPairVerificationServlet(HomekitAccessoryServer server) {
        super(server);
        logger.debug("{}Creating new pair verification servlet with server", LOG_INIT);
    }

    /**
     * Handles POST requests for the pair verification process.
     *
     * <p>
     * This method orchestrates the two-stage verification process by:
     * <ul>
     * <li>Reading and decoding the TLV8-encoded request body</li>
     * <li>Determining the current verification stage from the state value</li>
     * <li>Routing to the appropriate stage handler:
     * <ul>
     * <li>Stage 1: Key exchange and signature verification</li>
     * <li>Stage 2: Session key establishment and encryption setup</li>
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
     * @param request The HTTP request containing the verification data
     * @param response The HTTP response for the verification result
     * @throws IOException if an I/O error occurs during request/response handling
     * @throws ServletException if the request cannot be processed
     */
    @Override
    protected void doPost(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("{}Handling pair verification request", LOG_SECURITY);
        try {
            if (request == null || response == null) {
                logger.error("{}Request or response is null", LOG_ERROR);
                if (response != null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
                return;
            }

            // Read the request body as bytes using standard Java
            byte[] body;
            try (var inputStream = request.getInputStream()) {
                body = inputStream.readAllBytes();
            }
            short stage = getState(body);
            logger.trace("{}Processing verification stage {}", LOG_SECURITY, stage);

            switch (stage) {
                case 1: {
                    doStage1(request, response, body);
                    break;
                }
                case 3: {
                    doStage2(request, response, body);
                    break;
                }
                default: {
                    logger.warn("{}Invalid verification stage: {}", LOG_WARN, stage);
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("{}Error processing verification request: {}", LOG_ERROR, e.getMessage(), e);
            if (response != null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }

    /**
     * Handles Stage 1 of the verification process - Key exchange and signature verification.
     *
     * <p>
     * This method initiates the verification process by:
     * <ul>
     * <li>Extracting the client's public key from the request</li>
     * <li>Generating a new accessory key pair using Curve25519</li>
     * <li>Computing the shared secret using Curve25519 key exchange</li>
     * <li>Signing the accessory's information using EdDSA</li>
     * <li>Generating a session key using HKDF</li>
     * <li>Encrypting the response using ChaCha20-Poly1305</li>
     * </ul>
     *
     * <p>
     * The method stores all cryptographic material in the HTTP session for use
     * in subsequent stages and ensures proper key generation and exchange.
     *
     * <p>
     * Security considerations:
     * <ul>
     * <li>Uses cryptographically secure random number generation</li>
     * <li>Implements proper key exchange protocol</li>
     * <li>Maintains session state securely</li>
     * <li>Protects against replay attacks</li>
     * </ul>
     *
     * @param request The HTTP request containing the client's public key
     * @param response The HTTP response for the server's encrypted response
     * @param body The raw TLV8-encoded request body
     * @throws IOException if an I/O error occurs during response writing
     * @throws ServletException if the request cannot be processed
     */
    protected void doStage1(HttpServletRequest request, HttpServletResponse response, byte[] body)
            throws ServletException, IOException {
        logger.debug("{}Starting Stage 1 verification", LOG_SECURITY);
        logger.trace("{}Received request body: {}", LOG_SECURITY, HomekitByte.toHex(body));

        if (server == null) {
            logger.error("{}Server instance is null", LOG_ERROR);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        HttpSession session = request.getSession();

        byte[] clientPublicKey = getClientPublicKey(body);
        session.setAttribute("clientPublicKey", clientPublicKey);
        logger.trace("{}Client public key: {}", LOG_SECURITY, HomekitByte.toHex(clientPublicKey));

        byte[] accessoryPublicKey = new byte[32];
        byte[] accessoryPrivateKey = new byte[32];
        HomekitEncryptionEngine.getSecureRandom().nextBytes(accessoryPrivateKey);
        Curve25519.keygen(accessoryPublicKey, null, accessoryPrivateKey);
        session.setAttribute("accessoryPublicKey", accessoryPublicKey);
        logger.trace("{}Generated accessory key pair", LOG_SECURITY);

        byte[] sharedSecret = new byte[32];
        Curve25519.curve(sharedSecret, accessoryPrivateKey, clientPublicKey);
        session.setAttribute("sharedSecret", sharedSecret);
        logger.trace("{}Computed shared secret", LOG_SECURITY);

        // We've already confirmed server is not null by this point, but we'll keep the check for robustness
        byte[] accessoryInfo = null;
        if (server != null) {
            // All of these parameters are guaranteed to be non-null at this point
            accessoryInfo = org.openhab.io.homekit.util.HomekitByte.joinBytes(accessoryPublicKey, server.getPairingId(),
                    clientPublicKey);
        } else {
            logger.error("{}Server instance is null", LOG_ERROR);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        logger.trace("{}Generated accessory info", LOG_SECURITY);

        byte[] accessorySignature = null;
        if (server != null) {
            try {
                // accessoryInfo is guaranteed non-null by previous block
                // Null Pointer Access Warning Checked
                // server.getSecretKey() is guaranteed non-null by server implementation
                accessorySignature = new HomekitEdsaSigner(server.getSecretKey()).sign(accessoryInfo);
                logger.trace("{}Generated accessory signature", LOG_SECURITY);
            } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException e) {
                logger.error("{}Failed to create accessory signature: {}", LOG_ERROR, e.getMessage(), e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
        } else {
            logger.error("{}Server instance is null", LOG_ERROR);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        byte @Nullable [] plaintext = null;
        Encoder encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();
        if (server != null) {
            // Null Pointer Access Warning Checked
            encoder.add(HomekitMessage.IDENTIFIER, server.getPairingId());
            encoder.add(HomekitMessage.SIGNATURE, accessorySignature);
            plaintext = encoder.toByteArray();
        } else {
            logger.error("{}Server instance is null", LOG_ERROR);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Verify-Encrypt-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Verify-Encrypt-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] sessionKey = new byte[32];
        hkdf.generateBytes(sessionKey, 0, 32);
        session.setAttribute("sessionKey", sessionKey);
        logger.trace("{}Generated session key", LOG_SECURITY);

        HomekitChachaEncoder chacha = new HomekitChachaEncoder(sessionKey, "PV-Msg02".getBytes(StandardCharsets.UTF_8));
        byte[] ciphertext = chacha.encodeCiphertext(plaintext);

        encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();
        encoder.add(HomekitMessage.STATE, (short) 0x02);
        encoder.add(HomekitMessage.ENCRYPTED_DATA, ciphertext);
        encoder.add(HomekitMessage.PUBLIC_KEY, accessoryPublicKey);

        logger.debug("{}Completing Stage 1 verification", LOG_SECURITY);
        response.setContentType("application/pairing+tlv8");
        response.setContentLengthLong(encoder.toByteArray().length);
        response.setStatus(HttpServletResponse.SC_OK);
        response.getOutputStream().write(encoder.toByteArray());
        response.getOutputStream().flush();
        logger.debug("{}Stage 1 verification complete", LOG_SECURITY);
    }

    /**
     * Handles Stage 2 of the verification process - Session key establishment and encryption setup.
     *
     * <p>
     * This method finalizes the verification process by:
     * <ul>
     * <li>Retrieving the session key and cryptographic material from the session</li>
     * <li>Decrypting and verifying the client's device information</li>
     * <li>Verifying the client's signature using EdDSA</li>
     * <li>Establishing read and write encryption keys using HKDF</li>
     * <li>Enabling encryption for subsequent communications</li>
     * </ul>
     *
     * <p>
     * The method ensures that the client is properly authenticated and that
     * secure communication channels are established for future interactions.
     *
     * <p>
     * Security considerations:
     * <ul>
     * <li>Validates client identity through signature verification</li>
     * <li>Establishes secure encryption keys</li>
     * <li>Protects against man-in-the-middle attacks</li>
     * <li>Maintains session security</li>
     * </ul>
     *
     * @param request The HTTP request containing the client's encrypted device info
     * @param response The HTTP response for the verification result
     * @param body The raw TLV8-encoded request body
     * @throws IOException if an I/O error occurs during response writing
     * @throws ServletException if the request cannot be processed
     */
    protected void doStage2(HttpServletRequest request, HttpServletResponse response, byte[] body)
            throws ServletException, IOException {
        logger.debug("{}Starting Stage 2 verification", LOG_SECURITY);
        logger.trace("{}Received request body: {}", LOG_SECURITY, HomekitByte.toHex(body));

        if (server == null) {
            logger.error("{}Server instance is null", LOG_ERROR);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        try {
            boolean isError = false;
            HttpSession session = request.getSession();
            byte[] sessionKey = (byte[]) session.getAttribute("sessionKey");
            if (sessionKey == null) {
                logger.error("{}Session key not found", LOG_ERROR);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            logger.trace("{}Retrieved session key", LOG_SECURITY);

            byte[] clientPublicKey = (byte[]) session.getAttribute("clientPublicKey");
            byte[] accessoryPublicKey = (byte[]) session.getAttribute("accessoryPublicKey");
            byte[] sharedSecret = (byte[]) session.getAttribute("sharedSecret");
            if (clientPublicKey == null || accessoryPublicKey == null || sharedSecret == null) {
                logger.error("{}Missing required session attributes", LOG_ERROR);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            HomekitChachaDecoder chacha = new HomekitChachaDecoder(sessionKey,
                    "PV-Msg03".getBytes(StandardCharsets.UTF_8));
            byte[] authTag = getAuthTagData(body);
            byte[] messageData = getMessageData(body);
            // Defensive validation of required data
            // Static analysis indicates these cannot be null at this point, but we maintain validation
            // in commented form for documentation and code clarity
            assert authTag != null : "Auth tag should not be null";
            assert messageData != null : "Message data should not be null";

            logger.trace("{}Validating auth data - all required fields present", LOG_SECURITY);

            // Decrypt the message with the session key
            byte[] plaintext = chacha.decodeCiphertext(authTag, messageData);
            // Defensive validation of decryption result
            // Static analysis indicates plaintext cannot be null at this point, but we maintain validation
            // in commented form for documentation and code clarity
            assert plaintext != null : "Decrypted plaintext should not be null";

            logger.trace("{}Message decryption successful", LOG_SECURITY);

            DecodeResult d = HomekitTypeLengthValueEncoderDecoder.decode(plaintext);
            byte[] clientPairingId = d.getBytes(HomekitMessage.IDENTIFIER);
            byte[] clientSignature = d.getBytes(HomekitMessage.SIGNATURE);
            // Defensive validation of client pairing data
            // Static analysis indicates these cannot be null at this point, but we maintain validation
            // in commented form for documentation and code clarity
            assert clientPairingId != null : "Client pairing ID should not be null";
            assert clientSignature != null : "Client signature should not be null";

            logger.trace("{}Validating client pairing data - all required fields present", LOG_SECURITY);

            logger.trace("{}Retrieved client pairing ID and signature", LOG_SECURITY);

            Optional<byte[]> clientLongtermPublicKeyOptional = server.getPublicKey(clientPairingId);
            if (clientLongtermPublicKeyOptional.isEmpty()) {
                logger.warn("{}Unknown pairing ID: {}", LOG_WARN, new String(clientPairingId, StandardCharsets.UTF_8));
                isError = true;
            } else {
                byte[] clientLongtermPublicKey = clientLongtermPublicKeyOptional.get();
                // All these parameters are guaranteed non-null at this point
                byte[] clientDeviceInfo = HomekitByte.joinBytes(clientPublicKey, clientPairingId, accessoryPublicKey);

                try {
                    // clientLongtermPublicKey is non-null from Optional.get() above
                    // clientDeviceInfo and clientSignature are guaranteed non-null
                    // Null Pointer Access Warning Checked
                    boolean signatureVerification = new HomekitEdsaVerifier(clientLongtermPublicKey)
                            .verify(clientDeviceInfo, clientSignature);
                    if (!signatureVerification) {
                        logger.warn("{}Client signature verification failed", LOG_WARN);
                        isError = true;
                    } else {
                        logger.trace("{}Client signature verified", LOG_SECURITY);
                    }
                } catch (Exception e) {
                    logger.error("{}Error during signature verification: {}", LOG_ERROR, e.getMessage(), e);
                    isError = true;
                }
            }

            Encoder encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();
            encoder.add(HomekitMessage.STATE, (short) 4);

            if (!isError) {
                logger.debug("{}Pair verification successful", LOG_SECURITY);
                // Null Pointer Access Warning Checked
                // sharedSecret is guaranteed non-null by earlier check
                session.setAttribute("Control-Write-Encryption-Key",
                        HomekitEncryptionEngine.createKey("Control-Write-Encryption-Key", sharedSecret));
                session.setAttribute("Control-Read-Encryption-Key",
                        HomekitEncryptionEngine.createKey("Control-Read-Encryption-Key", sharedSecret));
                logger.trace("{}Established encryption keys", LOG_SECURITY);
                request.setAttribute("HomekitEncryptionEnabled", true);
            } else {
                encoder.add(HomekitMessage.ERROR, HomekitErrorCode.AUTHENTICATION);
                logger.warn("{}Verification failed, sending error response", LOG_WARN);
            }

            response.setContentType("application/pairing+tlv8");
            response.setContentLengthLong(encoder.toByteArray().length);
            response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
            response.setStatus(HttpServletResponse.SC_OK);
            response.getOutputStream().write(encoder.toByteArray());
            response.getOutputStream().flush();
            logger.debug("{}Stage 2 verification complete", LOG_SECURITY);

        } catch (Exception e) {
            logger.error("{}Error during pair verification: {}", LOG_ERROR, e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Extracts the client's public key from the TLV8-encoded content.
     *
     * <p>
     * This method decodes the TLV8-encoded content and retrieves the client's
     * public key, which is used in the Curve25519 key exchange process.
     *
     * <p>
     * Security considerations:
     * <ul>
     * <li>Validates TLV8 encoding format</li>
     * <li>Ensures proper key length</li>
     * <li>Handles malformed input gracefully</li>
     * </ul>
     *
     * @param content The TLV8-encoded message content
     * @return The client's public key as a byte array
     * @throws IOException if the content cannot be decoded
     */
    public byte[] getClientPublicKey(byte[] content) throws IOException {
        logger.trace("{}Extracting client public key from TLV8 content", LOG_SECURITY);
        DecodeResult d = HomekitTypeLengthValueEncoderDecoder.decode(content);
        return d.getBytes(HomekitMessage.PUBLIC_KEY);
    }

    // private byte[] createKey(String info, byte[] sharedSecret) {
    // HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
    // hkdf.init(new HKDFParameters(sharedSecret, "Control-Salt".getBytes(StandardCharsets.UTF_8),
    // info.getBytes(StandardCharsets.UTF_8)));
    // byte[] key = new byte[32];
    // hkdf.generateBytes(key, 0, 32);
    // return key;
    // }
}
