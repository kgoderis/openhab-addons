package org.openhab.io.homekit.internal.client;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.ProtocolHandlers;
import org.eclipse.jetty.client.api.Destination;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.api.AccessoryRegistry;
import org.openhab.io.homekit.api.NotificationRegistry;
import org.openhab.io.homekit.api.Pairing;
import org.openhab.io.homekit.api.PairingRegistry;
import org.openhab.io.homekit.crypto.ChachaDecoder;
import org.openhab.io.homekit.crypto.ChachaEncoder;
import org.openhab.io.homekit.crypto.EdsaSigner;
import org.openhab.io.homekit.crypto.EdsaVerifier;
import org.openhab.io.homekit.crypto.HomekitEncryptionEngine;
import org.openhab.io.homekit.internal.accessory.GenericAccessory;
import org.openhab.io.homekit.internal.http.jetty.HomekitHttpClientTransportOverHTTP;
import org.openhab.io.homekit.internal.http.jetty.HomekitHttpDestinationOverHTTP;
import org.openhab.io.homekit.internal.http.jetty.HomekitProtocolHandler;
import org.openhab.io.homekit.internal.server.AbstractAccessoryServer;
import org.openhab.io.homekit.internal.server.AccessoryServerUID;
import org.openhab.io.homekit.util.Byte;
import org.openhab.io.homekit.util.Error;
import org.openhab.io.homekit.util.Message;
import org.openhab.io.homekit.util.Method;
import org.openhab.io.homekit.util.TypeLengthValue;
import org.openhab.io.homekit.util.TypeLengthValue.DecodeResult;
import org.openhab.io.homekit.util.TypeLengthValue.Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.srp6.SRP6ClientCredentials;
import com.nimbusds.srp6.SRP6Exception;
import com.nimbusds.srp6.XRoutineWithUserIdentity;

import djb.Curve25519;

public class RemoteAccessoryServer extends AbstractAccessoryServer {

    protected static final Logger logger = LoggerFactory.getLogger(RemoteAccessoryServer.class);

    private static final String HTTP_SCHEME = "http";

    // private static volatile SecureRandom secureRandom = new SecureRandom();
    private HomekitClientSRP6Session SRP6Session;

    // private byte[] getPairingId();
    // private byte[] secretKey;

    private byte[] sessionKey;
    private byte[] sharedSecret;
    // private byte[] destinationPairingIdentifier;
    // private byte[] destinationPublicKey;
    private byte[] clientPublicKey;
    private byte[] clientPrivateKey;

    // private InetAddress address;
    // private int port;

    private @Nullable HttpClient httpClient;
    // private String setupCode;
    private boolean isPairVerified;

    // private final PairingRegistry pairingRegistry;

    public RemoteAccessoryServer(InetAddress address, int port, byte[] pairingIdentifier, byte[] secretKey,
            AccessoryRegistry accessoryRegistry, PairingRegistry pairingRegistry,
            NotificationRegistry notificationRegistry) {
        super(address, port, pairingIdentifier, secretKey, accessoryRegistry, pairingRegistry, notificationRegistry);
        // this.address = address;
        // this.port = port;
        // this.getPairingId() = getPairingId();
        // this.secretKey = secretKey;
        // this.destinationPairingIdentifier = accessoryPairingId;
        this.setupCode = null;
        this.isPairVerified = false;
        // this.pairingRegistry = pairingRegistry;

        this.httpClient = new HttpClient(new HomekitHttpClientTransportOverHTTP(), null);

        // TODO : Detect when the remote end closes the connection -> Thing should go offline

        try {
            httpClient.start();
            ProtocolHandlers handlers = httpClient.getProtocolHandlers();
            handlers.clear();
            handlers.put(new HomekitProtocolHandler(this));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void dispose() {
        try {
            httpClient.stop();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public RemoteAccessoryServer(InetAddress address, int port, AccessoryRegistry accessoryRegistry,
            PairingRegistry pairingRegistry, NotificationRegistry notificationRegistry) {
        this(address, port, generatePairingId(), generateSecretKey(), accessoryRegistry, pairingRegistry,
                notificationRegistry);
    }

    @Override
    public @NonNull AccessoryServerUID getUID() {
        return new AccessoryServerUID("Remote", getId());
    }

    // @Override
    // public boolean isPaired() {
    // if (getPairingId() != null && destinationPairingIdentifier != null) {
    // PairingUID pairingUID = new PairingUID(getPairingId(), destinationPairingIdentifier);
    // return pairingRegistry.get(pairingUID) != null ? true : false;
    // }
    //
    // return false;
    // }

    public boolean isPairVerified() {
        return isPairVerified;
    }

    // @Override
    // public byte[] getPairingId() {
    // return getPairingId();
    // }

    // @Override
    // public byte[] getSecretKey() {
    // return secretKey;
    // }
    //
    // public byte[] getDestinationPairingId() {
    // return destinationPairingIdentifier;
    // }

    // public byte[] getDestinationPublicKey() {
    // return destinationPublicKey;
    // }

    public boolean isSecure() {
        if (httpClient != null && address != null && port != 0) {
            Destination destination = httpClient.getDestination(HTTP_SCHEME, address.getHostAddress(), port);

            if (destination instanceof HomekitHttpDestinationOverHTTP) {
                return ((HomekitHttpDestinationOverHTTP) destination).hasEncryptionKeys();
            }
        }

        return false;
    }

    public void pairSetup() throws IOException {

        logger.info("'{}' : Pair setup", new String(getPairingId()));

        if (setupCode == null) {
            logger.warn("'{}' : Unable to pair with {}:{} because no setup code is set", new String(getPairingId()),
                    address.getHostAddress(), port);
            return;
        }

        sessionKey = null;
        sharedSecret = null;
        // destinationPairingIdentifier = null;
        // destinationPublicKey = null;
        clientPublicKey = null;
        clientPrivateKey = null;

        isPairVerified = false;

        StageResult stageResult = null;
        int failedStage = 0;

        try {
            byte[] payload = doPairSetupStage0();
            Future<StageResult> stageFuture = sendPairSetupStage(payload);
            stageResult = stageFuture.get();
        } catch (InterruptedException | ExecutionException e1) {
            e1.printStackTrace();
            return;
        } catch (HomekitException e) {
            return;
        }

        if (!stageResult.isFailure()) {
            try {
                byte[] payload = doPairSetupStage1(stageResult);
                Future<StageResult> stageFuture = sendPairSetupStage(payload);
                stageResult = stageFuture.get();
            } catch (InterruptedException | ExecutionException e1) {
                e1.printStackTrace();
                return;
            } catch (HomekitException e) {
                return;
            }

            if (!stageResult.isFailure()) {
                try {
                    byte[] payload = doPairSetupStage2(stageResult);
                    Future<StageResult> stageFuture = sendPairSetupStage(payload);
                    stageResult = stageFuture.get();
                } catch (InterruptedException | ExecutionException e1) {
                    e1.printStackTrace();
                    return;
                } catch (HomekitException e) {
                    return;
                }

                if (!stageResult.isFailure()) {
                    try {
                        byte[] payload = doPairSetupStage3(stageResult);
                    } catch (HomekitException e) {
                        return;
                    }
                } else {
                    failedStage = 2;
                }
            } else {
                failedStage = 1;
            }
        } else {
            failedStage = 0;
        }

        if (stageResult.isFailure()) {
            if (stageResult.error != null) {
                switch (stageResult.error) {
                    case UNAVAILABLE: {
                        logger.warn(
                                "'{}' : Pair setup failed because the Homekit Accessory is not available for pairing, e.g. it is already paired to another Homekit Controller",
                                new String(getPairingId()));
                        break;
                    }
                    default: {
                        logger.warn(
                                "'{}' : Pair setup failed because of a Homekit Automation Protocol Error at stage {} : {}",
                                new String(getPairingId()), failedStage, stageResult.error.toString());
                    }
                }
            } else {
                logger.warn("'{}' : Pair setup failed at Stage {} : {}", new String(getPairingId()), failedStage,
                        stageResult.message);
            }
        }

    }

    public boolean pairVerify() {

        logger.info("'{}' : Pair verify", new String(getPairingId()));

        sessionKey = null;
        sharedSecret = null;
        // destinationPublicKey = null;
        clientPublicKey = null;
        clientPrivateKey = null;

        isPairVerified = false;

        StageResult stageResult = null;
        int failedStage = 0;

        try {
            byte[] payload = doPairVerifyStage0();
            Future<StageResult> stageFuture = sendPairVerifyStage(payload);
            stageResult = stageFuture.get();
        } catch (InterruptedException | ExecutionException | IOException e) {
            e.printStackTrace();
            return false;
        } catch (HomekitException e) {
            return false;
        }

        if (!stageResult.isFailure()) {
            try {
                byte[] payload = doPairVerifyStage1(stageResult);
                Future<StageResult> stageFuture = sendPairVerifyStage(payload);
                stageResult = stageFuture.get();
            } catch (InterruptedException | ExecutionException | IOException e) {
                e.printStackTrace();
                return false;
            } catch (HomekitException e) {
                Encoder encoder = TypeLengthValue.getEncoder();
                encoder.add(Message.STATE, (short) 0x03);
                encoder.add(Message.ERROR, Error.AUTHENTICATION);
                Future<StageResult> stageFuture;
                try {
                    stageFuture = sendPairVerifyStage(encoder.toByteArray());
                    stageResult = stageFuture.get();
                } catch (InterruptedException | ExecutionException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                return false;
            }

            if (!stageResult.isFailure()) {
                try {
                    byte[] payload = doPairVerifyStage2(stageResult);
                    isPairVerified = true;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                } catch (HomekitException e) {
                    return false;
                }
            } else {
                failedStage = 1;
            }
        } else {
            failedStage = 0;
        }

        if (stageResult.isFailure()) {
            if (stageResult.error != null) {
                switch (stageResult.error) {
                    case UNAVAILABLE: {
                        logger.warn(
                                "'{}' : Pair verify failed because the Homekit Accessory is not available for pairing, e.g. it is already paired to another Homekit Controller");
                    }
                    default: {
                        logger.warn(
                                "'{}' : Pair verify failed because of a Homekit Automation Protocol Error at stage {} : {}",
                                failedStage, new String(getPairingId()), stageResult.error.toString());
                    }
                }
            } else {
                logger.warn("'{}' : Pair setup failed at Stage {} : {}", failedStage, new String(getPairingId()),
                        stageResult.message);
            }
        }

        return !stageResult.isFailure();
    }

    public void pairRemove() throws HomekitException, IOException {
        if (isPaired()) {
            if (isPairVerified() && isSecure()) {
                Encoder encoder = TypeLengthValue.getEncoder();
                encoder.add(Message.STATE, (short) 0x01);
                encoder.add(Message.METHOD, Method.REMOVE_PAIRING.getKey());
                try {
                    encoder.add(Message.IDENTIFIER, getPairingId());
                } catch (IOException e) {
                    throw e;
                }

                Future<StageResult> stageFuture;
                StageResult stageResult = null;
                try {
                    stageFuture = sendPairing(encoder.toByteArray());
                    stageResult = stageFuture.get();
                } catch (InterruptedException | ExecutionException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

                short state = stageResult.decodeResult.getByte(Message.STATE);
                if (state != 2) {
                    throw new HomekitException("Wrong STATE");
                }

                if (stageResult.decodeResult.getBytes(Message.ERROR) != null) {
                    logger.warn("'{}' : The Homekit Accessory failed to remove the pairing : {}",
                            new String(getPairingId()),
                            Error.get(stageResult.decodeResult.getByte(Message.ERROR)).toString());
                } else {
                    logger.warn("'{}' : The Homekit Accessory removed the pairing", new String(getPairingId()));

                    isPairVerified = false;
                }
            } else {
                logger.warn(
                        "'{}' : The Homekit Accessory pairing can not be removed because it is {} paired verified and the connection is {}secured",
                        new String(getPairingId()), isPairVerified() ? "already" : "not", isSecure() ? "" : "not ");
            }

            if (isPaired()) {
                removePairing(destinationPairingIdentifier);
            } else {
                logger.warn("'{}' : The pairing identifier for the Homekit Accessory is not set",
                        new String(getPairingId()));
            }
        }

    }

    protected byte[] doPairSetupStage0() throws IOException, HomekitException {
        Encoder encoder = TypeLengthValue.getEncoder();
        encoder.add(Message.STATE, (short) 0x01);
        encoder.add(Message.METHOD, Method.PAIR_SETUP_WITH_AUTH.getKey());

        return encoder.toByteArray();
    }

    protected byte[] doPairSetupStage1(StageResult stageResult) throws IOException, HomekitException {
        logger.info("'{}' : Setup Stage 1 : Start", new String(getPairingId()));

        short state = stageResult.decodeResult.getByte(Message.STATE);
        if (state != 2) {
            throw new HomekitException("Wrong STATE");
        }

        BigInteger publicKey = stageResult.decodeResult.getBigInt(Message.PUBLIC_KEY);
        logger.info("'{}' : Setup Stage 1 : Public Key is {}", new String(getPairingId()),
                Byte.toHexString(Byte.toByteArray(publicKey)));

        BigInteger salt = stageResult.decodeResult.getBigInt(Message.SALT);
        logger.info("'{}' : Setup Stage 1 : Salt is {}", new String(getPairingId()),
                Byte.toHexString(Byte.toByteArray(salt)));

        if (SRP6Session == null) {
            SRP6Session = new HomekitClientSRP6Session();
            SRP6Session.setClientEvidenceRoutine(new HomekitEncryptionEngine.ClientEvidenceRoutineImpl());
            SRP6Session.setServerEvidenceRoutine(new HomekitEncryptionEngine.ServerEvidenceRoutineImpl());
            SRP6Session.setXRoutine(new XRoutineWithUserIdentity());
        }

        SRP6Session.step1("Pair-Setup", setupCode);

        SRP6ClientCredentials clientCredentials = null;
        try {
            clientCredentials = SRP6Session.step2(HomekitEncryptionEngine.SRP6Params, salt, publicKey);
        } catch (SRP6Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        BigInteger clientPublicKey = clientCredentials.A;
        logger.info("'{}' : Setup Stage 1 : Client Public Key is {}", new String(getPairingId()),
                Byte.toHexString(Byte.toByteArray(clientPublicKey)));

        BigInteger clientProof = clientCredentials.M1;
        logger.info("'{}' : Setup Stage 1 : Client Proof is {}", new String(getPairingId()),
                Byte.toHexString(Byte.toByteArray(clientProof)));

        Encoder encoder = TypeLengthValue.getEncoder();
        encoder.add(Message.STATE, (short) 0x03);
        encoder.add(Message.PUBLIC_KEY, clientPublicKey);
        encoder.add(Message.PROOF, clientProof);
        logger.info("'{}' : Setup Stage 1 : End");

        return encoder.toByteArray();
    }

    protected byte[] doPairSetupStage2(StageResult stageResult) throws IOException, HomekitException {
        logger.info("'{}' : Setup Stage 2 : Start", new String(getPairingId()));

        short state = stageResult.decodeResult.getByte(Message.STATE);
        if (state != 4) {
            throw new HomekitException("Wrong STATE");
        }

        BigInteger proof = stageResult.decodeResult.getBigInt(Message.PROOF);

        try {
            SRP6Session.step3(proof);
        } catch (SRP6Exception e) {
            // Verification failed
        }

        MessageDigest digest = SRP6Session.getCryptoParams().getMessageDigestInstance();
        BigInteger S = SRP6Session.getSessionKey(false);
        byte[] sBytes = Byte.toByteArray(S);
        logger.info("'{}' : Setup Stage 2 : SRP Session Key is {}", new String(getPairingId()),
                Byte.toHexString(sBytes));
        sharedSecret = digest.digest(sBytes);
        logger.info("'{}' : Setup Stage 2 : Shared Secret is {}", new String(getPairingId()),
                Byte.toHexString(sharedSecret));

        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Setup-Encrypt-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-Encrypt-Info".getBytes(StandardCharsets.UTF_8)));
        sessionKey = new byte[32];
        hkdf.generateBytes(sessionKey, 0, 32);
        logger.info("'{}' : Setup Stage 2 : Session Key is {}", new String(getPairingId()),
                Byte.toHexString(sessionKey));

        hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Setup-Controller-Sign-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-Controller-Sign-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] clientDeviceX = new byte[32];
        hkdf.generateBytes(clientDeviceX, 0, 32);
        logger.info("'{}' : Setup Stage 2 : Client Device X is {}", new String(getPairingId()),
                Byte.toHexString(clientDeviceX));

        EdsaSigner signer = new EdsaSigner(secretKey);
        byte[] clientLongtermPublicKey = signer.getPublicKey();
        byte[] clientDeviceInfo = Byte.joinBytes(clientDeviceX, getPairingId(), clientLongtermPublicKey);
        byte[] clientSignature = null;
        try {
            clientSignature = signer.sign(clientDeviceInfo);
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
        logger.info("'{}' : Setup Stage 2 : Client Long Term Pubic Key is {}", new String(getPairingId()),
                Byte.toHexString(clientLongtermPublicKey));
        logger.info("'{}' : Setup Stage 2 : Client Signature X is {}", new String(getPairingId()),
                Byte.toHexString(clientSignature));

        Encoder encoder = TypeLengthValue.getEncoder();
        encoder.add(Message.IDENTIFIER, getPairingId());
        encoder.add(Message.PUBLIC_KEY, clientLongtermPublicKey);
        encoder.add(Message.SIGNATURE, clientSignature);

        ChachaEncoder chachaEncoder = new ChachaEncoder(sessionKey, "PS-Msg05".getBytes(StandardCharsets.UTF_8));
        byte[] ciphertext = chachaEncoder.encodeCiphertext(encoder.toByteArray());

        encoder = TypeLengthValue.getEncoder();
        encoder.add(Message.STATE, (short) 0x05);
        encoder.add(Message.ENCRYPTED_DATA, ciphertext);
        logger.info("'{}' : Setup Stage 2 : End", new String(getPairingId()));

        return encoder.toByteArray();
    }

    protected byte[] doPairSetupStage3(StageResult stageResult) throws IOException, HomekitException {
        logger.info("'{}' : Setup Stage 3 : Start", new String(getPairingId()));

        short state = stageResult.decodeResult.getByte(Message.STATE);
        if (state != 6) {
            throw new HomekitException("Wrong STATE");
        }

        byte[] messageData = new byte[stageResult.decodeResult.getLength(Message.ENCRYPTED_DATA) - 16];
        stageResult.decodeResult.getBytes(Message.ENCRYPTED_DATA, messageData, 0);
        byte[] authTagData = new byte[16];
        stageResult.decodeResult.getBytes(Message.ENCRYPTED_DATA, authTagData, messageData.length);

        ChachaDecoder chachaDecoder = new ChachaDecoder(sessionKey, "PS-Msg06".getBytes(StandardCharsets.UTF_8));
        byte[] plaintext = chachaDecoder.decodeCiphertext(authTagData, messageData);
        logger.info("'{}' : Setup Stage 3 : Plaintext is {}", new String(getPairingId()), Byte.toHexString(plaintext));

        DecodeResult d = TypeLengthValue.decode(plaintext);
        byte[] destinationPairingIdentifier = d.getBytes(Message.IDENTIFIER);
        logger.info("'{}' : Setup Stage 3 : Accessory Pairing Id is {}", new String(getPairingId()),
                Byte.toHexString(destinationPairingIdentifier));
        byte[] destinationPublicKey = d.getBytes(Message.PUBLIC_KEY);
        logger.info("'{}' : Setup Stage 3 : Accessory Long Term Public Key is {}", new String(getPairingId()),
                Byte.toHexString(destinationPublicKey));
        byte[] accessorySignature = d.getBytes(Message.SIGNATURE);
        logger.info("'{}' : Setup Stage 3 : Accessory Signature is {}", new String(getPairingId()),
                Byte.toHexString(accessorySignature));

        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Setup-Accessory-Sign-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-Accessory-Sign-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] accessoryDeviceX = new byte[32];
        hkdf.generateBytes(accessoryDeviceX, 0, 32);

        byte[] accessoryDeviceInfo = Byte.joinBytes(accessoryDeviceX, destinationPairingIdentifier,
                destinationPublicKey);
        logger.info("'{}' : Setup Stage 3 : Accessory Device Info is {}", new String(getPairingId()),
                Byte.toHexString(accessoryDeviceInfo));

        try {
            if (!new EdsaVerifier(destinationPublicKey).verify(accessoryDeviceInfo, accessorySignature)) {
                throw new HomekitException("Signature verification failed");
            }
        } catch (Exception e1) {
            throw new HomekitException("Signature verification failed");
        }

        addPairing(destinationPairingIdentifier, destinationPublicKey);
        SRP6Session = null;

        logger.info("'{}' : Setup Stage 3 : End", new String(getPairingId()));

        return null;
    }

    protected byte[] doPairVerifyStage0() throws IOException, HomekitException {
        logger.info("'{}' : Verify Stage 0 : Start", new String(getPairingId()));

        clientPublicKey = new byte[32];
        clientPrivateKey = new byte[32];
        HomekitEncryptionEngine.getSecureRandom().nextBytes(clientPrivateKey);
        Curve25519.keygen(clientPublicKey, null, clientPrivateKey);

        Encoder encoder = TypeLengthValue.getEncoder();
        encoder.add(Message.STATE, (short) 0x01);
        encoder.add(Message.PUBLIC_KEY, clientPublicKey);

        logger.info("'{}' : Verify Stage 0 : End", new String(getPairingId()));

        return encoder.toByteArray();
    }

    protected byte[] doPairVerifyStage1(StageResult stageResult) throws IOException, HomekitException {
        logger.info("'{}' : Verify Stage 1 : Start", new String(getPairingId()));

        short state = stageResult.decodeResult.getByte(Message.STATE);
        if (state != 2) {
            throw new HomekitException("Wrong STATE");
        }

        byte[] destinationPublicKey = stageResult.decodeResult.getBytes(Message.PUBLIC_KEY);
        logger.info("'{}' : Verify Stage 1 : Accessory Public Key is {}", new String(getPairingId()),
                Byte.toHexString(destinationPublicKey));

        byte[] messageData = new byte[stageResult.decodeResult.getLength(Message.ENCRYPTED_DATA) - 16];
        stageResult.decodeResult.getBytes(Message.ENCRYPTED_DATA, messageData, 0);
        byte[] authTagData = new byte[16];
        stageResult.decodeResult.getBytes(Message.ENCRYPTED_DATA, authTagData, messageData.length);

        sharedSecret = new byte[32];
        Curve25519.curve(sharedSecret, clientPrivateKey, destinationPublicKey);
        logger.info("'{}' : Verify Stage 1 : Shared Secret is {}", new String(getPairingId()),
                Byte.toHexString(sharedSecret));

        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Verify-Encrypt-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Verify-Encrypt-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] sessionKey = new byte[32];
        hkdf.generateBytes(sessionKey, 0, 32);
        logger.info("'{}' : Verify Stage 1 : Session Key is {}", new String(getPairingId()),
                Byte.toHexString(sessionKey));

        byte[] plaintext = null;
        ChachaDecoder chachaDecoder = new ChachaDecoder(sessionKey, "PV-Msg02".getBytes(StandardCharsets.UTF_8));
        try {
            plaintext = chachaDecoder.decodeCiphertext(authTagData, messageData);
            logger.info("'{}' : Verify Stage 1 : Plaintext is {}", new String(getPairingId()),
                    Byte.toHexString(plaintext));
        } catch (Exception e) {
            logger.warn("'{}' : Verify Stage 1 : Unable to decode the ciphertext");
            e.printStackTrace();
            throw new HomekitException("Ciphertext decoding failed");
        }

        Pairing accessoryPairing = null;
        byte[] accessorySignature = null;

        DecodeResult d = TypeLengthValue.decode(plaintext);
        byte[] destinationPairingIdentifier = d.getBytes(Message.IDENTIFIER);
        // byte[] remoteAccessoryPairingIdentifier = d.getBytes(Message.IDENTIFIER);
        // if (isPaired()) {
        // if (!Arrays.equals(remoteAccessoryPairingIdentifier, destinationPairingIdentifier)) {
        // logger.warn(
        // "'{}' : Verify Stage 1 : The Accessory reports an pairing identifier that differs from what we have",
        // new String(getPairingId()));
        // throw new HomekitException("Invalid pairing identifier");
        // }
        // } else {
        // destinationPairingIdentifier = remoteAccessoryPairingIdentifier;
        // }

        logger.info("'{}' : Verify Stage 1 : Accessory Pairing Id is {}", new String(getPairingId()),
                Byte.toHexString(destinationPairingIdentifier));
        accessorySignature = d.getBytes(Message.SIGNATURE);
        logger.info("'{}' : Verify Stage 1 : Accessory Signature is {}", new String(getPairingId()),
                Byte.toHexString(accessorySignature));

        // accessoryPairing = pairingRegistry.get(new PairingUID(getPairingId(), destinationPairingIdentifier));
        accessoryPairing = getPairing(destinationPairingIdentifier);

        if (accessoryPairing == null) {
            throw new HomekitException("Accessory is not paired");
        } else {
            logger.info("'{}' : Fetched the Pairing {} : {}", new String(getPairingId()), accessoryPairing.getUID(),
                    Byte.toHexString(accessoryPairing.getDestinationPublicKey()));
        }

        byte[] accessoryDeviceInfo = Byte.joinBytes(destinationPublicKey, destinationPairingIdentifier,
                clientPublicKey);

        try {
            boolean signatureVerification = new EdsaVerifier(accessoryPairing.getDestinationPublicKey())
                    .verify(accessoryDeviceInfo, accessorySignature);
            if (!signatureVerification) {
                throw new HomekitException("Signature verification failed");
            }
        } catch (Exception e) {
            throw new HomekitException("Signature verification failed");
        }

        byte[] clientDeviceInfo = Byte.joinBytes(clientPublicKey, getPairingId(), destinationPublicKey);

        byte[] clientSignature = null;
        try {
            logger.info("'{}' : Verify Stage 1 : Client Long Term Secret Key is {}", new String(getPairingId()),
                    Byte.toHexString(secretKey));
            clientSignature = new EdsaSigner(secretKey).sign(clientDeviceInfo);
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
        logger.info("'{}' : Verify Stage 1 : Client Pairing Id is {}", new String(getPairingId()),
                Byte.toHexString(getPairingId()));
        encoder.add(Message.IDENTIFIER, getPairingId());
        logger.info("'{}' : Verify Stage 1 : Client Signature is {}", new String(getPairingId()),
                Byte.toHexString(clientSignature));
        encoder.add(Message.SIGNATURE, clientSignature);
        plaintext = encoder.toByteArray();

        ChachaEncoder chacha = new ChachaEncoder(sessionKey, "PV-Msg03".getBytes(StandardCharsets.UTF_8));
        byte[] ciphertext = chacha.encodeCiphertext(plaintext);

        encoder = TypeLengthValue.getEncoder();
        encoder.add(Message.STATE, (short) 0x03);
        encoder.add(Message.ENCRYPTED_DATA, ciphertext);

        logger.info("'{}' : Verify Stage 1 : End", new String(getPairingId()));

        return encoder.toByteArray();
    }

    protected byte[] doPairVerifyStage2(StageResult stageResult) throws IOException, HomekitException {
        logger.info("'{}' : Verify Stage 2 : Start", new String(getPairingId()));

        short state = stageResult.decodeResult.getByte(Message.STATE);
        if (state != 4) {
            throw new HomekitException("Wrong STATE");
        }

        byte[] writeKey = HomekitEncryptionEngine.createKey("Control-Write-Encryption-Key", sharedSecret);
        logger.info("'{}' : Verify Stage 2 : Write Key is {}", new String(getPairingId()), Byte.toHexString(writeKey));

        byte[] readKey = HomekitEncryptionEngine.createKey("Control-Read-Encryption-Key", sharedSecret);
        logger.info("'{}' : Verify Stage 2 : Read Key is {}", new String(getPairingId()), Byte.toHexString(readKey));

        HomekitHttpDestinationOverHTTP destination = (HomekitHttpDestinationOverHTTP) httpClient.getDestination(
                stageResult.result.getRequest().getScheme(), stageResult.result.getRequest().getHost(),
                stageResult.result.getRequest().getPort());
        logger.info("'{}' : Verify Stage 2 : Setting the keys on destination ", new String(getPairingId()),
                destination.toString());
        destination.setEncryptionKeys(readKey, writeKey);
        // destination.secure();

        logger.info("'{}' : Verify Stage 2 : End", new String(getPairingId()));

        return null;
    }

    protected Future<StageResult> sendPairSetupStage(byte[] request) throws InterruptedException {
        return sendStage(request, "/pair-setup");
    }

    protected Future<StageResult> sendPairVerifyStage(byte[] request) throws InterruptedException {
        return sendStage(request, "/pair-verify");
    }

    protected Future<StageResult> sendPairing(byte[] request) throws InterruptedException {
        return sendStage(request, "/pairings");
    }

    protected Future<StageResult> sendStage(byte[] request, String url) throws InterruptedException {

        URI uri = null;
        try {
            uri = new URI("http", null, address.getHostAddress(), port, url, null, null);
        } catch (URISyntaxException e1) {
            e1.printStackTrace();
        }

        CompletableFuture<StageResult> completableFuture = new CompletableFuture<>();

        httpClient.newRequest(uri.toString()).method(HttpMethod.POST)
                .content(new BytesContentProvider(request), "application/pairing+tlv8")
                .header(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString())
                .send(new BufferingResponseListener(8 * 1024 * 1024) {
                    @Override
                    public void onComplete(Result result) {
                        if (!result.isFailed()) {
                            try {
                                byte[] body = getContent();

                                DecodeResult d = TypeLengthValue.decode(body);

                                if (d.getBytes(Message.ERROR) != null) {
                                    SRP6Session = null;
                                    StageResult stageResult = new StageResult(Error.get(d.getByte(Message.ERROR)));
                                    completableFuture.complete(stageResult);
                                    return;
                                }

                                short state = d.getByte(Message.STATE);
                                logger.info("'{}' : Received State {}", new String(getPairingId()), state);

                                StageResult stageResult = new StageResult(d, result);
                                completableFuture.complete(stageResult);
                            } catch (IOException e) {
                                SRP6Session = null;
                            }
                        } else {
                            StageResult stageResult = new StageResult(result.getResponseFailure().getMessage());
                            completableFuture.complete(stageResult);
                        }
                    }
                });

        return completableFuture;
    }

    protected Future<ContentResult> getContent(String url) throws InterruptedException {

        URI uri = null;
        try {
            uri = new URI("http", null, address.getHostAddress(), port, url, null, null);
        } catch (URISyntaxException e1) {
            e1.printStackTrace();
        }

        CompletableFuture<ContentResult> completableFuture = new CompletableFuture<>();

        httpClient.newRequest(uri.toString()).method(HttpMethod.GET)
                .send(new BufferingResponseListener(8 * 1024 * 1024) {
                    @Override
                    public void onComplete(Result result) {
                        if (!result.isFailed()) {
                            byte[] body = getContent();
                            ContentResult stageResult = new ContentResult(body, result);
                            completableFuture.complete(stageResult);
                        } else {
                            ContentResult stageResult = new ContentResult(
                                    result.getResponseFailure().getMessage().getBytes(), result);
                            completableFuture.complete(stageResult);
                        }
                    }
                });

        return completableFuture;
    }

    public void handleEvent(byte[] body) {

        try {
            Byte.logBuffer(logger, "handleEvent", Byte.toHexString(getPairingId()), ByteBuffer.wrap(body));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    protected class StageResult {
        public StageResult(DecodeResult decodeResult, Result result) {
            this.decodeResult = decodeResult;
            this.result = result;
        }

        public StageResult(String message) {
            this.message = message;
        }

        public StageResult(Error error) {
            this.error = error;
        }

        public boolean isFailure() {
            return message != null || error != null;
        }

        public DecodeResult decodeResult;
        public Result result;
        public String message;
        public Error error;
    }

    protected class ContentResult {
        public ContentResult(byte[] body, Result result) {
            this.body = body;
            this.result = result;
        }

        public ContentResult(String message) {
            this.message = message;
        }

        public byte[] body;
        public Result result;
        public String message;
    }

    @Override
    public Collection<Accessory> getAccessories() {
        Collection<Accessory> result = new HashSet<Accessory>();

        if (isPaired() && isPairVerified() && isSecure()) {

            Future<ContentResult> contentFuture;
            ContentResult contentResult = null;
            try {
                contentFuture = getContent("/accessories");
                contentResult = contentFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            logger.info("'{}' : Received {}", new String(getPairingId()), new String(contentResult.body));

            if (contentResult.result.getResponse().getStatus() == 200) {
                JsonArray accessories = Json.createReader(new ByteArrayInputStream(contentResult.body)).readObject()
                        .getJsonArray("accessories");
                for (JsonValue value : accessories) {
                    result.add(new GenericAccessory(value));
                }
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T> T fromJson(String json, Class<T> beanClass) {
        JsonValue value = Json.createReader(new StringReader(json)).read();
        return (T) decode(value, beanClass);
    }

    private static Object decode(JsonValue jsonValue, Type targetType) {
        if (jsonValue.getValueType() == ValueType.NULL) {
            return null;
        } else if (jsonValue.getValueType() == ValueType.TRUE || jsonValue.getValueType() == ValueType.FALSE) {
            return decodeBoolean(jsonValue, targetType);
        } else if (jsonValue instanceof JsonNumber) {
            return decodeNumber((JsonNumber) jsonValue, targetType);
        } else if (jsonValue instanceof JsonString) {
            return decodeString((JsonString) jsonValue, targetType);
        } else if (jsonValue instanceof JsonArray) {
            return decodeArray((JsonArray) jsonValue, targetType);
        } else if (jsonValue instanceof JsonObject) {
            return decodeObject((JsonObject) jsonValue, targetType);
        } else {
            throw new UnsupportedOperationException("Unsupported json value: " + jsonValue);
        }
    }

    private static Object decodeBoolean(JsonValue jsonValue, Type targetType) {
        if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.valueOf(jsonValue.toString());
        } else {
            throw new UnsupportedOperationException("Unsupported boolean type: " + targetType);
        }
    }

    private static Object decodeNumber(JsonNumber jsonNumber, Type targetType) {
        if (targetType == int.class || targetType == Integer.class) {
            return jsonNumber.intValue();
        } else if (targetType == long.class || targetType == Long.class) {
            return jsonNumber.longValue();
        } else {
            throw new UnsupportedOperationException("Unsupported number type: " + targetType);
        }
    }

    private static Object decodeString(JsonString jsonString, Type targetType) {
        if (targetType == String.class) {
            return jsonString.getString();
        } else if (targetType == Date.class) {
            try {
                return new SimpleDateFormat("MMM dd, yyyy H:mm:ss a", Locale.ENGLISH).parse(jsonString.getString());
            } catch (ParseException e) {
                throw new UnsupportedOperationException("Unsupported date format: " + jsonString.getString());
            }
        } else {
            throw new UnsupportedOperationException("Unsupported string type: " + targetType);
        }
    }

    private static Object decodeArray(JsonArray jsonArray, Type targetType) {
        Class<?> targetClass = (Class<?>) ((targetType instanceof ParameterizedType)
                ? ((ParameterizedType) targetType).getRawType()
                : targetType);

        if (List.class.isAssignableFrom(targetClass)) {
            Class<?> elementClass = (Class<?>) ((ParameterizedType) targetType).getActualTypeArguments()[0];
            List<Object> list = new ArrayList<>();

            for (JsonValue item : jsonArray) {
                list.add(decode(item, elementClass));
            }

            return list;
        } else if (targetClass.isArray()) {
            Class<?> elementClass = targetClass.getComponentType();
            Object array = Array.newInstance(elementClass, jsonArray.size());

            for (int i = 0; i < jsonArray.size(); i++) {
                Array.set(array, i, decode(jsonArray.get(i), elementClass));
            }

            return array;
        } else {
            throw new UnsupportedOperationException("Unsupported array type: " + targetClass);
        }
    }

    private static Object decodeObject(JsonObject object, Type targetType) {
        Class<?> targetClass = (Class<?>) ((targetType instanceof ParameterizedType)
                ? ((ParameterizedType) targetType).getRawType()
                : targetType);

        if (Map.class.isAssignableFrom(targetClass)) {
            Class<?> valueClass = (Class<?>) ((ParameterizedType) targetType).getActualTypeArguments()[1];
            Map<String, Object> map = new LinkedHashMap<>();

            for (Entry<String, JsonValue> entry : object.entrySet()) {
                map.put(entry.getKey(), decode(entry.getValue(), valueClass));
            }

            return map;
        } else {
            try {
                Constructor[] ctors = targetClass.getDeclaredConstructors();
                Constructor ctor = null;
                for (int i = 0; i < ctors.length; i++) {
                    ctor = ctors[i];
                    if (ctor.getGenericParameterTypes().length == 0) {
                        break;
                    }
                }

                // Object bean = targetClass.newInstance(); Constructor.newInstance(targetClass);
                Object bean = ctor.newInstance(targetClass);

                for (PropertyDescriptor property : Introspector.getBeanInfo(targetClass).getPropertyDescriptors()) {
                    if (property.getWriteMethod() != null && object.containsKey(property.getName())) {
                        property.getWriteMethod().invoke(bean, decode(object.get(property.getName()),
                                property.getWriteMethod().getGenericParameterTypes()[0]));
                    }
                }

                return bean;
            } catch (Exception e) {
                throw new UnsupportedOperationException("Unsupported object type: " + targetClass, e);
            }
        }
    }

}
