package org.openhab.io.homekit.internal.handler;

import org.eclipse.jetty.servlet.ServletHandler;
import org.openhab.io.homekit.api.AccessoryServer;

public abstract class BaseHandler extends ServletHandler {

    AccessoryServer server;

    public BaseHandler(AccessoryServer server) {
        this.server = server;
    }
    //
    // @Override
    // public void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse
    // response)
    // throws IOException, ServletException {
    // // TODO Auto-generated method stub
    // }
}
