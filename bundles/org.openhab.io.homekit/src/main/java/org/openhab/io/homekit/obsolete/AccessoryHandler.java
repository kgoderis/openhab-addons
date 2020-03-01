package org.openhab.io.homekit.obsolete;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.openhab.io.homekit.api.ManagedAccessory;
import org.openhab.io.homekit.api.AccessoryServer;

public class AccessoryHandler extends BaseHandler {

    public AccessoryHandler(AccessoryServer server) {
        super(server);
    }

    @Override
    public void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        JsonArrayBuilder accessories = Json.createArrayBuilder();

        for (ManagedAccessory accessory : server.getAccessories()) {
            accessories.add(accessory.toJson());
        }

        JsonObjectBuilder builder = Json.createObjectBuilder().add("accessories", accessories);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Json.createWriter(baos).write(builder.build());

            response.setContentType("application/hap+json");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getOutputStream().write(baos.toByteArray());
            response.getOutputStream().flush();

            baseRequest.setHandled(true);
        }
    }
}
