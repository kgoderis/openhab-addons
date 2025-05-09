package org.openhab.io.homekit.server.servlet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
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

@SuppressWarnings("serial")
public class HomekitPairVerificationServlet extends HomekitBaseServlet {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitPairVerificationServlet.class);
    protected static final String LOG_PREFIX = "Homekit HomekitPairVerificationServlet: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "HomekitAccessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    protected static final String LOG_EVENT = LOG_PREFIX + "Event - ";
    protected static final String LOG_SERVER = LOG_PREFIX + "Server - ";
    protected static final String LOG_PAIRING = LOG_PREFIX + "HomekitPairing - ";

    public HomekitPairVerificationServlet() {
    }

    public HomekitPairVerificationServlet(HomekitAccessoryServer server) {
        super(server);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            byte[] body = IOUtils.toByteArray(request.getInputStream());
            short stage = getState(body);

            switch (stage) {
                case 1: {
                    doStage1(request, response, body);
                    break;
                }
                case 3: {
                    doStage2(request, response, body);
                    break;
                }
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    protected void doStage1(HttpServletRequest request, HttpServletResponse response, byte[] body)
            throws ServletException, IOException {

        logger.info("{}Stage 1 Start", LOG_STATE);
        logger.info("{}Stage 1 Received Body {}", LOG_EVENT, HomekitByte.toHexString(body));

        HttpSession session = request.getSession();

        byte[] clientPublicKey = getClientPublicKey(body);
        session.setAttribute("clientPublicKey", clientPublicKey);
        logger.info("{}Stage 1 Client Public Key is {}", LOG_ACCESSORY, HomekitByte.toHexString(clientPublicKey));

        byte[] accessoryPublicKey = new byte[32];
        byte[] accessoryPrivateKey = new byte[32];
        HomekitEncryptionEngine.getSecureRandom().nextBytes(accessoryPrivateKey);
        Curve25519.keygen(accessoryPublicKey, null, accessoryPrivateKey);
        session.setAttribute("accessoryPublicKey", accessoryPublicKey);
        logger.info("{}Stage 1 HomekitAccessory Public Key is {}", LOG_ACCESSORY, HomekitByte.toHexString(accessoryPublicKey));
        logger.info("{}Stage 1 HomekitAccessory Private Key is {}", LOG_ACCESSORY, HomekitByte.toHexString(accessoryPrivateKey));

        byte[] sharedSecret = new byte[32];
        Curve25519.curve(sharedSecret, accessoryPrivateKey, clientPublicKey);
        session.setAttribute("sharedSecret", sharedSecret);
        logger.info("{}Stage 1 Shared Secret is {}", LOG_ACCESSORY, HomekitByte.toHexString(sharedSecret));

        logger.info("{}Stage 1 HomekitAccessory HomekitPairing Id is {}", LOG_PAIRING, server.getPairingId());
        byte[] accessoryInfo = org.openhab.io.homekit.util.HomekitByte.joinBytes(accessoryPublicKey, server.getPairingId(),
                clientPublicKey);
        logger.info("{}Stage 1 HomekitAccessory Info is {}", LOG_EVENT, HomekitByte.toHexString(accessoryInfo));

        byte[] accessorySignature = null;
        try {
            logger.info("{}Stage 1 HomekitAccessory Private Key is {}", LOG_ACCESSORY,
                    HomekitByte.toHexString(server.getSecretKey()));
            accessorySignature = new HomekitEdsaSigner(server.getSecretKey()).sign(accessoryInfo);
        } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException e) {
            logger.error("{}Stage 1 Error creating accessory signature", LOG_ERROR, e);
        }

        Encoder encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();

        logger.info("{}Stage 1 HomekitAccessory HomekitPairing Id is {}", LOG_PAIRING, server.getPairingId());
        encoder.add(HomekitMessage.IDENTIFIER, server.getPairingId());

        logger.info("{}Stage 1 HomekitAccessory Signature is {}", LOG_ACCESSORY, HomekitByte.toHexString(accessorySignature));
        encoder.add(HomekitMessage.SIGNATURE, accessorySignature);
        byte[] plaintext = encoder.toByteArray();

        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Verify-Encrypt-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Verify-Encrypt-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] sessionKey = new byte[32];
        hkdf.generateBytes(sessionKey, 0, 32);
        session.setAttribute("sessionKey", sessionKey);
        logger.info("{}Stage 1 Session Key is {}", LOG_ACCESSORY, HomekitByte.toHexString(sessionKey));

        HomekitChachaEncoder chacha = new HomekitChachaEncoder(sessionKey, "PV-Msg02".getBytes(StandardCharsets.UTF_8));
        byte[] ciphertext = chacha.encodeCiphertext(plaintext);

        encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();
        encoder.add(HomekitMessage.STATE, (short) 0x02);
        encoder.add(HomekitMessage.ENCRYPTED_DATA, ciphertext);
        encoder.add(HomekitMessage.PUBLIC_KEY, accessoryPublicKey);

        logger.info("{}Stage 1 End", LOG_STATE);
        response.setContentType("application/pairing+tlv8");
        response.setContentLengthLong(encoder.toByteArray().length);
        response.setStatus(HttpServletResponse.SC_OK);
        response.getOutputStream().write(encoder.toByteArray());
        response.getOutputStream().flush();
        logger.info("{}Stage 1 Flushed", LOG_STATE);
    }

    protected void doStage2(HttpServletRequest request, HttpServletResponse response, byte[] body)
            throws ServletException, IOException {
        try {
            boolean isError = false;
            logger.info("{}Stage 2 Start", LOG_STATE);
            logger.info("{}Stage 2 Received Body {}", LOG_EVENT, HomekitByte.toHexString(body));

            HttpSession session = request.getSession();
            byte[] sessionKey = (byte[]) session.getAttribute("sessionKey");
            logger.info("{}Stage 2 Get Session Key {} from Session", LOG_ACCESSORY, HomekitByte.toHexString(sessionKey));

            byte[] clientPublicKey = (byte[]) session.getAttribute("clientPublicKey");
            logger.info("{}Stage 2 Get Client Public Key {} from Session", LOG_ACCESSORY,
                    HomekitByte.toHexString(clientPublicKey));

            byte[] accessoryPublicKey = (byte[]) session.getAttribute("accessoryPublicKey");
            logger.info("{}Stage 2 Get HomekitAccessory Public Key {} from Session", LOG_ACCESSORY,
                    HomekitByte.toHexString(accessoryPublicKey));

            byte[] sharedSecret = (byte[]) session.getAttribute("sharedSecret");
            logger.info("{}Stage 2 Get Shared Secret {} from Session", LOG_ACCESSORY, HomekitByte.toHexString(sharedSecret));

            Encoder encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();

            byte[] plaintext = null;
            HomekitChachaDecoder chacha = new HomekitChachaDecoder(sessionKey, "PV-Msg03".getBytes(StandardCharsets.UTF_8));
            try {
                plaintext = chacha.decodeCiphertext(getAuthTagData(body), getMessageData(body));
            } catch (Exception e) {
                logger.warn("{}Stage 2 Unable to decode the ciphertext", LOG_WARN);
                isError = true;
            }

            byte[] clientPairingId = null;
            byte[] clientLongtermPublicKey = null;
            byte[] clientSignature = null;

            if (!isError) {
                DecodeResult d = HomekitTypeLengthValueEncoderDecoder.decode(plaintext);

                clientPairingId = d.getBytes(HomekitMessage.IDENTIFIER);
                logger.info("{}Stage 2 Client HomekitPairing Id is {}", LOG_ACCESSORY, HomekitByte.toHexString(clientPairingId));

                clientSignature = d.getBytes(HomekitMessage.SIGNATURE);
                logger.info("{}Stage 2 Client Signature is {}", LOG_ACCESSORY, HomekitByte.toHexString(clientSignature));

                clientLongtermPublicKey = server.getPublicKey(clientPairingId);
                if (clientLongtermPublicKey == null) {
                    isError = true;
                    logger.warn("{}Stage 2 Unknown HomekitPairing {}", LOG_WARN,
                            new String(clientPairingId, StandardCharsets.UTF_8));
                } else {
                    logger.info("{}Stage 2 Client Long Term Public Key is {}", LOG_ACCESSORY,
                            HomekitByte.toHexString(clientLongtermPublicKey));
                }
            }

            if (!isError) {
                byte[] clientDeviceInfo = HomekitByte.joinBytes(clientPublicKey, clientPairingId, accessoryPublicKey);

                try {
                    boolean signatureVerification = new HomekitEdsaVerifier(clientLongtermPublicKey).verify(clientDeviceInfo,
                            clientSignature);
                    if (!signatureVerification) {
                        isError = true;
                    }
                } catch (Exception e) {
                    isError = true;
                }

                if (isError) {
                    logger.warn("{}Stage 2 Unable to verify the Client Signature", LOG_WARN);
                }
            }

            if (!isError) {
                logger.info("{}Stage 2 Completed pair verification", LOG_STATE);

                session.setAttribute("Control-Write-Encryption-Key",
                        HomekitEncryptionEngine.createKey("Control-Write-Encryption-Key", sharedSecret));
                logger.info("{}Stage 2 Write Key is {}", LOG_ACCESSORY,
                        HomekitByte.toHexString((byte[]) session.getAttribute("Control-Write-Encryption-Key")));

                session.setAttribute("Control-Read-Encryption-Key",
                        HomekitEncryptionEngine.createKey("Control-Read-Encryption-Key", sharedSecret));
                logger.info("{}Stage 2 Read Key is {}", LOG_ACCESSORY,
                        HomekitByte.toHexString((byte[]) session.getAttribute("Control-Read-Encryption-Key")));

                request.setAttribute("HomekitEncryptionEnabled", true);
            }

            if (isError) {
                encoder.add(HomekitMessage.STATE, (short) 0x04);
                encoder.add(HomekitMessage.ERROR, HomekitErrorCode.AUTHENTICATION);
            } else {
                encoder.add(HomekitMessage.STATE, (short) 0x04);
            }

            logger.info("{}Stage 2 End", LOG_STATE);
            response.setContentType("application/pairing+tlv8");
            response.setContentLengthLong(encoder.toByteArray().length);
            // response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
            response.setStatus(HttpServletResponse.SC_OK);
            response.getOutputStream().write(encoder.toByteArray());
            response.getOutputStream().flush();
            logger.info("{}Stage 2 Flushed", LOG_STATE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] getClientPublicKey(byte[] content) throws IOException {
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
