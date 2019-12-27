package org.openhab.io.homekit.internal.server;

import java.util.Collection;
import java.util.HashSet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.SessionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomekitSessionTracker
        implements HttpSessionListener, HttpSessionIdListener, ServletContextListener, ServletRequestListener {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitSessionTracker.class);

    private final Server server;
    private final SessionHandler sessionHandler;
    private String contextPath;
    private HashSet<String> sessionIds = new HashSet<>();

    public HomekitSessionTracker(Server server, SessionHandler sessionHandler) {
        this.server = server;
        this.sessionHandler = sessionHandler;
        this.sessionHandler.addEventListener(this);
    }

    public String getContextPath() {
        return contextPath;
    }

    public SessionHandler getSessionHandler() {
        return sessionHandler;
    }

    public HashSet<String> getSessionIds() {
        return sessionIds;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        contextPath = sce.getServletContext().getContextPath();
        logger.debug("{} : initialized context {}", server.toString(), contextPath);
        sce.getServletContext().addListener(this);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        Collection<HomekitSessionTracker> trackers = this.server.getBeans(HomekitSessionTracker.class);
        trackers.removeIf((tracker) -> tracker.getContextPath().equals(sce.getServletContext().getContextPath()));
        logger.debug("{} : destroyed context {}", server.toString(), contextPath);
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        sessionIds.add(se.getSession().getId());
        logger.debug("{} : initialized session {}", server.toString(), se.getSession().getId());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        sessionIds.remove(se.getSession().getId());
        logger.debug("{} : destroyed session {}", server.toString(), se.getSession().getId());
    }

    @Override
    public void sessionIdChanged(HttpSessionEvent event, String oldSessionId) {
        sessionIds.add(oldSessionId);
        sessionIds.add(event.getSession().getId());
        logger.debug("{} : changed session {} -> {}", server.toString(), oldSessionId, event.getSession().getId());
    }

    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
        ServletRequest servletRequest = sre.getServletRequest();
        logger.debug("{} : destroyed servlet request {}", server.toString(), servletRequest.getRemoteAddr());
    }

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        ServletRequest servletRequest = sre.getServletRequest();
        logger.debug("{} : initialized servlet request {}", server.toString(), servletRequest.getRemoteAddr());
    }

}
