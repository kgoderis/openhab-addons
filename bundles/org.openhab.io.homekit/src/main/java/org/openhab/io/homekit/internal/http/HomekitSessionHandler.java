package org.openhab.io.homekit.internal.http;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.session.SessionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomekitSessionHandler extends SessionHandler {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitSessionHandler.class);

    protected static final String LOG_PREFIX = "HomeKit HomekitSessionHandler: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "HomekitAccessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    protected Map<String, String> ipSessionIds = new ConcurrentHashMap<String, String>();

    public HomekitSessionHandler() {
        this._usingCookies = false;
        this._usingURLs = false;
    }

    /**
     * Look for a requested session ID in remote IP address
     *
     * @param baseRequest the request to check
     * @param request the request to check
     */
    @Override
    protected void checkRequestedSessionId(Request baseRequest, HttpServletRequest request) {
        String requestedSessionId = request.getRequestedSessionId();

        if (requestedSessionId != null) {
            HttpSession session = getHttpSession(requestedSessionId);

            if (session != null && isValid(session)) {
                logger.trace("[{}] Fetched Session {}",
                        baseRequest.getRemoteAddr().toString() + ":" + baseRequest.getRemotePort(), requestedSessionId);
                baseRequest.setSession(session);
            }
            return;
        } else if (!DispatcherType.REQUEST.equals(baseRequest.getDispatcherType())) {
            return;
        }

        HttpSession session = null;

        requestedSessionId = ipSessionIds
                .get(baseRequest.getRemoteAddr().toString() + ":" + baseRequest.getRemotePort());

        if (requestedSessionId != null) {
            session = getHttpSession(requestedSessionId);
            logger.trace("[{}] Fetched Session {}",
                    baseRequest.getRemoteAddr().toString() + ":" + baseRequest.getRemotePort(), requestedSessionId);

            if (session != null && isValid(session)) {
                baseRequest.setRequestedSessionId(requestedSessionId);
                baseRequest.setSession(session);
            }
        } else {
            logger.trace("[{}] No existing Session",
                    baseRequest.getRemoteAddr().toString() + ":" + baseRequest.getRemotePort());
        }
    }

    public String getSessionId(String ipAddress) {
        return ipSessionIds.get(ipAddress);
    }

    /**
     * Creates a new <code>HttpSession</code>.
     *
     * @param request the HttpServletRequest containing the requested session id
     * @return the new <code>HttpSession</code>
     */
    @Override
    public HttpSession newHttpSession(HttpServletRequest request) {
        HttpSession session = super.newHttpSession(request);
        ipSessionIds.put(request.getRemoteAddr().toString() + ":" + request.getRemotePort(), session.getId());
        return session;
    }

    @Override
    public void invalidate(String id) {
        HashSet<String> keysToDelete = new HashSet<String>();

        for (String anId : ipSessionIds.keySet()) {
            if (ipSessionIds.get(anId).equals(id)) {
                keysToDelete.add(anId);
            }
        }

        for (String anId : keysToDelete) {
            ipSessionIds.remove(anId);
        }

        super.invalidate(id);
    }
}
