package org.openhab.io.homekit.internal.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.openhab.io.homekit.api.AccessoryServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class CatchAnyServlet extends BaseServlet {

    protected static final Logger logger = LoggerFactory.getLogger(CatchAnyServlet.class);

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

        logger.warn("Catch Any {}", request.getRequestURI().toString());

        try {
            byte[] body = IOUtils.toByteArray(request.getInputStream());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

}
