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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.HexDump;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles detailed logging of HomeKit HTTP requests and responses.
 *
 * <p>
 * This class extends Jetty's {@link RequestLogHandler} to provide comprehensive logging
 * of HomeKit HTTP communication. It captures and logs detailed information about
 * incoming requests and outgoing responses, including headers, payloads, and
 * connection details.
 * </p>
 *
 * <p>
 * <b>Key responsibilities:</b>
 * </p>
 * <ul>
 * <li>Request logging (headers, method, URI, payload)</li>
 * <li>Response logging (status, content)</li>
 * <li>Hex dump generation for binary payloads</li>
 * <li>Debug-level logging control</li>
 * <li>Request/response wrapping for logging</li>
 * <li>Error handling and recovery</li>
 * </ul>
 *
 * <p>
 * <b>Component Integration:</b>
 * </p>
 * <ul>
 * <li>{@link HomekitRequestWrapper} for request wrapping and logging</li>
 * <li>{@link HomekitResponseWrapper} for response wrapping and logging</li>
 * <li>{@link HomekitHttpParser} for parsing HTTP messages</li>
 * <li>{@link HomekitHttpGenerator} for generating HTTP messages</li>
 * <li>{@link HomekitHttpSender} for sending HTTP messages</li>
 * <li>{@link HomekitHttpReceiver} for receiving HTTP messages</li>
 * <li>{@link HomekitHttpConnection} for managing HTTP connections</li>
 * </ul>
 *
 * <p>
 * <b>Implementation Details:</b>
 * </p>
 * <ul>
 * <li>Uses {@link HexDump} for binary payload visualization</li>
 * <li>Uses {@link IOUtils} for efficient stream handling</li>
 * <li>Uses {@link ByteArrayOutputStream} for content buffering</li>
 * <li>Implements debug and trace level logging</li>
 * <li>Provides hex dump generation for binary content</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
     */
public class HomekitRequestLogHandler extends RequestLogHandler {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitRequestLogHandler.class);

    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit RequestLogHandler: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_REQUEST = LOG_PREFIX + "Request - ";
    protected static final String LOG_RESPONSE = LOG_PREFIX + "Response - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    /**
     * Creates a new HomeKit request log handler.
     *
     * <p>
     * This constructor initializes the handler with default settings for
     * logging HomeKit HTTP communication. It works in conjunction with
     * {@link HomekitRequestWrapper} and {@link HomekitResponseWrapper} to
     * provide comprehensive request/response logging.
     * </p>
     *
     * <p>
     * <b>Key initialization steps:</b>
     * </p>
     * <ul>
     * <li>Sets up logging prefixes</li>
     * <li>Initializes handler state</li>
     * <li>Configures debug logging</li>
     * </ul>
     */
    public HomekitRequestLogHandler() {
        logger.debug("{}Initializing HomeKit request log handler", LOG_INIT);
    }

    /**
     * Handles and logs HTTP requests and responses.
     *
     * <p>
     * This method processes incoming requests and outgoing responses, providing
     * detailed logging of all aspects of the HTTP communication. When debug
     * logging is enabled, it logs:
     * </p>
     * <ul>
     * <li>Request details (headers, method, URI, query parameters)</li>
     * <li>Request payload in hex dump format</li>
     * <li>Response status and content</li>
     * <li>Connection information</li>
     * </ul>
     *
     * <p>
     * <b>Implementation details:</b>
     * </p>
     * <ul>
     * <li>Wraps requests and responses for logging</li>
     * <li>Generates hex dumps for binary payloads</li>
     * <li>Maintains original request/response handling</li>
     * <li>Controls logging verbosity based on debug level</li>
     * </ul>
     *
     * @param target The target of the request
     * @param baseRequest The base Jetty request
     * @param request The HTTP servlet request
     * @param response The HTTP servlet response
     * @throws IOException if an I/O error occurs
     * @throws ServletException if a servlet error occurs
     */
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        if (logger.isDebugEnabled()) {
            try {
                logger.debug("{}Processing request for target: {}", LOG_REQUEST, target);
                final HomekitRequestWrapper wrappedRequest = new HomekitRequestWrapper(request);
                HomekitResponseWrapper wrappedResponse = new HomekitResponseWrapper(response);

                logRequestDetails(wrappedRequest);
                handleRequest(target, baseRequest, wrappedRequest, wrappedResponse);
                logResponseDetails(wrappedResponse);
                logger.debug("{}Request processing completed successfully", LOG_STATE);
            } catch (Exception e) {
                logger.error("{}Error handling request: {}", LOG_ERROR, e.getMessage(), e);
                throw e;
            }
        } else {
            if (_handler != null) {
                _handler.handle(target, baseRequest, request, response);
            }
        }
    }

    /**
     * Logs detailed information about the incoming request.
     *
     * <p>
     * This method uses {@link IOUtils} to efficiently read the request body
     * and {@link HexDump} to generate readable hex dumps of binary content.
     * It logs comprehensive request details including:
     * </p>
     * <ul>
     * <li>Client information (IP, port)</li>
     * <li>Request headers</li>
     * <li>Request method and URI</li>
     * <li>Query parameters</li>
     * <li>Request payload (with hex dump)</li>
     * </ul>
     *
     * @param request The wrapped HTTP request
     * @throws IOException if an I/O error occurs while reading the request
     */
    private void logRequestDetails(HomekitRequestWrapper request) throws IOException {
        final String userAgent = request.getHeader("User-Agent");
        logger.debug("{}Processing request from {}:{}", LOG_REQUEST, request.getRemoteAddr(), request.getRemotePort());
        logger.debug("{}User-Agent: {}", LOG_REQUEST, userAgent);
        logger.debug("{}Method: {}", LOG_REQUEST, request.getMethod().toUpperCase());
        logger.debug("{}Content-Type: {}", LOG_REQUEST, request.getContentType());
        logger.debug("{}URI: {}", LOG_REQUEST, request.getRequestURI());
        logger.debug("{}Query: {}", LOG_REQUEST, request.getQueryString());

        byte[] body = IOUtils.toByteArray(request.getInputStream());
        if (body.length > 0) {
            logger.debug("{}Payload size: {} bytes", LOG_REQUEST, body.length);
            if (logger.isTraceEnabled()) {
                try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                    HexDump.dump(body, 0, stream, 0);
                    stream.flush();
                    logger.trace("{}Payload hex dump:\n{}", LOG_REQUEST,
                            stream.toString(StandardCharsets.UTF_8.name()));
                }
            }
        } else {
            logger.debug("{}No request payload", LOG_REQUEST);
        }
    }

    /**
     * Handles the request by delegating to the next handler in the chain.
     *
     * <p>
     * This method works with {@link HomekitRequestWrapper} and {@link HomekitResponseWrapper}
     * to ensure proper request/response handling while maintaining logging capabilities.
     * It delegates the actual request processing to the next handler in the chain.
     * </p>
     *
     * @param target The target of the request
     * @param baseRequest The base Jetty request
     * @param request The wrapped HTTP request
     * @param response The wrapped HTTP response
     * @throws IOException if an I/O error occurs
     * @throws ServletException if a servlet error occurs
     */
    private void handleRequest(String target, Request baseRequest, HomekitRequestWrapper request,
            HomekitResponseWrapper response) throws IOException, ServletException {
        if (_handler != null) {
            logger.debug("{}Delegating request to next handler", LOG_STATE);
            _handler.handle(target, baseRequest, request, response);
        } else {
            logger.warn("{}No handler available for request", LOG_WARN);
        }
    }

    /**
     * Logs detailed information about the outgoing response.
     *
     * <p>
     * This method uses {@link HexDump} to generate readable hex dumps of binary
     * response content when trace logging is enabled. It logs:
     * </p>
     * <ul>
     * <li>Response status code</li>
     * <li>Response content size</li>
     * <li>Response content (with hex dump)</li>
     * </ul>
     *
     * @param response The wrapped HTTP response
     * @throws IOException if an I/O error occurs while reading the response
     */
    private void logResponseDetails(HomekitResponseWrapper response) throws IOException {
        logger.debug("{}Response status: {}", LOG_RESPONSE, response.getStatus());

        byte[] content = response.getContentAsByteArray();
        if (content.length > 0) {
            logger.debug("{}Response size: {} bytes", LOG_RESPONSE, content.length);
            if (logger.isTraceEnabled()) {
                try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                    HexDump.dump(content, 0, stream, 0);
                    stream.flush();
                    logger.trace("{}Response hex dump:\n{}", LOG_RESPONSE,
                            stream.toString(StandardCharsets.UTF_8.name()));
                }
            }
        } else {
            logger.debug("{}No response content", LOG_RESPONSE);
        }

        response.copyBodyToResponse();
        logger.debug("{}Response body copied to original response", LOG_RESPONSE);
    }
}
