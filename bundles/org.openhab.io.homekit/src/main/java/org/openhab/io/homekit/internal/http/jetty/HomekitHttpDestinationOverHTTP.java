package org.openhab.io.homekit.internal.http.jetty;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.client.Origin;
import org.eclipse.jetty.client.SendFailure;
import org.eclipse.jetty.client.api.Connection;
import org.eclipse.jetty.client.http.HttpDestinationOverHTTP;
import org.eclipse.jetty.util.FuturePromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomekitHttpDestinationOverHTTP extends HttpDestinationOverHTTP {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitHttpDestinationOverHTTP.class);

    protected byte[] decryptionKey;
    protected byte[] encryptionKey;

    public HomekitHttpDestinationOverHTTP(HttpClient client, Origin origin) {
        super(client, origin);
    }

    public void setEncryptionKeys(byte[] decryptionKey, byte[] encryptionKey) {
        this.decryptionKey = decryptionKey;
        this.encryptionKey = encryptionKey;

        // try {
        // doStop();
        // doStart();
        // } catch (Exception e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
    }

    public boolean hasEncryptionKeys() {
        return (decryptionKey != null && encryptionKey != null);
    }

    public byte[] getDecryptionKey() {
        return decryptionKey;
    }

    public byte[] getEncryptionKey() {
        return encryptionKey;
    }

    @Override
    protected SendFailure send(Connection connection, HttpExchange exchange) {
        // Check if keys
        // upgrade conn if conn not already upgraded

        if (hasEncryptionKeys() && connection instanceof HomekitHttpConnectionOverHTTP) {
            if (!(((HomekitHttpConnectionOverHTTP) connection).getEndPoint() instanceof DecryptedHomekitEndPoint)) {

                logger.debug("[{}] Creating a new connection for Endpoint {}",
                        ((HomekitHttpConnectionOverHTTP) connection).getEndPoint().getRemoteAddress().toString(),
                        ((HomekitHttpConnectionOverHTTP) connection).getEndPoint().toString());

                DecryptedHomekitEndPoint appEndPoint = new DecryptedHomekitEndPoint(
                        ((HomekitHttpConnectionOverHTTP) connection).getEndPoint(), getHttpClient().getExecutor(),
                        getHttpClient().getByteBufferPool(), true, getEncryptionKey(), getDecryptionKey());

                FuturePromise<Connection> futureConnection = new FuturePromise<>();
                // destination.newConnection(futureConnection);
                // Connection connection = futureConnection.get get(5, TimeUnit.SECONDS);

                HomekitHttpConnectionOverHTTP appConnection = new HomekitHttpConnectionOverHTTP(appEndPoint, this,
                        futureConnection);
                appEndPoint.setConnection(appConnection);
                ((HomekitHttpConnectionOverHTTP) connection).getEndPoint().upgrade(appConnection);

                return super.send(appConnection, exchange);

            } else {
                logger.debug("[{}] Endpoint {} is already upgraded",
                        ((HomekitHttpConnectionOverHTTP) connection).getEndPoint().getRemoteAddress().toString(),
                        ((HomekitHttpConnectionOverHTTP) connection).getEndPoint().toString());
            }

        }

        return super.send(connection, exchange);
    }

}
