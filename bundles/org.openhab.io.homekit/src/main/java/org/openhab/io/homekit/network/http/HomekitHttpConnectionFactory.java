package org.openhab.io.homekit.network.http;

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
 * This class manages the creation of HTTP connections for HomeKit communication,
 * providing specialized handling for HomeKit-specific connection requirements
 * and encryption support.
 *
 * The factory works in conjunction with:
 * - {@link HomekitHttpConnection} for connection management
 * - {@link HomekitSessionHandler} for session handling
 * - {@link HomekitDecryptedEndPoint} for encrypted endpoints
 * - {@link org.eclipse.jetty.server.Connector Connector} for server connections
 * - {@link org.eclipse.jetty.server.HttpConfiguration HttpConfiguration} for HTTP settings
 * - {@link org.eclipse.jetty.http.HttpCompliance HttpCompliance} for protocol compliance
 *
 * Key responsibilities:
 * 1. Creating new HTTP connections
 * 2. Managing connection configuration
 * 3. Supporting connection upgrades
 * 4. Handling session management
 * 5. Providing encryption support
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0
 */
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
    private HomekitSessionHandler sessionHandler;

    /**
     * Creates a new HomeKit HTTP connection factory with a session handler.
     *
     * This constructor initializes a factory with the specified session handler,
     * preparing it for creating HTTP connections with session support.
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
     * This constructor initializes a factory with the specified HTTP configuration,
     * preparing it for creating HTTP connections.
     *
     * @param config The {@link HttpConfiguration} to use
     */
    public HomekitHttpConnectionFactory(@Name("config") HttpConfiguration config) {
        this(config, null);
    }

    /**
     * Creates a new HomeKit HTTP connection factory with configuration and compliance.
     *
     * This constructor initializes a factory with the specified HTTP configuration
     * and compliance mode, preparing it for creating HTTP connections.
     *
     * @param config The {@link HttpConfiguration} to use
     * @param compliance The {@link HttpCompliance} mode to use
     * @throws IllegalArgumentException if config is null
     */
    public HomekitHttpConnectionFactory(@Name("config") HttpConfiguration config,
            @Name("compliance") HttpCompliance compliance) {
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
     * @param useDirectBuffers true to use direct buffers, false otherwise
     */
    public void setDirectBuffersF(boolean useDirectBuffers) {
        this.useDirectBuffers = useDirectBuffers;
    }

    /**
     * Checks if direct buffers are being used.
     *
     * @return true if direct buffers are being used
     */
    public boolean isDirectBuffers() {
        return useDirectBuffers;
    }

    /**
     * Gets the HTTP configuration.
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
     * @return The {@link HttpCompliance} mode
     */
    public HttpCompliance getHttpCompliance() {
        return httpCompliance;
    }

    /**
     * Checks if HTTP compliance violations are being recorded.
     *
     * @return true if violations are being recorded
     */
    public boolean isRecordHttpComplianceViolations() {
        return recordHttpComplianceViolations;
    }

    /**
     * Sets the HTTP compliance mode.
     *
     * @param httpCompliance The {@link HttpCompliance} mode to use
     */
    public void setHttpCompliance(HttpCompliance httpCompliance) {
        this.httpCompliance = httpCompliance;
    }

    /**
     * Creates a new connection for the given connector and endpoint.
     *
     * This method handles the creation of new HTTP connections, including
     * support for encrypted connections when a valid session exists.
     *
     * @param connector The {@link Connector} for the server
     * @param endPoint The {@link EndPoint} for the connection
     * @return A new {@link Connection} instance
     */
    @Override
    public Connection newConnection(Connector connector, EndPoint endPoint) {
        logger.trace("{}Creating a new connection for Endpoint {} {}", LOG_STATE,
                endPoint.getRemoteAddress().toString(), endPoint.toString());

        String sessionId = sessionHandler
                .getSessionId(endPoint.getRemoteAddress().getAddress().getHostAddress().toString() + ":"
                        + endPoint.getRemoteAddress().getPort());

        if (sessionId != null) {
            logger.trace("{}Fetching Session {} {}", LOG_STATE, endPoint.getRemoteAddress().toString(), sessionId);
            Session session = sessionHandler.getSession(sessionId);

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
     * @param recordHttpComplianceViolations true to record violations
     */
    public void setRecordHttpComplianceViolations(boolean recordHttpComplianceViolations) {
        this.recordHttpComplianceViolations = recordHttpComplianceViolations;
    }
}
