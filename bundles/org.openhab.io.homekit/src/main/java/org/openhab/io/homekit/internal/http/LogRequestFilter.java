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
            if (logger.isDebugEnabled()) {
                final RequestWrapper wrappedRequest = new RequestWrapper((HttpServletRequest) request);
                logPayLoad(wrappedRequest);
                chain.doFilter(wrappedRequest, response);
            } else {
                chain.doFilter(request, response);
            }
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("=======Request Processed=====");
            }
        }
    }

    private void logPayLoad(HttpServletRequest request) {
        final String userAgent = request.getHeader("User-Agent");
        logger.debug(String.format("============Request=========="));
        logger.debug(String.format("From %s:%s ; ua:%s", request.getRemoteAddr(), request.getRemotePort(), userAgent));
        logger.debug(String.format("Method : %s", request.getMethod().toUpperCase()));
        logger.debug(String.format("Content-Type : %s", request.getContentType()));
        logger.debug(String.format("Payload-Size : %s", request.getContentLength()));
        logger.debug(String.format("URI : %s", request.getRequestURI()));
        logger.debug(String.format("Query : %s", request.getQueryString()));
        logger.debug(String.format("Payload :"));
        try {
            byte[] body = IOUtils.toByteArray(request.getInputStream());

            try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                HexDump.dump(body, 0, stream, 0);
                stream.flush();
                logger.debug(stream.toString(StandardCharsets.UTF_8.name()));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.debug(String.format("============================="));
    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

}
