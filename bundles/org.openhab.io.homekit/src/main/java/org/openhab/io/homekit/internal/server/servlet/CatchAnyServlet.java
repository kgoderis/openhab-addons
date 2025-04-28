package org.openhab.io.homekit.internal.server.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.openhab.io.homekit.api.hap.AccessoryServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class CatchAnyServlet extends BaseServlet {

    protected static final Logger logger = LoggerFactory.getLogger(CatchAnyServlet.class);
    protected static final String LOG_PREFIX = "HomeKit CatchAnyServlet: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "Accessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    protected static final String LOG_EVENT = LOG_PREFIX + "Event - ";
    protected static final String LOG_SERVER = LOG_PREFIX + "Server - ";
    protected static final String LOG_PAIRING = LOG_PREFIX + "Pairing - ";

    public CatchAnyServlet() {
    }

    public CatchAnyServlet(AccessoryServer server) {
        super(server);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        logger.warn("{}Catch Any {}", LOG_WARN, request.getRequestURI().toString());

        try {
            byte[] body = IOUtils.toByteArray(request.getInputStream());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
