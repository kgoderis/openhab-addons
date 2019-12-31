package org.openhab.io.homekit.obsolete;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

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
import org.openhab.io.homekit.crypto.EdsaVerifier;
import org.openhab.io.homekit.util.Error;
import org.openhab.io.homekit.util.Message;
import org.openhab.io.homekit.util.TypeLengthValue;
import org.openhab.io.homekit.util.TypeLengthValue.DecodeResult;
import org.openhab.io.homekit.util.TypeLengthValue.Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PairVerificationStageTwoHandler extends PairVerificationHandler {

    protected static final Logger logger = LoggerFactory.getLogger(PairVerificationStageTwoHandler.class);

    private static volatile SecureRandom secureRandom;

    public PairVerificationStageTwoHandler(AccessoryServer server) {
        super(server);
    }

    @Override
    public void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        byte[] body = IOUtils.toByteArray(request.getInputStream());

        if (getStage(body) == 3) {

            HttpSession session = request.getSession();
            byte[] sessionKey = (byte[]) session.getAttribute("sessionKey");
            byte[] clientPublicKey = (byte[]) session.getAttribute("clientPublicKey");
            byte[] accessoryPublicKey = (byte[]) session.getAttribute("accessoryPublicKey");
            byte[] sharedSecret = (byte[]) session.getAttribute("sharedSecret");

            ChachaDecoder chacha = new ChachaDecoder(sessionKey, "PV-Msg03".getBytes(StandardCharsets.UTF_8));
            byte[] plaintext = chacha.decodeCiphertext(getAuthTagData(body), getMessageData(body));

            DecodeResult d = TypeLengthValue.decode(plaintext);
            byte[] clientPairingId = d.getBytes(Message.IDENTIFIER);
            byte[] clientSignature = d.getBytes(Message.SIGNATURE);

            byte[] clientDeviceInfo = org.openhab.io.homekit.util.Byte.joinBytes(clientPublicKey, clientPairingId,
                    accessoryPublicKey);

            byte[] clientLongtermPublicKey = server
                    .getPairingPublicKey(server.getId() + new String(clientPairingId, StandardCharsets.UTF_8));

            Encoder encoder = TypeLengthValue.getEncoder();

            if (clientLongtermPublicKey == null) {

                logger.error("Unknown pairing: " + new String(clientPairingId, StandardCharsets.UTF_8));

                encoder.add(Message.STATE, (short) 4);
                encoder.add(Message.ERROR, Error.AUTHENTICATION);

                response.setContentType("application/pairing+tlv8");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getOutputStream().write(encoder.toByteArray());
                response.getOutputStream().flush();
            }

            try {
                if (new EdsaVerifier(clientLongtermPublicKey).verify(clientDeviceInfo, clientSignature)) {
                    encoder.add(Message.STATE, (short) 4);
                    logger.debug("Completed pair verification for " + server.getId());

                    session.setAttribute("Control-Write-Encryption-Key",
                            createKey("Control-Write-Encryption-Key", sharedSecret));
                    session.setAttribute("Control-Read-Encryption-Key",
                            createKey("Control-Read-Encryption-Key", sharedSecret));

                    response.setContentType("application/pairing+tlv8");
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getOutputStream().write(encoder.toByteArray());
                    response.getOutputStream().flush();

                    // TODO : Reset the notifications for all the characteristics of this accessory

                } else {
                    encoder.add(Message.ERROR, (short) 4);
                    logger.warn("Invalid signature. Could not pair " + server.getId());

                    response.setContentType("application/pairing+tlv8");
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getOutputStream().write(encoder.toByteArray());
                    response.getOutputStream().flush();
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            baseRequest.setHandled(true);
        }
    }

    private byte[] createKey(String info, byte[] sharedSecret) {
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Control-Salt".getBytes(StandardCharsets.UTF_8),
                info.getBytes(StandardCharsets.UTF_8)));
        byte[] key = new byte[32];
        hkdf.generateBytes(key, 0, 32);
        return key;
    }

}
