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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.HexDump;
import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a servlet filter for logging HomeKit HTTP requests.
 *
 * This filter intercepts incoming HTTP requests to the HomeKit server and logs
 * detailed information about each request, including headers, payload, and
 * request metadata. It is used for debugging and monitoring HomeKit communication.
 *
 * The filter works in conjunction with:
 * - {@link HomekitRequestWrapper} for request wrapping
 * - {@link HomekitHttpParser} for request parsing
 * - {@link HomekitHttpGenerator} for response generation
 *
 * Key responsibilities:
 * 1. Intercepting HTTP requests
 * 2. Logging request details
 * 3. Wrapping requests for payload inspection
 * 4. Maintaining request context
 *
 * The implementation uses SLF4J for logging and provides detailed hex dumps
 * of request payloads for debugging purposes.
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@NonNullByDefault
public class HomekitLogRequestFilter implements Filter {

    private final Logger logger = LoggerFactory.getLogger(HomekitLogRequestFilter.class);

    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit HomekitLogRequestFilter: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_REQUEST = LOG_PREFIX + "Request - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    /**
     * Creates a new HomeKit log request filter.
     *
     * This constructor initializes the filter with default settings.
     * The filter is ready to process requests after initialization.
     */
    public HomekitLogRequestFilter() {
        logger.debug("{}Initializing HomekitLogRequestFilter", LOG_INIT);
    }

    /**
     * Initializes the filter with the given configuration.
     *
     * This method is called by the servlet container when the filter is being
     * initialized. It sets up any necessary resources or configuration.
     *
     * @param filterConfig The filter configuration
     * @throws ServletException if initialization fails
     */
    @Override
    @SuppressWarnings("null") // Parent Filter interface doesn't constrain this parameter
    public void init(@Nullable FilterConfig filterConfig) throws ServletException {
        logger.debug("{}Initializing filter with configuration", LOG_INIT);
    }

    /**
     * Processes the incoming request and logs its details.
     *
     * This method intercepts the request, wraps it for payload inspection,
     * and logs detailed information about the request before passing it
     * along the filter chain.
     *
     * Key implementation details:
     * - Wraps the request for payload access
     * - Logs request metadata
     * - Logs request payload in hex format
     * - Maintains request context
     *
     * @param request The servlet request
     * @param response The servlet response
     * @param chain The filter chain
     * @throws IOException if an I/O error occurs
     * @throws ServletException if the request cannot be processed
     */
    @Override
    @SuppressWarnings("null") // Parent Filter interface doesn't constrain these parameters
    public void doFilter(@Nullable ServletRequest request, @Nullable ServletResponse response,
            @Nullable FilterChain chain) throws IOException, ServletException {

        if (request == null || response == null || chain == null) {
            return;
        }

        try {
            logger.debug("{}Processing request: {}", LOG_REQUEST, ((HttpServletRequest) request).getRequestURI());
            if (logger.isDebugEnabled()) {
                final HomekitRequestWrapper wrappedRequest = new HomekitRequestWrapper((HttpServletRequest) request);
                logPayLoad(wrappedRequest);
                chain.doFilter(wrappedRequest, response);
            } else {
                chain.doFilter(request, response);
            }
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("{}Request processing completed", LOG_REQUEST);
            }
        }
    }

    /**
     * Logs the details of an HTTP request.
     *
     * This method logs comprehensive information about the request,
     * including headers, metadata, and a hex dump of the payload.
     *
     * @param request The HTTP request to log
     */
    private void logPayLoad(HttpServletRequest request) {
        final String userAgent = request.getHeader("User-Agent");
        logger.debug("{}Request details:", LOG_REQUEST);
        logger.debug("{}Source: {}:{} ; User-Agent: {}", LOG_REQUEST, request.getRemoteAddr(), request.getRemotePort(),
                userAgent);
        logger.debug("{}Method: {}", LOG_REQUEST, request.getMethod().toUpperCase());
        logger.debug("{}Content-Type: {}", LOG_REQUEST, request.getContentType());
        logger.debug("{}Content-Length: {}", LOG_REQUEST, request.getContentLength());
        logger.debug("{}URI: {}", LOG_REQUEST, request.getRequestURI());
        logger.debug("{}Query: {}", LOG_REQUEST, request.getQueryString());
        logger.debug("{}Payload:", LOG_REQUEST);
        try {
            byte[] body = IOUtils.toByteArray(request.getInputStream());

            try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                HexDump.dump(body, 0, stream, 0);
                stream.flush();
                logger.trace("{}{}", LOG_REQUEST, stream.toString(StandardCharsets.UTF_8.name()));
            }

        } catch (IOException e) {
            logger.error("{}Failed to read request payload: {}", LOG_ERROR, e.getMessage(), e);
        }

        logger.debug("{}Request logging completed", LOG_REQUEST);
    }

    /**
     * Cleans up resources used by the filter.
     *
     * This method is called by the servlet container when the filter is being
     * destroyed. It performs any necessary cleanup operations.
     */
    @Override
    public void destroy() {
        logger.debug("{}Destroying HomekitLogRequestFilter", LOG_INIT);
    }
}
