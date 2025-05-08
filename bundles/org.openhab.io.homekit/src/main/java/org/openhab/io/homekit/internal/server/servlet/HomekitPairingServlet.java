package org.openhab.io.homekit.internal.server.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.openhab.io.homekit.api.hap.HomekitAccessoryServer;
import org.openhab.io.homekit.api.hap.HomekitMessage;
import org.openhab.io.homekit.api.hap.HomekitMethod;
import org.openhab.io.homekit.exception.HomekitServerException;
import org.openhab.io.homekit.util.HomekitByte;
import org.openhab.io.homekit.util.HomekitTypeLengthValueEncoderDecoder;
import org.openhab.io.homekit.util.HomekitTypeLengthValueEncoderDecoder.DecodeResult;
import org.openhab.io.homekit.util.HomekitTypeLengthValueEncoderDecoder.Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class HomekitPairingServlet extends HomekitBaseServlet {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitPairingServlet.class);
    protected static final String LOG_PREFIX = "Homekit HomekitPairingServlet: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "HomekitAccessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    protected static final String LOG_EVENT = LOG_PREFIX + "Event - ";
    protected static final String LOG_SERVER = LOG_PREFIX + "Server - ";
    protected static final String LOG_PAIRING = LOG_PREFIX + "HomekitPairing - ";

    public HomekitPairingServlet() {
    }

    public HomekitPairingServlet(HomekitAccessoryServer server) {
        super(server);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        try {
            byte[] body = IOUtils.toByteArray(request.getInputStream());

            DecodeResult d = HomekitTypeLengthValueEncoderDecoder.decode(body);
            HomekitMethod method = HomekitMethod.get(d.getByte(HomekitMessage.METHOD));

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
        logger.info("{}Start", LOG_PAIRING);
        logger.info("{}Received Body {}", LOG_EVENT, HomekitByte.toHexString(body));

        DecodeResult d = HomekitTypeLengthValueEncoderDecoder.decode(body);

        byte[] additionalControllerPairingIdentifier = d.getBytes(HomekitMessage.IDENTIFIER);
        byte[] additionalControllerLTPK = d.getBytes(HomekitMessage.PUBLIC_KEY);
        byte[] additionalControllerPermissions = d.getBytes(HomekitMessage.PERSMISSIONS);

        try {
            server.addPairing(additionalControllerPairingIdentifier, additionalControllerLTPK);
        } catch (HomekitServerException e) {
            logger.error("{}Error adding pairing", LOG_ERROR, e);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Encoder encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();
        encoder.add(HomekitMessage.STATE, (short) 2);

        response.setContentType("application/pairing+tlv8");
        response.setContentLengthLong(encoder.toByteArray().length);
        response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
        response.setStatus(HttpServletResponse.SC_OK);
        response.getOutputStream().write(encoder.toByteArray());
        response.getOutputStream().flush();
        logger.info("{}Flushed", LOG_PAIRING);

        logger.info("{}End", LOG_PAIRING);
    }

    protected void doRemovePairing(HttpServletRequest request, HttpServletResponse response, byte[] body)
            throws ServletException, IOException {
        logger.info("{}Start", LOG_PAIRING);
        logger.info("{}Received Body {}", LOG_EVENT, HomekitByte.toHexString(body));

        DecodeResult d = HomekitTypeLengthValueEncoderDecoder.decode(body);

        byte[] removedControllerPairingIdentifier = d.getBytes(HomekitMessage.IDENTIFIER);
        try {
            server.removePairing(removedControllerPairingIdentifier);
        } catch (HomekitServerException e) {
            logger.error("{}Error removing pairing", LOG_ERROR, e);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Encoder encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();
        encoder.add(HomekitMessage.STATE, (short) 2);

        response.setContentType("application/pairing+tlv8");
        response.setContentLengthLong(encoder.toByteArray().length);
        response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
        response.setStatus(HttpServletResponse.SC_OK);
        response.getOutputStream().write(encoder.toByteArray());
        response.getOutputStream().flush();
        logger.info("{}Flushed", LOG_PAIRING);

        logger.info("{}End", LOG_PAIRING);
    }

    protected void doListPairing(HttpServletRequest request, HttpServletResponse response, byte[] body)
            throws ServletException, IOException {
        logger.info("{}Start", LOG_PAIRING);
        logger.info("{}Received Body {}", LOG_EVENT, HomekitByte.toHexString(body));
        logger.info("{}End", LOG_PAIRING);
    }
}
