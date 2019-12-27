package org.openhab.io.homekit.internal.handler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Executor;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnection;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.api.Characteristic;
import org.openhab.io.homekit.api.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class CharacteristicServlet extends BaseServlet {

    protected static final Logger logger = LoggerFactory.getLogger(CharacteristicServlet.class);

    public CharacteristicServlet(AccessoryServer server) {
        super(server);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // Characteristics are requested with /characteristics?id=1.1,2.1,3.1
        String[] ids = request.getParameterValues("id");
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
            response.setContentLengthLong(baos.toByteArray().length);
            response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
            response.setStatus(HttpServletResponse.SC_OK);
            response.getOutputStream().write(baos.toByteArray());
            response.getOutputStream().flush();

            return;
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        HttpConnection http = (HttpConnection) request.getAttribute("org.eclipse.jetty.server.HttpConnection");

        EndPoint endp = http.getEndPoint();
        Connector connector = http.getConnector();
        Executor executor = connector.getExecutor();
        ByteBufferPool bufferPool = connector.getByteBufferPool();

        // subscriptions.batchUpdate();
        try {
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
                                    characteristic.setValue((JsonObject) jsonCharacteristic.get("value"));
                                }
                                if (jsonCharacteristic.containsKey("ev")) {
                                    if (jsonCharacteristic.getBoolean("ev")) {
                                        server.addNotification(characteristic, HttpConnection.getCurrentConnection());
                                    } else {
                                        server.removeNotification(characteristic);
                                    }
                                }

                            }
                        }
                    }
                }
            }
        } finally {
            // subscriptions.completeUpdateBatch();
        }

        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

}
