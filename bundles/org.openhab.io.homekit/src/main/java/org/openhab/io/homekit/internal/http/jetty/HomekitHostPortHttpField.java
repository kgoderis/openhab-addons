package org.openhab.io.homekit.internal.http.jetty;

import org.eclipse.jetty.http.HostPortHttpField;
import org.eclipse.jetty.http.HttpHeader;

public class HomekitHostPortHttpField extends HostPortHttpField {

    public HomekitHostPortHttpField(HttpHeader header, String name, String authority) {
        super(header, name, authority);
    }
}
