/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.openhab.io.homekit.server.servlet;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Placeholder for HomeKit old characteristic servlet implementation.
 *
 * @author Karel Goderis - Initial contribution
 */

@NonNullByDefault
public class HomekitOldCharacteristicServlet {
    // Implementation placeholder - code is commented out below
}

// package org.openhab.io.homekit.internal.servlet;

// import java.io.ByteArrayInputStream;
// import java.io.ByteArrayOutputStream;
// import java.io.IOException;
// import java.util.Collection;

// import javax.json.Json;
// import javax.json.JsonArray;
// import javax.json.JsonArrayBuilder;
// import javax.json.JsonObject;
// import javax.json.JsonObjectBuilder;
// import javax.json.JsonValue;
// import javax.json.JsonWriter;
// import javax.servlet.ServletException;
// import javax.servlet.http.HttpServletRequest;
// import javax.servlet.http.HttpServletResponse;

// import org.apache.commons.io.IOUtils;
// import org.eclipse.jetty.http.HttpHeader;
// import org.eclipse.jetty.server.HttpConnection;
// import org.openhab.io.homekit.api.ManagedCharacteristic;
// import org.openhab.io.homekit.api.hap.HomekitAccessory;
// import org.openhab.io.homekit.api.hap.HomekitService;
// import org.openhab.io.homekit.api.server.HomekitLocalAccessoryServer;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// @SuppressWarnings("serial")
// public class HomekitOldCharacteristicServlet extends HomekitBaseServlet {

// protected static final Logger logger = LoggerFactory.getLogger(HomekitCharacteristicServlet.class);

// protected static final int SC_MULTI_STATUS = 207;

// public HomekitCharacteristicServlet() {
// }

// public HomekitCharacteristicServlet(HomekitLocalAccessoryServer server) {
// super(server);
// }

// @Override
// public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
// // Characteristics are requested with /characteristics?id=1.1,2.1,3.1
// String[] ids = request.getParameterValues("id")[0].split(",");
// boolean includeMeta = request.getParameter("meta") == null ? false
// : Boolean.getBoolean(request.getParameter("meta"));
// boolean includePermissions = request.getParameter("perms") == null ? false
// : Boolean.getBoolean(request.getParameter("perms"));
// boolean includeType = request.getParameter("type") == null ? false
// : Boolean.getBoolean(request.getParameter("type"));
// boolean includeEvent = request.getParameter("ev") == null ? false
// : Boolean.getBoolean(request.getParameter("ev"));

// JsonArrayBuilder characteristics = Json.createArrayBuilder();

// for (String id : ids) {
// String[] parts = id.split("\\.");
// if (parts.length != 2) {
// logger.error("Unexpected characteristics request: " + request.getRequestURI());
// response.setStatus(HttpServletResponse.SC_NOT_FOUND);
// return;
// }

// int aid = Integer.parseInt(parts[0]);
// int iid = Integer.parseInt(parts[1]);

// HomekitAccessory theAccessory = server.getAccessory(aid);
// if (theAccessory != null) {
// Collection<HomekitService> services = theAccessory.getServices();

// for (HomekitService aService : services) {
// ManagedCharacteristic<?> characteristic = (ManagedCharacteristic<?>) aService
// .getCharacteristic(iid);
// if (characteristic != null) {
// characteristics
// .add(characteristic.toJson(includeMeta, includePermissions, includeType, includeEvent));
// }
// }
// } else {
// logger.warn("HomekitAccessory {} does exist. Request: {}", aid, request.getRequestURI());
// }
// }

// JsonObjectBuilder builder = Json.createObjectBuilder().add("characteristics", characteristics);

// try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
// Json.createWriter(baos).write(builder.build());

// response.setContentType("application/hap+json");
// response.setContentLengthLong(baos.toByteArray().length);
// response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
// response.setStatus(HttpServletResponse.SC_OK);
// response.getOutputStream().write(baos.toByteArray());
// response.getOutputStream().flush();

// return;
// }

// // TODO Handle errors via HTTP Code 207
// }

// @Override
// public void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

// HttpConnection http = (HttpConnection) request.getAttribute("org.eclipse.jetty.server.HttpConnection");
// JsonArrayBuilder errorResponse = Json.createArrayBuilder();

// try {
// try (ByteArrayInputStream bais = new ByteArrayInputStream(IOUtils.toByteArray(request.getInputStream()))) {
// JsonArray characteristics = Json.createReader(bais).readObject().getJsonArray("characteristics");
// for (JsonValue value : characteristics) {
// JsonObject characteristicWriteObject = (JsonObject) value;
// int aid = characteristicWriteObject.getInt("aid");
// int iid = characteristicWriteObject.getInt("iid");

// HomekitAccessory theAccessory = server.getAccessory(aid);
// if (theAccessory != null) {
// Collection<HomekitService> services = theAccessory.getServices();

// for (HomekitService aService : services) {
// ManagedCharacteristic<?> characteristic = (ManagedCharacteristic<?>) aService
// .getCharacteristic(iid);
// if (characteristic != null) {
// if (characteristicWriteObject.containsKey("value")) {
// characteristic.setValue(characteristicWriteObject.get("value"));
// }
// if (characteristicWriteObject.containsKey("ev")) {
// if (characteristicWriteObject.getBoolean("ev")) {
// server.addNotification(characteristic, HttpConnection.getCurrentConnection());
// } else {
// server.removeNotification(characteristic);
// }
// }
// // TODO : Table 6-11: HAP Status Codes
// }
// }
// }
// }
// }

// response.setStatus(HttpServletResponse.SC_NO_CONTENT);

// return;

// } catch (Exception e) {
// e.printStackTrace();
// } finally {
// }

// try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
// JsonObjectBuilder builder = Json.createObjectBuilder().add("characteristics", errorResponse);

// response.setStatus(SC_MULTI_STATUS);
// response.addHeader(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString());
// response.setContentType("application/hap+json");

// JsonWriter jwr = Json.createWriter(baos);
// jwr.write(builder.build());
// jwr.close();

// logger.debug("Response : {}", baos.toString());

// response.setContentLengthLong(baos.toByteArray().length);
// response.getOutputStream().write(baos.toByteArray());
// response.getOutputStream().flush();
// }
// }

// //Add async support instead of using httpConnection fed back to server
// // @WebServlet(asyncSupported = true)
// // public class AsyncServlet extends HttpServlet {
// // private final Set<AsyncContext> contexts = new ConcurrentHashSet<>();

// // @Override
// // protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
// // AsyncContext asyncContext = req.startAsync();
// // asyncContext.setTimeout(0); // no timeout
// // contexts.add(asyncContext);
// // }

// // public void sendToAllClients(String data) {
// // for (AsyncContext context : contexts) {
// // try {
// // HttpServletResponse response = (HttpServletResponse) context.getResponse();
// // PrintWriter writer = response.getWriter();
// // writer.write(data);
// // writer.flush();
// // } catch (IOException e) {
// // contexts.remove(context);
// // }
// // }
// // }
// }
// }
