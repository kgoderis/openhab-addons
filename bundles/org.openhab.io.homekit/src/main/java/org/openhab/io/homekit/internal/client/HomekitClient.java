package org.openhab.io.homekit.internal.client;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.io.homekit.crypto.ChachaDecoder;
import org.openhab.io.homekit.crypto.ChachaEncoder;
import org.openhab.io.homekit.crypto.EdsaSigner;
import org.openhab.io.homekit.crypto.EdsaVerifier;
import org.openhab.io.homekit.internal.http.jetty.HomekitHttpClientTransportOverHTTP;
import org.openhab.io.homekit.util.Byte;
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

import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;

public class HomekitClient {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitClient.class);

    protected static final BigInteger N_3072 = new BigInteger(
            "5809605995369958062791915965639201402176612226902900533702900882779736177890990861472094774477339581147373410185646378328043729800750470098210924487866935059164371588168047540943981644516632755067501626434556398193186628990071248660819361205119793693985433297036118232914410171876807536457391277857011849897410207519105333355801121109356897459426271845471397952675959440793493071628394122780510124618488232602464649876850458861245784240929258426287699705312584509625419513463605155428017165714465363094021609290561084025893662561222573202082865797821865270991145082200656978177192827024538990239969175546190770645685893438011714430426409338676314743571154537142031573004276428701433036381801705308659830751190352946025482059931306571004727362479688415574702596946457770284148435989129632853918392117997472632693078113129886487399347796982772784615865232621289656944284216824611318709764535152507354116344703769998514148343807");
    protected static final BigInteger G = BigInteger.valueOf(5);
    private static volatile SecureRandom secureRandom = new SecureRandom();

    protected HomekitClientSRP6Session SRP6Session;
    protected final SRP6CryptoParams config = new SRP6CryptoParams(N_3072, G, "SHA-512");

    protected byte[] sessionKey;
    protected byte[] sharedSecret;
    protected String clientPairingIdentifier;
    protected byte[] clientLongtermSecretKey;

    protected @Nullable HttpClient httpClient;
    protected String setupCode;

    public HomekitClient(String clientPairingIdentifier, byte[] clientLongtermSecretKey, String setupCode) {
        super();
        this.httpClient = new HttpClient(new HomekitHttpClientTransportOverHTTP(), null);
        this.clientPairingIdentifier = clientPairingIdentifier;
        this.clientLongtermSecretKey = clientLongtermSecretKey;
        this.setupCode = setupCode;

        try {
            if (httpClient != null) {
                httpClient.start();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public HomekitClient(String setupCode) {
        this(generatePairingId(), generatePrivateKey(), setupCode);
    }

    public void PairSetup() throws IOException {
        doStage0();
    }

    public String getSetupCode() {
        return setupCode;
    }

    public void setSetupCode(String setupCode) {
        this.setupCode = setupCode;
    }

    protected void doStage0() throws IOException {
        Encoder encoder = TypeLengthValue.getEncoder();
        encoder.add(Message.STATE, (short) 0x01);
        encoder.add(Message.METHOD, Method.PAIR_SETUP_WITH_AUTH.getKey());

        doStage(encoder.toByteArray());
    }

    protected void doStage1(byte[] body) throws IOException {
        logger.info("Stage 1 : Start");
        logger.info("Stage 1 : Received Body {}", Byte.byteToHexString(body));

        DecodeResult d = TypeLengthValue.decode(body);
        BigInteger publicKey = d.getBigInt(Message.PUBLIC_KEY);
        BigInteger salt = d.getBigInt(Message.SALT);

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
        BigInteger clientProof = clientCredentials.M1;

        Encoder encoder = TypeLengthValue.getEncoder();
        encoder.add(Message.STATE, (short) 0x03);
        encoder.add(Message.PUBLIC_KEY, clientPublicKey);
        encoder.add(Message.PROOF, clientProof);
        logger.info("Stage 1 : End");

        doStage(encoder.toByteArray());
    }

    protected void doStage2(byte[] body) throws IOException {
        logger.info("Stage 2 : Start");
        logger.info("Stage 2 : Received Body {}", Byte.byteToHexString(body));

        DecodeResult d = TypeLengthValue.decode(body);
        BigInteger proof = d.getBigInt(Message.PROOF);

        try {
            SRP6Session.step3(proof);
        } catch (SRP6Exception e) {
            // Verification failed
        }

        MessageDigest digest = SRP6Session.getCryptoParams().getMessageDigestInstance();
        BigInteger S = SRP6Session.getSessionKey(false);
        byte[] sBytes = bigIntegerToUnsignedByteArray(S);
        logger.info("Stage 2 : SRP Session Key is {}", Byte.byteToHexString(sBytes));
        sharedSecret = digest.digest(sBytes);
        logger.info("Stage 2 : Shared Secret is {}", Byte.byteToHexString(sharedSecret));

        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Setup-Encrypt-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-Encrypt-Info".getBytes(StandardCharsets.UTF_8)));
        sessionKey = new byte[32];
        hkdf.generateBytes(sessionKey, 0, 32);
        logger.info("Stage 2 : Session Key is {}", Byte.byteToHexString(sessionKey));

        hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Setup-Controller-Sign-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-Controller-Sign-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] clientDeviceX = new byte[32];
        hkdf.generateBytes(clientDeviceX, 0, 32);
        logger.info("Stage 2 : Client Device X is {}", Byte.byteToHexString(clientDeviceX));

        EdsaSigner signer = new EdsaSigner(clientLongtermSecretKey);
        byte[] clientLongtermPublicKey = signer.getPublicKey();
        byte[] clientDeviceInfo = Byte.joinBytes(clientDeviceX,
                clientPairingIdentifier.getBytes(StandardCharsets.UTF_8), clientLongtermPublicKey);
        byte[] clientDeviceSignature = null;
        try {
            clientDeviceSignature = signer.sign(clientDeviceInfo);
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
        encoder.add(Message.IDENTIFIER, clientPairingIdentifier.getBytes(StandardCharsets.UTF_8));
        encoder.add(Message.PUBLIC_KEY, clientLongtermPublicKey);
        encoder.add(Message.SIGNATURE, clientDeviceSignature);

        ChachaEncoder chachaEncoder = new ChachaEncoder(sessionKey, "PS-Msg05".getBytes(StandardCharsets.UTF_8));
        byte[] ciphertext = chachaEncoder.encodeCiphertext(encoder.toByteArray());

        encoder = TypeLengthValue.getEncoder();
        encoder.add(Message.STATE, (short) 0x05);
        encoder.add(Message.ENCRYPTED_DATA, ciphertext);
        logger.info("Stage 2 : End");

        doStage(encoder.toByteArray());
    }

    protected void doStage3(byte[] body) throws IOException {
        logger.info("Stage 3 : Start");
        logger.info("Stage 3 : Received Body {}", Byte.byteToHexString(body));

        DecodeResult d = TypeLengthValue.decode(body);
        byte[] messageData = new byte[d.getLength(Message.ENCRYPTED_DATA) - 16];
        d.getBytes(Message.ENCRYPTED_DATA, messageData, 0);
        byte[] authTagData = new byte[16];
        d.getBytes(Message.ENCRYPTED_DATA, authTagData, messageData.length);

        ChachaDecoder chachaDecoder = new ChachaDecoder(sessionKey, "PS-Msg06".getBytes(StandardCharsets.UTF_8));
        byte[] plaintext = chachaDecoder.decodeCiphertext(authTagData, messageData);
        logger.info("Stage 3 : Plaintext is {}", Byte.byteToHexString(plaintext));

        d = TypeLengthValue.decode(plaintext);
        byte[] accessoryPairingIdentifier = d.getBytes(Message.IDENTIFIER);
        logger.info("Stage 3 : Accessory Pairing Id is {}", Byte.byteToHexString(accessoryPairingIdentifier));
        byte[] accessoryLongtermPublicKey = d.getBytes(Message.PUBLIC_KEY);
        logger.info("Stage 3 : Accessory Long Term Public Key is {}", Byte.byteToHexString(accessoryLongtermPublicKey));
        byte[] accessorySignature = d.getBytes(Message.SIGNATURE);
        logger.info("Stage 3 : Accessory Signature is {}", Byte.byteToHexString(accessorySignature));

        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Setup-Accessory-Sign-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-Accessory-Sign-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] accessoryDeviceX = new byte[32];
        hkdf.generateBytes(accessoryDeviceX, 0, 32);

        byte[] accessoryDeviceInfo = Byte.joinBytes(accessoryDeviceX, accessoryPairingIdentifier,
                accessoryLongtermPublicKey);
        logger.info("Stage 3 : Accessory Device Info is {}", Byte.byteToHexString(accessoryDeviceInfo));

        boolean isError = false;
        try {
            if (!new EdsaVerifier(accessoryLongtermPublicKey).verify(accessoryDeviceInfo, accessorySignature)) {
                isError = true;
            }
        } catch (Exception e1) {
            isError = true;
        }

        if (!isError) {
            // TODO : Save pairing information
        }

        logger.info("Stage 3 : End");
    }

    protected void doStage(byte[] request) {

        httpClient.newRequest("http://domain.com/path").method(HttpMethod.POST)
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
                                    // Error
                                    return;
                                }

                                short stage = d.getByte(Message.STATE);

                                switch (stage) {
                                    case 2: {
                                        doStage1(body);
                                        break;
                                    }
                                    case 4: {
                                        doStage2(body);
                                        break;
                                    }
                                    case 6: {
                                        doStage3(body);
                                        break;
                                    }
                                }
                            } catch (IOException e) {
                                SRP6Session = null;
                            }
                        }
                    }
                });
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
        return seed;
    }

    public static String generatePairingId() {
        int byte1 = ((secureRandom.nextInt(255) + 1) | 2) & 0xFE; // Unicast locally administered MAC;
        return Integer.toHexString(byte1).toUpperCase() + ":" + Stream.generate(() -> secureRandom.nextInt(255) + 1)
                .limit(5).map(i -> Integer.toHexString(i).toUpperCase()).collect(Collectors.joining(":"));
    }

}
