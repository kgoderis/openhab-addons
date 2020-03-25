package org.openhab.io.homekit.internal.http.jetty;

import org.eclipse.jetty.client.HttpDestination;
import org.eclipse.jetty.client.Origin;
import org.eclipse.jetty.client.api.Connection;
import org.eclipse.jetty.client.http.HttpClientTransportOverHTTP;
import org.eclipse.jetty.client.http.HttpConnectionOverHTTP;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.util.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomekitHttpClientTransportOverHTTP extends HttpClientTransportOverHTTP {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitHttpClientTransportOverHTTP.class);

    @Override
    protected HttpConnectionOverHTTP newHttpConnection(EndPoint endPoint, HttpDestination destination,
            Promise<Connection> promise) {

        logger.info("[{}] Creating a new connection for Endpoint {} for destination {}",
                endPoint.getRemoteAddress().toString(), endPoint.toString(), destination.toString());

        if (destination instanceof HomekitHttpDestinationOverHTTP
                && ((HomekitHttpDestinationOverHTTP) destination).hasEncryptionKeys()) {
            DecryptedHomekitEndPoint appEndPoint = new DecryptedHomekitEndPoint(endPoint, getHttpClient().getExecutor(),
                    getHttpClient().getByteBufferPool(), true,
                    ((HomekitHttpDestinationOverHTTP) destination).getEncryptionKey(),
                    ((HomekitHttpDestinationOverHTTP) destination).getDecryptionKey());

            HomekitHttpConnectionOverHTTP appConnection = new HomekitHttpConnectionOverHTTP(appEndPoint, destination,
                    promise);
            // appConnection.setUpgradable(false);
            appEndPoint.setConnection(appConnection);

            return appConnection;
        }

        if (logger.isInfoEnabled()) {
            logger.info("[{}] There are no encryption keys set", endPoint.getRemoteAddress().toString());
        }

        return new HomekitHttpConnectionOverHTTP(endPoint, destination, promise);
    }

    @Override
    public HttpDestination newHttpDestination(Origin origin) {
        return new HomekitHttpDestinationOverHTTP(getHttpClient(), origin);
    }

}
