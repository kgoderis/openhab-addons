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

import org.eclipse.jetty.http.HostPortHttpField;
import org.eclipse.jetty.http.HttpHeader;

/**
 * Custom implementation of HostPortHttpField for HomeKit HTTP communication.
 *
 * <p>
 * This class extends Jetty's HostPortHttpField to provide specialized handling of host and port
 * information in HomeKit HTTP requests and responses. It is used to properly format and parse
 * the Host header field according to HomeKit protocol specifications.
 * </p>
 *
 * <p>
 * <b>Key features:</b>
 * </p>
 * <ul>
 * <li>Extends standard HTTP host/port field handling</li>
 * <li>Supports HomeKit-specific host header formatting</li>
 * <li>Maintains compatibility with Jetty's HTTP implementation</li>
 * </ul>
 *
 * <p>
 * <b>Component Integration:</b>
 * </p>
 * <ul>
 * <li>{@link org.eclipse.jetty.http.HostPortHttpField} for base host/port field functionality</li>
 * <li>{@link org.eclipse.jetty.http.HttpHeader} for HTTP header definitions</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
     */
public class HomekitHostPortHttpField extends HostPortHttpField {

    /**
     * Creates a new HomeKit host/port HTTP field.
     *
     * <p>
     * This constructor initializes a host/port field with the specified header type,
     * field name, and authority string. The authority string should contain the host
     * and optional port in the format "host:port".
     * </p>
     *
     * <p>
     * <b>Key implementation details:</b>
     * </p>
     * <ul>
     * <li>Validates header type and field name</li>
     * <li>Parses authority string for host and port</li>
     * <li>Initializes base HostPortHttpField</li>
     * </ul>
     *
     * @param header The HTTP header type (e.g., HOST)
     * @param name The field name
     * @param authority The authority string containing host and optional port
     */
    public HomekitHostPortHttpField(HttpHeader header, String name, String authority) {
        super(header, name, authority);
    }
}
