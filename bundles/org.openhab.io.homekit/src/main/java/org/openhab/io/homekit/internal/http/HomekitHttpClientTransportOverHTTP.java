package org.openhab.io.homekit.internal.http;

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
    protected static final String LOG_PREFIX = "HomeKit HttpClientTransportOverHTTP: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "Accessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    public HomekitHttpClientTransportOverHTTP() {
        super();
        setConnectionPoolFactory(destination -> new HomekitConnnectionPool(destination,
                getHttpClient().getMaxConnectionsPerDestination(), destination));
    }

    @Override
    protected HttpConnectionOverHTTP newHttpConnection(EndPoint endPoint, HttpDestination destination,
            Promise<Connection> promise) {

        logger.debug("{}newHttpConnection called for Endpoint {} and destination {}", LOG_CONFIG, endPoint.toString(),
                destination.toString());
        logger.info("{}Creating a new connection for Endpoint {} for destination {}", LOG_STATE,
                endPoint.getRemoteAddress().toString(), endPoint.toString(), destination.toString());

        HomekitHttpConnectionOverHTTP newConnection = new HomekitHttpConnectionOverHTTP(endPoint, destination, promise);

        if (destination instanceof HomekitHttpDestinationOverHTTP
                && ((HomekitHttpDestinationOverHTTP) destination).hasEncryptionKeys()) {
            // DecryptedHomekitEndPoint appEndPoint = new DecryptedHomekitEndPoint(endPoint,
            // getHttpClient().getExecutor(),
            // getHttpClient().getByteBufferPool(), true,
            // ((HomekitHttpDestinationOverHTTP) destination).getEncryptionKey(),
            // ((HomekitHttpDestinationOverHTTP) destination).getDecryptionKey());
            //
            // HomekitHttpConnectionOverHTTP appConnection = new HomekitHttpConnectionOverHTTP(endPoint, destination,
            // promise);
            logger.info("{}Setting the encryption keys on connection {} for destination {}", LOG_CONFIG,
                    endPoint.getRemoteAddress().toString(), newConnection.toString(), destination.toString());
            if (logger.isTraceEnabled()) {
                logger.trace("{}DecryptionKey: {}", LOG_CONFIG, javax.xml.bind.DatatypeConverter
                        .printHexBinary(((HomekitHttpDestinationOverHTTP) destination).getDecryptionKey()));
                logger.trace("{}EncryptionKey: {}", LOG_CONFIG, javax.xml.bind.DatatypeConverter
                        .printHexBinary(((HomekitHttpDestinationOverHTTP) destination).getEncryptionKey()));
            }
            newConnection.setEncryptionKeys(((HomekitHttpDestinationOverHTTP) destination).getDecryptionKey(),
                    ((HomekitHttpDestinationOverHTTP) destination).getEncryptionKey());
            // appConnection.setUpgradable(false);
            // appEndPoint.setConnection(appConnection);
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("{}There are no encryption keys set for Endpoint {}", LOG_STATE,
                        endPoint.getRemoteAddress().toString());
            }
        }

        return newConnection;
    }

    @Override
    public HttpDestination newHttpDestination(Origin origin) {
        return new HomekitHttpDestinationOverHTTP(getHttpClient(), origin);
    }
}
