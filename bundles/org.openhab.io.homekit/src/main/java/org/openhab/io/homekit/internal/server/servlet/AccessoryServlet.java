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
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.AccessoryServer;
import org.openhab.io.homekit.exception.AccessoryOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class AccessoryServlet extends BaseServlet {

    protected static final Logger logger = LoggerFactory.getLogger(AccessoryServlet.class);

    public AccessoryServlet() {
    }

    public AccessoryServlet(AccessoryServer server) {
        super(server);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        JsonArrayBuilder accessories = Json.createArrayBuilder();

        try {
            for (Accessory accessory : server.getAccessories()) {
                accessories.add(accessory.toReducedJson());
            }
        } catch (AccessoryOperationException e) {
            logger.error("Error accessing accessories: {}", e.getMessage());
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

            logger.debug("Accessories : {}", baos.toString());

            response.setContentLengthLong(baos.toByteArray().length);
            response.getOutputStream().write(baos.toByteArray());
            response.getOutputStream().flush();
        }
    }
}
