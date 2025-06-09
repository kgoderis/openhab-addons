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
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.ProtocolHandler;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Response.Listener;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.openhab.io.homekit.server.HomekitRemoteAccessoryServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles HomeKit event protocol communication.
 *
 * <p>
 * This class implements a custom protocol handler for HomeKit event communication,
 * extending Jetty's {@link ProtocolHandler} to support HomeKit-specific event handling.
 * It manages the processing of HomeKit events through HTTP responses and provides
 * buffering capabilities for event content.
 * </p>
 *
 * <p>
 * <b>Key responsibilities:</b>
 * </p>
 * <ul>
 * <li>Protocol identification and acceptance</li>
 * <li>Response buffering and processing</li>
 * <li>Event forwarding to the HomeKit server</li>
 * <li>Content buffering management</li>
 * <li>Error handling and recovery</li>
 * </ul>
 *
 * <p>
 * <b>Component Integration:</b>
 * </p>
 * <ul>
 * <li>{@link HomekitRemoteAccessoryServer} for event processing</li>
 * <li>{@link BufferingResponseListener} for response buffering</li>
 * <li>{@link org.eclipse.jetty.client.api.Request Request} for request handling</li>
 * <li>{@link org.eclipse.jetty.client.api.Response Response} for response handling</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@NonNullByDefault
public class HomekitProtocolHandler implements ProtocolHandler {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitProtocolHandler.class);

    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit HomekitProtocolHandler: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    protected HomekitRemoteAccessoryServer server;

    /**
     * Creates a new HomeKit protocol handler.
     *
     * <p>
     * This constructor initializes the protocol handler with a reference to the
     * HomeKit remote accessory server that will process the events. The handler
     * is responsible for managing the communication between the client and the
     * HomeKit server.
     * </p>
     *
     * @param server The HomeKit remote accessory server instance that will process events
     * @throws IllegalArgumentException if server is null
     */
    public HomekitProtocolHandler(HomekitRemoteAccessoryServer server) {
        this.server = server;
        logger.debug("{}Initialized with server instance", LOG_INIT);
    }

    /**
     * Gets the name of this protocol handler.
     *
     * <p>
     * This method returns the protocol identifier used to identify HomeKit event
     * communication. The name is used by the Jetty client to route responses
     * to the appropriate handler.
     * </p>
     *
     * @return The protocol name "homekit.event"
     */
    @Override
    public String getName() {
        return "homekit.event";
    }

    /**
     * Determines if this handler can process the given request/response pair.
     *
     * <p>
     * This method checks if the response contains the HomeKit event header,
     * indicating that it should be processed by this handler. The header
     * "X-HOMEKIT-EVENT" with value "True" identifies HomeKit event responses.
     * </p>
     *
     * @param request The HTTP request to check
     * @param response The HTTP response to check
     * @return true if the response contains the HomeKit event header
     */
    @Override
    @SuppressWarnings("null") // Parent ProtocolHandler interface doesn't constrain these parameters
    public boolean accept(@Nullable Request request, @Nullable Response response) {
        if (request == null || response == null) {
            return false;
        }
        boolean accepted = response.getHeaders().contains("X-HOMEKIT-EVENT", "True");
        if (accepted) {
            logger.debug("{}Accepted HomeKit event response", LOG_STATE);
        }
        return accepted;
    }

    /**
     * Gets the response listener for buffering and processing responses.
     *
     * <p>
     * This method creates a new response listener with a maximum buffer size
     * of 8MB for handling HomeKit event content. The listener is responsible
     * for buffering the response content and forwarding it to the HomeKit server.
     * </p>
     *
     * @return A new HomekitResponseListener instance configured for event handling
     */
    @Override
    public Listener getResponseListener() {
        logger.debug("{}Creating response listener with 8MB buffer", LOG_STATE);
        return new HomekitResponseListener(8 * 1024 * 1024);
    }

    /**
     * Custom response listener for handling HomeKit events.
     *
     * <p>
     * This inner class extends {@link BufferingResponseListener} to provide specialized
     * handling of HomeKit event responses. It manages content buffering and
     * event processing, ensuring reliable delivery of events to the HomeKit server.
     * </p>
     *
     * <p>
     * <b>Key features:</b>
     * </p>
     * <ul>
     * <li>Configurable buffer size</li>
     * <li>Event content buffering</li>
     * <li>Success/failure handling</li>
     * <li>Completion notification</li>
     * </ul>
     */
    protected class HomekitResponseListener extends BufferingResponseListener {

        /**
         * Creates a new HomeKit response listener.
         *
         * <p>
         * This constructor initializes the listener with the specified maximum
         * content length for buffering. The buffer size should be sufficient to
         * handle the largest expected event payload.
         * </p>
         *
         * @param maxLength The maximum content length to buffer in bytes
         */
        public HomekitResponseListener(int maxLength) {
            super(maxLength);
            logger.debug("{}Created response listener with max length: {} bytes", LOG_INIT, maxLength);
        }

        /**
         * Handles successful response processing.
         *
         * <p>
         * This method is called when a response is successfully received and
         * buffered. It forwards the buffered content to the HomeKit server
         * for event processing.
         * </p>
         *
         * @param response The HTTP response that was successfully processed
         */
        @Override
        @SuppressWarnings("null") // Parent ProtocolHandler interface doesn't constrain these parameters
        public void onSuccess(@Nullable Response response) {
            logger.debug("{}Processing successful response", LOG_STATE);
            try {
                server.handleEvent(getContent());
                logger.debug("{}Successfully forwarded event to server", LOG_STATE);
            } catch (Exception e) {
                logger.error("{}Failed to process event: {}", LOG_ERROR, e.getMessage(), e);
            }
        }

        /**
         * Handles response completion.
         *
         * <p>
         * This method is called when the response processing is complete,
         * regardless of success or failure. It provides an opportunity to
         * perform cleanup or logging of the final result.
         * </p>
         *
         * @param result The result of the response processing
         */
        @Override
        @SuppressWarnings("null") // Parent ProtocolHandler interface doesn't constrain these parameters
        public void onComplete(@Nullable Result result) {
            if (result != null && result.isFailed()) {
                logger.error("{}Response processing failed: {}", LOG_ERROR, result.getFailure().getMessage());
            } else {
                logger.debug("{}Response processing completed successfully", LOG_STATE);
            }
        }
    }
}
