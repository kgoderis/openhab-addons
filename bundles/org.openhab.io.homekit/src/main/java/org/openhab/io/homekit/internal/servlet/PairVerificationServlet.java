package org.openhab.io.homekit.internal.servlet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.crypto.ChachaDecoder;
import org.openhab.io.homekit.crypto.ChachaEncoder;
import org.openhab.io.homekit.crypto.EdsaSigner;
import org.openhab.io.homekit.crypto.EdsaVerifier;
import org.openhab.io.homekit.obsolete.PairVerificationStageOneHandler;
import org.openhab.io.homekit.util.Byte;
import org.openhab.io.homekit.util.Error;
import org.openhab.io.homekit.util.Message;
import org.openhab.io.homekit.util.TypeLengthValue;
import org.openhab.io.homekit.util.TypeLengthValue.DecodeResult;
import org.openhab.io.homekit.util.TypeLengthValue.Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import djb.Curve25519;

@SuppressWarnings("serial")
public class PairVerificationServlet extends BaseServlet {

    protected static final Logger logger = LoggerFactory.getLogger(PairVerificationServlet.class);

    private static volatile SecureRandom secureRandom;

    public PairVerificationServlet() {
    }

    public PairVerificationServlet(AccessoryServer server) {
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

        logger.info("Stage 1 : Start");
        logger.info("Stage 1 : Received Body {}", Byte.toHexString(body));

        HttpSession session = request.getSession();

        byte[] clientPublicKey = getClientPublicKey(body);
        session.setAttribute("clientPublicKey", clientPublicKey);
        logger.info("Stage 1 : Client Public Key is {}", Byte.toHexString(clientPublicKey));

        byte[] accessoryPublicKey = new byte[32];
        byte[] accessoryPrivateKey = new byte[32];
        getSecureRandom().nextBytes(accessoryPrivateKey);
        Curve25519.keygen(accessoryPublicKey, null, accessoryPrivateKey);
        session.setAttribute("accessoryPublicKey", accessoryPublicKey);
        logger.info("Stage 1 : Accessory Public Key is {}", Byte.toHexString(accessoryPublicKey));
        logger.info("Stage 1 : Accessory Private Key is {}", Byte.toHexString(accessoryPrivateKey));

        byte[] sharedSecret = new byte[32];
        Curve25519.curve(sharedSecret, accessoryPrivateKey, clientPublicKey);
        session.setAttribute("sharedSecret", sharedSecret);
        logger.info("Stage 1 : Shared Secret is {}", Byte.toHexString(sharedSecret));

        logger.info("Stage 1 : Accessory Pairing Id is {}", server.getPairingId());
        byte[] accessoryInfo = org.openhab.io.homekit.util.Byte.joinBytes(accessoryPublicKey, server.getPairingId(),
                clientPublicKey);
        logger.info("Stage 1 : Accessory Info is {}", Byte.toHexString(accessoryInfo));

        byte[] accessorySignature = null;
        try {
            logger.info("Stage 1 : Accessory Private Key is {}", Byte.toHexString(server.getPrivateKey()));
            accessorySignature = new EdsaSigner(server.getPrivateKey()).sign(accessoryInfo);
        } catch (InvalidKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SignatureException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Encoder encoder = TypeLengthValue.getEncoder();

        logger.info("Stage 1 : Accessory Pairing Id is {}", server.getPairingId());
        encoder.add(Message.IDENTIFIER, server.getPairingId());

        logger.info("Stage 1 : Accessory Signature is {}", Byte.toHexString(accessorySignature));
        encoder.add(Message.SIGNATURE, accessorySignature);
        byte[] plaintext = encoder.toByteArray();

        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Verify-Encrypt-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Verify-Encrypt-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] sessionKey = new byte[32];
        hkdf.generateBytes(sessionKey, 0, 32);
        session.setAttribute("sessionKey", sessionKey);
        logger.info("Stage 1 : Session Key is {}", Byte.toHexString(sessionKey));

        ChachaEncoder chacha = new ChachaEncoder(sessionKey, "PV-Msg02".getBytes(StandardCharsets.UTF_8));
        byte[] ciphertext = chacha.encodeCiphertext(plaintext);

        encoder = TypeLengthValue.getEncoder();
        encoder.add(Message.STATE, (short) 0x02);
        encoder.add(Message.ENCRYPTED_DATA, ciphertext);
        encoder.add(Message.PUBLIC_KEY, accessoryPublicKey);

        logger.info("Stage 1 : End");
        response.setContentType("application/pairing+tlv8");
        response.setContentLengthLong(encoder.toByteArray().length);
        response.setStatus(HttpServletResponse.SC_OK);
        response.getOutputStream().write(encoder.toByteArray());
        response.getOutputStream().flush();
        logger.info("Stage 1 : Flushed");
    }

    protected void doStage2(HttpServletRequest request, HttpServletResponse response, byte[] body)
            throws ServletException, IOException {
        try {
            boolean isError = false;
            logger.info("Stage 2 : Start");
            logger.info("Stage 2 : Received Body {}", Byte.toHexString(body));

            HttpSession session = request.getSession();
            byte[] sessionKey = (byte[]) session.getAttribute("sessionKey");
            logger.info("Stage 2 : Get Session Key {} from Session", Byte.toHexString(sessionKey));

            byte[] clientPublicKey = (byte[]) session.getAttribute("clientPublicKey");
            logger.info("Stage 2 : Get Client Public Key {} from Session", Byte.toHexString(clientPublicKey));

            byte[] accessoryPublicKey = (byte[]) session.getAttribute("accessoryPublicKey");
            logger.info("Stage 2 : Get Accessory Public Key {} from Session", Byte.toHexString(accessoryPublicKey));

            byte[] sharedSecret = (byte[]) session.getAttribute("sharedSecret");
            logger.info("Stage 2 : Get Shared Secret {} from Session", Byte.toHexString(sharedSecret));

            Encoder encoder = TypeLengthValue.getEncoder();

            byte[] plaintext = null;
            ChachaDecoder chacha = new ChachaDecoder(sessionKey, "PV-Msg03".getBytes(StandardCharsets.UTF_8));
            try {
                plaintext = chacha.decodeCiphertext(getAuthTagData(body), getMessageData(body));
            } catch (Exception e) {
                logger.warn("Stage 2 : Unable to decode the ciphertext");
                isError = true;
            }

            byte[] clientPairingId = null;
            byte[] clientLongtermPublicKey = null;
            byte[] clientSignature = null;

            if (!isError) {
                DecodeResult d = TypeLengthValue.decode(plaintext);

                clientPairingId = d.getBytes(Message.IDENTIFIER);
                logger.info("Stage 2 : Client Pairing Id is {}", Byte.toHexString(clientPairingId));

                clientSignature = d.getBytes(Message.SIGNATURE);
                logger.info("Stage 2 : Client Signature is {}", Byte.toHexString(clientSignature));

                clientLongtermPublicKey = server.getPairingPublicKey(clientPairingId);
                if (clientLongtermPublicKey == null) {
                    isError = true;
                    logger.warn("Stage 2 : Unknown Pairing {}", new String(clientPairingId, StandardCharsets.UTF_8));
                } else {
                    logger.info("Stage 2 : Client Long Term Public Key is {}",
                            Byte.toHexString(clientLongtermPublicKey));
                }
            }

            if (!isError) {
                byte[] clientDeviceInfo = Byte.joinBytes(clientPublicKey, clientPairingId, accessoryPublicKey);

                try {
                    boolean signatureVerification = new EdsaVerifier(clientLongtermPublicKey).verify(clientDeviceInfo,
                            clientSignature);
                    if (!signatureVerification) {
                        isError = true;
                    }
                } catch (Exception e) {
                    isError = true;
                }

                if (isError) {
                    logger.warn("Stage 2 : Unable to verify the Client Signature");
                }
            }

            if (!isError) {
                logger.info("Stage 2 : Completed pair verification");

                session.setAttribute("Control-Write-Encryption-Key",
                        createKey("Control-Write-Encryption-Key", sharedSecret));
                logger.info("Stage 2 : Write Key is {}",
                        Byte.toHexString((byte[]) session.getAttribute("Control-Write-Encryption-Key")));

                session.setAttribute("Control-Read-Encryption-Key",
                        createKey("Control-Read-Encryption-Key", sharedSecret));
                logger.info("Stage 2 : Read Key is {}",
                        Byte.toHexString((byte[]) session.getAttribute("Control-Read-Encryption-Key")));

                request.setAttribute("HomekitEncryptionEnabled", true);
            }

            if (isError) {
                encoder.add(Message.STATE, (short) 0x04);
                encoder.add(Message.ERROR, Error.AUTHENTICATION);
            } else {
                encoder.add(Message.STATE, (short) 0x04);
            }

            logger.info("Stage 2 : End");
            response.setContentType("application/pairing+tlv8");
            response.setContentLengthLong(encoder.toByteArray().length);
            // response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
            response.setStatus(HttpServletResponse.SC_OK);
            response.getOutputStream().write(encoder.toByteArray());
            response.getOutputStream().flush();
            logger.info("Stage 2 : Flushed");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] getClientPublicKey(byte[] content) throws IOException {
        DecodeResult d = TypeLengthValue.decode(content);
        return d.getBytes(Message.PUBLIC_KEY);
    }

    private byte[] createKey(String info, byte[] sharedSecret) {
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Control-Salt".getBytes(StandardCharsets.UTF_8),
                info.getBytes(StandardCharsets.UTF_8)));
        byte[] key = new byte[32];
        hkdf.generateBytes(key, 0, 32);
        return key;
    }

    private static SecureRandom getSecureRandom() {
        if (secureRandom == null) {
            synchronized (PairVerificationStageOneHandler.class) {
                if (secureRandom == null) {
                    secureRandom = new SecureRandom();
                }
            }
        }
        return secureRandom;
    }

}
