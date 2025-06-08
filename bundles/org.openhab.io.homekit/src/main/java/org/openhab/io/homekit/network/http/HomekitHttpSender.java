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
import java.nio.ByteBuffer;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpContent;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.client.HttpRequest;
import org.eclipse.jetty.client.HttpRequestException;
import org.eclipse.jetty.client.api.ContentProvider;
import org.eclipse.jetty.client.http.HttpSenderOverHTTP;
import org.eclipse.jetty.http.HttpGenerator;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.IteratingCallback;
import org.openhab.io.homekit.protocol.crypto.HomekitEncryptionEngine;
import org.openhab.io.homekit.protocol.crypto.HomekitEncryptionEngine.SequenceBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A specialized HTTP sender for HomeKit communication with encryption support.
 *
 * <p>
 * This class extends {@link org.eclipse.jetty.client.http.HttpSenderOverHTTP HttpSenderOverHTTP} to provide specialized
 * HTTP request
 * handling for HomeKit accessories, including encryption of request payloads and
 * proper sequence number management for secure communication.
 * </p>
 *
 * <p>
 * <b>Component Integration:</b>
 * </p>
 * <ul>
 * <li>{@link HomekitHttpChannel} for channel management</li>
 * <li>{@link HomekitHttpConnectionOverHTTP} for connection management</li>
 * <li>{@link HomekitEncryptionEngine} for payload encryption</li>
 * <li>{@link org.eclipse.jetty.client.HttpClient HttpClient} for HTTP operations</li>
 * </ul>
 *
 * <p>
 * <b>Key responsibilities:</b>
 * </p>
 * <ul>
 * <li>Sending and encrypting HTTP requests</li>
 * <li>Managing sequence numbers for secure communication</li>
 * <li>Handling request formatting and validation</li>
 * <li>Providing buffer management and recycling</li>
 * <li>Supporting both encrypted and unencrypted communication</li>
 * </ul>
 *
 * <p>
 * <b>Implementation details:</b>
 * </p>
 * <ul>
 * <li>{@link org.eclipse.jetty.io.ByteBufferPool ByteBufferPool} for efficient buffer management</li>
 * <li>{@link HomekitEncryptionEngine} for secure communication</li>
 * <li>{@link org.eclipse.jetty.client.HttpClient HttpClient} for HTTP client functionality</li>
 * <li>{@link org.eclipse.jetty.client.api.ContentProvider ContentProvider} for request content</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@NonNullByDefault
public class HomekitHttpSender extends HttpSenderOverHTTP {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitHttpSender.class);

    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit HttpSender: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_REQUEST = LOG_PREFIX + "Request - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    private final HttpGenerator generator = new HttpGenerator();
    private final HttpClient httpClient;
    private boolean shutdown;

    private byte @Nullable [] encryptionKey = null;
    private long outboundSequenceCount = 0;

    /**
     * Creates a new HomeKit HTTP sender for the given channel.
     *
     * <p>
     * This constructor initializes a sender with the specified HTTP channel.
     * </p>
     *
     * <p>
     * <b>Key implementation details:</b>
     * </p>
     * <ul>
     * <li>Uses provided {@link HomekitHttpChannel}</li>
     * <li>Sets up buffer management using {@link org.eclipse.jetty.io.ByteBufferPool ByteBufferPool}</li>
     * <li>Configures {@link org.eclipse.jetty.client.HttpClient HttpClient} for HTTP operations</li>
     * </ul>
     *
     * @param channel The HTTP channel to use for communication
     */
    public HomekitHttpSender(HomekitHttpChannel channel) {
        super(channel);
        httpClient = channel.getHttpDestination().getHttpClient();
        logger.debug("{}Initialized for channel {}", LOG_INIT, channel);
    }

    /**
     * Gets the HTTP channel associated with this sender.
     *
     * <p>
     * This method returns the specialized {@link HomekitHttpChannel} instance
     * that manages this sender's communication channel.
     * </p>
     *
     * @return The {@link HomekitHttpChannel} instance
     */
    @Override
    public HomekitHttpChannel getHttpChannel() {
        return (HomekitHttpChannel) super.getHttpChannel();
    }

    /**
     * Sends HTTP headers for the given exchange.
     *
     * <p>
     * This method handles the generation and transmission of HTTP headers,
     * including any necessary encryption of header content.
     * </p>
     *
     * <p>
     * <b>Key implementation details:</b>
     * </p>
     * <ul>
     * <li>Creates HeadersCallback for async processing</li>
     * <li>Handles header generation and encryption</li>
     * <li>Manages buffer lifecycle</li>
     * <li>Supports chunked transfer encoding</li>
     * </ul>
     *
     * @param exchange The HTTP exchange
     * @param content The HTTP content
     * @param callback The callback to invoke on completion
     */
    @Override
    @SuppressWarnings("null") // Parent HttpSenderOverHTTP interface doesn't constrain these parameters
    protected void sendHeaders(@Nullable HttpExchange exchange, @Nullable HttpContent content,
            @Nullable Callback callback) {
        if (exchange == null || content == null || callback == null) {
            if (callback != null) {
                callback.failed(new IllegalArgumentException("Exchange, content, and callback cannot be null"));
            }
            return;
        }

        try {
            logger.debug("{}Sending headers for exchange {}", LOG_REQUEST, exchange);
            new HeadersCallback(exchange, content, callback).iterate();
        } catch (Throwable x) {
            logger.error("{}Failed to send headers: {}", LOG_ERROR, x.getMessage(), x);
            callback.failed(x);
        }
    }

    /**
     * Sends HTTP content for the given exchange.
     *
     * <p>
     * This method handles the generation and transmission of HTTP content,
     * including encryption of the content when a key is set.
     * </p>
     *
     * <p>
     * <b>Key implementation details:</b>
     * </p>
     * <ul>
     * <li>Manages buffer allocation and recycling</li>
     * <li>Handles chunked transfer encoding</li>
     * <li>Supports content encryption</li>
     * <li>Manages sequence numbers</li>
     * <li>Handles buffer lifecycle</li>
     * </ul>
     *
     * @param exchange The HTTP exchange
     * @param content The HTTP content
     * @param callback The callback to invoke on completion
     */
    @Override
    @SuppressWarnings("null") // Parent HttpSenderOverHTTP interface doesn't constrain these parameters
    protected void sendContent(@Nullable HttpExchange exchange, @Nullable HttpContent content,
            @Nullable Callback callback) {
        if (exchange == null || content == null || callback == null) {
            if (callback != null) {
                callback.failed(new IllegalArgumentException("Exchange, content, and callback cannot be null"));
            }
            return;
        }

        try {
            ByteBufferPool bufferPool = httpClient.getByteBufferPool();
            ByteBuffer chunk = null;
            while (true) {
                ByteBuffer contentBuffer = content.getByteBuffer();
                boolean lastContent = content.isLast();
                HttpGenerator.Result result = generator.generateRequest(null, null, chunk, contentBuffer, lastContent);
                logger.debug("{}Generated content ({} bytes) - {}/{}", LOG_REQUEST,
                        contentBuffer == null ? -1 : contentBuffer.remaining(), result, generator);
                switch (result) {
                    case NEED_CHUNK: {
                        chunk = bufferPool.acquire(HttpGenerator.CHUNK_SIZE, false);
                        break;
                    }
                    case NEED_CHUNK_TRAILER: {
                        chunk = bufferPool.acquire(httpClient.getRequestBufferSize(), false);
                        break;
                    }
                    case FLUSH: {
                        EndPoint endPoint = getHttpChannel().getHttpConnection().getEndPoint();
                        if (chunk != null) {
                            if (!hasEncryptionKey()) {
                                endPoint.write(new ByteBufferRecyclerCallback(callback, bufferPool, chunk), chunk,
                                        contentBuffer);
                            } else {
                                ByteBuffer encryptedBuffer = encryptBuffers(endPoint, chunk, contentBuffer);
                                endPoint.write(new ByteBufferRecyclerCallback(callback, bufferPool, encryptedBuffer),
                                        encryptedBuffer);
                            }
                        } else {
                            if (!hasEncryptionKey()) {
                                endPoint.write(callback, contentBuffer);
                            } else {
                                ByteBuffer encryptedBuffer = encryptBuffers(endPoint, contentBuffer);
                                endPoint.write(callback, encryptedBuffer);
                            }
                        }
                        return;
                    }
                    case SHUTDOWN_OUT: {
                        shutdownOutput();
                        break;
                    }
                    case CONTINUE: {
                        if (lastContent) {
                            break;
                        }
                        callback.succeeded();
                        return;
                    }
                    case DONE: {
                        callback.succeeded();
                        return;
                    }
                    default: {
                        throw new IllegalStateException(result.toString());
                    }
                }
            }
        } catch (Throwable x) {
            logger.error("{}Failed to send content: {}", LOG_ERROR, x.getMessage(), x);
            callback.failed(x);
        }
    }

    /**
     * Encrypts one or more byte buffers using the HomeKit encryption engine.
     *
     * <p>
     * This method handles the encryption of request content using the HomeKit
     * encryption engine and manages sequence numbers for secure communication.
     * </p>
     *
     * <p>
     * <b>Key implementation details:</b>
     * </p>
     * <ul>
     * <li>Uses {@link HomekitEncryptionEngine} for encryption</li>
     * <li>Manages sequence numbers</li>
     * <li>Handles buffer concatenation</li>
     * <li>Supports multiple input buffers</li>
     * </ul>
     *
     * @param endpoint The endpoint for the connection
     * @param buffers The buffers to encrypt
     * @return A new encrypted buffer
     */
    protected ByteBuffer encryptBuffers(EndPoint endpoint, ByteBuffer... buffers) {
        int totalRemaining = 0;
        for (ByteBuffer b : buffers) {
            totalRemaining += b.remaining();
        }

        ByteBufferPool bufferPool = httpClient.getByteBufferPool();
        ByteBuffer flushBuffer = bufferPool.acquire(totalRemaining, true);
        BufferUtil.flipToFill(flushBuffer);

        for (ByteBuffer b : buffers) {
            BufferUtil.put(b, flushBuffer);
        }

        BufferUtil.flipToFlush(flushBuffer, 0);

        logger.debug("{}Encrypting {} bytes for endpoint {}", LOG_REQUEST, totalRemaining, endpoint.getRemoteAddress());

        ByteBuffer encryptedBuffer = bufferPool.acquire(httpClient.getResponseBufferSize(), true);

        SequenceBuffer sBuffer = null;
        try {
            byte[] nonNullEncryptionKey = encryptionKey;
            if (nonNullEncryptionKey == null) {
                throw new IllegalStateException("Encryption key is null but encryption was requested");
            }
            sBuffer = HomekitEncryptionEngine.encryptBuffer(encryptedBuffer, flushBuffer, nonNullEncryptionKey,
                    outboundSequenceCount);
            encryptedBuffer = sBuffer.buffer;
            outboundSequenceCount = sBuffer.sequenceNumber;

            logger.debug("{}Updated sequence number to {} for endpoint {}", LOG_REQUEST, outboundSequenceCount,
                    endpoint.getRemoteAddress());
        } catch (IOException e) {
            logger.error("{}Failed to encrypt buffer: {}", LOG_ERROR, e.getMessage(), e);
        }

        return encryptedBuffer;
    }

    /**
     * Resets the sender's state.
     *
     * This method resets both the generator and the parent class state.
     *
     * Key implementation details:
     * - Resets generator state
     * - Calls parent reset
     * - Logs state change
     */
    @Override
    protected void reset() {
        generator.reset();
        super.reset();
        logger.debug("{}Reset sender state", LOG_STATE);
    }

    /**
     * Disposes of the sender's resources.
     *
     * This method aborts the generator and cleans up any resources.
     *
     * Key implementation details:
     * - Aborts generator
     * - Calls parent dispose
     * - Shuts down output
     * - Logs state change
     */
    @Override
    protected void dispose() {
        generator.abort();
        super.dispose();
        shutdownOutput();
        logger.debug("{}Disposed sender resources", LOG_STATE);
    }

    /**
     * Shuts down the output stream.
     *
     * This method marks the sender as shutdown and logs the state change.
     *
     * Key implementation details:
     * - Sets shutdown flag
     * - Logs state change
     */
    private void shutdownOutput() {
        logger.debug("{}Shutting down output for request {}", LOG_STATE, getHttpExchange().getRequest());
        shutdown = true;
    }

    /**
     * Checks if the sender is shutdown.
     *
     * @return true if the sender is shutdown, false otherwise
     */
    @Override
    protected boolean isShutdown() {
        return shutdown;
    }

    /**
     * Returns a string representation of the sender.
     *
     * @return A string containing the class name and generator state
     */
    @Override
    public String toString() {
        return String.format("%s[%s]", super.toString(), generator);
    }

    /**
     * Sets the encryption key for this sender.
     *
     * @param encryptionKey The encryption key to set
     */
    public void setEncryptionKey(byte[] encryptionKey) {
        this.encryptionKey = encryptionKey;
        logger.info("{}Encryption key set", LOG_CONFIG);
    }

    /**
     * Checks if this sender has an encryption key set.
     *
     * @return true if an encryption key is set
     */
    public boolean hasEncryptionKey() {
        return encryptionKey != null;
    }

    /**
     * Gets the encryption key used by this sender.
     *
     * @return The encryption key, or null if not set
     */
    public byte @Nullable [] getEncryptionKey() {
        return encryptionKey;
    }

    /**
     * Gets the current outbound sequence number.
     *
     * This method returns the sequence number used for encrypting outgoing messages.
     * The sequence number is managed by {@link HomekitEncryptionEngine} for secure communication.
     *
     * @return The current outbound sequence number
     */
    public long getOutboundSequenceCount() {
        return outboundSequenceCount;
    }

    /**
     * Sets the outbound sequence number.
     *
     * This method configures the sequence number used for encrypting outgoing messages.
     * The sequence number is managed by {@link HomekitEncryptionEngine} for secure communication.
     *
     * @param outboundSequenceCount The sequence number to use
     */
    public void setOutboundSequenceCount(long outboundSequenceCount) {
        this.outboundSequenceCount = outboundSequenceCount;
        logger.debug("{}Outbound sequence count set to {}", LOG_STATE, outboundSequenceCount);
    }

    /**
     * Callback for handling header generation and transmission.
     *
     * This inner class manages the asynchronous generation and transmission
     * of HTTP headers, including any necessary encryption.
     */
    private class HeadersCallback extends IteratingCallback {
        private final HttpExchange exchange;
        private final Callback callback;
        private final MetaData.Request metaData;
        private @Nullable ByteBuffer headerBuffer;
        private @Nullable ByteBuffer chunkBuffer;
        private @Nullable ByteBuffer contentBuffer;
        private boolean lastContent;
        private boolean generated;

        /**
         * Creates a new headers callback.
         *
         * @param exchange The HTTP exchange
         * @param content The HTTP content
         * @param callback The callback to invoke on completion
         */
        public HeadersCallback(HttpExchange exchange, HttpContent content, Callback callback) {
            super(false);
            this.exchange = exchange;
            this.callback = callback;

            HttpRequest request = exchange.getRequest();
            ContentProvider requestContent = request.getContent();
            long contentLength = requestContent == null ? -1 : requestContent.getLength();
            String path = request.getPath();
            String query = request.getQuery();
            if (query != null) {
                path += "?" + query;
            }
            metaData = new MetaData.Request(request.getMethod(), new HttpURI(path), request.getVersion(),
                    request.getHeaders(), contentLength);
            metaData.setTrailerSupplier(request.getTrailers());

            if (!expects100Continue(request)) {
                content.advance();
                contentBuffer = content.getByteBuffer();
                lastContent = content.isLast();
            }
        }

        /**
         * Processes the next iteration of header generation.
         *
         * This method handles the generation and transmission of HTTP headers,
         * including any necessary encryption.
         *
         * @return The next action to take
         * @throws Exception if an error occurs
         */
        @Override
        protected Action process() throws Exception {
            while (true) {
                HttpGenerator.Result result = generator.generateRequest(metaData, headerBuffer, chunkBuffer,
                        contentBuffer, lastContent);
                logger.debug("{}Generated headers ({} bytes), chunk ({} bytes), content ({} bytes) - {}/{}",
                        LOG_REQUEST, headerBuffer == null ? -1 : headerBuffer.remaining(),
                        chunkBuffer == null ? -1 : chunkBuffer.remaining(),
                        contentBuffer == null ? -1 : contentBuffer.remaining(), result, generator);
                switch (result) {
                    case NEED_HEADER: {
                        headerBuffer = httpClient.getByteBufferPool().acquire(httpClient.getRequestBufferSize(), false);
                        break;
                    }
                    case NEED_CHUNK: {
                        chunkBuffer = httpClient.getByteBufferPool().acquire(HttpGenerator.CHUNK_SIZE, false);
                        break;
                    }
                    case NEED_CHUNK_TRAILER: {
                        chunkBuffer = httpClient.getByteBufferPool().acquire(httpClient.getRequestBufferSize(), false);
                        break;
                    }
                    case FLUSH: {
                        EndPoint endPoint = getHttpChannel().getHttpConnection().getEndPoint();
                        if (headerBuffer == null) {
                            headerBuffer = BufferUtil.EMPTY_BUFFER;
                        }
                        if (chunkBuffer == null) {
                            chunkBuffer = BufferUtil.EMPTY_BUFFER;
                        }
                        if (contentBuffer == null) {
                            contentBuffer = BufferUtil.EMPTY_BUFFER;
                        }
                        if (headerBuffer != null && chunkBuffer != null && contentBuffer != null) {
                            long bytes = Objects.requireNonNull(headerBuffer).remaining()
                                    + Objects.requireNonNull(chunkBuffer).remaining()
                                    + Objects.requireNonNull(contentBuffer).remaining();
                            ((HomekitHttpConnectionOverHTTP) getHttpChannel().getHttpConnection()).addBytesOut(bytes);

                            if (!hasEncryptionKey()) {
                                endPoint.write(this, headerBuffer, chunkBuffer, contentBuffer);
                            } else {
                                ByteBuffer encryptedBuffer = encryptBuffers(endPoint, headerBuffer, chunkBuffer,
                                        contentBuffer);
                                endPoint.write(this, encryptedBuffer);
                            }

                            generated = true;
                            return Action.SCHEDULED;
                        }
                    }
                    case SHUTDOWN_OUT: {
                        shutdownOutput();
                        return Action.SUCCEEDED;
                    }
                    case CONTINUE: {
                        if (generated) {
                            return Action.SUCCEEDED;
                        }
                        break;
                    }
                    case DONE: {
                        if (generated) {
                            return Action.SUCCEEDED;
                        }
                        // The headers have already been generated by some
                        // other thread, perhaps by a concurrent abort().
                        throw new HttpRequestException("Could not generate headers", exchange.getRequest());
                    }
                    default: {
                        throw new IllegalStateException(result.toString());
                    }
                }
            }
        }

        /**
         * Handles successful completion of the callback.
         */
        @Override
        public void succeeded() {
            release();
            super.succeeded();
        }

        /**
         * Handles failure of the callback.
         *
         * @param x The exception that caused the failure
         */
        @Override
        @SuppressWarnings("null") // Parent IteratingCallback interface doesn't constrain this parameter
        public void failed(@Nullable Throwable x) {
            release();
            if (x != null) {
                callback.failed(x);
                super.failed(x);
            } else {
                callback.failed(new IllegalStateException("Failed with null throwable"));
                super.failed(new IllegalStateException("Failed with null throwable"));
            }
        }

        /**
         * Handles successful completion of the callback.
         */
        @Override
        protected void onCompleteSuccess() {
            super.onCompleteSuccess();
            callback.succeeded();
        }

        /**
         * Releases the buffers used by this callback.
         */
        private void release() {
            ByteBufferPool bufferPool = httpClient.getByteBufferPool();
            if (!BufferUtil.isTheEmptyBuffer(headerBuffer)) {
                bufferPool.release(headerBuffer);
            }
            headerBuffer = null;
            if (!BufferUtil.isTheEmptyBuffer(chunkBuffer)) {
                bufferPool.release(chunkBuffer);
            }
            chunkBuffer = null;
            contentBuffer = null;
        }
    }

    /**
     * Callback for recycling byte buffers.
     *
     * This inner class manages the recycling of byte buffers after they have
     * been used, ensuring proper resource cleanup.
     */
    private class ByteBufferRecyclerCallback extends Callback.Nested {
        private final ByteBufferPool pool;
        private final ByteBuffer[] buffers;

        /**
         * Creates a new byte buffer recycler callback.
         *
         * @param callback The callback to invoke on completion
         * @param pool The buffer pool to use
         * @param buffers The buffers to recycle
         */
        private ByteBufferRecyclerCallback(Callback callback, ByteBufferPool pool, ByteBuffer... buffers) {
            super(callback);
            this.pool = pool;
            this.buffers = buffers;
        }

        /**
         * Handles successful completion of the callback.
         */
        @Override
        public void succeeded() {
            for (ByteBuffer buffer : buffers) {
                assert !buffer.hasRemaining();
                pool.release(buffer);
            }
            super.succeeded();
        }

        /**
         * Handles failure of the callback.
         *
         * @param x The exception that caused the failure
         */
        @Override
        @SuppressWarnings("null") // Parent Callback.Nested interface doesn't constrain this parameter
        public void failed(@Nullable Throwable x) {
            for (ByteBuffer buffer : buffers) {
                pool.release(buffer);
            }
            if (x != null) {
                super.failed(x);
            } else {
                super.failed(new IllegalStateException("Failed with null throwable"));
            }
        }
    }
}
