package org.openhab.io.homekit.internal.servlet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.HttpConnection;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.api.ManagedAccessory;
import org.openhab.io.homekit.api.ManagedCharacteristic;
import org.openhab.io.homekit.api.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class CharacteristicServlet extends BaseServlet {

    protected static final Logger logger = LoggerFactory.getLogger(CharacteristicServlet.class);

    protected static final int SC_MULTI_STATUS = 207;

    public CharacteristicServlet() {
    }

    public CharacteristicServlet(AccessoryServer server) {
        super(server);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // Characteristics are requested with /characteristics?id=1.1,2.1,3.1
        String[] ids = request.getParameterValues("id")[0].split(",");
        boolean includeMeta = request.getParameter("meta") == null ? false
                : Boolean.getBoolean(request.getParameter("meta"));
        boolean includePermissions = request.getParameter("perms") == null ? false
                : Boolean.getBoolean(request.getParameter("perms"));
        boolean includeType = request.getParameter("type") == null ? false
                : Boolean.getBoolean(request.getParameter("type"));
        boolean includeEvent = request.getParameter("ev") == null ? false
                : Boolean.getBoolean(request.getParameter("ev"));

        JsonArrayBuilder characteristics = Json.createArrayBuilder();

        for (String id : ids) {
            String[] parts = id.split("\\.");
            if (parts.length != 2) {
                logger.error("Unexpected characteristics request: " + request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            int aid = Integer.parseInt(parts[0]);
            int iid = Integer.parseInt(parts[1]);

            ManagedAccessory theAccessory = server.getAccessory(aid);
            if (theAccessory != null) {
                Collection<Service> services = theAccessory.getServices();

                for (Service aService : services) {
                    ManagedCharacteristic<?> characteristic = (ManagedCharacteristic<?>) aService
                            .getCharacteristic(iid);
                    if (characteristic != null) {
                        characteristics
                                .add(characteristic.toJson(includeMeta, includePermissions, includeType, includeEvent));
                    }
                }
            } else {
                logger.warn("Accessory {} does exist. Request: {}", aid, request.getRequestURI());
            }
        }

        JsonObjectBuilder builder = Json.createObjectBuilder().add("characteristics", characteristics);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Json.createWriter(baos).write(builder.build());

            response.setContentType("application/hap+json");
            response.setContentLengthLong(baos.toByteArray().length);
            response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
            response.setStatus(HttpServletResponse.SC_OK);
            response.getOutputStream().write(baos.toByteArray());
            response.getOutputStream().flush();

            return;
        }

        // TODO Handle errors via HTTP Code 207
    }

    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        HttpConnection http = (HttpConnection) request.getAttribute("org.eclipse.jetty.server.HttpConnection");
        JsonArrayBuilder errorResponse = Json.createArrayBuilder();

        try {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(IOUtils.toByteArray(request.getInputStream()))) {
                JsonArray characteristics = Json.createReader(bais).readObject().getJsonArray("characteristics");
                for (JsonValue value : characteristics) {
                    JsonObject characteristicWriteObject = (JsonObject) value;
                    int aid = characteristicWriteObject.getInt("aid");
                    int iid = characteristicWriteObject.getInt("iid");

                    ManagedAccessory theAccessory = server.getAccessory(aid);
                    if (theAccessory != null) {
                        Collection<Service> services = theAccessory.getServices();

                        for (Service aService : services) {
                            ManagedCharacteristic<?> characteristic = (ManagedCharacteristic<?>) aService
                                    .getCharacteristic(iid);
                            if (characteristic != null) {
                                if (characteristicWriteObject.containsKey("value")) {
                                    characteristic.setValue(characteristicWriteObject.get("value"));
                                }
                                if (characteristicWriteObject.containsKey("ev")) {
                                    if (characteristicWriteObject.getBoolean("ev")) {
                                        server.addNotification(characteristic, HttpConnection.getCurrentConnection());
                                    } else {
                                        server.removeNotification(characteristic);
                                    }
                                }
                                // TODO : Table 6-11: HAP Status Codes
                            }
                        }
                    }
                }
            }

            response.setStatus(HttpServletResponse.SC_NO_CONTENT);

            return;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            JsonObjectBuilder builder = Json.createObjectBuilder().add("characteristics", errorResponse);

            response.setStatus(SC_MULTI_STATUS);
            response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
            response.setContentType("application/hap+json");

            JsonWriter jwr = Json.createWriter(baos);
            jwr.write(builder.build());
            jwr.close();

            logger.debug("Response : {}", baos.toString());

            response.setContentLengthLong(baos.toByteArray().length);
            response.getOutputStream().write(baos.toByteArray());
            response.getOutputStream().flush();
        }
    }
}
