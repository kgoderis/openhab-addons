package org.openhab.io.homekit.internal.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.openhab.io.homekit.api.LocalAccessoryServer;
import org.openhab.io.homekit.util.Byte;
import org.openhab.io.homekit.util.Message;
import org.openhab.io.homekit.util.Method;
import org.openhab.io.homekit.util.TypeLengthValue;
import org.openhab.io.homekit.util.TypeLengthValue.DecodeResult;
import org.openhab.io.homekit.util.TypeLengthValue.Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class PairingServlet extends BaseServlet {

    protected static final Logger logger = LoggerFactory.getLogger(PairingServlet.class);

    public PairingServlet() {
    }

    public PairingServlet(LocalAccessoryServer server) {
        super(server);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        try {
            byte[] body = IOUtils.toByteArray(request.getInputStream());

            DecodeResult d = TypeLengthValue.decode(body);
            Method method = Method.get(d.getByte(Message.METHOD));

            switch (method) {
                case ADD_PAIRING: {
                    doAddPairing(request, response, body);
                    break;
                }
                case REMOVE_PAIRING: {
                    doRemovePairing(request, response, body);
                    break;
                }
                case LIST_PAIRINGS: {
                    doListPairing(request, response, body);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    protected void doAddPairing(HttpServletRequest request, HttpServletResponse response, byte[] body)
            throws ServletException, IOException {
        logger.info("doAddPairing : Start");
        logger.info("doAddPairing : Received Body {}", Byte.toHexString(body));

        DecodeResult d = TypeLengthValue.decode(body);

        byte[] additionalControllerPairingIdentifier = d.getBytes(Message.IDENTIFIER);
        byte[] additionalControllerLTPK = d.getBytes(Message.PUBLIC_KEY);
        byte[] additionalControllerPermissions = d.getBytes(Message.PERSMISSIONS);

        server.addPairing(additionalControllerPairingIdentifier, additionalControllerLTPK);

        Encoder encoder = TypeLengthValue.getEncoder();
        encoder.add(Message.STATE, (short) 2);

        response.setContentType("application/pairing+tlv8");
        response.setContentLengthLong(encoder.toByteArray().length);
        response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
        response.setStatus(HttpServletResponse.SC_OK);
        response.getOutputStream().write(encoder.toByteArray());
        response.getOutputStream().flush();
        logger.info("doAddPairing : Flushed");

        logger.info("doAddPairing : End");
    }

    protected void doRemovePairing(HttpServletRequest request, HttpServletResponse response, byte[] body)
            throws ServletException, IOException {
        logger.info("doRemovePairing : Start");
        logger.info("doRemovePairing : Received Body {}", Byte.toHexString(body));

        DecodeResult d = TypeLengthValue.decode(body);

        byte[] removedControllerPairingIdentifier = d.getBytes(Message.IDENTIFIER);
        server.removePairing(removedControllerPairingIdentifier);

        Encoder encoder = TypeLengthValue.getEncoder();
        encoder.add(Message.STATE, (short) 2);

        response.setContentType("application/pairing+tlv8");
        response.setContentLengthLong(encoder.toByteArray().length);
        response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
        response.setStatus(HttpServletResponse.SC_OK);
        response.getOutputStream().write(encoder.toByteArray());
        response.getOutputStream().flush();
        logger.info("doRemovePairing : Flushed");

        logger.info("doRemovePairing : End");
    }

    protected void doListPairing(HttpServletRequest request, HttpServletResponse response, byte[] body)
            throws ServletException, IOException {
        logger.info("doListPairing : Start");
        logger.info("doListPairing : Received Body {}", Byte.toHexString(body));
        logger.info("doListPairing : End");
    }
}
