package org.openhab.io.homekit.internal.servlet;

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
import org.openhab.io.homekit.internal.servlet.HomekitSRP6ServerSession.State;
import org.openhab.io.homekit.util.Error;
import org.openhab.io.homekit.util.Message;
import org.openhab.io.homekit.util.TypeLengthValue;
import org.openhab.io.homekit.util.TypeLengthValue.DecodeResult;
import org.openhab.io.homekit.util.TypeLengthValue.Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.srp6.ClientEvidenceRoutine;
import com.nimbusds.srp6.SRP6ClientEvidenceContext;
import com.nimbusds.srp6.SRP6CryptoParams;
import com.nimbusds.srp6.SRP6Exception;
import com.nimbusds.srp6.SRP6ServerEvidenceContext;
import com.nimbusds.srp6.SRP6VerifierGenerator;
import com.nimbusds.srp6.ServerEvidenceRoutine;
import com.nimbusds.srp6.XRoutineWithUserIdentity;

@SuppressWarnings("serial")
public class PairSetupServlet extends BaseServlet {

    protected static final Logger logger = LoggerFactory.getLogger(PairSetupServlet.class);

    protected static final BigInteger N_3072 = new BigInteger(
            "5809605995369958062791915965639201402176612226902900533702900882779736177890990861472094774477339581147373410185646378328043729800750470098210924487866935059164371588168047540943981644516632755067501626434556398193186628990071248660819361205119793693985433297036118232914410171876807536457391277857011849897410207519105333355801121109356897459426271845471397952675959440793493071628394122780510124618488232602464649876850458861245784240929258426287699705312584509625419513463605155428017165714465363094021609290561084025893662561222573202082865797821865270991145082200656978177192827024538990239969175546190770645685893438011714430426409338676314743571154537142031573004276428701433036381801705308659830751190352946025482059931306571004727362479688415574702596946457770284148435989129632853918392117997472632693078113129886487399347796982772784615865232621289656944284216824611318709764535152507354116344703769998514148343807");
    protected static final BigInteger G = BigInteger.valueOf(5);
    protected static final String IDENTIFIER = "Pair-Setup";

    protected final SRP6CryptoParams config = new SRP6CryptoParams(N_3072, G, "SHA-512");
    private byte[] hkdf_enc_key;

    public PairSetupServlet() {
    }

    public PairSetupServlet(AccessoryServer server) {
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
                case 5: {
                    doStage3(request, response, body);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    protected void doStage1(HttpServletRequest request, HttpServletResponse response, byte[] body)
            throws ServletException, IOException {
        logger.info("Stage 1 : Start");
        logger.info("Stage 1 : Received Body {}", byteToHexString(body));

        HttpSession session = request.getSession();
        HomekitSRP6ServerSession SRP6Session = (HomekitSRP6ServerSession) session.getAttribute("SRP6Session");

        if (SRP6Session == null) {
            SRP6Session = new HomekitSRP6ServerSession(config);
            SRP6Session.setClientEvidenceRoutine(new ClientEvidenceRoutineImpl());
            SRP6Session.setServerEvidenceRoutine(new ServerEvidenceRoutineImpl());
            session.setAttribute("SRP6Session", SRP6Session);
            logger.info("Stage 1 : Added {} to Session", SRP6Session.toString());
        }

        if (SRP6Session.getState() != State.INIT) {
            logger.error("Stage 1 : Session is not in state INIT");
            response.setStatus(HttpServletResponse.SC_CONFLICT);
        } else {
            SRP6VerifierGenerator verifierGenerator = new SRP6VerifierGenerator(config);
            verifierGenerator.setXRoutine(new XRoutineWithUserIdentity());
            BigInteger verifier = verifierGenerator.generateVerifier(server.getSalt(), IDENTIFIER,
                    server.getSetupCode());
            logger.info("Stage 1 : Verifier is {} ", byteToHexString(bigIntegerToUnsignedByteArray(verifier)));

            Encoder encoder = TypeLengthValue.getEncoder();
            encoder.add(Message.STATE, (short) 0x02);
            encoder.add(Message.SALT, server.getSalt());

            BigInteger publicKey = SRP6Session.step1(IDENTIFIER, server.getSalt(), verifier);
            encoder.add(Message.PUBLIC_KEY, publicKey);

            logger.info("Stage 1 : End");
            response.setContentType("application/pairing+tlv8");
            response.setContentLengthLong(encoder.toByteArray().length);
            response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
            response.setStatus(HttpServletResponse.SC_OK);
            response.getOutputStream().write(encoder.toByteArray());
            response.getOutputStream().flush();
            logger.info("Stage 1 : Flushed");
        }
    }

    protected void doStage2(HttpServletRequest request, HttpServletResponse response, byte[] body)
            throws ServletException, IOException {

        logger.info("Stage 2 : Start");
        logger.info("Stage 2 : Received Body {}", byteToHexString(body));

        HttpSession session = request.getSession();
        HomekitSRP6ServerSession SRP6Session = (HomekitSRP6ServerSession) session.getAttribute("SRP6Session");

        if (SRP6Session == null) {
            logger.info("Stage 2 : Responding {}", HttpServletResponse.SC_UNAUTHORIZED);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            logger.info("Stage 2 : Get {} from Session", SRP6Session.toString());
            if (SRP6Session.getState() != State.STEP_1) {
                logger.error("Stage 2 : Session is not in state Step 1");
                response.setStatus(HttpServletResponse.SC_CONFLICT);
            } else {
                BigInteger proof = null;
                Encoder encoder = TypeLengthValue.getEncoder();
                try {
                    proof = SRP6Session.step2(getA(body), getM1(body));
                    encoder.add(Message.STATE, (short) 0x04);
                    encoder.add(Message.PROOF, proof);

                    logger.info("Stage 2 : End");
                    response.setContentType("application/pairing+tlv8");
                    response.setContentLengthLong(encoder.toByteArray().length);
                    response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getOutputStream().write(encoder.toByteArray());
                    response.getOutputStream().flush();
                    logger.info("Stage 2 : Flushed");

                } catch (SRP6Exception e) {
                    e.printStackTrace();
                    session.removeAttribute("SRP6Session");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getOutputStream().flush();
                }
            }
        }
    }

    protected void doStage3(HttpServletRequest request, HttpServletResponse response, byte[] body)
            throws ServletException, IOException {

        logger.info("Stage 3 : Start");
        logger.info("Stage 3 : Received Body {}", byteToHexString(body));

        HttpSession session = request.getSession();
        HomekitSRP6ServerSession SRP6Session = (HomekitSRP6ServerSession) session.getAttribute("SRP6Session");

        if (SRP6Session == null) {
            logger.info("Stage 3 : Responding {}", HttpServletResponse.SC_UNAUTHORIZED);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            logger.info("Stage 3 : Get {} from Session", SRP6Session.toString());
            MessageDigest digest = SRP6Session.getCryptoParams().getMessageDigestInstance();
            BigInteger S = SRP6Session.getSessionKey(false);
            byte[] sBytes = bigIntegerToUnsignedByteArray(S);
            logger.info("Stage 3 : Session Key is {}", byteToHexString(sBytes));
            byte[] sharedSecret = digest.digest(sBytes);
            logger.info("Stage 3 : Shared Secret is {}", byteToHexString(sharedSecret));

            HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
            hkdf.init(new HKDFParameters(sharedSecret, "Pair-Setup-Encrypt-Salt".getBytes(StandardCharsets.UTF_8),
                    "Pair-Setup-Encrypt-Info".getBytes(StandardCharsets.UTF_8)));
            byte[] clientDeviceX = hkdf_enc_key = new byte[32];
            hkdf.generateBytes(clientDeviceX, 0, 32);
            logger.info("Stage 3 : Client Device X is {}", byteToHexString(clientDeviceX));

            ChachaDecoder chachaDecoder = new ChachaDecoder(clientDeviceX, "PS-Msg05".getBytes(StandardCharsets.UTF_8));
            byte[] plaintext = chachaDecoder.decodeCiphertext(getAuthTagData(body), getMessageData(body));
            logger.info("Stage 3 : Cipher is {}", byteToHexString(plaintext));

            DecodeResult d = TypeLengthValue.decode(plaintext);
            byte[] clientPairingIdentifier = d.getBytes(Message.IDENTIFIER);
            logger.info("Stage 3 : Client Pairing Id is {}", byteToHexString(clientPairingIdentifier));

            byte[] clientLongtermPublicKey = d.getBytes(Message.PUBLIC_KEY);
            logger.info("Stage 3 : Client Long Term Public Key is {}", byteToHexString(clientLongtermPublicKey));

            byte[] clientSignature = d.getBytes(Message.SIGNATURE);
            logger.info("Stage 3 : Client Signature is {}", byteToHexString(clientSignature));

            hkdf = new HKDFBytesGenerator(new SHA512Digest());
            hkdf.init(
                    new HKDFParameters(sharedSecret, "Pair-Setup-Controller-Sign-Salt".getBytes(StandardCharsets.UTF_8),
                            "Pair-Setup-Controller-Sign-Info".getBytes(StandardCharsets.UTF_8)));
            clientDeviceX = new byte[32];
            hkdf.generateBytes(clientDeviceX, 0, 32);

            byte[] clientDeviceInfo = org.openhab.io.homekit.util.Byte.joinBytes(clientDeviceX, clientPairingIdentifier,
                    clientLongtermPublicKey);

            logger.info("Stage 3 : Client Device Info is {}", byteToHexString(clientDeviceInfo));

            Encoder encoder = TypeLengthValue.getEncoder();

            boolean isError = false;
            try {
                if (!new EdsaVerifier(clientLongtermPublicKey).verify(clientDeviceInfo, clientSignature)) {
                    isError = true;
                }
            } catch (Exception e1) {
                isError = true;
            }

            if (isError) {
                logger.info("Stage 3 : Reporting an Error");

                encoder = TypeLengthValue.getEncoder();
                encoder.add(Message.STATE, (short) 6);
                encoder.add(Message.ERROR, Error.AUTHENTICATION);

                logger.info("Stage 3 : Removing SRP6Session");
                session.removeAttribute("SRP6Session");

                response.setContentType("application/pairing+tlv8");
                response.setContentLengthLong(encoder.toByteArray().length);
                response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
                response.setStatus(HttpServletResponse.SC_OK);
                response.getOutputStream().write(encoder.toByteArray());
                response.getOutputStream().flush();
                logger.info("Stage 3 : Flushed");
            } else {
                logger.info("Stage 3 : Adding pairing to Accessory Server");
                try {
                    server.addPairing(new String(clientPairingIdentifier, StandardCharsets.UTF_8),
                            clientLongtermPublicKey);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                hkdf = new HKDFBytesGenerator(new SHA512Digest());
                hkdf.init(new HKDFParameters(sharedSecret,
                        "Pair-Setup-Accessory-Sign-Salt".getBytes(StandardCharsets.UTF_8),
                        "Pair-Setup-Accessory-Sign-Info".getBytes(StandardCharsets.UTF_8)));
                byte[] accessoryDeviceX = new byte[32];
                hkdf.generateBytes(accessoryDeviceX, 0, 32);
                logger.info("Stage 3 : Accessory Device X is {}", byteToHexString(accessoryDeviceX));

                logger.info("Stage 3 : Server Private Key is {}", byteToHexString(server.getPrivateKey()));
                EdsaSigner signer = new EdsaSigner(server.getPrivateKey());

                byte[] accessoryInfo = org.openhab.io.homekit.util.Byte.joinBytes(accessoryDeviceX,
                        server.getPairingId().getBytes(StandardCharsets.UTF_8), signer.getPublicKey());
                logger.info("Stage 3 : Accessory Device Info is {}", byteToHexString(accessoryInfo));

                byte[] accessorySignature = null;
                try {
                    accessorySignature = signer.sign(accessoryInfo);
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
                logger.info("Stage 3 : Accessory Signature is {}", byteToHexString(accessorySignature));

                logger.info("Stage 3 : Server Pairing Id is {}", server.getPairingId());
                encoder.add(Message.IDENTIFIER, server.getPairingId().getBytes(StandardCharsets.UTF_8));
                encoder.add(Message.PUBLIC_KEY, signer.getPublicKey());
                encoder.add(Message.SIGNATURE, accessorySignature);

                plaintext = encoder.toByteArray();

                ChachaEncoder chachaEncoder = new ChachaEncoder(hkdf_enc_key,
                        "PS-Msg06".getBytes(StandardCharsets.UTF_8));
                byte[] ciphertext = chachaEncoder.encodeCiphertext(plaintext);

                encoder = TypeLengthValue.getEncoder();
                encoder.add(Message.STATE, (short) 6);
                encoder.add(Message.ENCRYPTED_DATA, ciphertext);
                logger.info("Stage 3 : End");

                logger.info("Stage 3 : Removing SRP6Session");
                session.removeAttribute("SRP6Session");

                response.setContentType("application/pairing+tlv8");
                response.setContentLengthLong(encoder.toByteArray().length);
                response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
                response.setStatus(HttpServletResponse.SC_OK);
                response.getOutputStream().write(encoder.toByteArray());
                response.getOutputStream().flush();
                logger.info("Stage 3 : Flushed");
            }
        }

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

    protected BigInteger getA(byte[] content) throws IOException {
        DecodeResult d = TypeLengthValue.decode(content);
        return d.getBigInt(Message.PUBLIC_KEY);
    }

    protected BigInteger getM1(byte[] content) throws IOException {
        DecodeResult d = TypeLengthValue.decode(content);
        return d.getBigInt(Message.PROOF);
    }
}
