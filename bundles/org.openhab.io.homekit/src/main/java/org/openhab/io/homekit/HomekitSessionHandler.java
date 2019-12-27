package org.openhab.io.homekit;

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
                baseRequest.setSession(session);
            }
            return;
        } else if (!DispatcherType.REQUEST.equals(baseRequest.getDispatcherType())) {
            return;
        }

        boolean requestedSessionIdFromCookie = false;
        HttpSession session = null;

        // requestedSessionId = ipSessionIds.get(baseRequest.getRemoteAddr());
        requestedSessionId = ipSessionIds
                .get(baseRequest.getRemoteAddr().toString() + ":" + baseRequest.getRemotePort());
        ;

        if (requestedSessionId != null) {
            session = getHttpSession(requestedSessionId);
            logger.debug("Got Session with Id {} for InetAddress {}", requestedSessionId,
                    baseRequest.getRemoteAddr().toString() + ":" + baseRequest.getRemotePort());
        } else {
            logger.debug("There is no Session Id for InetAddress {}",
                    baseRequest.getRemoteAddr().toString() + ":" + baseRequest.getRemotePort());
        }

        baseRequest.setRequestedSessionId(requestedSessionId);
        baseRequest.setRequestedSessionIdFromCookie(requestedSessionId != null && requestedSessionIdFromCookie);
        if (session != null && isValid(session)) {
            baseRequest.setSession(session);
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
        ipSessionIds.put(request.getRemoteAddr() + ":" + request.getRemotePort(), session.getId());
        return session;
    }

}
