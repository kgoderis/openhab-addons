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
import org.eclipse.jetty.server.Request;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.crypto.ChachaEncoder;
import org.openhab.io.homekit.crypto.EdsaSigner;
import org.openhab.io.homekit.util.Message;
import org.openhab.io.homekit.util.TypeLengthValue;
import org.openhab.io.homekit.util.TypeLengthValue.Encoder;

import djb.Curve25519;

public class PairVerificationStageOneHandler extends PairVerificationHandler {

    private static volatile SecureRandom secureRandom;

    public PairVerificationStageOneHandler(AccessoryServer server) {
        super(server);
    }

    @Override
    public void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        byte[] body = IOUtils.toByteArray(request.getInputStream());

        if (getStage(body) == 1) {

            logger.debug("Starting pair verification for " + server.getId());

            HttpSession session = request.getSession();

            byte[] clientPublicKey = getClientPublicKey(body);
            session.setAttribute("clientPublicKey", clientPublicKey);

            byte[] accessoryPublicKey = new byte[32];
            byte[] accessoryPrivateKey = new byte[32];
            getSecureRandom().nextBytes(accessoryPrivateKey);
            Curve25519.keygen(accessoryPublicKey, null, accessoryPrivateKey);
            session.setAttribute("accessoryPublicKey", accessoryPublicKey);

            byte[] sharedSecret = new byte[32];
            Curve25519.curve(sharedSecret, accessoryPrivateKey, clientPublicKey);
            session.setAttribute("sharedSecret", sharedSecret);

            byte[] accessoryInfo = org.openhab.io.homekit.util.Byte.joinBytes(accessoryPublicKey,
                    server.getId().getBytes(StandardCharsets.UTF_8), clientPublicKey);

            byte[] accessorySignature = null;
            try {
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

            HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
            hkdf.init(new HKDFParameters(sharedSecret, "Pair-Verify-Encrypt-Salt".getBytes(StandardCharsets.UTF_8),
                    "Pair-Verify-Encrypt-Info".getBytes(StandardCharsets.UTF_8)));
            byte[] sessionKey = new byte[32];
            hkdf.generateBytes(sessionKey, 0, 32);
            session.setAttribute("sessionKey", sessionKey);

            Encoder encoder = TypeLengthValue.getEncoder();
            encoder.add(Message.IDENTIFIER, server.getId().getBytes(StandardCharsets.UTF_8));
            encoder.add(Message.SIGNATURE, accessorySignature);
            byte[] plaintext = encoder.toByteArray();

            ChachaEncoder chacha = new ChachaEncoder(sessionKey, "PV-Msg02".getBytes(StandardCharsets.UTF_8));
            byte[] ciphertext = chacha.encodeCiphertext(plaintext);

            encoder = TypeLengthValue.getEncoder();
            encoder.add(Message.STATE, (short) 2);
            encoder.add(Message.ENCRYPTED_DATA, ciphertext);
            encoder.add(Message.PUBLIC_KEY, accessoryPublicKey);

            response.setContentType("application/pairing+tlv8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getOutputStream().write(encoder.toByteArray());
            response.getOutputStream().flush();

            baseRequest.setHandled(true);
        }
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
