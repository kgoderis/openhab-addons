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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.http.HttpHeader;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet that implements the HomeKit accessory information protocol.
 *
 * <p>
 * This servlet provides the core functionality for exposing HomeKit accessory
 * information to clients, implementing the HomeKit Accessory Protocol (HAP)
 * specification for accessory discovery and configuration. It serves as the
 * primary interface for clients to discover and understand the capabilities
 * of connected accessories.
 *
 * <p>
 * Key features:
 * <ul>
 * <li>Complete accessory information exposure</li>
 * <li>HAP-compliant JSON response formatting</li>
 * <li>Efficient accessory enumeration</li>
 * <li>Error handling and logging</li>
 * <li>Proper HTTP header management</li>
 * <li>Streaming response handling</li>
 * </ul>
 *
 * <p>
 * Protocol implementation details:
 * <ul>
 * <li>Accessory metadata exposure (name, manufacturer, model)</li>
 * <li>Service and characteristic definitions</li>
 * <li>Reduced JSON format for efficient transmission</li>
 * <li>Proper HTTP headers and content types</li>
 * <li>Keep-alive connection management</li>
 * <li>Content length optimization</li>
 * </ul>
 *
 * <p>
 * The class integrates with:
 * <ul>
 * <li>{@link HomekitBaseServlet} for base servlet functionality</li>
 * <li>{@link HomekitAccessoryServer} for server and accessory management</li>
 * <li>{@link HomekitAccessory} for accessory information and metadata</li>
 * <li>{@link javax.json.Json} for HAP-compliant JSON handling</li>
 * <li>{@link org.eclipse.jetty.http.HttpHeader} for HTTP header management</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@NonNullByDefault
public class HomekitAccessoryServlet extends HomekitBaseServlet {

    private static final long serialVersionUID = 1L;

    // ========== Log Message Prefixes ==========
    protected static final Logger logger = LoggerFactory.getLogger(HomekitAccessoryServlet.class);
    protected static final String LOG_PREFIX = "Homekit AccessoryServlet: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "Accessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    protected static final String LOG_EVENT = LOG_PREFIX + "Event - ";
    protected static final String LOG_SERVER = LOG_PREFIX + "Server - ";
    protected static final String LOG_PAIRING = LOG_PREFIX + "Pairing - ";
    protected static final String LOG_REQUEST = LOG_PREFIX + "Request - ";

    /**
     * Creates a new accessory servlet with default configuration.
     */
    public HomekitAccessoryServlet() {
        logger.debug("{}Creating new accessory servlet with default configuration", LOG_INIT);
    }

    /**
     * Creates a new accessory servlet with the specified server instance.
     *
     * <p>
     * This constructor initializes the servlet with a specific server instance,
     * enabling proper integration with the HomeKit server infrastructure.
     *
     * @param server The HomeKit accessory server instance to associate with this servlet
     */
    public HomekitAccessoryServlet(HomekitAccessoryServer server) {
        super(server);
        logger.debug("{}Creating new accessory servlet with server instance", LOG_INIT);
    }

    /**
     * Handles GET requests for accessory information.
     *
     * <p>
     * This method implements the HAP accessory information protocol by:
     * <ul>
     * <li>Retrieving all registered accessories from the server</li>
     * <li>Converting each accessory to a reduced JSON representation</li>
     * <li>Building a complete JSON response with all accessories</li>
     * <li>Setting appropriate HTTP headers and content type</li>
     * <li>Writing the JSON response to the output stream</li>
     * </ul>
     *
     * <p>
     * The response follows the HAP specification and includes:
     * <ul>
     * <li>Accessory metadata (name, manufacturer, model, serial number)</li>
     * <li>Service definitions with their types and characteristics</li>
     * <li>Characteristic definitions with their properties and values</li>
     * <li>Proper HAP content type and connection headers</li>
     * </ul>
     *
     * <p>
     * Error handling ensures:
     * <ul>
     * <li>Proper error status codes for server errors</li>
     * <li>Detailed error logging for debugging</li>
     * <li>Graceful failure handling</li>
     * <li>Resource cleanup in error cases</li>
     * <li>Proper exception propagation</li>
     * </ul>
     *
     * <p>
     * Implementation details:
     * <ul>
     * <li>Uses JsonArrayBuilder for efficient JSON construction</li>
     * <li>Implements streaming response handling</li>
     * <li>Manages keep-alive connections</li>
     * <li>Optimizes content length calculation</li>
     * <li>Ensures proper resource cleanup</li>
     * </ul>
     *
     * @param request The HTTP request for accessory information
     * @param response The HTTP response containing the accessory data
     * @throws IOException if an I/O error occurs during response writing
     * @throws ServletException if the request cannot be handled
     */
    @Override
    public void doGet(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response)
            throws IOException, ServletException {
        logger.debug("{}Handling GET request for accessory information", LOG_REQUEST);

        if (request == null || response == null) {
            logger.error("{}Request or response is null", LOG_ERROR);
            if (response != null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
            return;
        }
        JsonArrayBuilder accessories = Json.createArrayBuilder();

        try {
            if (server != null && server.getAccessories() != null) {
                @SuppressWarnings("null") // getAccessories() returns non-null list
                var serverAccessories = server.getAccessories();
                for (HomekitAccessory accessory : serverAccessories) {
                    logger.trace("{}Processing accessory: {}", LOG_ACCESSORY, accessory.getClass().getSimpleName());
                    accessories.add(accessory.toReducedJson());
                }
            }
        } catch (HomekitAccessoryOperationException e) {
            logger.error("{}Error accessing accessories: {}", LOG_ERROR, e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        } catch (Exception e) {
            logger.error("{}Unexpected error processing accessories: {}", LOG_ERROR, e.getMessage(), e);
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

            logger.debug("{}Generated accessory information response ({} bytes)", LOG_EVENT, baos.toByteArray().length);

            response.setContentLengthLong(baos.toByteArray().length);
            response.getOutputStream().write(baos.toByteArray());
            response.getOutputStream().flush();
            logger.debug("{}Successfully sent accessory information response", LOG_REQUEST);
        } catch (IOException e) {
            logger.error("{}Failed to write response: {}", LOG_ERROR, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("{}Unexpected error writing response: {}", LOG_ERROR, e.getMessage(), e);
            throw new ServletException("Failed to write response", e);
        }
    }
}
