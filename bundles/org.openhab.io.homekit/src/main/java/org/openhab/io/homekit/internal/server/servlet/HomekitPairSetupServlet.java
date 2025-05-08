package org.openhab.io.homekit.internal.server.servlet;

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
import org.openhab.io.homekit.api.hap.HomekitAccessoryServer;
import org.openhab.io.homekit.api.hap.HomekitErrorCode;
import org.openhab.io.homekit.api.hap.HomekitMessage;
import org.openhab.io.homekit.crypto.HomekitChachaDecoder;
import org.openhab.io.homekit.crypto.HomekitChachaEncoder;
import org.openhab.io.homekit.crypto.HomekitEdsaSigner;
import org.openhab.io.homekit.crypto.HomekitEdsaVerifier;
import org.openhab.io.homekit.crypto.HomekitEncryptionEngine;
import org.openhab.io.homekit.internal.server.servlet.HomekitServerSRP6Session.State;
import org.openhab.io.homekit.util.HomekitByte;
import org.openhab.io.homekit.util.HomekitTypeLengthValueEncoderDecoder;
import org.openhab.io.homekit.util.HomekitTypeLengthValueEncoderDecoder.DecodeResult;
import org.openhab.io.homekit.util.HomekitTypeLengthValueEncoderDecoder.Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.srp6.SRP6Exception;
import com.nimbusds.srp6.SRP6Routines;
import com.nimbusds.srp6.SRP6VerifierGenerator;
import com.nimbusds.srp6.XRoutineWithUserIdentity;

@SuppressWarnings("serial")
public class HomekitPairSetupServlet extends HomekitBaseServlet {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitPairSetupServlet.class);
    protected static final String LOG_PREFIX = "Homekit HomekitPairSetupServlet: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "HomekitAccessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    protected static final String LOG_EVENT = LOG_PREFIX + "Event - ";
    protected static final String LOG_SERVER = LOG_PREFIX + "Server - ";
    protected static final String LOG_PAIRING = LOG_PREFIX + "HomekitPairing - ";

    protected byte[] sessionKey;

    public HomekitPairSetupServlet() {
    }

    public HomekitPairSetupServlet(HomekitAccessoryServer server) {
        super(server);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            byte[] body = IOUtils.toByteArray(request.getInputStream());
            short state = getState(body);

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
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    protected void doStage1(HttpServletRequest request, HttpServletResponse response, byte[] body)
            throws ServletException, IOException {
        logger.info("{}Stage 1 Start", LOG_PAIRING);
        logger.info("{}Stage 1 Received Body {}", LOG_EVENT, HomekitByte.toHexString(body));

        HttpSession session = request.getSession();
        HomekitServerSRP6Session SRP6Session = (HomekitServerSRP6Session) session.getAttribute("SRP6Session");

        if (SRP6Session == null) {
            SRP6Session = new HomekitServerSRP6Session(HomekitEncryptionEngine.SRP6Params);
            SRP6Session.setClientEvidenceRoutine(new HomekitEncryptionEngine.ClientEvidenceRoutineImpl());
            SRP6Session.setServerEvidenceRoutine(new HomekitEncryptionEngine.ServerEvidenceRoutineImpl());
            session.setAttribute("SRP6Session", SRP6Session);
            logger.info("{}Stage 1 Added {} to Session", LOG_PAIRING, SRP6Session.toString());
        }

        if (SRP6Session.getState() != State.INIT) {
            logger.error("{}Stage 1 Session is not in state INIT", LOG_ERROR);
            response.setStatus(HttpServletResponse.SC_CONFLICT);
        } else {
            SRP6VerifierGenerator verifierGenerator = new SRP6VerifierGenerator(HomekitEncryptionEngine.SRP6Params);
            verifierGenerator.setXRoutine(new XRoutineWithUserIdentity());

            BigInteger salt = generateSalt();
            BigInteger verifier = verifierGenerator.generateVerifier(salt, "Pair-Setup", server.getSetupCode());
            logger.info("{}Stage 1 Verifier is {}", LOG_PAIRING,
                    HomekitByte.toHexString(bigIntegerToUnsignedByteArray(verifier)));

            Encoder encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();
            encoder.add(HomekitMessage.STATE, (short) 0x02);

            encoder.add(HomekitMessage.SALT, salt);

            BigInteger publicKey = SRP6Session.step1("Pair-Setup", salt, verifier);
            encoder.add(HomekitMessage.PUBLIC_KEY, publicKey);

            logger.info("{}Stage 1 End", LOG_PAIRING);
            response.setContentType("application/pairing+tlv8");
            response.setContentLengthLong(encoder.toByteArray().length);
            response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
            response.setStatus(HttpServletResponse.SC_OK);
            response.getOutputStream().write(encoder.toByteArray());
            response.getOutputStream().flush();
            logger.info("{}Stage 1 Flushed", LOG_PAIRING);
        }
    }

    protected void doStage2(HttpServletRequest request, HttpServletResponse response, byte[] body)
            throws ServletException, IOException {

        logger.info("{}Stage 2 Start", LOG_PAIRING);
        logger.info("{}Stage 2 Received Body {}", LOG_EVENT, HomekitByte.toHexString(body));

        HttpSession session = request.getSession();
        HomekitServerSRP6Session SRP6Session = (HomekitServerSRP6Session) session.getAttribute("SRP6Session");

        if (SRP6Session == null) {
            logger.info("{}Stage 2 Responding {}", LOG_PAIRING, HttpServletResponse.SC_UNAUTHORIZED);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            logger.info("{}Stage 2 Get {} from Session", LOG_PAIRING, SRP6Session.toString());
            if (SRP6Session.getState() != State.STEP_1) {
                logger.error("{}Stage 2 Session is not in state Step 1", LOG_ERROR);
                response.setStatus(HttpServletResponse.SC_CONFLICT);
            } else {
                BigInteger proof = null;
                Encoder encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();
                try {
                    proof = SRP6Session.step2(getPublicKey(body), getProof(body));
                    encoder.add(HomekitMessage.STATE, (short) 0x04);
                    encoder.add(HomekitMessage.PROOF, proof);

                    logger.info("{}Stage 2 End", LOG_PAIRING);
                    response.setContentType("application/pairing+tlv8");
                    response.setContentLengthLong(encoder.toByteArray().length);
                    response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getOutputStream().write(encoder.toByteArray());
                    response.getOutputStream().flush();
                    logger.info("{}Stage 2 Flushed", LOG_PAIRING);

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

        logger.info("{}Stage 3 Start", LOG_PAIRING);
        logger.info("{}Stage 3 Received Body {}", LOG_EVENT, HomekitByte.toHexString(body));

        HttpSession session = request.getSession();
        HomekitServerSRP6Session SRP6Session = (HomekitServerSRP6Session) session.getAttribute("SRP6Session");

        if (SRP6Session == null) {
            logger.info("{}Stage 3 Responding {}", LOG_PAIRING, HttpServletResponse.SC_UNAUTHORIZED);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            logger.info("{}Stage 3 Get {} from Session", LOG_PAIRING, SRP6Session.toString());
            MessageDigest digest = SRP6Session.getCryptoParams().getMessageDigestInstance();
            BigInteger S = SRP6Session.getSessionKey(false);
            byte[] sBytes = bigIntegerToUnsignedByteArray(S);
            logger.info("{}Stage 3 SRP Session Key is {}", LOG_PAIRING, HomekitByte.toHexString(sBytes));
            byte[] sharedSecret = digest.digest(sBytes);
            logger.info("{}Stage 3 Shared Secret is {}", LOG_PAIRING, HomekitByte.toHexString(sharedSecret));

            HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
            hkdf.init(new HKDFParameters(sharedSecret, "Pair-Setup-Encrypt-Salt".getBytes(StandardCharsets.UTF_8),
                    "Pair-Setup-Encrypt-Info".getBytes(StandardCharsets.UTF_8)));
            sessionKey = new byte[32];
            hkdf.generateBytes(sessionKey, 0, 32);
            logger.info("{}Stage 3 Session Key is {}", LOG_PAIRING, HomekitByte.toHexString(sessionKey));

            HomekitChachaDecoder chachaDecoder = new HomekitChachaDecoder(sessionKey, "PS-Msg05".getBytes(StandardCharsets.UTF_8));
            byte[] plaintext = chachaDecoder.decodeCiphertext(getAuthTagData(body), getMessageData(body));
            logger.info("{}Stage 3 Plaintext is {}", LOG_EVENT, HomekitByte.toHexString(plaintext));

            DecodeResult d = HomekitTypeLengthValueEncoderDecoder.decode(plaintext);
            byte[] clientPairingIdentifier = d.getBytes(HomekitMessage.IDENTIFIER);
            logger.info("{}Stage 3 Client HomekitPairing Id is {}", LOG_PAIRING, HomekitByte.toHexString(clientPairingIdentifier));

            byte[] clientLongtermPublicKey = d.getBytes(HomekitMessage.PUBLIC_KEY);
            logger.info("{}Stage 3 Client Long Term Public Key is {}", LOG_PAIRING,
                    HomekitByte.toHexString(clientLongtermPublicKey));

            byte[] clientSignature = d.getBytes(HomekitMessage.SIGNATURE);
            logger.info("{}Stage 3 Client Signature is {}", LOG_PAIRING, HomekitByte.toHexString(clientSignature));

            hkdf = new HKDFBytesGenerator(new SHA512Digest());
            hkdf.init(
                    new HKDFParameters(sharedSecret, "Pair-Setup-Controller-Sign-Salt".getBytes(StandardCharsets.UTF_8),
                            "Pair-Setup-Controller-Sign-Info".getBytes(StandardCharsets.UTF_8)));
            byte[] clientDeviceX = new byte[32];
            hkdf.generateBytes(clientDeviceX, 0, 32);

            byte[] clientDeviceInfo = HomekitByte.joinBytes(clientDeviceX, clientPairingIdentifier, clientLongtermPublicKey);
            logger.info("{}Stage 3 Client Device Info is {}", LOG_PAIRING, HomekitByte.toHexString(clientDeviceInfo));

            Encoder encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();

            boolean isError = false;
            try {
                if (!new HomekitEdsaVerifier(clientLongtermPublicKey).verify(clientDeviceInfo, clientSignature)) {
                    isError = true;
                }
            } catch (Exception e1) {
                isError = true;
            }

            if (isError) {
                logger.info("{}Stage 3 Reporting an Error", LOG_ERROR);

                encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();
                encoder.add(HomekitMessage.STATE, (short) 6);
                encoder.add(HomekitMessage.ERROR, HomekitErrorCode.AUTHENTICATION);

                logger.info("{}Stage 3 Removing SRP6Session", LOG_PAIRING);
                session.removeAttribute("SRP6Session");

                response.setContentType("application/pairing+tlv8");
                response.setContentLengthLong(encoder.toByteArray().length);
                response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
                response.setStatus(HttpServletResponse.SC_OK);
                response.getOutputStream().write(encoder.toByteArray());
                response.getOutputStream().flush();
                logger.info("{}Stage 3 Flushed", LOG_PAIRING);
            } else {
                logger.info("{}Stage 3 Adding pairing for HomekitAccessory Server", LOG_PAIRING);
                try {
                    server.addPairing(clientPairingIdentifier, clientLongtermPublicKey);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                hkdf = new HKDFBytesGenerator(new SHA512Digest());
                hkdf.init(new HKDFParameters(sharedSecret,
                        "Pair-Setup-HomekitAccessory-Sign-Salt".getBytes(StandardCharsets.UTF_8),
                        "Pair-Setup-HomekitAccessory-Sign-Info".getBytes(StandardCharsets.UTF_8)));
                byte[] accessoryDeviceX = new byte[32];
                hkdf.generateBytes(accessoryDeviceX, 0, 32);
                logger.info("{}Stage 3 HomekitAccessory Device X is {}", LOG_PAIRING, HomekitByte.toHexString(accessoryDeviceX));

                logger.info("{}Stage 3 Server Private Key is {}", LOG_PAIRING, HomekitByte.toHexString(server.getSecretKey()));
                HomekitEdsaSigner signer = new HomekitEdsaSigner(server.getSecretKey());

                byte[] accessoryInfo = HomekitByte.joinBytes(accessoryDeviceX, server.getPairingId(), signer.getPublicKey());
                logger.info("{}Stage 3 HomekitAccessory Device Info is {}", LOG_PAIRING, HomekitByte.toHexString(accessoryInfo));

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
                logger.info("{}Stage 3 HomekitAccessory Signature is {}", LOG_PAIRING, HomekitByte.toHexString(accessorySignature));

                logger.info("{}Stage 3 Server HomekitPairing Id is {}", LOG_PAIRING, server.getPairingId());
                encoder.add(HomekitMessage.IDENTIFIER, server.getPairingId());
                encoder.add(HomekitMessage.PUBLIC_KEY, signer.getPublicKey());
                encoder.add(HomekitMessage.SIGNATURE, accessorySignature);

                plaintext = encoder.toByteArray();

                HomekitChachaEncoder chachaEncoder = new HomekitChachaEncoder(sessionKey,
                        "PS-Msg06".getBytes(StandardCharsets.UTF_8));
                byte[] ciphertext = chachaEncoder.encodeCiphertext(plaintext);

                encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();
                encoder.add(HomekitMessage.STATE, (short) 6);
                encoder.add(HomekitMessage.ENCRYPTED_DATA, ciphertext);
                logger.info("{}Stage 3 End", LOG_PAIRING);

                logger.info("{}Stage 3 Removing SRP6Session", LOG_PAIRING);
                session.removeAttribute("SRP6Session");

                response.setContentType("application/pairing+tlv8");
                response.setContentLengthLong(encoder.toByteArray().length);
                response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
                response.setStatus(HttpServletResponse.SC_OK);
                response.getOutputStream().write(encoder.toByteArray());
                response.getOutputStream().flush();
                logger.info("{}Stage 3 Flushed", LOG_PAIRING);
            }
        }
    }

    // class ClientEvidenceRoutineImpl implements ClientEvidenceRoutine {
    //
    // public ClientEvidenceRoutineImpl() {
    // }
    //
    // /**
    // * Calculates M1 according to the following formula:
    // *
    // * <p>
    // * M1 = H(H(N) xor H(g) || H(username) || s || A || B || H(S))
    // */
    // @Override
    // public BigInteger computeClientEvidence(SRP6CryptoParams cryptoParams, SRP6ClientEvidenceContext ctx) {
    //
    // MessageDigest digest;
    // try {
    // digest = MessageDigest.getInstance(cryptoParams.H);
    // } catch (NoSuchAlgorithmException e) {
    // throw new RuntimeException("Could not locate requested algorithm", e);
    // }
    // digest.update(bigIntegerToUnsignedByteArray(cryptoParams.N));
    // byte[] hN = digest.digest();
    //
    // digest.update(bigIntegerToUnsignedByteArray(cryptoParams.g));
    // byte[] hg = digest.digest();
    //
    // byte[] hNhg = xor(hN, hg);
    //
    // digest.update(ctx.userID.getBytes(StandardCharsets.UTF_8));
    // byte[] hu = digest.digest();
    //
    // digest.update(bigIntegerToUnsignedByteArray(ctx.S));
    // byte[] hS = digest.digest();
    //
    // digest.update(hNhg);
    // digest.update(hu);
    // digest.update(bigIntegerToUnsignedByteArray(ctx.s));
    // digest.update(bigIntegerToUnsignedByteArray(ctx.A));
    // digest.update(bigIntegerToUnsignedByteArray(ctx.B));
    // digest.update(hS);
    // BigInteger ret = new BigInteger(1, digest.digest());
    // return ret;
    // }
    //
    // private byte[] xor(byte[] b1, byte[] b2) {
    // byte[] result = new byte[b1.length];
    // for (int i = 0; i < b1.length; i++) {
    // result[i] = (byte) (b1[i] ^ b2[i]);
    // }
    // return result;
    // }
    // }

    // class ServerEvidenceRoutineImpl implements ServerEvidenceRoutine {
    //
    // @Override
    // public BigInteger computeServerEvidence(SRP6CryptoParams cryptoParams, SRP6ServerEvidenceContext ctx) {
    //
    // MessageDigest digest;
    // try {
    // digest = MessageDigest.getInstance(cryptoParams.H);
    // } catch (NoSuchAlgorithmException e) {
    // throw new RuntimeException("Could not locate requested algorithm", e);
    // }
    //
    // byte[] hS = digest.digest(bigIntegerToUnsignedByteArray(ctx.S));
    //
    // digest.update(bigIntegerToUnsignedByteArray(ctx.A));
    // digest.update(bigIntegerToUnsignedByteArray(ctx.M1));
    // digest.update(hS);
    //
    // return new BigInteger(1, digest.digest());
    // }
    // }

    protected BigInteger getPublicKey(byte[] content) throws IOException {
        DecodeResult d = HomekitTypeLengthValueEncoderDecoder.decode(content);
        return d.getBigInt(HomekitMessage.PUBLIC_KEY);
    }

    protected BigInteger getProof(byte[] content) throws IOException {
        DecodeResult d = HomekitTypeLengthValueEncoderDecoder.decode(content);
        return d.getBigInt(HomekitMessage.PROOF);
    }

    public BigInteger generateSalt() {
        return new BigInteger(SRP6Routines.generateRandomSalt(16));
    }
}
