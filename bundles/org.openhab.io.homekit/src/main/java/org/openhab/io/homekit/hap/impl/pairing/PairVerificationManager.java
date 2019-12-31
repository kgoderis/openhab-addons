package org.openhab.io.homekit.hap.impl.pairing;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.openhab.io.homekit.crypto.HomekitEncryptionEngine;
import org.openhab.io.homekit.hap.HomekitAuthInfo;
import org.openhab.io.homekit.hap.impl.HomekitRegistry;
import org.openhab.io.homekit.hap.impl.crypto.ChachaDecoder;
import org.openhab.io.homekit.hap.impl.crypto.ChachaEncoder;
import org.openhab.io.homekit.hap.impl.crypto.EdsaSigner;
import org.openhab.io.homekit.hap.impl.crypto.EdsaVerifier;
import org.openhab.io.homekit.hap.impl.http.HttpRequest;
import org.openhab.io.homekit.hap.impl.http.HttpResponse;
import org.openhab.io.homekit.hap.impl.pairing.PairVerificationRequest.Stage1Request;
import org.openhab.io.homekit.hap.impl.pairing.PairVerificationRequest.Stage2Request;
import org.openhab.io.homekit.hap.impl.responses.NotFoundResponse;
import org.openhab.io.homekit.hap.impl.responses.OkResponse;
import org.openhab.io.homekit.util.Message;
import org.openhab.io.homekit.util.TypeLengthValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import djb.Curve25519;

public class PairVerificationManager {

    private static final Logger logger = LoggerFactory.getLogger(PairVerificationManager.class);
    private static volatile SecureRandom secureRandom;

    private final HomekitAuthInfo authInfo;
    private final HomekitRegistry registry;

    private byte[] sessionKey;
    private byte[] clientPublicKey;
    private byte[] accessoryPublicKey;
    private byte[] sharedSecret;

    public PairVerificationManager(HomekitAuthInfo authInfo, HomekitRegistry registry) {
        this.authInfo = authInfo;
        this.registry = registry;
    }

    public HttpResponse handle(HttpRequest rawRequest) throws Exception {
        PairVerificationRequest request = PairVerificationRequest.of(rawRequest.getBody());
        switch (request.getStage()) {
            case ONE:
                return stage1((Stage1Request) request);

            case TWO:
                return stage2((Stage2Request) request);

            default:
                return new NotFoundResponse();
        }
    }

    private HttpResponse stage1(Stage1Request request) throws Exception {
        logger.info("Stage 1 : Start");
        logger.debug("Starting pair verification for " + registry.getLabel());
        clientPublicKey = request.getClientPublicKey();
        logger.info("Stage 1 : Client Public Key is {}", byteToHexString(clientPublicKey));

        accessoryPublicKey = new byte[32];
        byte[] accessoryPrivateKey = new byte[32];
        HomekitEncryptionEngine.getSecureRandom().nextBytes(accessoryPrivateKey);
        Curve25519.keygen(accessoryPublicKey, null, accessoryPrivateKey);
        logger.info("Stage 1 : Accessory Public Key is {}", byteToHexString(accessoryPublicKey));
        logger.info("Stage 1 : Accessory Private Key is {}", byteToHexString(accessoryPrivateKey));

        sharedSecret = new byte[32];
        Curve25519.curve(sharedSecret, accessoryPrivateKey, clientPublicKey);
        logger.info("Stage 1 : Shared Secret is {}", byteToHexString(sharedSecret));

        byte[] accessoryInfo = ByteUtils.joinBytes(accessoryPublicKey,
                authInfo.getMac().getBytes(StandardCharsets.UTF_8), clientPublicKey);
        logger.info("Stage 1 : Accessory Pairing Id is {}", authInfo.getMac());

        logger.info("Stage 1 : Accessory Info is {}", byteToHexString(accessoryInfo));

        logger.info("Stage 1 : Accessory Private Key is {}", byteToHexString(authInfo.getPrivateKey()));

        byte[] accessorySignature = new EdsaSigner(authInfo.getPrivateKey()).sign(accessoryInfo);

        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Verify-Encrypt-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Verify-Encrypt-Info".getBytes(StandardCharsets.UTF_8)));
        sessionKey = new byte[32];
        hkdf.generateBytes(sessionKey, 0, 32);
        logger.info("Stage 1 : Session Key is {}", byteToHexString(sessionKey));

        org.openhab.io.homekit.util.TypeLengthValue.Encoder encoder = TypeLengthValue.getEncoder();
        encoder.add(Message.IDENTIFIER, authInfo.getMac().getBytes(StandardCharsets.UTF_8));
        encoder.add(Message.SIGNATURE, accessorySignature);
        byte[] plaintext = encoder.toByteArray();

        ChachaEncoder chacha = new ChachaEncoder(sessionKey, "PV-Msg02".getBytes(StandardCharsets.UTF_8));
        byte[] ciphertext = chacha.encodeCiphertext(plaintext);

        encoder = TypeLengthValue.getEncoder();
        encoder.add(Message.STATE, (short) 2);
        encoder.add(Message.ENCRYPTED_DATA, ciphertext);
        encoder.add(Message.PUBLIC_KEY, accessoryPublicKey);
        return new PairingResponse(encoder.toByteArray());
    }

    private HttpResponse stage2(Stage2Request request) throws Exception {
        logger.info("Stage 2 : Start");

        ChachaDecoder chacha = new ChachaDecoder(sessionKey, "PV-Msg03".getBytes(StandardCharsets.UTF_8));
        byte[] plaintext = chacha.decodeCiphertext(request.getAuthTagData(), request.getMessageData());

        org.openhab.io.homekit.util.TypeLengthValue.DecodeResult d = TypeLengthValue.decode(plaintext);
        byte[] clientPairingId = d.getBytes(Message.IDENTIFIER);
        logger.info("Stage 2 : Client Pairing Id is {}", byteToHexString(clientPairingId));

        byte[] clientSignature = d.getBytes(Message.SIGNATURE);
        logger.info("Stage 2 : Client Signature is {}", byteToHexString(clientSignature));

        byte[] clientDeviceInfo = ByteUtils.joinBytes(clientPublicKey, clientPairingId, accessoryPublicKey);

        byte[] clientLongtermPublicKey = authInfo
                .getUserPublicKey(authInfo.getMac() + new String(clientPairingId, StandardCharsets.UTF_8));
        if (clientLongtermPublicKey == null) {
            throw new Exception("Unknown user: " + new String(clientPairingId, StandardCharsets.UTF_8));
        }

        logger.info("Stage 2 : Client Long Term Public Key is {}", byteToHexString(clientLongtermPublicKey));

        org.openhab.io.homekit.util.TypeLengthValue.Encoder encoder = TypeLengthValue.getEncoder();
        if (new EdsaVerifier(clientLongtermPublicKey).verify(clientDeviceInfo, clientSignature)) {
            encoder.add(Message.STATE, (short) 4);
            logger.debug("Completed pair verification for " + registry.getLabel());
            return new UpgradeResponse(encoder.toByteArray(),
                    HomekitEncryptionEngine.createKey("Control-Write-Encryption-Key", sharedSecret),
                    HomekitEncryptionEngine.createKey("Control-Read-Encryption-Key", sharedSecret));
        } else {
            encoder.add(Message.ERROR, (short) 4);
            logger.warn("Invalid signature. Could not pair " + registry.getLabel());
            return new OkResponse(encoder.toByteArray());
        }
    }

    // private byte[] createKey(String info) {
    // HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
    // hkdf.init(new HKDFParameters(sharedSecret, "Control-Salt".getBytes(StandardCharsets.UTF_8),
    // info.getBytes(StandardCharsets.UTF_8)));
    // byte[] key = new byte[32];
    // hkdf.generateBytes(key, 0, 32);
    // return key;
    // }
    //
    // private static SecureRandom getSecureRandom() {
    // if (secureRandom == null) {
    // synchronized (PairVerificationManager.class) {
    // if (secureRandom == null) {
    // secureRandom = new SecureRandom();
    // }
    // }
    // }
    // return secureRandom;
    // }

    protected static String byteToHexString(byte[] input) {
        StringBuilder sb = new StringBuilder();
        for (byte b : input) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }
}
