package org.openhab.io.homekit.obsolete;

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
import org.eclipse.jetty.server.Request;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.crypto.ChachaDecoder;
import org.openhab.io.homekit.crypto.ChachaEncoder;
import org.openhab.io.homekit.crypto.EdsaSigner;
import org.openhab.io.homekit.crypto.EdsaVerifier;
import org.openhab.io.homekit.internal.servlet.HomekitServerSRP6Session;
import org.openhab.io.homekit.util.Error;
import org.openhab.io.homekit.util.Message;
import org.openhab.io.homekit.util.TypeLengthValue;
import org.openhab.io.homekit.util.TypeLengthValue.DecodeResult;
import org.openhab.io.homekit.util.TypeLengthValue.Encoder;

public class PairSetupStageThreeHandler extends PairSetupHandler {

    private byte[] hkdf_enc_key;

    public PairSetupStageThreeHandler(AccessoryServer server) {
        super(server);
    }

    @Override
    public void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        byte[] body = IOUtils.toByteArray(request.getInputStream());

        if (getStage(body) == 3) {
            HttpSession session = request.getSession();
            HomekitServerSRP6Session SRP6Session = (HomekitServerSRP6Session) session.getAttribute("SRP6Session");

            if (SRP6Session == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                MessageDigest digest = SRP6Session.getCryptoParams().getMessageDigestInstance();
                BigInteger S = SRP6Session.getSessionKey(false);
                byte[] sBytes = bigIntegerToUnsignedByteArray(S);
                byte[] sharedSecret = digest.digest(sBytes);

                HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
                hkdf.init(new HKDFParameters(sharedSecret, "Pair-Setup-Encrypt-Salt".getBytes(StandardCharsets.UTF_8),
                        "Pair-Setup-Encrypt-Info".getBytes(StandardCharsets.UTF_8)));
                byte[] clientDeviceX = hkdf_enc_key = new byte[32];
                hkdf.generateBytes(clientDeviceX, 0, 32);

                ChachaDecoder chachaDecoder = new ChachaDecoder(clientDeviceX,
                        "PS-Msg05".getBytes(StandardCharsets.UTF_8));
                byte[] plaintext = chachaDecoder.decodeCiphertext(getAuthTagData(body), getMessageData(body));

                DecodeResult d = TypeLengthValue.decode(plaintext);
                byte[] clientPairingIdentifier = d.getBytes(Message.IDENTIFIER);
                byte[] clientLongtermPublicKey = d.getBytes(Message.PUBLIC_KEY);
                byte[] clientSignature = d.getBytes(Message.SIGNATURE);

                hkdf = new HKDFBytesGenerator(new SHA512Digest());
                hkdf.init(new HKDFParameters(sharedSecret,
                        "Pair-Setup-Controller-Sign-Salt".getBytes(StandardCharsets.UTF_8),
                        "Pair-Setup-Controller-Sign-Info".getBytes(StandardCharsets.UTF_8)));
                clientDeviceX = new byte[32];
                hkdf.generateBytes(clientDeviceX, 0, 32);

                byte[] clientDeviceInfo = org.openhab.io.homekit.util.Byte.joinBytes(clientDeviceX,
                        clientPairingIdentifier, clientLongtermPublicKey);

                Encoder encoder = TypeLengthValue.getEncoder();

                boolean reportError = false;
                try {
                    if (!new EdsaVerifier(clientLongtermPublicKey).verify(clientDeviceInfo, clientSignature)) {
                        reportError = true;
                    }
                } catch (Exception e1) {
                    reportError = true;
                }

                if (reportError) {
                    encoder = TypeLengthValue.getEncoder();
                    encoder.add(Message.STATE, (short) 6);
                    encoder.add(Message.ERROR, Error.AUTHENTICATION);

                    response.setContentType("application/pairing+tlv8");
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getOutputStream().write(encoder.toByteArray());
                    response.getOutputStream().flush();

                } else {
                    server.addPairing(server.getId() + new String(clientPairingIdentifier, StandardCharsets.UTF_8),
                            clientLongtermPublicKey);

                    hkdf = new HKDFBytesGenerator(new SHA512Digest());
                    hkdf.init(new HKDFParameters(sharedSecret,
                            "Pair-Setup-Accessory-Sign-Salt".getBytes(StandardCharsets.UTF_8),
                            "Pair-Setup-Accessory-Sign-Info".getBytes(StandardCharsets.UTF_8)));
                    byte[] accessoryDeviceX = new byte[32];
                    hkdf.generateBytes(accessoryDeviceX, 0, 32);

                    EdsaSigner signer = new EdsaSigner(server.getPrivateKey());

                    byte[] accessoryInfo = org.openhab.io.homekit.util.Byte.joinBytes(accessoryDeviceX,
                            server.getId().getBytes(StandardCharsets.UTF_8), signer.getPublicKey());

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

                    encoder.add(Message.IDENTIFIER, server.getId().getBytes(StandardCharsets.UTF_8));
                    encoder.add(Message.PUBLIC_KEY, signer.getPublicKey());
                    encoder.add(Message.SIGNATURE, accessorySignature);
                    plaintext = encoder.toByteArray();

                    ChachaEncoder chachaEncoder = new ChachaEncoder(hkdf_enc_key,
                            "PS-Msg06".getBytes(StandardCharsets.UTF_8));
                    byte[] ciphertext = chachaEncoder.encodeCiphertext(plaintext);

                    encoder = TypeLengthValue.getEncoder();
                    encoder.add(Message.STATE, (short) 6);
                    encoder.add(Message.ENCRYPTED_DATA, ciphertext);

                    response.setContentType("application/pairing+tlv8");
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getOutputStream().write(encoder.toByteArray());
                    response.getOutputStream().flush();
                }
            }

            baseRequest.setHandled(true);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            baseRequest.setHandled(true);
        }
    }
}
