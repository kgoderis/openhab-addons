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

package org.openhab.io.homekit.network.http;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Placeholder for HomeKit HTTP response implementation.
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public class HomekitHttpResponse {
    // Implementation placeholder - code is commented out below
}

// package org.openhab.io.homekit.internal.http.jetty;
//
// import java.util.ArrayList;
// import java.util.List;
//
// import org.eclipse.jetty.client.HttpResponse;
// import org.eclipse.jetty.client.api.Request;
// import org.eclipse.jetty.client.api.Response;
// import org.eclipse.jetty.http.HttpField;
// import org.eclipse.jetty.http.HttpFields;
//
// public class HomekitHttpResponse implements Response {
//
// private final HttpFields headers = new HttpFields();
// private final Request request;
// private final List<ResponseListener> listeners;
// private HomekitHttpVersion version;
// private int status;
// private String reason;
// private HttpFields trailers;
//
// public HomekitHttpResponse(Request request, List<ResponseListener> listeners) {
// this.request = request;
// this.listeners = listeners;
// }
//
// @Override
// public Request getRequest() {
// return request;
// }
//
// @Override
// public HomekitHttpVersion getVersion() {
// return version;
// }
//
// public HttpResponse version(HomekitHttpVersion version) {
// this.version = version;
// return this;
// }
//
// @Override
// public int getStatus() {
// return status;
// }
//
// @Override
// public HttpResponse status(int status) {
// this.status = status;
// return this;
// }
//
// @Override
// public String getReason() {
// return reason;
// }
//
// @Override
// public HttpResponse reason(String reason) {
// this.reason = reason;
// return this;
// }
//
// @Override
// public HttpFields getHeaders() {
// return headers;
// }
//
// @Override
// public <T extends ResponseListener> List<T> getListeners(Class<T> type) {
// ArrayList<T> result = new ArrayList<>();
// for (ResponseListener listener : listeners) {
// if (type == null || type.isInstance(listener)) {
// result.add((T) listener);
// }
// }
// return result;
// }
//
// @Override
// public HttpFields getTrailers() {
// return trailers;
// }
//
// @Override
// public HttpResponse trailer(HttpField trailer) {
// if (trailers == null) {
// trailers = new HttpFields();
// }
// trailers.add(trailer);
// return this;
// }
//
// @Override
// public boolean abort(Throwable cause) {
// return request.abort(cause);
// }
//
// @Override
// public String toString() {
// return String.format("%s[%s %d %s]@%x", HttpResponse.class.getSimpleName(), getVersion(), getStatus(),
// getReason(), hashCode());
// }
//
// }
