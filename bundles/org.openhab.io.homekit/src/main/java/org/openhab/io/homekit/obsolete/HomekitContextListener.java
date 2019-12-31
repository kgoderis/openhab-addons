package org.openhab.io.homekit.obsolete;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomekitContextListener implements ServletContextListener {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        String contextPath = sce.getServletContext().getContextPath();
        logger.debug("Initialized context {}", contextPath);
        sce.getServletContext().addListener(new HomekitRequestListener());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        String contextPath = sce.getServletContext().getContextPath();
        logger.debug("Destroyed context {}", contextPath);
    }

}
