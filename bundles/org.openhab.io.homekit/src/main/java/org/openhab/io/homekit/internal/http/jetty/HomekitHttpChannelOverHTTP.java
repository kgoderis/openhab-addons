package org.openhab.io.homekit.internal.http.jetty;

import org.eclipse.jetty.client.http.HttpChannelOverHTTP;
import org.eclipse.jetty.client.http.HttpConnectionOverHTTP;
import org.eclipse.jetty.client.http.HttpReceiverOverHTTP;

public class HomekitHttpChannelOverHTTP extends HttpChannelOverHTTP {

    public HomekitHttpChannelOverHTTP(HttpConnectionOverHTTP connection) {
        super(connection);
    }

    // @Override
    // protected HttpSenderOverHTTP newHttpSender() {
    // return new HomekitHttpSenderOverHTTP(this);
    // }
    //
    @Override
    protected HttpReceiverOverHTTP newHttpReceiver() {
        return new HomekitHttpReceiverOverHTTP(this);
    }

    @Override
    public long getMessagesIn() {
        return super.getMessagesIn();
    }

    @Override
    public long getMessagesOut() {
        return super.getMessagesOut();
    }

    @Override
    protected HomekitHttpReceiverOverHTTP getHttpReceiver() {
        return (HomekitHttpReceiverOverHTTP) super.getHttpReceiver();
    }
}
