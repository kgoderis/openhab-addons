package org.openhab.io.homekit.internal.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.HexDump;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogRequestFilter implements Filter {

    protected static final Logger logger = LoggerFactory.getLogger(LogRequestFilter.class);
    protected static final String LOG_PREFIX = "HomeKit LogRequestFilter: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "Accessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    public LogRequestFilter() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // TODO Auto-generated method stub
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            logger.debug("{}Entering doFilter for request URI: {}", LOG_STATE, ((HttpServletRequest) request).getRequestURI());
            if (logger.isDebugEnabled()) {
                final RequestWrapper wrappedRequest = new RequestWrapper((HttpServletRequest) request);
                logPayLoad(wrappedRequest);
                chain.doFilter(wrappedRequest, response);
            } else {
                chain.doFilter(request, response);
            }
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("{}=======Request Processed=====", LOG_STATE);
            }
        }
    }

    private void logPayLoad(HttpServletRequest request) {
        final String userAgent = request.getHeader("User-Agent");
        logger.debug("{}============Request==========", LOG_STATE);
        logger.debug("{}From {}:{} ; ua:{}", LOG_STATE, request.getRemoteAddr(), request.getRemotePort(), userAgent);
        logger.debug("{}Method : {}", LOG_STATE, request.getMethod().toUpperCase());
        logger.debug("{}Content-Type : {}", LOG_STATE, request.getContentType());
        logger.debug("{}Payload-Size : {}", LOG_STATE, request.getContentLength());
        logger.debug("{}URI : {}", LOG_STATE, request.getRequestURI());
        logger.debug("{}Query : {}", LOG_STATE, request.getQueryString());
        logger.debug("{}Payload :", LOG_STATE);
        try {
            byte[] body = IOUtils.toByteArray(request.getInputStream());

            try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                HexDump.dump(body, 0, stream, 0);
                stream.flush();
                logger.trace("{}{}", LOG_STATE, stream.toString(StandardCharsets.UTF_8.name()));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.debug("{}=============================", LOG_STATE);
    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub
    }
}
