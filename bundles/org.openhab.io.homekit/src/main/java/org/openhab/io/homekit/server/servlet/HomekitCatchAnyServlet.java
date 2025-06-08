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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet that implements the HomeKit catch-all request handler.
 *
 * <p>
 * This servlet serves as a security and debugging component in the HomeKit
 * server implementation, handling any HTTP requests that don't match the
 * specific paths handled by other servlets. It provides a consistent and
 * secure way to handle unmatched requests while maintaining proper logging
 * for security monitoring and debugging purposes.
 *
 * <p>
 * Key features:
 * <ul>
 * <li>Unified handling of unmatched requests</li>
 * <li>Consistent 404 Not Found responses</li>
 * <li>Request logging for security monitoring</li>
 * <li>Request body capture for debugging</li>
 * <li>Graceful error handling</li>
 * </ul>
 *
 * <p>
 * Security considerations:
 * <ul>
 * <li>Prevents information leakage about server structure</li>
 * <li>Provides consistent error responses</li>
 * <li>Enables security monitoring through logging</li>
 * <li>Handles both GET and POST requests uniformly</li>
 * <li>Captures request details for security analysis</li>
 * <li>Implements proper error handling to prevent stack traces</li>
 * </ul>
 *
 * <p>
 * The class integrates with:
 * <ul>
 * <li>{@link HomekitBaseServlet} for base servlet functionality</li>
 * <li>{@link HomekitAccessoryServer} for server instance management</li>
 * <li>{@link org.apache.commons.io.IOUtils} for request body handling</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@NonNullByDefault
public class HomekitCatchAnyServlet extends HomekitBaseServlet {

    // ========== Log Message Prefixes ==========
    protected static final Logger logger = LoggerFactory.getLogger(HomekitCatchAnyServlet.class);
    protected static final String LOG_PREFIX = "Homekit CatchAnyServlet: ";
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
     * Creates a new catch-all servlet with default configuration.
     */
    public HomekitCatchAnyServlet() {
        logger.debug("{}Creating new catch-all servlet with default configuration", LOG_INIT);
    }

    /**
     * Creates a new catch-all servlet with the specified server instance.
     *
     * <p>
     * This constructor initializes the servlet with a specific server instance,
     * enabling proper integration with the HomeKit server infrastructure.
     *
     * @param server The HomeKit accessory server instance to associate with this servlet
     */
    public HomekitCatchAnyServlet(HomekitAccessoryServer server) {
        super(server);
        logger.debug("{}Creating new catch-all servlet with server instance", LOG_INIT);
    }

    /**
     * Handles POST requests by delegating to doGet for consistent handling.
     *
     * <p>
     * This method implements a unified request handling strategy by:
     * <ul>
     * <li>Forwarding POST requests to the doGet method</li>
     * <li>Ensuring consistent handling of all unmatched requests</li>
     * <li>Maintaining uniform logging and response behavior</li>
     * </ul>
     *
     * <p>
     * This approach ensures:
     * <ul>
     * <li>Consistent security monitoring</li>
     * <li>Uniform error responses</li>
     * <li>Simplified request handling logic</li>
     * </ul>
     *
     * @param request The HTTP POST request to handle
     * @param response The HTTP response for the request
     * @throws ServletException if the request cannot be handled
     * @throws IOException if an I/O error occurs during processing
     */
    @Override
    protected void doPost(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("{}Handling POST request by delegating to doGet", LOG_REQUEST);
        if (request == null || response == null) {
            logger.error("{}Request or response is null", LOG_ERROR);
            if (response != null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
            return;
        }
        doGet(request, response);
    }

    /**
     * Handles GET requests for unmatched paths.
     *
     * <p>
     * This method implements the catch-all request handling by:
     * <ul>
     * <li>Logging the unmatched request URI for security monitoring</li>
     * <li>Attempting to read and capture the request body for debugging</li>
     * <li>Returning a 404 Not Found response</li>
     * <li>Handling any I/O errors gracefully</li>
     * </ul>
     *
     * <p>
     * Security features:
     * <ul>
     * <li>Request logging for security monitoring</li>
     * <li>Request body capture for debugging</li>
     * <li>Consistent error responses</li>
     * <li>Graceful error handling</li>
     * <li>No sensitive information exposure</li>
     * </ul>
     *
     * @param request The HTTP GET request to handle
     * @param response The HTTP response for the request
     * @throws ServletException if the request cannot be handled
     * @throws IOException if an I/O error occurs during processing
     */
    @Override
    protected void doGet(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("{}Handling GET request by delegating to doGet", LOG_REQUEST);
        if (request == null || response == null) {
            logger.error("{}Request or response is null", LOG_ERROR);
            if (response != null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
            return;
        }

        String requestURI = request.getRequestURI();
        logger.warn("{}Unmatched request received: {}", LOG_WARN, requestURI);

        try {
            byte[] body = IOUtils.toByteArray(request.getInputStream());
            if (body.length > 0) {
                logger.debug("{}Request body captured ({} bytes)", LOG_REQUEST, body.length);
            }
        } catch (IOException e) {
            logger.error("{}Failed to read request body: {}", LOG_ERROR, e.getMessage());
        } catch (Exception e) {
            logger.error("{}Unexpected error handling request: {}", LOG_ERROR, e.getMessage());
        } finally {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            logger.debug("{}Returning 404 Not Found for request: {}", LOG_REQUEST, requestURI);
        }
    }
}
