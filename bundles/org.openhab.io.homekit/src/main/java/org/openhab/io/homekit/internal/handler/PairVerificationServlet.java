package org.openhab.io.homekit.internal.handler;

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
import org.eclipse.jetty.http.HttpHeader;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.crypto.ChachaDecoder;
import org.openhab.io.homekit.crypto.ChachaEncoder;
import org.openhab.io.homekit.crypto.EdsaSigner;
import org.openhab.io.homekit.crypto.EdsaVerifier;
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

    public PairVerificationServlet(AccessoryServer server) {
        super(server);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            byte[] body = IOUtils.toByteArray(request.getInputStream());
            short stage = getStage(body);

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
        logger.info("Stage 1 : Received Body {}", byteToHexString(body));

        HttpSession session = request.getSession();

        byte[] clientPublicKey = getClientPublicKey(body);
        session.setAttribute("clientPublicKey", clientPublicKey);
        logger.info("Stage 1 : Client Public Key is {}", byteToHexString(clientPublicKey));

        byte[] accessoryPublicKey = new byte[32];
        byte[] accessoryPrivateKey = new byte[32];
        getSecureRandom().nextBytes(accessoryPrivateKey);
        Curve25519.keygen(accessoryPublicKey, null, accessoryPrivateKey);
        session.setAttribute("accessoryPublicKey", accessoryPublicKey);
        logger.info("Stage 1 : Accessory Public Key is {}", byteToHexString(accessoryPublicKey));
        logger.info("Stage 1 : Accessory Private Key is {}", byteToHexString(accessoryPrivateKey));

        byte[] sharedSecret = new byte[32];
        Curve25519.curve(sharedSecret, accessoryPrivateKey, clientPublicKey);
        session.setAttribute("sharedSecret", sharedSecret);
        logger.info("Stage 1 : Shared Secret is {}", byteToHexString(sharedSecret));

        logger.info("Stage 1 : Server Pairing Id is {}", server.getId());
        byte[] accessoryInfo = org.openhab.io.homekit.util.Byte.joinBytes(accessoryPublicKey,
                server.getId().getBytes(StandardCharsets.UTF_8), clientPublicKey);
        logger.info("Stage 1 : Accessory Info is {}", byteToHexString(accessoryInfo));

        byte[] accessorySignature = null;
        try {
            logger.info("Stage 1 : Server Private Key is {}", byteToHexString(server.getPrivateKey()));
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
        logger.info("Stage 1 : Accessory Signature is {}", byteToHexString(accessorySignature));

        Encoder encoder = TypeLengthValue.getEncoder();
        encoder.add(Message.IDENTIFIER, server.getId().getBytes(StandardCharsets.UTF_8));
        logger.info("Stage 1 : Encoder : Add {} : {} ", Message.IDENTIFIER,
                byteToHexString(server.getId().getBytes(StandardCharsets.UTF_8)));
        encoder.add(Message.SIGNATURE, accessorySignature);
        logger.info("Stage 1 : Encoder : Add {} : {} ", Message.SIGNATURE, byteToHexString(accessorySignature));
        byte[] plaintext = encoder.toByteArray();

        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Verify-Encrypt-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Verify-Encrypt-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] sessionKey = new byte[32];
        hkdf.generateBytes(sessionKey, 0, 32);
        session.setAttribute("sessionKey", sessionKey);
        logger.info("Stage 1 : Session Key is {}", byteToHexString(sessionKey));

        ChachaEncoder chacha = new ChachaEncoder(sessionKey, "PV-Msg02".getBytes(StandardCharsets.UTF_8));
        byte[] ciphertext = chacha.encodeCiphertext(plaintext);

        encoder = TypeLengthValue.getEncoder();
        encoder.add(Message.STATE, (short) 0x02);
        logger.info("Stage 1 : Encoder : Add {} : {} ", Message.STATE, (short) 0x02);
        encoder.add(Message.ENCRYPTED_DATA, ciphertext);
        logger.info("Stage 1 : Encoder : Add {} : {} ", Message.ENCRYPTED_DATA, byteToHexString(ciphertext));
        encoder.add(Message.PUBLIC_KEY, accessoryPublicKey);
        logger.info("Stage 1 : Encoder : Add {} : {} ", Message.PUBLIC_KEY, byteToHexString(accessoryPublicKey));

        logger.info("Stage 1 : End");
        response.setContentType("application/pairing+tlv8");
        response.setContentLengthLong(encoder.toByteArray().length);
        response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());

        response.setStatus(HttpServletResponse.SC_OK);
        response.getOutputStream().write(encoder.toByteArray());
        response.getOutputStream().flush();
        logger.info("Stage 1 : Flushed");
    }

    protected void doStage2(HttpServletRequest request, HttpServletResponse response, byte[] body)
            throws ServletException, IOException {
        try {
            logger.info("Stage 2 : Start");
            logger.info("Stage 2 : Received Body {}", byteToHexString(body));

            HttpSession session = request.getSession();
            byte[] sessionKey = (byte[]) session.getAttribute("sessionKey");
            logger.info("Stage 2 : Get Session Key {} from Session", byteToHexString(sessionKey));

            byte[] clientPublicKey = (byte[]) session.getAttribute("clientPublicKey");
            logger.info("Stage 2 : Get Client Public Key {} from Session", byteToHexString(clientPublicKey));

            byte[] accessoryPublicKey = (byte[]) session.getAttribute("accessoryPublicKey");
            logger.info("Stage 2 : Get Accessory Public Key {} from Session", byteToHexString(accessoryPublicKey));

            byte[] sharedSecret = (byte[]) session.getAttribute("sharedSecret");
            logger.info("Stage 2 : Get Shared Secret {} from Session", byteToHexString(sharedSecret));

            ChachaDecoder chacha = new ChachaDecoder(sessionKey, "PV-Msg03".getBytes(StandardCharsets.UTF_8));
            byte[] plaintext = chacha.decodeCiphertext(getAuthTagData(body), getMessageData(body));

            DecodeResult d = TypeLengthValue.decode(plaintext);
            byte[] clientPairingId = d.getBytes(Message.IDENTIFIER);
            logger.info("Stage 2 : Client Pairing Id is {}", byteToHexString(clientPairingId));

            byte[] clientSignature = d.getBytes(Message.SIGNATURE);
            logger.info("Stage 2 : Client Signature is {}", byteToHexString(clientSignature));

            byte[] clientDeviceInfo = org.openhab.io.homekit.util.Byte.joinBytes(clientPublicKey, clientPairingId,
                    accessoryPublicKey);

            byte[] clientLongtermPublicKey = server
                    .getPairingPublicKey(new String(clientPairingId, StandardCharsets.UTF_8));

            Encoder encoder = TypeLengthValue.getEncoder();

            if (clientLongtermPublicKey == null) {

                logger.info("Stage 2 : Unknown Pairing {}", new String(clientPairingId, StandardCharsets.UTF_8));

                encoder.add(Message.STATE, (short) 4);
                encoder.add(Message.ERROR, Error.AUTHENTICATION);

                logger.info("Stage 2 : End");
                response.setContentType("application/pairing+tlv8");
                response.setContentLengthLong(encoder.toByteArray().length);
                response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
                response.setStatus(HttpServletResponse.SC_OK);
                response.getOutputStream().write(encoder.toByteArray());
                response.getOutputStream().flush();
                logger.info("Stage 2 : Flushed");
            } else {
                logger.info("Stage 2 : Client Long Term Public Key is {}", byteToHexString(clientLongtermPublicKey));

                try {
                    if (new EdsaVerifier(clientLongtermPublicKey).verify(clientDeviceInfo, clientSignature)) {

                        logger.info("Stage 2 : Completed pair verification");

                        session.setAttribute("Control-Write-Encryption-Key",
                                createKey("Control-Write-Encryption-Key", sharedSecret));
                        session.setAttribute("Control-Read-Encryption-Key",
                                createKey("Control-Read-Encryption-Key", sharedSecret));

                        // HttpConnection http = (HttpConnection) request
                        // .getAttribute("org.eclipse.jetty.server.HttpConnection");
                        // EndPoint endp = http.getEndPoint();
                        // Connector connector = http.getConnector();
                        // Executor executor = connector.getExecutor();
                        // ByteBufferPool bufferPool = connector.getByteBufferPool();
                        //
                        // SecureHomekitHttpConnection newConnection = new SecureHomekitHttpConnection(bufferPool,
                        // executor, endp, (byte[]) session.getAttribute("Control-Read-Encryption-Key"),
                        // (byte[]) session.getAttribute("Control-Write-Encryption-Key"), false, false);
                        //
                        // response.setStatus(HttpServletResponse.SC_SWITCHING_PROTOCOLS);

                        encoder.add(Message.STATE, (short) 0x04);

                        logger.info("Stage 2 : End");
                        response.setContentType("application/pairing+tlv8");
                        response.setContentLengthLong(encoder.toByteArray().length);
                        response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.getOutputStream().write(encoder.toByteArray());
                        response.getOutputStream().flush();
                        logger.info("Stage 2 : Flushed");

                        // Upgrade the connection

                        // ConnectionFactory next = connector.getConnectionFactory(_nextProtocol);
                        // EndPoint decryptedEndPoint = sslConnection.getDecryptedEndPoint();
                        // Connection connection = next.newConnection(connector, decryptedEndPoint);
                        // decryptedEndPoint.setConnection(connection);

                        // TODO : Reset the notifications for all the characteristics of this accessory

                    } else {
                        logger.warn("Stage 2 : Invalid signature");
                        encoder.add(Message.STATE, (short) 0x04);
                        encoder.add(Message.ERROR, Error.AUTHENTICATION);

                        logger.info("Stage 2 : End");
                        response.setContentType("application/pairing+tlv8");
                        response.setContentLengthLong(encoder.toByteArray().length);
                        response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.getOutputStream().write(encoder.toByteArray());
                        response.getOutputStream().flush();
                        logger.info("Stage 2 : Flushed");
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
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
