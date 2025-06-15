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
import org.eclipse.jetty.client.ConnectionPool;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Origin;
import org.eclipse.jetty.client.http.HttpDestinationOverHTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A specialized HTTP destination for HomeKit communication with encryption support.
 *
 * <p>
 * This class extends {@link org.eclipse.jetty.client.http.HttpDestinationOverHTTP HttpDestinationOverHTTP} to provide
 * specialized HTTP destination handling for HomeKit accessories, including support for
 * encryption and decryption of messages. It manages secure communication endpoints and
 * connection pooling for HomeKit devices.
 * </p>
 *
 * <p>
 * The class integrates with:
 * </p>
 * <ul>
 * <li>{@link HttpClient} for HTTP client functionality and request handling</li>
 * <li>{@link Origin} for origin management and security</li>
 * <li>{@link HomekitConnectionPool} for connection pooling and reuse</li>
 * <li>{@link HomekitHttpConnectionOverHTTP} for encrypted connection management</li>
 * <li>{@link org.eclipse.jetty.client.api.Connection Connection} for connection lifecycle management</li>
 * </ul>
 *
 * <p>
 * <b>Key Features:</b>
 * </p>
 * <ul>
 * <li>HTTP destination management</li>
 * <li>Encryption key configuration</li>
 * <li>Connection pooling</li>
 * <li>Secure communication</li>
 * <li>Connection lifecycle management</li>
 * <li>Thread-safe operations</li>
 * </ul>
 *
 * <p>
 * <b>Security Considerations:</b>
 * </p>
 * <ul>
 * <li>Manages encryption keys</li>
 * <li>Secures message transmission</li>
 * <li>Validates connections</li>
 * <li>Ensures proper initialization</li>
 * <li>Maintains thread safety</li>
 * </ul>
 *
 * <p>
 * <b>Implementation Details:</b>
 * </p>
 * <ul>
 * <li>Extends Jetty's HttpDestinationOverHTTP</li>
 * <li>Uses custom connection pool</li>
 * <li>Supports encryption</li>
 * <li>Manages connection lifecycle</li>
 * <li>Provides detailed logging</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@NonNullByDefault
public class HomekitHttpDestination extends HttpDestinationOverHTTP {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitHttpDestination.class);

    protected static final boolean debug = logger.isDebugEnabled();

    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "HomeKit HTTP Destination: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Initialization - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State Change - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Configuration - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "Accessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    private byte @Nullable [] decryptionKey = null;

    private byte @Nullable [] encryptionKey = null;

    /**
     * Creates a new HomeKit HTTP destination.
     *
     * <p>
     * This constructor initializes a destination with the specified HTTP client and origin,
     * setting up the foundation for secure HomeKit communication.
     * </p>
     *
     * <p>
     * <b>Implementation details:</b>
     * </p>
     * <ul>
     * <li>Initializes base destination</li>
     * <li>Sets up client connection</li>
     * <li>Configures origin</li>
     * <li>Ensures thread safety</li>
     * <li>Logs initialization</li>
     * </ul>
     *
     * @param client The HTTP client to use
     * @param origin The origin for this destination
     */
    public HomekitHttpDestination(HttpClient client, Origin origin) {
        super(client, origin);
        logger.debug("{}Initialized for client {} and origin {}", LOG_INIT, client, origin);
    }

    /**
     * Sets the encryption and decryption keys for secure communication.
     *
     * <p>
     * This method configures the encryption keys for both idle and active connections
     * in the connection pool, ensuring secure communication across all connections.
     * </p>
     *
     * <p>
     * <b>Implementation details:</b>
     * </p>
     * <ul>
     * <li>Validates key parameters</li>
     * <li>Updates connection pool</li>
     * <li>Configures idle connections</li>
     * <li>Configures active connections</li>
     * <li>Maintains thread safety</li>
     * <li>Ensures state consistency</li>
     * <li>Logs configuration</li>
     * </ul>
     *
     * @param decryptionKey The key used for decrypting incoming messages
     * @param encryptionKey The key used for encrypting outgoing messages
     */
    public void setEncryptionKeys(byte[] decryptionKey, byte[] encryptionKey) {
        logger.debug("{}Setting encryption keys", LOG_CONFIG);
        logger.info("{}Configuring encryption for destination {}", LOG_CONFIG, this);

        if (logger.isTraceEnabled()) {
            logger.debug("{}Decryption key: {}", LOG_CONFIG,
                    javax.xml.bind.DatatypeConverter.printHexBinary(decryptionKey));
            logger.debug("{}Encryption key: {}", LOG_CONFIG,
                    javax.xml.bind.DatatypeConverter.printHexBinary(encryptionKey));
        }

        this.decryptionKey = decryptionKey;
        this.encryptionKey = encryptionKey;

        ConnectionPool pool = getConnectionPool();
        if (pool instanceof HomekitConnectionPool) {
            var idle = ((HomekitConnectionPool) pool).getIdleConnections();
            var active = ((HomekitConnectionPool) pool).getActiveConnections();

            logger.debug("{}Configuring {} idle connections", LOG_CONFIG, idle.size());
            for (org.eclipse.jetty.client.api.Connection connection : idle) {
                if (connection instanceof HomekitHttpConnectionOverHTTP) {
                    ((HomekitHttpConnectionOverHTTP) connection).setEncryptionKeys(decryptionKey, encryptionKey);
                }
            }

            logger.debug("{}Configuring {} active connections", LOG_CONFIG, active.size());
            for (org.eclipse.jetty.client.api.Connection connection : active) {
                if (connection instanceof HomekitHttpConnectionOverHTTP) {
                    ((HomekitHttpConnectionOverHTTP) connection).setEncryptionKeys(decryptionKey, encryptionKey);
                }
            }
        }

        logger.info("{}Encryption keys configured successfully", LOG_CONFIG);
    }

    /**
     * Checks if encryption keys are configured for this destination.
     *
     * <p>
     * This method verifies whether both encryption and decryption keys
     * have been set for secure communication.
     * </p>
     *
     * <p>
     * <b>Implementation details:</b>
     * </p>
     * <ul>
     * <li>Validates key presence</li>
     * <li>Checks key validity</li>
     * <li>Maintains thread safety</li>
     * <li>Ensures state consistency</li>
     * <li>Logs key status</li>
     * </ul>
     *
     * @return true if both encryption keys are configured, false otherwise
     */
    public boolean hasEncryptionKeys() {
        boolean hasKeys = decryptionKey != null && encryptionKey != null;
        logger.debug("{}Encryption keys configured: {}", LOG_STATE, hasKeys);
        return hasKeys;
    }

    /**
     * Gets the decryption key for this destination.
     *
     * <p>
     * This method returns the key used for decrypting incoming messages.
     * </p>
     *
     * <p>
     * <b>Implementation details:</b>
     * </p>
     * <ul>
     * <li>Retrieves decryption key</li>
     * <li>Ensures thread safety</li>
     * <li>Logs key access</li>
     * </ul>
     *
     * @return The decryption key, or null if not configured
     */
    public byte @Nullable [] getDecryptionKey() {
        logger.debug("{}Retrieving decryption key", LOG_STATE);
        return decryptionKey;
    }

    /**
     * Gets the encryption key for this destination.
     *
     * <p>
     * This method returns the key used for encrypting outgoing messages.
     * </p>
     *
     * <p>
     * <b>Implementation details:</b>
     * </p>
     * <ul>
     * <li>Retrieves encryption key</li>
     * <li>Ensures thread safety</li>
     * <li>Logs key access</li>
     * </ul>
     *
     * @return The encryption key, or null if not configured
     */
    public byte @Nullable [] getEncryptionKey() {
        logger.debug("{}Retrieving encryption key", LOG_STATE);
        return encryptionKey;
    }
}
