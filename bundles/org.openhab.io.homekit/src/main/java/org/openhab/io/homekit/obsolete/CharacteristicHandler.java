package org.openhab.io.homekit.obsolete;

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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.server.Request;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.api.Characteristic;
import org.openhab.io.homekit.api.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CharacteristicHandler extends BaseHandler {

    protected static final Logger logger = LoggerFactory.getLogger(CharacteristicHandler.class);

    public CharacteristicHandler(AccessoryServer server) {
        super(server);
    }

    @Override
    public void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        if (request.getMethod() == HttpMethod.GET) {
            String[] ids = request.getParameterValues("id");
            JsonArrayBuilder characteristics = Json.createArrayBuilder();

            for (String id : ids) {
                String[] parts = id.split("\\.");
                if (parts.length != 2) {
                    logger.error("Unexpected characteristics request: " + request.getRequestURI());
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    baseRequest.setHandled(true);
                    return;
                }

                int aid = Integer.parseInt(parts[0]);
                int iid = Integer.parseInt(parts[1]);

                Accessory theAccessory = server.getAccessory(aid);
                if (theAccessory != null) {
                    Collection<Service> services = theAccessory.getServices();

                    for (Service aService : services) {
                        Characteristic<?> characteristic = aService.getCharacteristic(iid);
                        if (characteristic != null) {
                            characteristics.add(characteristic.toJson());
                        } else {
                            logger.warn("Accessory " + aid + " does not have characteristic " + iid + "Request: "
                                    + request.getRequestURI());
                        }
                    }
                } else {
                    logger.warn("Accessory " + aid + " does not exist. Request: " + request.getRequestURI());
                }
            }

            JsonObjectBuilder builder = Json.createObjectBuilder().add("characteristics", characteristics);

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                Json.createWriter(baos).write(builder.build());

                response.setContentType("application/hap+json");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getOutputStream().write(baos.toByteArray());
                response.getOutputStream().flush();

                baseRequest.setHandled(true);

                return;
            }
        }

        if (request.getMethod() == HttpMethod.POST)

        {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(IOUtils.toByteArray(request.getInputStream()))) {
                JsonArray jsonCharacteristics = Json.createReader(bais).readObject().getJsonArray("characteristics");
                for (JsonValue value : jsonCharacteristics) {
                    JsonObject jsonCharacteristic = (JsonObject) value;
                    int aid = jsonCharacteristic.getInt("aid");
                    int iid = jsonCharacteristic.getInt("iid");

                    Accessory theAccessory = server.getAccessory(aid);
                    if (theAccessory != null) {
                        Collection<Service> services = theAccessory.getServices();

                        for (Service aService : services) {
                            @SuppressWarnings("rawtypes")
                            Characteristic characteristic = aService.getCharacteristic(iid);
                            if (characteristic != null) {
                                if (jsonCharacteristic.containsKey("value")) {
                                    characteristic.setValue(jsonCharacteristic.get("value"));
                                }
                                if (jsonCharacteristic.containsKey("ev")) {
                                    if (jsonCharacteristic.getBoolean("ev")) {
                                        server.addNotification(characteristic, (HttpConnection) baseRequest
                                                .getHttpChannel().getEndPoint().getConnection());
                                    } else {
                                        server.removeNotification(characteristic);
                                    }
                                }

                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            baseRequest.setHandled(true);
        }
    }
}
