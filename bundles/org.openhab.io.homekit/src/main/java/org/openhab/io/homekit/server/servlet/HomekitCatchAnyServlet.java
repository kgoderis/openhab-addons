package org.openhab.io.homekit.server.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class HomekitCatchAnyServlet extends HomekitBaseServlet {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitCatchAnyServlet.class);
    protected static final String LOG_PREFIX = "Homekit HomekitCatchAnyServlet: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "HomekitAccessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    protected static final String LOG_EVENT = LOG_PREFIX + "Event - ";
    protected static final String LOG_SERVER = LOG_PREFIX + "Server - ";
    protected static final String LOG_PAIRING = LOG_PREFIX + "HomekitPairing - ";

    public HomekitCatchAnyServlet() {
    }

    public HomekitCatchAnyServlet(HomekitAccessoryServer server) {
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
