package org.openhab.io.homekit.internal.http;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.openhab.io.homekit.api.hap.HomekitAccessoryServer;

public class HomekitServletConfig implements ServletConfig {

    private final HomekitAccessoryServer server;
    private final String servletName;
    private final ServletContext servletContext;

    public HomekitServletConfig(HomekitAccessoryServer server, String servletName, ServletContext servletContext) {
        this.server = server;
        this.servletName = servletName;
        this.servletContext = servletContext;
    }

    @Override
    public String getServletName() {
        return servletName;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public String getInitParameter(String name) {
        return null;
    }

    @Override
    public java.util.Enumeration<String> getInitParameterNames() {
        return java.util.Collections.emptyEnumeration();
    }

    public HomekitAccessoryServer getAccessoryServer() {
        return server;
    }
}
