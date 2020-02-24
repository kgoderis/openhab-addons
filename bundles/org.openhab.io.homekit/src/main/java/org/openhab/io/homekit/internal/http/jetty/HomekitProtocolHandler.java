package org.openhab.io.homekit.internal.http.jetty;

import org.eclipse.jetty.client.ProtocolHandler;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Response.Listener;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.openhab.io.homekit.internal.client.HomekitClient;

public class HomekitProtocolHandler implements ProtocolHandler {

    protected HomekitClient homekitClient;

    public HomekitProtocolHandler(HomekitClient homekitClient) {
        this.homekitClient = homekitClient;
    }

    @Override
    public String getName() {
        return "homekit.event";
    }

    @Override
    public boolean accept(Request request, Response response) {
        return response.getHeaders().contains("X-HOMEKIT-EVENT", "True");
    }

    @Override
    public Listener getResponseListener() {
        return new HomekitResponseListener(8 * 1024 * 1024);
    }

    protected class HomekitResponseListener extends BufferingResponseListener {

        public HomekitResponseListener(int maxLength) {
            super(maxLength);
        }

        @Override
        public void onSuccess(Response response) {
            homekitClient.handleEvent(getContent());
        }

        @Override
        public void onComplete(Result result) {
        }
    }

}
