package org.openhab.io.homekit.internal.http.jetty;

import org.eclipse.jetty.client.HttpDestination;
import org.eclipse.jetty.client.api.Connection;
import org.eclipse.jetty.client.http.HttpClientTransportOverHTTP;
import org.eclipse.jetty.client.http.HttpConnectionOverHTTP;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.util.Promise;

public class HomekitHttpClientTransportOverHTTP extends HttpClientTransportOverHTTP {

    @Override
    protected HttpConnectionOverHTTP newHttpConnection(EndPoint endPoint, HttpDestination destination,
            Promise<Connection> promise) {
        return new HomekitHttpConnectionOverHTTP(endPoint, destination, promise);
    }
}
