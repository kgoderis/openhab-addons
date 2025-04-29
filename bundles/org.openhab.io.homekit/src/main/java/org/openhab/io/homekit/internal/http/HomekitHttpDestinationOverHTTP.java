package org.openhab.io.homekit.internal.http;

import org.eclipse.jetty.client.ConnectionPool;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Origin;
import org.eclipse.jetty.client.http.HttpDestinationOverHTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomekitHttpDestinationOverHTTP extends HttpDestinationOverHTTP {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitHttpDestinationOverHTTP.class);
    protected static final String LOG_PREFIX = "HomeKit HttpDestinationOverHTTP: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "Accessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    private byte[] decryptionKey;
    private byte[] encryptionKey;

    public HomekitHttpDestinationOverHTTP(HttpClient client, Origin origin) {
        super(client, origin);
    }

    public void setEncryptionKeys(byte[] decryptionKey, byte[] encryptionKey) {
        logger.debug("{}setEncryptionKeys called for {}", LOG_CONFIG, this);
        logger.info("{}Setting Encryption Keys on {}", LOG_CONFIG, this);
        if (logger.isTraceEnabled()) {
            logger.trace("{}DecryptionKey: {}", LOG_CONFIG, javax.xml.bind.DatatypeConverter.printHexBinary(decryptionKey));
            logger.trace("{}EncryptionKey: {}", LOG_CONFIG, javax.xml.bind.DatatypeConverter.printHexBinary(encryptionKey));
        }

        this.decryptionKey = decryptionKey;
        this.encryptionKey = encryptionKey;

        ConnectionPool pool = getConnectionPool();
        if (pool instanceof HomekitConnnectionPool) {
            var idle = ((HomekitConnnectionPool) pool).getIdleConnections();
            var active = ((HomekitConnnectionPool) pool).getActiveConnections();
            for (org.eclipse.jetty.client.api.Connection connection : idle) {
                if (connection instanceof HomekitHttpConnectionOverHTTP) {
                    ((HomekitHttpConnectionOverHTTP) connection).setEncryptionKeys(decryptionKey, encryptionKey);
                }
            }
            for (org.eclipse.jetty.client.api.Connection connection : active) {
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
