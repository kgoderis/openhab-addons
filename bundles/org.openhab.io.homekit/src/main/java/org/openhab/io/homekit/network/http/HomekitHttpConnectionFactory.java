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
import org.eclipse.jetty.http.HttpCompliance;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.AbstractConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.server.session.Session;
import org.eclipse.jetty.util.annotation.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating HomeKit HTTP connections.
 *
 * <p>
 * This class manages the creation of HTTP connections for HomeKit communication,
 * providing specialized handling for HomeKit-specific connection requirements
 * and encryption support.
 * </p>
 *
 * <p>
 * <b>Component Integration:</b>
 * </p>
 * <ul>
 * <li>{@link HomekitHttpConnection} for connection management</li>
 * <li>{@link HomekitSessionHandler} for session handling</li>
 * <li>{@link HomekitDecryptedEndPoint} for encrypted endpoints</li>
 * <li>{@link org.eclipse.jetty.server.Connector Connector} for server connections</li>
 * <li>{@link org.eclipse.jetty.server.HttpConfiguration HttpConfiguration} for HTTP settings</li>
 * <li>{@link org.eclipse.jetty.http.HttpCompliance HttpCompliance} for protocol compliance</li>
 * </ul>
 *
 * <p>
 * <b>Key responsibilities:</b>
 * </p>
 * <ul>
 * <li>Creating new HTTP connections</li>
 * <li>Managing connection configuration</li>
 * <li>Supporting connection upgrades</li>
 * <li>Handling session management</li>
 * <li>Providing encryption support</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@NonNullByDefault
public class HomekitHttpConnectionFactory extends AbstractConnectionFactory
        implements HttpConfiguration.ConnectionFactory {

    private static final Logger logger = LoggerFactory.getLogger(HomekitHttpConnectionFactory.class);

    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit HttpConnectionFactory: ";
    private static final String LOG_STATE = LOG_PREFIX + "State - ";

    private final HttpConfiguration config;
    private HttpCompliance httpCompliance;
    private boolean recordHttpComplianceViolations = false;
    private boolean useDirectBuffers = false;
    private @Nullable HomekitSessionHandler sessionHandler;

    /**
     * Creates a new HomeKit HTTP connection factory with a session handler.
     *
     * <p>
     * This constructor initializes a factory with the specified session handler,
     * preparing it for creating HTTP connections with session support.
     * </p>
     *
     * @param sessionHandler The {@link HomekitSessionHandler} for managing connections
     */
    public HomekitHttpConnectionFactory(HomekitSessionHandler sessionHandler) {
        this(new HttpConfiguration());
        this.sessionHandler = sessionHandler;
    }

    /**
     * Creates a new HomeKit HTTP connection factory with configuration.
     *
     * <p>
     * This constructor initializes a factory with the specified HTTP configuration,
     * preparing it for creating HTTP connections.
     * </p>
     *
     * @param config The {@link HttpConfiguration} to use
     */
    public HomekitHttpConnectionFactory(@Name("config") HttpConfiguration config) {
        this(config, null);
    }

    /**
     * Creates a new HomeKit HTTP connection factory with configuration and compliance.
     *
     * <p>
     * This constructor initializes a factory with the specified HTTP configuration
     * and compliance mode, preparing it for creating HTTP connections.
     * </p>
     *
     * @param config The {@link HttpConfiguration} to use
     * @param compliance The {@link HttpCompliance} mode to use
     * @throws IllegalArgumentException if config is null
     */
    public HomekitHttpConnectionFactory(@Name("config") HttpConfiguration config,
            @Name("compliance") @Nullable HttpCompliance compliance) {
        super(HttpVersion.HTTP_1_1.asString(), "HOMEKIT");
        this.config = config;
        httpCompliance = compliance == null ? HttpCompliance.RFC7230 : compliance;
        if (config == null) {
            throw new IllegalArgumentException("Null HttpConfiguration");
        }
        addBean(config);
    }

    /**
     * Sets whether to use direct buffers.
     *
     * <p>
     * This method configures whether the factory should use direct buffers
     * for connection management.
     * </p>
     *
     * @param useDirectBuffers true to use direct buffers, false otherwise
     */
    public void setDirectBuffersF(boolean useDirectBuffers) {
        this.useDirectBuffers = useDirectBuffers;
    }

    /**
     * Checks if direct buffers are being used.
     *
     * <p>
     * This method indicates whether the factory is configured to use
     * direct buffers for connection management.
     * </p>
     *
     * @return true if direct buffers are being used
     */
    public boolean isDirectBuffers() {
        return useDirectBuffers;
    }

    /**
     * Gets the HTTP configuration.
     *
     * <p>
     * This method returns the HTTP configuration used by this factory
     * for creating connections.
     * </p>
     *
     * @return The {@link HttpConfiguration} instance
     */
    @Override
    public HttpConfiguration getHttpConfiguration() {
        return config;
    }

    /**
     * Gets the HTTP compliance mode.
     *
     * <p>
     * This method returns the HTTP compliance mode used by this factory
     * for validating connections.
     * </p>
     *
     * @return The {@link HttpCompliance} mode
     */
    public HttpCompliance getHttpCompliance() {
        return httpCompliance;
    }

    /**
     * Checks if HTTP compliance violations are being recorded.
     *
     * <p>
     * This method indicates whether the factory is configured to record
     * HTTP compliance violations.
     * </p>
     *
     * @return true if violations are being recorded
     */
    public boolean isRecordHttpComplianceViolations() {
        return recordHttpComplianceViolations;
    }

    /**
     * Sets the HTTP compliance mode.
     *
     * <p>
     * This method configures the HTTP compliance mode used by this factory
     * for validating connections.
     * </p>
     *
     * @param httpCompliance The {@link HttpCompliance} mode to use
     */
    public void setHttpCompliance(HttpCompliance httpCompliance) {
        this.httpCompliance = httpCompliance;
    }

    /**
     * Creates a new connection for the given connector and endpoint.
     *
     * <p>
     * This method handles the creation of new HTTP connections, including
     * support for encrypted connections when a valid session exists.
     * </p>
     *
     * <p>
     * <b>Key implementation details:</b>
     * </p>
     * <ul>
     * <li>Checks for existing session</li>
     * <li>Creates encrypted endpoint if needed</li>
     * <li>Configures connection parameters</li>
     * <li>Handles connection upgrades</li>
     * </ul>
     *
     * @param connector The {@link Connector} for the server
     * @param endPoint The {@link EndPoint} for the connection
     * @return A new {@link Connection} instance
     */
    @Override
    public Connection newConnection(@Nullable Connector connector, @Nullable EndPoint endPoint) {
        if (connector == null || endPoint == null) {
            throw new IllegalArgumentException("Connector and EndPoint cannot be null");
        }

        logger.trace("{}Creating a new connection for Endpoint {} {}", LOG_STATE,
                endPoint.getRemoteAddress().toString(), endPoint.toString());

        String sessionId = sessionHandler != null
                ? sessionHandler.getSessionId(endPoint.getRemoteAddress().getAddress().getHostAddress().toString() + ":"
                        + endPoint.getRemoteAddress().getPort())
                : null;

        if (sessionId != null && sessionHandler != null) {
            logger.trace("{}Fetching Session {} {}", LOG_STATE, endPoint.getRemoteAddress().toString(), sessionId);

            @Nullable
            Session session;
            if (sessionHandler != null) {
                session = sessionHandler.getSession(sessionId);
            } else {
                session = null;
            }

            if (session != null) {
                if (session.getAttribute("Control-Read-Encryption-Key") != null) {
                    HomekitDecryptedEndPoint appEndPoint = new HomekitDecryptedEndPoint(endPoint,
                            connector.getExecutor(), connector.getByteBufferPool(), isDirectBuffers(),
                            (byte[]) session.getAttribute("Control-Read-Encryption-Key"),
                            (byte[]) session.getAttribute("Control-Write-Encryption-Key"));

                    HomekitHttpConnection appConnection = new HomekitHttpConnection(config, connector, appEndPoint,
                            httpCompliance, isRecordHttpComplianceViolations());
                    appConnection.setUpgradable(false);
                    appEndPoint.setConnection(appConnection);

                    return configure(appConnection, connector, endPoint);
                }
            }
        }

        if (logger.isDebugEnabled()) {
            logger.trace("{}There is no existing Session {}", LOG_STATE, endPoint.getRemoteAddress().toString());
        }

        HttpConnection conn = new HomekitHttpConnection(config, connector, endPoint, httpCompliance,
                isRecordHttpComplianceViolations());
        return configure(conn, connector, endPoint);
    }

    /**
     * Sets whether to record HTTP compliance violations.
     *
     * <p>
     * This method configures whether the factory should record HTTP compliance
     * violations for debugging and monitoring purposes.
     * </p>
     *
     * @param recordHttpComplianceViolations true to record violations
     */
    public void setRecordHttpComplianceViolations(boolean recordHttpComplianceViolations) {
        this.recordHttpComplianceViolations = recordHttpComplianceViolations;
    }
}
