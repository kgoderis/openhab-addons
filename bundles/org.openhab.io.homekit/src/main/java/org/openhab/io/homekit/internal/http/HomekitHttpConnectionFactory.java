
package org.openhab.io.homekit.internal.http;

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
import org.openhab.io.homekit.HomekitSessionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Connection Factory for HTTP Connections.
 * <p>
 * Accepts connections either directly or via SSL and/or ALPN chained connection factories. The accepted
 * {@link HttpConnection}s are configured by a {@link HttpConfiguration} instance that is either created by
 * default or passed in to the constructor.
 */
public class HomekitHttpConnectionFactory extends AbstractConnectionFactory
        implements HttpConfiguration.ConnectionFactory {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitHttpConnectionFactory.class);

    private final HttpConfiguration config;
    private HttpCompliance httpCompliance;
    private boolean recordHttpComplianceViolations = false;
    private boolean useDirectBuffers = false;
    private HomekitSessionHandler sessionHandler;

    public HomekitHttpConnectionFactory(HomekitSessionHandler sessionHandler) {
        this(new HttpConfiguration());
        this.sessionHandler = sessionHandler;
    }

    public HomekitHttpConnectionFactory(@Name("config") HttpConfiguration config) {
        this(config, null);
    }

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

    public void setDirectBuffersF(boolean useDirectBuffers) {
        this.useDirectBuffers = useDirectBuffers;
    }

    public boolean isDirectBuffers() {
        return useDirectBuffers;
    }

    @Override
    public HttpConfiguration getHttpConfiguration() {
        return config;
    }

    public HttpCompliance getHttpCompliance() {
        return httpCompliance;
    }

    public boolean isRecordHttpComplianceViolations() {
        return recordHttpComplianceViolations;
    }

    /**
     * @param httpCompliance String value of {@link HttpCompliance}
     */
    public void setHttpCompliance(HttpCompliance httpCompliance) {
        this.httpCompliance = httpCompliance;
    }

    @Override
    public Connection newConnection(Connector connector, EndPoint endPoint) {

        logger.debug("Creating a new connection for InetAddress {}",
                endPoint.getRemoteAddress().getAddress().getHostAddress().toString() + ":"
                        + endPoint.getRemoteAddress().getPort());

        // String sessionId = sessionHandler
        // .getSessionId(endPoint.getRemoteAddress().getAddress().getHostAddress().toString());
        String sessionId = sessionHandler
                .getSessionId(endPoint.getRemoteAddress().getAddress().getHostAddress().toString() + ":"
                        + endPoint.getRemoteAddress().getPort());

        if (sessionId != null) {
            logger.debug("Fetching the Session for {}", sessionId);
            Session session = sessionHandler.getSession(sessionId);

            if (session != null) {
                if (session.getAttribute("Control-Read-Encryption-Key") != null) {
                    logger.debug("The new connection will be encrypted with keys {}/{}",
                            byteToHexString((byte[]) session.getAttribute("Control-Read-Encryption-Key")),
                            byteToHexString((byte[]) session.getAttribute("Control-Write-Encryption-Key")));
                    SecureHomekitHttpConnection conn = new SecureHomekitHttpConnection(connector.getByteBufferPool(),
                            connector.getExecutor(), endPoint,
                            (byte[]) session.getAttribute("Control-Read-Encryption-Key"),
                            (byte[]) session.getAttribute("Control-Write-Encryption-Key"), isDirectBuffers());

                    EndPoint appEndPoint = conn.getDecryptedEndPoint();
                    HomekitHttpConnection appConnection = new HomekitHttpConnection(config, connector, appEndPoint,
                            httpCompliance, isRecordHttpComplianceViolations());
                    appConnection.setUpgradable(false);
                    appEndPoint.setConnection(appConnection);

                    return configure(conn, connector, endPoint);
                }
            }
        }

        HttpConnection conn = new HomekitHttpConnection(config, connector, endPoint, httpCompliance,
                isRecordHttpComplianceViolations());
        return configure(conn, connector, endPoint);

    }

    public void setRecordHttpComplianceViolations(boolean recordHttpComplianceViolations) {
        this.recordHttpComplianceViolations = recordHttpComplianceViolations;
    }

    protected static String byteToHexString(byte[] input) {
        StringBuilder sb = new StringBuilder();
        for (byte b : input) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }
}
