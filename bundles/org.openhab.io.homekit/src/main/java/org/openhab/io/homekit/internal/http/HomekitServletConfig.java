package org.openhab.io.homekit.internal.http;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.internal.http.netty.ConfigAdapter;
import org.openhab.io.homekit.internal.http.netty.ServletContextImpl;

public class HomekitServletConfig extends ConfigAdapter implements ServletConfig {

    AccessoryServer server;

    public HomekitServletConfig(AccessoryServer server, String servletName) {
        super(servletName);
        this.server = server;
    }

    @Override
    public String getServletName() {
        return super.getOwnerName();
    }

    @Override
    public ServletContext getServletContext() {
        return ServletContextImpl.get();
    }

    public AccessoryServer getAccessoryServer() {
        return server;
    }

}
