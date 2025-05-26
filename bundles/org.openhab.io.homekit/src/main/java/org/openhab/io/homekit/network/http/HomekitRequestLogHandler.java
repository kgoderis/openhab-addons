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
 * This class extends Jetty's {@link RequestLogHandler} to provide comprehensive logging
 * of HomeKit HTTP communication. It captures and logs detailed information about
 * incoming requests and outgoing responses, including headers, payloads, and
 * connection details.
 *
 * The class integrates with:
 * - {@link HomekitRequestWrapper} for request wrapping and logging
 * - {@link HomekitResponseWrapper} for response wrapping and logging
 * - {@link HomekitHttpParser} for parsing HTTP messages
 * - {@link HomekitHttpGenerator} for generating HTTP messages
 * - {@link HomekitHttpSender} for sending HTTP messages
 * - {@link HomekitHttpReceiver} for receiving HTTP messages
 * - {@link HomekitHttpConnection} for managing HTTP connections
 *
 * Key responsibilities:
 * 1. Request logging (headers, method, URI, payload)
 * 2. Response logging (status, content)
 * 3. Hex dump generation for binary payloads
 * 4. Debug-level logging control
 *
 * The logging implementation uses:
 * - {@link HexDump} for binary payload visualization
 * - {@link IOUtils} for efficient stream handling
 * - {@link ByteArrayOutputStream} for content buffering
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0
 */
public class HomekitRequestLogHandler extends RequestLogHandler {

    // ========== Log Message Prefixes ==========
    protected static final Logger logger = LoggerFactory.getLogger(HomekitRequestLogHandler.class);
    protected static final String LOG_PREFIX = "Homekit RequestLogHandler: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_REQUEST = LOG_PREFIX + "Request - ";
    protected static final String LOG_RESPONSE = LOG_PREFIX + "Response - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";

    /**
     * Creates a new HomeKit request log handler.
     *
     * This constructor initializes the handler with default settings for
     * logging HomeKit HTTP communication. It works in conjunction with
     * {@link HomekitRequestWrapper} and {@link HomekitResponseWrapper} to
     * provide comprehensive request/response logging.
     */
    public HomekitRequestLogHandler() {
        logger.debug("{}Initializing HomeKit request log handler", LOG_INIT);
    }

    /**
     * Handles and logs HTTP requests and responses.
     *
     * This method processes incoming requests and outgoing responses, providing
     * detailed logging of all aspects of the HTTP communication. When debug
     * logging is enabled, it logs:
     * - Request details (headers, method, URI, query parameters)
     * - Request payload in hex dump format
     * - Response status and content
     * - Connection information
     *
     * Key implementation details:
     * - Wraps requests and responses for logging using {@link HomekitRequestWrapper} and {@link HomekitResponseWrapper}
     * - Generates hex dumps for binary payloads using {@link HexDump}
     * - Maintains original request/response handling
     * - Controls logging verbosity based on debug level
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
                final HomekitRequestWrapper wrappedRequest = new HomekitRequestWrapper(request);
                HomekitResponseWrapper wrappedResponse = new HomekitResponseWrapper(response);

                logRequestDetails(wrappedRequest);
                handleRequest(target, baseRequest, wrappedRequest, wrappedResponse);
                logResponseDetails(wrappedResponse);
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
     * This method uses {@link IOUtils} to efficiently read the request body
     * and {@link HexDump} to generate readable hex dumps of binary content.
     *
     * @param request The wrapped HTTP request
     * @throws IOException if an I/O error occurs while reading the request
     */
    private void logRequestDetails(HomekitRequestWrapper request) throws IOException {
        final String userAgent = request.getHeader("User-Agent");
        logger.debug("{}Processing request from {}:{}", LOG_REQUEST, request.getRemoteAddr(),
                request.getRemotePort());
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
        }
    }

    /**
     * Handles the request by delegating to the next handler in the chain.
     *
     * This method works with {@link HomekitRequestWrapper} and {@link HomekitResponseWrapper}
     * to ensure proper request/response handling while maintaining logging capabilities.
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
            _handler.handle(target, baseRequest, request, response);
        }
    }

    /**
     * Logs detailed information about the outgoing response.
     *
     * This method uses {@link HexDump} to generate readable hex dumps of binary
     * response content when trace logging is enabled.
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
        }

        response.copyBodyToResponse();
    }
}
