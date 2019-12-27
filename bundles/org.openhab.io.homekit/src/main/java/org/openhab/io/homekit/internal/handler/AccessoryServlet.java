package org.openhab.io.homekit.internal.handler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpHeader;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.api.AccessoryServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class AccessoryServlet extends BaseServlet {

    protected static final Logger logger = LoggerFactory.getLogger(AccessoryServlet.class);

    public AccessoryServlet(AccessoryServer server) {
        super(server);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        JsonArrayBuilder accessories = Json.createArrayBuilder();

        for (Accessory accessory : server.getAccessories()) {
            accessories.add(accessory.toReducedJson());
        }

        JsonObjectBuilder builder = Json.createObjectBuilder().add("accessories", accessories);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
            response.setContentType("application/hap+json");

            String dummy0 = "{\"accessories\":[{\"aid\":1,\"services\":[{\"iid\":1,\"type\":\"3E\",\"characteristics\":[{\"iid\":2,\"type\":\"23\",\"perms\":[\"pr\"],\"format\":\"string\",\"ev\":false,\"description\":\"Name of the accessory\",\"value\":\"openHAB\",\"maxLen\":255},{\"iid\":3,\"type\":\"20\",\"perms\":[\"pr\"],\"format\":\"string\",\"ev\":false,\"description\":\"The name of the manufacturer\",\"value\":\"openHAB\",\"maxLen\":255},{\"iid\":4,\"type\":\"21\",\"perms\":[\"pr\"],\"format\":\"string\",\"ev\":false,\"description\":\"The name of the model\",\"value\":\"3.0.0.201912271459\",\"maxLen\":255},{\"iid\":5,\"type\":\"30\",\"perms\":[\"pr\"],\"format\":\"string\",\"ev\":false,\"description\":\"The serial number of the accessory\",\"value\":\"none\",\"maxLen\":255},{\"iid\":6,\"type\":\"14\",\"perms\":[\"pw\"],\"format\":\"bool\",\"ev\":false,\"description\":\"Identifies the accessory via a physical action on the accessory\"}]}]},{\"aid\":128474223,\"services\":[{\"iid\":1,\"type\":\"3E\",\"characteristics\":[{\"iid\":2,\"type\":\"23\",\"perms\":[\"pr\"],\"format\":\"string\",\"ev\":false,\"description\":\"Name of the accessory\",\"value\":\"On/Off Light\",\"maxLen\":255},{\"iid\":3,\"type\":\"20\",\"perms\":[\"pr\"],\"format\":\"string\",\"ev\":false,\"description\":\"The name of the manufacturer\",\"value\":\"none\",\"maxLen\":255},{\"iid\":4,\"type\":\"21\",\"perms\":[\"pr\"],\"format\":\"string\",\"ev\":false,\"description\":\"The name of the model\",\"value\":\"none\",\"maxLen\":255},{\"iid\":5,\"type\":\"30\",\"perms\":[\"pr\"],\"format\":\"string\",\"ev\":false,\"description\":\"The serial number of the accessory\",\"value\":\"none\",\"maxLen\":255},{\"iid\":6,\"type\":\"14\",\"perms\":[\"pw\"],\"format\":\"bool\",\"ev\":false,\"description\":\"Identifies the accessory via a physical action on the accessory\"}]},{\"iid\":7,\"type\":\"43\",\"characteristics\":[{\"iid\":8,\"type\":\"23\",\"perms\":[\"pr\"],\"format\":\"string\",\"ev\":false,\"description\":\"Name of the accessory\",\"value\":\"On/Off Light\",\"maxLen\":255},{\"iid\":9,\"type\":\"25\",\"perms\":[\"pw\",\"pr\",\"ev\"],\"format\":\"bool\",\"ev\":false,\"description\":\"Turn on and off\",\"value\":false}]}]}]}";
            String dummy1 = "{\"accessories\":[{\"aid\":1,\"services\":[{\"iid\":1,\"type\":\"3E\",\"characteristics\":[{\"iid\":2,\"type\":\"23\",\"perms\":[\"pr\"],\"format\":\"string\",\"ev\":false,\"description\":\"Name of the accessory\",\"value\":\"openHAB\",\"maxLen\":255},{\"iid\":3,\"type\":\"20\",\"perms\":[\"pr\"],\"format\":\"string\",\"ev\":false,\"description\":\"The name of the manufacturer\",\"value\":\"openHAB\",\"maxLen\":255},{\"iid\":4,\"type\":\"21\",\"perms\":[\"pr\"],\"format\":\"string\",\"ev\":false,\"description\":\"The name of the model\",\"value\":\"3.0.0.201912271459\",\"maxLen\":255},{\"iid\":5,\"type\":\"30\",\"perms\":[\"pr\"],\"format\":\"string\",\"ev\":false,\"description\":\"The serial number of the accessory\",\"value\":\"none\",\"maxLen\":255},{\"iid\":6,\"type\":\"14\",\"perms\":[\"pw\"],\"format\":\"bool\",\"ev\":false,\"description\":\"Identifies the accessory via a physical action on the accessory\"}]}]},{\"aid\":128474223,\"services\":[{\"iid\":1,\"type\":\"8d300883-4321-5fd4-97c0-888af6c5caf7\",\"characteristics\":[{\"iid\":2,\"type\":\"23\",\"perms\":[\"pr\"],\"format\":\"string\",\"ev\":false,\"description\":\"Name of the accessory\",\"value\":\"On/Off Light\",\"maxLen\":255},{\"iid\":3,\"type\":\"20\",\"perms\":[\"pr\"],\"format\":\"string\",\"ev\":false,\"description\":\"The name of the manufacturer\",\"value\":\"none\",\"maxLen\":255},{\"iid\":4,\"type\":\"21\",\"perms\":[\"pr\"],\"format\":\"string\",\"ev\":false,\"description\":\"The name of the model\",\"value\":\"none\",\"maxLen\":255},{\"iid\":5,\"type\":\"30\",\"perms\":[\"pr\"],\"format\":\"string\",\"ev\":false,\"description\":\"The serial number of the accessory\",\"value\":\"none\",\"maxLen\":255},{\"iid\":6,\"type\":\"14\",\"perms\":[\"pw\"],\"format\":\"bool\",\"ev\":false,\"description\":\"Identifies the accessory via a physical action on the accessory\"}]},{\"iid\":7,\"type\":\"43\",\"characteristics\":[{\"iid\":8,\"type\":\"23\",\"perms\":[\"pr\"],\"format\":\"string\",\"ev\":false,\"description\":\"Name of the accessory\",\"value\":\"On/Off Light\",\"maxLen\":255},{\"iid\":9,\"type\":\"25\",\"perms\":[\"pw\",\"pr\",\"ev\"],\"format\":\"bool\",\"ev\":false,\"description\":\"Turn on and off\",\"value\":false}]}]}]}";

            response.setContentLengthLong(dummy0.length());
            response.getOutputStream().write(dummy0.getBytes());

            // JsonWriter jwr = Json.createWriter(baos);
            // jwr.write(builder.build());
            // jwr.close();
            //
            // logger.debug("Accessories : {}", baos.toString());
            //
            // response.setContentLengthLong(baos.toByteArray().length);
            // response.getOutputStream().write(baos.toByteArray());
            response.getOutputStream().flush();
        }
    }
}
