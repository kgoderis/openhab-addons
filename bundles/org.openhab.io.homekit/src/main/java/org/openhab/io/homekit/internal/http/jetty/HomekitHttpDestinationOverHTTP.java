package org.openhab.io.homekit.internal.http.jetty;

import org.eclipse.jetty.client.ConnectionPool;
import org.eclipse.jetty.client.DuplexConnectionPool;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Origin;
import org.eclipse.jetty.client.http.HttpDestinationOverHTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomekitHttpDestinationOverHTTP extends HttpDestinationOverHTTP {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitHttpDestinationOverHTTP.class);

    private byte[] decryptionKey;
    private byte[] encryptionKey;

    public HomekitHttpDestinationOverHTTP(HttpClient client, Origin origin) {
        super(client, origin);
    }

    public void setEncryptionKeys(byte[] decryptionKey, byte[] encryptionKey) {

        logger.info("Setting Encryption Keys on {}", this);

        this.decryptionKey = decryptionKey;
        this.encryptionKey = encryptionKey;

        ConnectionPool pool = getConnectionPool();
        if (pool instanceof DuplexConnectionPool) {
            for (org.eclipse.jetty.client.api.Connection connection : ((DuplexConnectionPool) pool)
                    .getIdleConnections()) {
                if (connection instanceof HomekitHttpConnectionOverHTTP) {
                    ((HomekitHttpConnectionOverHTTP) connection).setEncryptionKeys(decryptionKey, encryptionKey);
                }
            }

            for (org.eclipse.jetty.client.api.Connection connection : ((DuplexConnectionPool) pool)
                    .getActiveConnections()) {
                if (connection instanceof HomekitHttpConnectionOverHTTP) {
                    ((HomekitHttpConnectionOverHTTP) connection).setEncryptionKeys(decryptionKey, encryptionKey);
                }
            }
        }
    }

    // private void secureConnection(Connection connection) {
    // logger.info("Securing connection {}", connection.toString());
    // if (connection instanceof HomekitHttpConnectionOverHTTP) {
    // if (!(((HomekitHttpConnectionOverHTTP) connection).getEndPoint() instanceof DecryptedHomekitEndPoint)) {
    // logger.info("[{}] Creating a new connection for Endpoint {}",
    // ((HomekitHttpConnectionOverHTTP) connection).getEndPoint().getRemoteAddress().toString(),
    // ((HomekitHttpConnectionOverHTTP) connection).getEndPoint().toString());
    //
    // DecryptedHomekitEndPoint appEndPoint = new DecryptedHomekitEndPoint(
    // ((HomekitHttpConnectionOverHTTP) connection).getEndPoint(), getHttpClient().getExecutor(),
    // getHttpClient().getByteBufferPool(), true, getEncryptionKey(), getDecryptionKey());
    //
    // FuturePromise<Connection> futureConnection = new FuturePromise<>();
    // // destination.newConnection(futureConnection);
    // // Connection connection = futureConnection.get get(5, TimeUnit.SECONDS);
    //
    // HomekitHttpConnectionOverHTTP appConnection = new HomekitHttpConnectionOverHTTP(appEndPoint, this,
    // futureConnection);
    // appEndPoint.setConnection(appConnection);
    // ((HomekitHttpConnectionOverHTTP) connection).getEndPoint().upgrade(appConnection);
    //
    // } else {
    // logger.info("[{}] Endpoint {} is already upgraded",
    // ((HomekitHttpConnectionOverHTTP) connection).getEndPoint().getRemoteAddress().toString(),
    // ((HomekitHttpConnectionOverHTTP) connection).getEndPoint().toString());
    // }
    // } else {
    // logger.info("[{}] Connection is of class {}", connection.toString(),
    // connection.getClass().getCanonicalName());
    // }
    // }

    public boolean hasEncryptionKeys() {
        return (decryptionKey != null && encryptionKey != null);
    }

    public byte[] getDecryptionKey() {
        return decryptionKey;
    }

    public byte[] getEncryptionKey() {
        return encryptionKey;
    }

}
