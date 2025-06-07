/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.openhab.io.homekit.network.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.io.IOUtils;

/**
 * Wraps HTTP requests to enable multiple reads of the request body.
 *
 * This class extends {@link HttpServletRequestWrapper} to provide the ability to
 * read the request body multiple times, which is essential for logging and
 * processing HomeKit HTTP requests. It caches the request body in memory and
 * provides a new input stream for each read operation.
 *
 * The class integrates with:
 * - {@link HomekitRequestLogHandler} for request logging
 * - {@link HomekitHttpParser} for request parsing
 * - {@link HomekitHttpGenerator} for response generation
 *
 * Key responsibilities:
 * 1. Request body caching
 * 2. Multiple read support
 * 3. Stream delegation
 * 4. Memory-efficient body handling
 *
 * The implementation uses:
 * - {@link IOUtils} for efficient stream handling
 * - {@link ByteArrayInputStream} for body caching
 * - {@link ServletInputStream} for stream delegation
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
     */
public class HomekitRequestWrapper extends HttpServletRequestWrapper {

    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit RequestWrapper: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";

    private byte[] body;

    /**
     * Creates a new request wrapper.
     *
     * This constructor initializes the wrapper by reading and caching the
     * request body. If the body cannot be read, an empty byte array is used.
     *
     * @param request The HTTP request to wrap
     */
    public HomekitRequestWrapper(HttpServletRequest request) {
        super(request);
        try {
            body = IOUtils.toByteArray(request.getInputStream());
        } catch (IOException ex) {
            body = new byte[0];
        }
    }

    /**
     * Gets a new input stream for reading the request body.
     *
     * This method creates a new {@link DelegatingServletInputStream} that
     * reads from the cached request body, allowing multiple reads of the
     * same content.
     *
     * @return A new ServletInputStream for reading the request body
     * @throws IOException if an I/O error occurs
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new DelegatingServletInputStream(new ByteArrayInputStream(body));
    }

    /**
     * Delegating servlet input stream implementation.
     *
     * This inner class extends {@link ServletInputStream} to provide
     * stream delegation functionality, allowing the wrapped request to
     * be read multiple times while maintaining proper stream state.
     */
    public class DelegatingServletInputStream extends ServletInputStream {

        private final InputStream sourceStream;
        private boolean finished = false;

        /**
         * Creates a new delegating servlet input stream.
         *
         * This constructor initializes the stream with the given source
         * stream, which will be used for reading the request body.
         *
         * @param sourceStream The source stream to delegate to (never null)
     */
        public DelegatingServletInputStream(InputStream sourceStream) {
            this.sourceStream = sourceStream;
        }

        /**
         * Gets the underlying source stream.
         *
         * @return The source stream used for reading
     */
        public final InputStream getSourceStream() {
            return this.sourceStream;
        }

        /**
         * Reads a single byte from the stream.
         *
         * This method delegates to the source stream and tracks the
         * finished state of the stream.
         *
         * @return The next byte of data, or -1 if the end of the stream is reached
         * @throws IOException if an I/O error occurs
     */
        @Override
        public int read() throws IOException {
            int data = this.sourceStream.read();
            if (data == -1) {
                this.finished = true;
            }
            return data;
        }

        /**
         * Gets the number of bytes available for reading.
         *
         * @return The number of bytes available
         * @throws IOException if an I/O error occurs
     */
        @Override
        public int available() throws IOException {
            return this.sourceStream.available();
        }

        /**
         * Closes the stream and its underlying source stream.
         *
         * @throws IOException if an I/O error occurs
     */
        @Override
        public void close() throws IOException {
            super.close();
            this.sourceStream.close();
        }

        /**
         * Checks if the stream has reached its end.
         *
         * @return true if the stream has finished reading
     */
        @Override
        public boolean isFinished() {
            return this.finished;
        }

        /**
         * Checks if the stream is ready for reading.
         *
         * This implementation always returns true as the content
         * is already cached in memory.
         *
         * @return true
     */
        @Override
        public boolean isReady() {
            return true;
        }

        /**
         * Sets a read listener for asynchronous reading.
         *
         * This operation is not supported as the content is cached
         * in memory and read synchronously.
         *
         * @param readListener The read listener to set
         * @throws UnsupportedOperationException always
     */
        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException();
        }
    }
}
