package org.openhab.io.homekit.internal.server.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpHeader;
import org.openhab.io.homekit.api.hap.HomekitAccessory;
import org.openhab.io.homekit.api.hap.HomekitAccessoryServer;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class HomekitAccessoryServlet extends HomekitBaseServlet {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitAccessoryServlet.class);
    protected static final String LOG_PREFIX = "Homekit HomekitAccessoryServlet: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "HomekitAccessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    protected static final String LOG_EVENT = LOG_PREFIX + "Event - ";
    protected static final String LOG_SERVER = LOG_PREFIX + "Server - ";
    protected static final String LOG_PAIRING = LOG_PREFIX + "HomekitPairing - ";

    public HomekitAccessoryServlet() {
    }

    public HomekitAccessoryServlet(HomekitAccessoryServer server) {
        super(server);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        JsonArrayBuilder accessories = Json.createArrayBuilder();

        try {
            for (HomekitAccessory accessory : server.getAccessories()) {
                accessories.add(accessory.toReducedJson());
            }
        } catch (HomekitAccessoryOperationException e) {
            logger.error("{}Error accessing accessories: {}", LOG_ERROR, e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        JsonObjectBuilder builder = Json.createObjectBuilder().add("accessories", accessories);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
            response.setContentType("application/hap+json");

            JsonWriter jwr = Json.createWriter(baos);
            jwr.write(builder.build());
            jwr.close();

            logger.debug("{}Accessories : {}", LOG_EVENT, baos.toString());

            response.setContentLengthLong(baos.toByteArray().length);
            response.getOutputStream().write(baos.toByteArray());
            response.getOutputStream().flush();
        }
    }
}
