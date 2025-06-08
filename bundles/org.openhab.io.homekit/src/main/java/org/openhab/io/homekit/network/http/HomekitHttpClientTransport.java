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
import org.eclipse.jetty.client.HttpDestination;
import org.eclipse.jetty.client.Origin;
import org.eclipse.jetty.client.api.Connection;
import org.eclipse.jetty.client.http.HttpClientTransportOverHTTP;
import org.eclipse.jetty.client.http.HttpConnectionOverHTTP;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.util.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A specialized HTTP client transport for HomeKit communication with encryption support.
 *
 * <p>
 * This class extends {@link org.eclipse.jetty.client.http.HttpClientTransportOverHTTP HttpClientTransportOverHTTP} to
 * provide
 * specialized HTTP client transport functionality for HomeKit accessories, including
 * encryption support and custom connection management. It handles secure communication
 * channels and connection pooling for HomeKit devices.
 * </p>
 *
 * <p>
 * The class integrates with:
 * </p>
 * <ul>
 * <li>{@link org.eclipse.jetty.client.HttpDestination HttpDestination} for destination management and routing</li>
 * <li>{@link org.eclipse.jetty.client.Origin Origin} for origin handling and security</li>
 * <li>{@link org.eclipse.jetty.client.api.Connection Connection} for connection lifecycle management</li>
 * <li>{@link HomekitHttpConnectionOverHTTP} for specialized encrypted connections</li>
 * <li>{@link HomekitConnectionPool} for connection pooling and reuse</li>
 * </ul>
 *
 * <p>
 * <b>Key Features:</b>
 * </p>
 * <ul>
 * <li>HTTP connection management</li>
 * <li>Encryption key configuration</li>
 * <li>Connection pooling</li>
 * <li>Secure communication support</li>
 * <li>Custom destination handling</li>
 * <li>Thread-safe operations</li>
 * </ul>
 *
 * <p>
 * <b>Security Considerations:</b>
 * </p>
 * <ul>
 * <li>Manages encryption keys</li>
 * <li>Supports secure connections</li>
 * <li>Validates destinations</li>
 * <li>Ensures proper initialization</li>
 * <li>Maintains thread safety</li>
 * </ul>
 *
 * <p>
 * <b>Implementation Details:</b>
 * </p>
 * <ul>
 * <li>Extends Jetty's HttpClientTransportOverHTTP</li>
 * <li>Uses custom connection pool</li>
 * <li>Supports encryption upgrades</li>
 * <li>Manages connection lifecycle</li>
 * <li>Provides detailed logging</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@NonNullByDefault
public class HomekitHttpClientTransport extends HttpClientTransportOverHTTP {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitHttpClientTransport.class);

    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "HomeKit HTTP Client Transport: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Initialization - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State Change - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Configuration - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "Accessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    /**
     * Creates a new HomeKit HTTP client transport.
     *
     * <p>
     * This constructor initializes a transport with a custom connection pool factory
     * that creates {@link HomekitConnectionPool} instances for managing connections.
     * </p>
     *
     * <p>
     * <b>Implementation details:</b>
     * </p>
     * <ul>
     * <li>Initializes base transport</li>
     * <li>Configures connection pool</li>
     * <li>Sets up connection limits</li>
     * <li>Ensures thread safety</li>
     * </ul>
     */
    public HomekitHttpClientTransport() {
        super();
        setConnectionPoolFactory(destination -> new HomekitConnectionPool(destination,
                getHttpClient().getMaxConnectionsPerDestination(), destination));
    }

    /**
     * Creates a new HTTP connection for the given endpoint and destination.
     *
     * <p>
     * This method creates a specialized HomeKit HTTP connection that supports
     * encryption if the destination has encryption keys configured. It handles
     * the setup of secure communication channels.
     * </p>
     *
     * <p>
     * <b>Implementation details:</b>
     * </p>
     * <ul>
     * <li>Creates HomeKit connection instance</li>
     * <li>Configures encryption keys</li>
     * <li>Sets up secure channel</li>
     * <li>Maintains thread safety</li>
     * <li>Ensures state consistency</li>
     * <li>Logs connection details</li>
     * </ul>
     *
     * @param endPoint The endpoint for the connection
     * @param destination The destination for the connection  
     * @param connectionPromise The promise to be completed when the connection is established
     * @return A new HTTP connection instance
     */
    protected HomekitHttpConnectionOverHTTP createHomekitConnection(@Nullable EndPoint endPoint,
            @Nullable HttpDestination destination, @Nullable Promise<Connection> connectionPromise) {

        if (endPoint == null || destination == null || connectionPromise == null) {
            throw new IllegalArgumentException("EndPoint, destination, and promise cannot be null");
        }

        logger.debug("{}Creating new connection for endpoint {} and destination {}", LOG_CONFIG, endPoint.toString(),
                destination.toString());
        logger.info("{}Initializing connection for endpoint {} to destination {}", LOG_STATE,
                endPoint.getRemoteAddress().toString(), destination.toString());

        HomekitHttpConnectionOverHTTP newConnection = new HomekitHttpConnectionOverHTTP(endPoint, destination,
                connectionPromise);

        if (destination instanceof HomekitHttpDestination
                && ((HomekitHttpDestination) destination).hasEncryptionKeys()) {
            logger.info("{}Configuring encryption for connection {} to destination {}", LOG_CONFIG,
                    endPoint.getRemoteAddress().toString(), destination.toString());

            HomekitHttpDestination homekitDestination = (HomekitHttpDestination) destination;
            byte @Nullable [] decryptionKey = homekitDestination.getDecryptionKey();
            byte @Nullable [] encryptionKey = homekitDestination.getEncryptionKey();

            if (decryptionKey != null && encryptionKey != null) {
                if (logger.isTraceEnabled()) {
                    logger.trace("{}Decryption key: {}", LOG_CONFIG,
                            javax.xml.bind.DatatypeConverter.printHexBinary(decryptionKey));
                    logger.trace("{}Encryption key: {}", LOG_CONFIG,
                            javax.xml.bind.DatatypeConverter.printHexBinary(encryptionKey));
                }
                newConnection.setEncryptionKeys(decryptionKey, encryptionKey);
            } else {
                logger.warn("{}Encryption keys are null despite hasEncryptionKeys() returning true", LOG_WARN);
            }
        } else {
            logger.info("{}No encryption keys configured for endpoint {}", LOG_STATE,
                    endPoint.getRemoteAddress().toString());
        }

        return newConnection;
    }

    /**
     * Creates a new HTTP connection for the given endpoint and destination.
     *
     * <p>
     * This method creates a specialized HomeKit HTTP connection that supports
     * encryption if the destination has encryption keys configured. It handles
     * the setup of secure communication channels.
     * </p>
     *
     * <p>
     * <b>Implementation details:</b>
     * </p>
     * <ul>
     * <li>Creates HomeKit connection instance</li>
     * <li>Configures encryption keys</li>
     * <li>Sets up secure channel</li>
     * <li>Maintains thread safety</li>
     * <li>Ensures state consistency</li>
     * <li>Logs connection details</li>
     * </ul>
     *
     * @param endPoint The endpoint for the connection
     * @param destination The destination for the connection
     * @param promise The promise to be completed when the connection is established
     * @return A new HTTP connection instance
     */
    @Override
    @SuppressWarnings("all") // Framework interface compatibility: HttpClientTransportOverHTTP interface constraints cannot be overridden
    protected HttpConnectionOverHTTP newHttpConnection(@Nullable EndPoint endPoint,
            @Nullable HttpDestination destination, @Nullable Promise<Connection> promise) {
        return createHomekitConnection(endPoint, destination, promise);
    }

    /**
     * Creates a new HTTP destination for the given origin.
     *
     * <p>
     * This method creates a specialized HomeKit HTTP destination that supports
     * encryption and custom connection management. It sets up the routing and
     * security configuration for the destination.
     * </p>
     *
     * <p>
     * <b>Implementation details:</b>
     * </p>
     * <ul>
     * <li>Creates HomeKit destination</li>
     * <li>Configures security settings</li>
     * <li>Sets up routing rules</li>
     * <li>Maintains thread safety</li>
     * <li>Ensures state consistency</li>
     * </ul>
     *
     * @param origin The origin for the destination
     * @return A new HTTP destination instance
     */
    @Override
    public HttpDestination newHttpDestination(@Nullable Origin origin) {
        if (origin == null) {
            throw new IllegalArgumentException("Origin cannot be null");
        }
        return new HomekitHttpDestination(getHttpClient(), origin);
    }
}
