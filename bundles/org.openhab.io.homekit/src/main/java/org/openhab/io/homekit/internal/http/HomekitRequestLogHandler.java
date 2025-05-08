package org.openhab.io.homekit.internal.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.HexDump;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomekitRequestLogHandler extends RequestLogHandler {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitRequestLogHandler.class);
    protected static final String LOG_PREFIX = "HomeKit RequestLogHandler: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "HomekitAccessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    public HomekitRequestLogHandler() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        if (logger.isDebugEnabled()) {
            final HomekitRequestWrapper wrappedRequest = new HomekitRequestWrapper(request);
            HomekitResponseWrapper wrappedResponse = new HomekitResponseWrapper(response);

            final String userAgent = wrappedRequest.getHeader("User-Agent");
            logger.debug("{}=============Request==========", LOG_STATE);
            logger.debug("{}From {}:{} ; ua:{}", LOG_STATE, wrappedRequest.getRemoteAddr(),
                    wrappedRequest.getRemotePort(), userAgent);
            logger.debug("{}HomekitMethod : {}", LOG_STATE, wrappedRequest.getMethod().toUpperCase());
            logger.debug("{}Content-Type : {}", LOG_STATE, wrappedRequest.getContentType());
            logger.debug("{}Payload-Size : {}", LOG_STATE, wrappedRequest.getContentLength());
            logger.debug("{}URI : {}", LOG_STATE, wrappedRequest.getRequestURI());
            logger.debug("{}Query : {}", LOG_STATE, wrappedRequest.getQueryString());
            logger.debug("{}Payload :", LOG_STATE);
            try {
                byte[] body = IOUtils.toByteArray(wrappedRequest.getInputStream());

                if (body.length > 0) {
                    try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                        HexDump.dump(body, 0, stream, 0);
                        stream.flush();
                        logger.trace("{}\n{}", LOG_STATE, stream.toString(StandardCharsets.UTF_8.name()));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            logger.debug("{}==============================", LOG_STATE);

            if (_handler != null) {
                _handler.handle(target, baseRequest, wrappedRequest, wrappedResponse);
            }

            logger.debug("{}============Response==========", LOG_STATE);
            logger.debug("{}Status : {}", LOG_STATE, wrappedResponse.getStatus());
            logger.debug("{}Response :", LOG_STATE);

            byte[] buf = wrappedResponse.getContentAsByteArray();
            if (buf.length > 0) {
                try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                    HexDump.dump(buf, 0, stream, 0);
                    stream.flush();
                    logger.trace("{}\n{}", LOG_STATE, stream.toString(StandardCharsets.UTF_8.name()));
                }
            }

            wrappedResponse.copyBodyToResponse();

            logger.debug("{}==============================", LOG_STATE);

        } else {
            if (_handler != null) {
                _handler.handle(target, baseRequest, request, response);
            }
        }
    }
}
