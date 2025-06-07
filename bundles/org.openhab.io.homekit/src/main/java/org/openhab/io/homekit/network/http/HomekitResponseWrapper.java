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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.util.HomekitResizableByteArrayOutputStream;

/**
 * Wraps HTTP responses to enable caching and multiple writes of the response body.
 *
 * This class extends {@link HttpServletResponseWrapper} to provide the ability to
 * cache response content and write it multiple times, which is essential for
 * logging and processing HomeKit HTTP responses. It uses a resizable byte array
 * output stream to efficiently handle response content of varying sizes.
 *
 * The class integrates with:
 * - {@link HomekitRequestLogHandler} for response logging
 * - {@link HomekitHttpParser} for response parsing
 * - {@link HomekitHttpGenerator} for response generation
 * - {@link HomekitResizableByteArrayOutputStream} for content buffering
 *
 * Key responsibilities:
 * 1. Response body caching
 * 2. Multiple write support
 * 3. Content length management
 * 4. Buffer size control
 * 5. Response state tracking
 *
 * The implementation uses:
 * - {@link ServletOutputStream} for binary content
 * - {@link PrintWriter} for character content
 * - {@link StandardCharsets} for character encoding
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
     */
public class HomekitResponseWrapper extends HttpServletResponseWrapper {

    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit ResponseWrapper: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";

    private final HomekitResizableByteArrayOutputStream content = new HomekitResizableByteArrayOutputStream(1024);

    @Nullable
    private ServletOutputStream outputStream;

    @Nullable
    private PrintWriter writer;

    @Nullable
    private Integer contentLength;

    /**
     * Creates a new response wrapper.
     *
     * This constructor initializes the wrapper with a resizable byte array
     * output stream for caching response content.
     *
     * @param response The HTTP response to wrap
     */
    public HomekitResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    /**
     * Sends an error response with the specified status code.
     *
     * This method copies any cached content to the response before sending
     * the error. If the response is already committed, it sets the status
     * code instead.
     *
     * @param sc The status code to send
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void sendError(int sc) throws IOException {
        copyBodyToResponse(false);
        try {
            super.sendError(sc);
        } catch (IllegalStateException ex) {
            super.setStatus(sc);
        }
    }

    /**
     * Sends an error response with the specified status code and message.
     *
     * This method copies any cached content to the response before sending
     * the error. If the response is already committed, it sets the status
     * code and message instead.
     *
     * @param sc The status code to send
     * @param msg The error message to send
     * @throws IOException if an I/O error occurs
     */
    @Override
    @SuppressWarnings("deprecation")
    public void sendError(int sc, String msg) throws IOException {
        copyBodyToResponse(false);
        try {
            super.sendError(sc, msg);
        } catch (IllegalStateException ex) {
            super.setStatus(sc, msg);
        }
    }

    /**
     * Sends a redirect response to the specified location.
     *
     * This method copies any cached content to the response before sending
     * the redirect.
     *
     * @param location The location to redirect to
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void sendRedirect(String location) throws IOException {
        copyBodyToResponse(false);
        super.sendRedirect(location);
    }

    /**
     * Gets the output stream for writing binary content.
     *
     * This method creates a new {@link ResponseServletOutputStream} if one
     * doesn't exist, which caches the content while writing to the response.
     *
     * @return A ServletOutputStream for writing binary content
     * @throws IOException if an I/O error occurs
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (this.outputStream == null) {
            ServletOutputStream responseStream = getResponse().getOutputStream();
            if (responseStream != null) {
                this.outputStream = new ResponseServletOutputStream(responseStream);
            } else {
                throw new IOException("ServletOutputStream is null");
            }
        }
        ServletOutputStream currentStream = this.outputStream;
        if (currentStream != null) {
            return currentStream;
        } else {
            throw new IOException("ServletOutputStream not initialized");
        }
    }

    /**
     * Gets the writer for writing character content.
     *
     * This method creates a new {@link ResponsePrintWriter} if one doesn't
     * exist, which caches the content while writing to the response.
     *
     * @return A PrintWriter for writing character content
     * @throws IOException if an I/O error occurs
     */
    @Override
    public PrintWriter getWriter() throws IOException {
        if (this.writer == null) {
            String characterEncoding = getCharacterEncoding();
            @SuppressWarnings("null") // ResponsePrintWriter constructor guaranteed non-null result
            PrintWriter newWriter = (characterEncoding != null ? new ResponsePrintWriter(characterEncoding)
                    : new ResponsePrintWriter(StandardCharsets.UTF_8.name()));
            this.writer = newWriter;
        }
        PrintWriter currentWriter = this.writer;
        if (currentWriter != null) {
            return currentWriter;
        } else {
            throw new IOException("PrintWriter is null");
        }
    }

    /**
     * Flushes the buffer.
     *
     * This method does not flush the underlying response as the content
     * has not been copied to it yet.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void flushBuffer() throws IOException {
        // do not flush the underlying response as the content as not been copied to it yet
    }

    /**
     * Sets the content length of the response.
     *
     * This method resizes the content buffer if necessary to accommodate
     * the specified length.
     *
     * @param len The content length to set
     */
    @Override
    public void setContentLength(int len) {
        if (len > this.content.size()) {
            this.content.resize(len);
        }
        this.contentLength = len;
    }

    /**
     * Sets the content length of the response as a long value.
     *
     * This method resizes the content buffer if necessary to accommodate
     * the specified length. It throws an exception if the length exceeds
     * Integer.MAX_VALUE.
     *
     * @param len The content length to set
     * @throws IllegalArgumentException if the length exceeds Integer.MAX_VALUE
     */
    @Override
    public void setContentLengthLong(long len) {
        if (len > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Content-Length exceeds ContentCachingResponseWrapper's maximum ("
                    + Integer.MAX_VALUE + "): " + len);
        }
        int lenInt = (int) len;
        if (lenInt > this.content.size()) {
            this.content.resize(lenInt);
        }
        this.contentLength = lenInt;
    }

    /**
     * Sets the buffer size of the response.
     *
     * This method resizes the content buffer if necessary to accommodate
     * the specified size.
     *
     * @param size The buffer size to set
     */
    @Override
    public void setBufferSize(int size) {
        if (size > this.content.size()) {
            this.content.resize(size);
        }
    }

    /**
     * Resets the buffer.
     *
     * This method clears the cached content without affecting the response.
     */
    @Override
    public void resetBuffer() {
        this.content.reset();
    }

    /**
     * Resets the response.
     *
     * This method clears the cached content and resets the response state.
     */
    @Override
    public void reset() {
        super.reset();
        this.content.reset();
    }

    /**
     * Gets the cached response content as a byte array.
     *
     * @return The cached content as a byte array
     */
    public byte[] getContentAsByteArray() {
        return this.content.toByteArray();
    }

    /**
     * Gets the current size of the cached content.
     *
     * @return The size of the cached content
     */
    public int getContentSize() {
        return this.content.size();
    }

    /**
     * Copies the complete cached body content to the response.
     *
     * This method copies all cached content to the response and flushes
     * the response buffer.
     *
     * @throws IOException if an I/O error occurs
     */
    public void copyBodyToResponse() throws IOException {
        copyBodyToResponse(true);
    }

    /**
     * Copies the cached body content to the response.
     *
     * This method copies the cached content to the response, optionally
     * setting the content length based on the complete parameter.
     *
     * @param complete Whether to set the content length for the complete
     *            cached body content
     * @throws IOException if an I/O error occurs
     */
    protected void copyBodyToResponse(boolean complete) throws IOException {
        if (this.content.size() > 0) {
            HttpServletResponse rawResponse = (HttpServletResponse) getResponse();
            if ((complete || this.contentLength != null) && !rawResponse.isCommitted()) {
                rawResponse.setContentLength(
                        complete ? this.content.size() : (this.contentLength != null ? this.contentLength : 0));
                this.contentLength = null;
            }
            this.content.writeTo(rawResponse.getOutputStream());
            this.content.reset();
            if (complete) {
                super.flushBuffer();
            }
        }
    }

    /**
     * Response servlet output stream implementation.
     *
     * This inner class extends {@link ServletOutputStream} to provide
     * stream delegation functionality, allowing the wrapped response to
     * be written to while caching the content.
     */
    private class ResponseServletOutputStream extends ServletOutputStream {

        private final ServletOutputStream os;

        /**
         * Creates a new response servlet output stream.
         *
         * @param os The servlet output stream to delegate to
     */
        public ResponseServletOutputStream(ServletOutputStream os) {
            this.os = os;
        }

        /**
         * Writes a single byte to the stream.
         *
         * This method writes the byte to both the cache and the
         * underlying output stream.
         *
         * @param b The byte to write
         * @throws IOException if an I/O error occurs
     */
        @Override
        public void write(int b) throws IOException {
            content.write(b);
        }

        /**
         * Writes a portion of a byte array to the stream.
         *
         * This method writes the bytes to both the cache and the
         * underlying output stream.
         *
         * @param b The byte array to write
         * @param off The offset in the array
         * @param len The number of bytes to write
         * @throws IOException if an I/O error occurs
     */
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            content.write(b, off, len);
        }

        /**
         * Checks if the stream is ready for writing.
         *
         * @return true if the stream is ready
     */
        @Override
        public boolean isReady() {
            return this.os.isReady();
        }

        /**
         * Sets a write listener for asynchronous writing.
         *
         * This operation is not supported as the content is cached
         * in memory and written synchronously.
         *
         * @param writeListener The write listener to set
         * @throws UnsupportedOperationException always
     */
        @Override
        public void setWriteListener(WriteListener writeListener) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Response print writer implementation.
     *
     * This inner class extends {@link PrintWriter} to provide
     * writer delegation functionality, allowing the wrapped response to
     * be written to while caching the content.
     */
    private class ResponsePrintWriter extends PrintWriter {

        /**
         * Creates a new response print writer.
         *
         * @param characterEncoding The character encoding to use
         * @throws UnsupportedEncodingException if the encoding is not supported
     */
        public ResponsePrintWriter(String characterEncoding) throws UnsupportedEncodingException {
            super(new OutputStreamWriter(content, characterEncoding));
        }

        /**
         * Writes a portion of a character array.
         *
         * This method writes the characters to both the cache and the
         * underlying writer.
         *
         * @param buf The character array to write
         * @param off The offset in the array
         * @param len The number of characters to write
     */
        @Override
        public void write(char[] buf, int off, int len) {
            super.write(buf, off, len);
            super.flush();
        }

        /**
         * Writes a portion of a string.
         *
         * This method writes the string to both the cache and the
         * underlying writer.
         *
         * @param s The string to write
         * @param off The offset in the string
         * @param len The number of characters to write
     */
        @Override
        public void write(String s, int off, int len) {
            super.write(s, off, len);
            super.flush();
        }

        /**
         * Writes a single character.
         *
         * This method writes the character to both the cache and the
         * underlying writer.
         *
         * @param c The character to write
     */
        @Override
        public void write(int c) {
            super.write(c);
            super.flush();
        }
    }
}
