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
import org.eclipse.jetty.client.api.Connection;
import org.eclipse.jetty.client.http.HttpChannelOverHTTP;
import org.eclipse.jetty.client.http.HttpConnectionOverHTTP;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.util.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP-specific implementation of HomeKit HTTP connections.
 *
 * <p>
 * This class extends {@link HttpConnectionOverHTTP} to provide HTTP-specific
 * connection handling for HomeKit accessories. It implements the connection
 * logic for standard HTTP communication, including request/response handling
 * and connection lifecycle management.
 * </p>
 *
 * <p>
 * <b>Component Integration:</b>
 * </p>
 * <ul>
 * <li>{@link HomekitHttpChannel} for channel management</li>
 * <li>{@link HttpDestination} for connection configuration</li>
 * <li>{@link org.eclipse.jetty.client.HttpClient HttpClient} for HTTP operations</li>
 * </ul>
 *
 * <p>
 * <b>Key responsibilities:</b>
 * </p>
 * <ul>
 * <li>Managing HTTP-specific connection lifecycle</li>
 * <li>Handling HTTP request/response cycles</li>
 * <li>Supporting HTTP protocol features</li>
 * <li>Managing connection state</li>
 * <li>Handling encryption keys for secure communication</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@NonNullByDefault
public class HomekitHttpConnectionOverHTTP extends HttpConnectionOverHTTP {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitHttpConnectionOverHTTP.class);

    protected static final String LOG_PREFIX = "Homekit HttpConnectionOverHTTP: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    private byte @Nullable [] decryptionKey = null;
    private byte @Nullable [] encryptionKey = null;

    /**
     * Creates a new HTTP-specific HomeKit connection.
     *
     * <p>
     * This constructor initializes a connection with the specified endpoint,
     * destination, and connection promise.
     * </p>
     *
     * @param endPoint The connection endpoint
     * @param destination The HTTP destination
     * @param promise The connection promise
     */
    @SuppressWarnings("null") // Framework type Promise<Connection> has incomplete null annotations
    public HomekitHttpConnectionOverHTTP(EndPoint endPoint, HttpDestination destination, Promise<Connection> promise) {
        super(endPoint, destination, promise);
        logger.debug("{}Created new HTTP connection to {}", LOG_INIT, endPoint.getRemoteAddress());
    }

    /**
     * Creates a new HTTP channel for this connection.
     *
     * <p>
     * This method creates a specialized HTTP channel for HomeKit communication
     * over HTTP.
     * </p>
     *
     * @return A new HomekitHttpChannel instance
     */
    @Override
    protected HomekitHttpChannel newHttpChannel() {
        logger.debug("{}Creating new HTTP channel", LOG_INIT);
        return new HomekitHttpChannel(this);
    }

    /**
     * Gets the number of messages received.
     *
     * <p>
     * This method returns the total count of messages received through this connection.
     * </p>
     *
     * @return The number of messages received
     */
    @Override
    public long getMessagesIn() {
        return ((HomekitHttpChannel) getHttpChannel()).getMessagesIn();
    }

    /**
     * Gets the number of messages sent.
     *
     * <p>
     * This method returns the total count of messages sent through this connection.
     * </p>
     *
     * @return The number of messages sent
     */
    @Override
    public long getMessagesOut() {
        return ((HomekitHttpChannel) getHttpChannel()).getMessagesOut();
    }

    /**
     * Adds received bytes to the connection statistics.
     *
     * <p>
     * This method updates the connection's byte count for received data.
     * </p>
     *
     * @param bytesIn The number of bytes received
     */
    @Override
    protected void addBytesIn(long bytesIn) {
        super.addBytesIn(bytesIn);
    }

    /**
     * Gets the number of bytes sent.
     *
     * <p>
     * This method returns the total number of bytes sent through this connection.
     * </p>
     *
     * @return The number of bytes sent
     */
    @Override
    public long getBytesOut() {
        return super.getBytesOut();
    }

    /**
     * Adds sent bytes to the connection statistics.
     *
     * <p>
     * This method updates the connection's byte count for sent data.
     * </p>
     *
     * @param bytesOut The number of bytes sent
     */
    @Override
    protected void addBytesOut(long bytesOut) {
        super.addBytesOut(bytesOut);
    }

    /**
     * Closes the connection with an optional failure reason.
     *
     * <p>
     * This method handles the graceful shutdown of the connection, logging
     * any failure reason if provided.
     * </p>
     *
     * @param failure The failure reason, or null if none
     */
    @Override
    public void close(@Nullable Throwable failure) {
        if (failure != null) {
            logger.error("{}Closing connection due to failure: {}", LOG_ERROR, failure.getMessage());
        } else {
            logger.debug("{}Closing connection", LOG_STATE);
        }
        super.close(failure);
    }

    /**
     * Sets the encryption keys for secure communication.
     *
     * <p>
     * This method configures the encryption and decryption keys used for
     * secure communication over this connection.
     * </p>
     *
     * <p>
     * <b>Key implementation details:</b>
     * </p>
     * <ul>
     * <li>Updates encryption keys for the connection</li>
     * <li>Configures the associated HTTP channel</li>
     * <li>Logs key information at appropriate levels</li>
     * </ul>
     *
     * @param decryptionKey The key used for decrypting incoming messages
     * @param encryptionKey The key used for encrypting outgoing messages
     */
    public void setEncryptionKeys(byte[] decryptionKey, byte[] encryptionKey) {
        logger.debug("{}Setting encryption keys for {}", LOG_CONFIG, this);
        this.decryptionKey = decryptionKey;
        this.encryptionKey = encryptionKey;

        logger.info("{}Setting encryption keys on {}", LOG_CONFIG, this);
        if (logger.isTraceEnabled()) {
            logger.debug("{}DecryptionKey: {}", LOG_CONFIG,
                    javax.xml.bind.DatatypeConverter.printHexBinary(decryptionKey));
            logger.debug("{}EncryptionKey: {}", LOG_CONFIG,
                    javax.xml.bind.DatatypeConverter.printHexBinary(encryptionKey));
        }

        HttpChannelOverHTTP channel = this.getHttpChannel();
        if (channel instanceof HomekitHttpChannel) {
            ((HomekitHttpChannel) channel).setEncryptionKeys(decryptionKey, encryptionKey);
        }
    }

    /**
     * Checks if encryption keys are set.
     *
     * <p>
     * This method verifies that both encryption and decryption keys are available.
     * </p>
     *
     * @return true if both encryption and decryption keys are set
     */
    public boolean hasEncryptionKeys() {
        return decryptionKey != null && encryptionKey != null;
    }

    /**
     * Gets the decryption key.
     *
     * <p>
     * This method returns the key used for decrypting incoming messages.
     * </p>
     *
     * @return The decryption key, or null if not set
     */
    public byte @Nullable [] getDecryptionKey() {
        return decryptionKey;
    }

    /**
     * Gets the encryption key.
     *
     * <p>
     * This method returns the key used for encrypting outgoing messages.
     * </p>
     *
     * @return The encryption key, or null if not set
     */
    public byte @Nullable [] getEncryptionKey() {
        return encryptionKey;
    }
}
