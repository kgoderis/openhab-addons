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

    public HomekitRequestLogHandler() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        if (logger.isDebugEnabled()) {
            final RequestWrapper wrappedRequest = new RequestWrapper(request);
            ResponseWrapper wrappedResponse = new ResponseWrapper(response);

            final String userAgent = wrappedRequest.getHeader("User-Agent");
            logger.debug(String.format("=============Request=========="));
            logger.debug(String.format("From %s:%s ; ua:%s", wrappedRequest.getRemoteAddr(),
                    wrappedRequest.getRemotePort(), userAgent));
            logger.debug(String.format("Method : %s", wrappedRequest.getMethod().toUpperCase()));
            logger.debug(String.format("Content-Type : %s", wrappedRequest.getContentType()));
            logger.debug(String.format("Payload-Size : %s", wrappedRequest.getContentLength()));
            logger.debug(String.format("URI : %s", wrappedRequest.getRequestURI()));
            logger.debug(String.format("Query : %s", wrappedRequest.getQueryString()));
            logger.debug(String.format("Payload :"));
            try {
                byte[] body = IOUtils.toByteArray(wrappedRequest.getInputStream());

                if (body.length > 0) {
                    try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                        HexDump.dump(body, 0, stream, 0);
                        stream.flush();
                        logger.debug("\n{}", stream.toString(StandardCharsets.UTF_8.name()));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            logger.debug(String.format("=============================="));

            if (_handler != null) {
                _handler.handle(target, baseRequest, wrappedRequest, wrappedResponse);
            }

            logger.debug(String.format("============Response=========="));
            logger.debug(String.format("Status : %s", wrappedResponse.getStatus()));
            logger.debug("Response :");

            byte[] buf = wrappedResponse.getContentAsByteArray();
            if (buf.length > 0) {
                try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                    HexDump.dump(buf, 0, stream, 0);
                    stream.flush();
                    logger.debug("\n{}", stream.toString(StandardCharsets.UTF_8.name()));
                }
            }

            wrappedResponse.copyBodyToResponse();

            logger.debug(String.format("=============================="));

        } else {
            if (_handler != null) {
                _handler.handle(target, baseRequest, request, response);
            }
        }
    }

}
