package org.openhab.io.homekit.internal.client;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.ProtocolHandlers;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.io.homekit.api.Pairing;
import org.openhab.io.homekit.api.PairingRegistry;
import org.openhab.io.homekit.crypto.ChachaDecoder;
import org.openhab.io.homekit.crypto.ChachaEncoder;
import org.openhab.io.homekit.crypto.EdsaSigner;
import org.openhab.io.homekit.crypto.EdsaVerifier;
import org.openhab.io.homekit.internal.http.jetty.HomekitHttpClientTransportOverHTTP;
import org.openhab.io.homekit.internal.http.jetty.HomekitHttpDestinationOverHTTP;
import org.openhab.io.homekit.internal.http.jetty.HomekitProtocolHandler;
import org.openhab.io.homekit.internal.pairing.PairingImpl;
import org.openhab.io.homekit.internal.pairing.PairingUID;
import org.openhab.io.homekit.util.Byte;
import org.openhab.io.homekit.util.Error;
import org.openhab.io.homekit.util.Message;
import org.openhab.io.homekit.util.Method;
import org.openhab.io.homekit.util.TypeLengthValue;
import org.openhab.io.homekit.util.TypeLengthValue.DecodeResult;
import org.openhab.io.homekit.util.TypeLengthValue.Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.srp6.ClientEvidenceRoutine;
import com.nimbusds.srp6.SRP6ClientCredentials;
import com.nimbusds.srp6.SRP6ClientEvidenceContext;
import com.nimbusds.srp6.SRP6CryptoParams;
import com.nimbusds.srp6.SRP6Exception;
import com.nimbusds.srp6.SRP6ServerEvidenceContext;
import com.nimbusds.srp6.ServerEvidenceRoutine;
import com.nimbusds.srp6.XRoutineWithUserIdentity;

import djb.Curve25519;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;

public class HomekitClient {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitClient.class);

    private static final BigInteger N_3072 = new BigInteger(
            "5809605995369958062791915965639201402176612226902900533702900882779736177890990861472094774477339581147373410185646378328043729800750470098210924487866935059164371588168047540943981644516632755067501626434556398193186628990071248660819361205119793693985433297036118232914410171876807536457391277857011849897410207519105333355801121109356897459426271845471397952675959440793493071628394122780510124618488232602464649876850458861245784240929258426287699705312584509625419513463605155428017165714465363094021609290561084025893662561222573202082865797821865270991145082200656978177192827024538990239969175546190770645685893438011714430426409338676314743571154537142031573004276428701433036381801705308659830751190352946025482059931306571004727362479688415574702596946457770284148435989129632853918392117997472632693078113129886487399347796982772784615865232621289656944284216824611318709764535152507354116344703769998514148343807");
    private static final BigInteger G = BigInteger.valueOf(5);
    private static volatile SecureRandom secureRandom = new SecureRandom();
    private HomekitClientSRP6Session SRP6Session;
    private final SRP6CryptoParams config = new SRP6CryptoParams(N_3072, G, "SHA-512");

    private byte[] clientPairingIdentifier;
    private byte[] clientLongtermSecretKey;

    private byte[] sessionKey;
    private byte[] sharedSecret;
    private byte[] accessoryPairingIdentifier;
    private byte[] accessoryPublicKey;
    private byte[] clientPublicKey;
    private byte[] clientPrivateKey;

    private InetAddress address;
    private int port;

    private @Nullable HttpClient httpClient;
    private String setupCode;
    private boolean isPaired;
    private boolean isPairVerified;

    private final PairingRegistry pairingRegistry;

    public HomekitClient(InetAddress ipv4Address, int port, byte[] clientPairingIdentifier,
            byte[] clientLongtermSecretKey, String setupCode, PairingRegistry pairingRegistry) {
        super();
        this.address = ipv4Address;
        this.port = port;
        this.clientPairingIdentifier = clientPairingIdentifier;
        this.clientLongtermSecretKey = clientLongtermSecretKey;
        this.setupCode = setupCode;
        this.isPaired = false;
        this.isPairVerified = false;
        this.pairingRegistry = pairingRegistry;

        this.httpClient = new HttpClient(new HomekitHttpClientTransportOverHTTP(), null);

        try {
            if (httpClient != null) {
                httpClient.start();
                ProtocolHandlers handlers = httpClient.getProtocolHandlers();
                handlers.clear();
                handlers.put(new HomekitProtocolHandler(this));
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public HomekitClient(InetAddress ipv4Address, int port, String setupCode, PairingRegistry pairingRegistry) {
        this(ipv4Address, port, generatePairingId(), generatePrivateKey(), setupCode, pairingRegistry);
    }

    public String getSetupCode() {
        return setupCode;
    }

    public void setSetupCode(String setupCode) {
        this.setupCode = setupCode;
    }

    public boolean isPaired() {
        return isPaired;
    }

    public boolean isPairVerified() {
        return isPairVerified;
    }

    public byte[] getPairingId() {
        return clientPairingIdentifier;
    }

    public byte[] getLongTermSecretKey() {
        return clientLongtermSecretKey;
    }

    public byte[] getAccessoryPairingId() {
        return accessoryPairingIdentifier;
    }

    public byte[] getAccessoryPublicKey() {
        return accessoryPublicKey;
    }

    public void pairSetup() throws IOException {

        sessionKey = null;
        sharedSecret = null;
        accessoryPairingIdentifier = null;
        accessoryPublicKey = null;
        clientPublicKey = null;
        clientPrivateKey = null;

        isPaired = false;

        StageResult stageResult = null;

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

        try {
            byte[] payload = doPairSetupStage3(stageResult);
            isPaired = true;
        } catch (HomekitException e) {
            return;
        }
    }

    public void pairVerify() throws IOException {

        sessionKey = null;
        sharedSecret = null;
        accessoryPairingIdentifier = null;
        accessoryPublicKey = null;
        clientPublicKey = null;
        clientPrivateKey = null;

        isPairVerified = false;

        StageResult stageResult = null;

        try {
            byte[] payload = doPairVerifyStage0();
            Future<StageResult> stageFuture = sendPairVerifyStage(payload);
            stageResult = stageFuture.get();
        } catch (InterruptedException | ExecutionException e1) {
            e1.printStackTrace();
            return;
        } catch (HomekitException e) {
            return;
        }

        try {
            byte[] payload = doPairVerifyStage1(stageResult);
            Future<StageResult> stageFuture = sendPairVerifyStage(payload);
            stageResult = stageFuture.get();
        } catch (InterruptedException | ExecutionException e1) {
            e1.printStackTrace();
            return;
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
            return;
        }

        try {
            byte[] payload = doPairVerifyStage2(stageResult);
            isPairVerified = true;
        } catch (HomekitException e) {
            return;
        }
    }

    protected byte[] doPairSetupStage0() throws IOException, HomekitException {
        Encoder encoder = TypeLengthValue.getEncoder();
        encoder.add(Message.STATE, (short) 0x01);
        encoder.add(Message.METHOD, Method.PAIR_SETUP_WITH_AUTH.getKey());

        return encoder.toByteArray();
    }

    protected byte[] doPairSetupStage1(StageResult stageResult) throws IOException, HomekitException {
        logger.info("Setup Stage 1 : Start");

        short state = stageResult.decodeResult.getByte(Message.STATE);
        if (state != 2) {
            throw new HomekitException("Wrong STATE");
        }

        BigInteger publicKey = stageResult.decodeResult.getBigInt(Message.PUBLIC_KEY);
        logger.info("Setup Stage 1 : Public Key is {}", Byte.byteToHexString(bigIntegerToUnsignedByteArray(publicKey)));

        BigInteger salt = stageResult.decodeResult.getBigInt(Message.SALT);
        logger.info("Setup Stage 1 : Salt is {}", Byte.byteToHexString(bigIntegerToUnsignedByteArray(salt)));

        if (SRP6Session == null) {
            SRP6Session = new HomekitClientSRP6Session();
            SRP6Session.setClientEvidenceRoutine(new ClientEvidenceRoutineImpl());
            SRP6Session.setServerEvidenceRoutine(new ServerEvidenceRoutineImpl());
            SRP6Session.setXRoutine(new XRoutineWithUserIdentity());
        }

        SRP6Session.step1("Pair-Setup", setupCode);

        SRP6ClientCredentials clientCredentials = null;
        try {
            clientCredentials = SRP6Session.step2(config, salt, publicKey);
        } catch (SRP6Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        BigInteger clientPublicKey = clientCredentials.A;
        logger.info("Setup Stage 1 : Client Public Key is {}",
                Byte.byteToHexString(bigIntegerToUnsignedByteArray(clientPublicKey)));

        BigInteger clientProof = clientCredentials.M1;
        logger.info("Setup Stage 1 : Client Proof is {}",
                Byte.byteToHexString(bigIntegerToUnsignedByteArray(clientProof)));

        Encoder encoder = TypeLengthValue.getEncoder();
        encoder.add(Message.STATE, (short) 0x03);
        encoder.add(Message.PUBLIC_KEY, clientPublicKey);
        encoder.add(Message.PROOF, clientProof);
        logger.info("Setup Stage 1 : End");

        return encoder.toByteArray();
    }

    protected byte[] doPairSetupStage2(StageResult stageResult) throws IOException, HomekitException {
        logger.info("Setup Stage 2 : Start");

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
        byte[] sBytes = bigIntegerToUnsignedByteArray(S);
        logger.info("Setup Stage 2 : SRP Session Key is {}", Byte.byteToHexString(sBytes));
        sharedSecret = digest.digest(sBytes);
        logger.info("Setup Stage 2 : Shared Secret is {}", Byte.byteToHexString(sharedSecret));

        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Setup-Encrypt-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-Encrypt-Info".getBytes(StandardCharsets.UTF_8)));
        sessionKey = new byte[32];
        hkdf.generateBytes(sessionKey, 0, 32);
        logger.info("Setup Stage 2 : Session Key is {}", Byte.byteToHexString(sessionKey));

        hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Setup-Controller-Sign-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-Controller-Sign-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] clientDeviceX = new byte[32];
        hkdf.generateBytes(clientDeviceX, 0, 32);
        logger.info("Setup Stage 2 : Client Device X is {}", Byte.byteToHexString(clientDeviceX));

        EdsaSigner signer = new EdsaSigner(clientLongtermSecretKey);
        byte[] clientLongtermPublicKey = signer.getPublicKey();
        byte[] clientDeviceInfo = Byte.joinBytes(clientDeviceX, clientPairingIdentifier, clientLongtermPublicKey);
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
        logger.info("Setup Stage 2 : Client Long Term Pubic Key is {}", Byte.byteToHexString(clientLongtermPublicKey));
        logger.info("Setup Stage 2 : Client Signature X is {}", Byte.byteToHexString(clientSignature));

        Encoder encoder = TypeLengthValue.getEncoder();
        encoder.add(Message.IDENTIFIER, clientPairingIdentifier);
        encoder.add(Message.PUBLIC_KEY, clientLongtermPublicKey);
        encoder.add(Message.SIGNATURE, clientSignature);

        ChachaEncoder chachaEncoder = new ChachaEncoder(sessionKey, "PS-Msg05".getBytes(StandardCharsets.UTF_8));
        byte[] ciphertext = chachaEncoder.encodeCiphertext(encoder.toByteArray());

        encoder = TypeLengthValue.getEncoder();
        encoder.add(Message.STATE, (short) 0x05);
        encoder.add(Message.ENCRYPTED_DATA, ciphertext);
        logger.info("Setup Stage 2 : End");

        return encoder.toByteArray();
    }

    protected byte[] doPairSetupStage3(StageResult stageResult) throws IOException, HomekitException {
        logger.info("Setup Stage 3 : Start");

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
        logger.info("Setup Stage 3 : Plaintext is {}", Byte.byteToHexString(plaintext));

        DecodeResult d = TypeLengthValue.decode(plaintext);
        accessoryPairingIdentifier = d.getBytes(Message.IDENTIFIER);
        logger.info("Setup Stage 3 : Accessory Pairing Id is {}", Byte.byteToHexString(accessoryPairingIdentifier));
        accessoryPublicKey = d.getBytes(Message.PUBLIC_KEY);
        logger.info("Setup Stage 3 : Accessory Long Term Public Key is {}", Byte.byteToHexString(accessoryPublicKey));
        byte[] accessorySignature = d.getBytes(Message.SIGNATURE);
        logger.info("Setup Stage 3 : Accessory Signature is {}", Byte.byteToHexString(accessorySignature));

        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Setup-Accessory-Sign-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-Accessory-Sign-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] accessoryDeviceX = new byte[32];
        hkdf.generateBytes(accessoryDeviceX, 0, 32);

        byte[] accessoryDeviceInfo = Byte.joinBytes(accessoryDeviceX, accessoryPairingIdentifier, accessoryPublicKey);
        logger.info("Setup Stage 3 : Accessory Device Info is {}", Byte.byteToHexString(accessoryDeviceInfo));

        try {
            if (!new EdsaVerifier(accessoryPublicKey).verify(accessoryDeviceInfo, accessorySignature)) {
                throw new HomekitException("Signature verification failed");
            }
        } catch (Exception e1) {
            throw new HomekitException("Signature verification failed");
        }

        addPairing(accessoryPairingIdentifier, accessoryPublicKey);
        SRP6Session = null;

        logger.info("Setup Stage 3 : End");

        return null;
    }

    protected byte[] doPairVerifyStage0() throws IOException, HomekitException {
        logger.info("Verifiy Stage 0 : Start");

        clientPublicKey = new byte[32];
        clientPrivateKey = new byte[32];
        secureRandom.nextBytes(clientPrivateKey);
        Curve25519.keygen(clientPublicKey, null, clientPrivateKey);

        Encoder encoder = TypeLengthValue.getEncoder();
        encoder.add(Message.STATE, (short) 0x01);
        encoder.add(Message.PUBLIC_KEY, clientPublicKey);

        logger.info("Verifiy Stage 0 : End");

        return encoder.toByteArray();
    }

    protected byte[] doPairVerifyStage1(StageResult stageResult) throws IOException, HomekitException {
        logger.info("Verifiy Stage 1 : Start");

        short state = stageResult.decodeResult.getByte(Message.STATE);
        if (state != 2) {
            throw new HomekitException("Wrong STATE");
        }

        accessoryPublicKey = stageResult.decodeResult.getBytes(Message.PUBLIC_KEY);
        logger.info("Verifiy Stage 1 : Accessory Public Key is {}", Byte.byteToHexString(accessoryPublicKey));

        byte[] messageData = new byte[stageResult.decodeResult.getLength(Message.ENCRYPTED_DATA) - 16];
        stageResult.decodeResult.getBytes(Message.ENCRYPTED_DATA, messageData, 0);
        byte[] authTagData = new byte[16];
        stageResult.decodeResult.getBytes(Message.ENCRYPTED_DATA, authTagData, messageData.length);

        sharedSecret = new byte[32];
        Curve25519.curve(sharedSecret, clientPrivateKey, accessoryPublicKey);
        logger.info("Verifiy Stage 1 : Shared Secret is {}", Byte.byteToHexString(sharedSecret));

        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Verify-Encrypt-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Verify-Encrypt-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] sessionKey = new byte[32];
        hkdf.generateBytes(sessionKey, 0, 32);
        logger.info("Verifiy Stage 1 : Session Key is {}", Byte.byteToHexString(sessionKey));

        byte[] plaintext = null;
        ChachaDecoder chachaDecoder = new ChachaDecoder(sessionKey, "PV-Msg02".getBytes(StandardCharsets.UTF_8));
        try {
            plaintext = chachaDecoder.decodeCiphertext(authTagData, messageData);
            logger.info("Verifiy Stage 1 : Plaintext is {}", Byte.byteToHexString(plaintext));
        } catch (Exception e) {
            logger.warn("Verifiy Stage 1 : Unable to decode the ciphertext");
            e.printStackTrace();
            throw new HomekitException("Ciphertext decoding failed");
        }

        Pairing accessoryPairing = null;
        byte[] accessorySignature = null;

        DecodeResult d = TypeLengthValue.decode(plaintext);
        accessoryPairingIdentifier = d.getBytes(Message.IDENTIFIER);
        logger.info("Verifiy Stage 1 : Accessory Pairing Id is {}", Byte.byteToHexString(accessoryPairingIdentifier));
        accessorySignature = d.getBytes(Message.SIGNATURE);
        logger.info("Verifiy Stage 1 : Accessory Signature is {}", Byte.byteToHexString(accessorySignature));

        accessoryPairing = pairingRegistry.get(new PairingUID(clientPairingIdentifier, accessoryPairingIdentifier));

        if (accessoryPairing == null) {
            throw new HomekitException("Adding pairing to registry failed");
        } else {
            logger.info("Fetched the Pairing {} : {}", accessoryPairing.getUID(),
                    Byte.byteToHexString(accessoryPairing.getDestinationLongtermPublicKey()));
        }

        byte[] accessoryDeviceInfo = Byte.joinBytes(accessoryPublicKey, accessoryPairingIdentifier, clientPublicKey);

        try {
            boolean signatureVerification = new EdsaVerifier(accessoryPairing.getDestinationLongtermPublicKey())
                    .verify(accessoryDeviceInfo, accessorySignature);
            if (!signatureVerification) {
                throw new HomekitException("Signature verification failed");
            }
        } catch (Exception e) {
            throw new HomekitException("Signature verification failed");
        }

        byte[] clientDeviceInfo = Byte.joinBytes(clientPublicKey, clientPairingIdentifier, accessoryPublicKey);

        byte[] clientSignature = null;
        try {
            logger.info("Verifiy Stage 1 : Client Long Term Secret Key is {}",
                    Byte.byteToHexString(clientLongtermSecretKey));
            clientSignature = new EdsaSigner(clientLongtermSecretKey).sign(clientDeviceInfo);
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
        logger.info("Verifiy Stage 1 : Client Pairing Id is {}", Byte.byteToHexString(clientPairingIdentifier));
        encoder.add(Message.IDENTIFIER, clientPairingIdentifier);
        logger.info("Verifiy Stage 1 : Client Signature is {}", Byte.byteToHexString(clientSignature));
        encoder.add(Message.SIGNATURE, clientSignature);
        plaintext = encoder.toByteArray();

        ChachaEncoder chacha = new ChachaEncoder(sessionKey, "PV-Msg03".getBytes(StandardCharsets.UTF_8));
        byte[] ciphertext = chacha.encodeCiphertext(plaintext);

        encoder = TypeLengthValue.getEncoder();
        encoder.add(Message.STATE, (short) 0x03);
        encoder.add(Message.ENCRYPTED_DATA, ciphertext);

        logger.info("Verifiy Stage 1 : End");

        return encoder.toByteArray();
    }

    protected byte[] doPairVerifyStage2(StageResult stageResult) throws IOException, HomekitException {
        logger.info("Verifiy Stage 2 : Start");

        short state = stageResult.decodeResult.getByte(Message.STATE);
        if (state != 4) {
            throw new HomekitException("Wrong STATE");
        }

        byte[] writeKey = createKey("Control-Write-Encryption-Key", sharedSecret);
        logger.info("Verifiy Stage 2 : Write Key is {}", Byte.byteToHexString(writeKey));

        byte[] readKey = createKey("Control-Read-Encryption-Key", sharedSecret);
        logger.info("Verifiy Stage 2 : Read Key is {}", Byte.byteToHexString(readKey));

        HomekitHttpDestinationOverHTTP destination = (HomekitHttpDestinationOverHTTP) httpClient.getDestination(
                stageResult.result.getRequest().getScheme(), stageResult.result.getRequest().getHost(),
                stageResult.result.getRequest().getPort());
        logger.info("Verifiy Stage 2 : Setting the keys on destination ", destination.toString());
        destination.setEncryptionKeys(readKey, writeKey);

        logger.info("Verifiy Stage 2 : End");

        return null;
    }

    protected Future<StageResult> sendPairSetupStage(byte[] request) throws InterruptedException {
        return sendStage(request, "/pair-setup");
    }

    protected Future<StageResult> sendPairVerifyStage(byte[] request) throws InterruptedException {
        return sendStage(request, "/pair-verify");
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
                                    return;
                                }

                                short state = d.getByte(Message.STATE);
                                logger.info("Received State {}", state);

                                StageResult stageResult = new StageResult(d, result);

                                completableFuture.complete(stageResult);
                            } catch (IOException e) {
                                SRP6Session = null;
                            }
                        }
                    }
                });

        return completableFuture;
    }

    class ClientEvidenceRoutineImpl implements ClientEvidenceRoutine {

        public ClientEvidenceRoutineImpl() {
        }

        /**
         * Calculates M1 according to the following formula:
         *
         * <p>
         * M1 = H(H(N) xor H(g) || H(username) || s || A || B || H(S))
         */
        @Override
        public BigInteger computeClientEvidence(SRP6CryptoParams cryptoParams, SRP6ClientEvidenceContext ctx) {

            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance(cryptoParams.H);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Could not locate requested algorithm", e);
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
            BigInteger ret = new BigInteger(1, digest.digest());
            return ret;
        }

        private byte[] xor(byte[] b1, byte[] b2) {
            byte[] result = new byte[b1.length];
            for (int i = 0; i < b1.length; i++) {
                result[i] = (byte) (b1[i] ^ b2[i]);
            }
            return result;
        }
    }

    class ServerEvidenceRoutineImpl implements ServerEvidenceRoutine {

        @Override
        public BigInteger computeServerEvidence(SRP6CryptoParams cryptoParams, SRP6ServerEvidenceContext ctx) {

            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance(cryptoParams.H);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Could not locate requested algorithm", e);
            }

            byte[] hS = digest.digest(bigIntegerToUnsignedByteArray(ctx.S));

            digest.update(bigIntegerToUnsignedByteArray(ctx.A));
            digest.update(bigIntegerToUnsignedByteArray(ctx.M1));
            digest.update(hS);

            return new BigInteger(1, digest.digest());
        }

    }

    protected static byte[] bigIntegerToUnsignedByteArray(BigInteger i) {
        byte[] array = i.toByteArray();
        if (array[0] == 0) {
            array = Arrays.copyOfRange(array, 1, array.length);
        }
        return array;
    }

    public static byte[] generatePrivateKey() {
        EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("ed25519-sha-512");
        byte[] seed = new byte[spec.getCurve().getField().getb() / 8];
        secureRandom.nextBytes(seed);
        logger.info("Generated a Private Key {}", Byte.byteToHexString(seed));
        return seed;
    }

    public static byte[] generatePairingId() {
        int byte1 = ((secureRandom.nextInt(255) + 1) | 2) & 0xFE; // Unicast locally administered MAC;
        return (Integer.toHexString(byte1).toUpperCase() + ":"
                + Stream.generate(() -> secureRandom.nextInt(255) + 1).limit(5)
                        .map(i -> Integer.toHexString(i).toUpperCase()).collect(Collectors.joining(":")))
                                .getBytes(StandardCharsets.UTF_8);
    }

    private byte[] createKey(String info, byte[] sharedSecret) {
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Control-Salt".getBytes(StandardCharsets.UTF_8),
                info.getBytes(StandardCharsets.UTF_8)));
        byte[] key = new byte[32];
        hkdf.generateBytes(key, 0, 32);
        return key;
    }

    public void addPairing(byte @NonNull [] accessoryPairingId, byte @NonNull [] accessoryPublicKey) {
        try {
            Pairing newPairing = new PairingImpl(clientPairingIdentifier, accessoryPairingId, accessoryPublicKey);
            Pairing oldPairing = pairingRegistry.remove(newPairing.getUID());

            if (oldPairing != null) {
                logger.info("Removed Pairing of Client {} with Accessory {} and Public Key {}",
                        Byte.byteToHexString(clientPairingIdentifier),
                        Byte.byteToHexString(oldPairing.getDestinationPairingId()),
                        Byte.byteToHexString(oldPairing.getDestinationLongtermPublicKey()));
            }

            pairingRegistry.add(newPairing);
            isPaired = true;
            logger.info("Paired Server {} with Client {} and Public Key {}",
                    Byte.byteToHexString(clientPairingIdentifier), Byte.byteToHexString(accessoryPairingId),
                    Byte.byteToHexString(accessoryPublicKey));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleEvent(byte[] body) {

        try {
            Byte.logBuffer(logger, "handleEvent", Byte.byteToHexString(clientPairingIdentifier), ByteBuffer.wrap(body));
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

        public DecodeResult decodeResult;
        public Result result;
    }

}
