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
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.client.http.HttpChannelOverHTTP;
import org.eclipse.jetty.client.http.HttpReceiverOverHTTP;
import org.eclipse.jetty.http.BadMessageException;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.openhab.io.homekit.protocol.crypto.HomekitEncryptionEngine;
import org.openhab.io.homekit.protocol.crypto.HomekitEncryptionEngine.SequenceBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A specialized HTTP receiver for HomeKit communication with decryption support.
 *
 * <p>
 * This class extends {@link org.eclipse.jetty.client.http.HttpReceiverOverHTTP HttpReceiverOverHTTP} to provide
 * specialized HTTP response
 * handling for HomeKit accessories, including decryption of response payloads and
 * proper sequence number management for secure communication.
 * </p>
 *
 * <p>
 * The class integrates with:
 * </p>
 * <ul>
 * <li>{@link HomekitHttpChannel} for channel lifecycle management</li>
 * <li>{@link HomekitHttpParser} for response parsing and validation</li>
 * <li>{@link HomekitEncryptionEngine} for secure message handling</li>
 * <li>{@link HomekitHttpConnectionOverHTTP} for connection management</li>
 * <li>{@link org.eclipse.jetty.client.HttpClient HttpClient} for HTTP operations</li>
 * </ul>
 *
 * <p>
 * <b>Key Features:</b>
 * </p>
 * <ul>
 * <li>HTTP response reception and parsing</li>
 * <li>Message decryption support</li>
 * <li>Sequence number management</li>
 * <li>Buffer pool optimization</li>
 * <li>Protocol upgrade handling</li>
 * <li>Thread-safe operations</li>
 * </ul>
 *
 * <p>
 * <b>Security Considerations:</b>
 * </p>
 * <ul>
 * <li>Validates message integrity</li>
 * <li>Enforces protocol compliance</li>
 * <li>Manages encryption keys</li>
 * <li>Handles sequence numbers</li>
 * <li>Ensures proper initialization</li>
 * <li>Maintains thread safety</li>
 * </ul>
 *
 * <p>
 * <b>Implementation Details:</b>
 * </p>
 * <ul>
 * <li>Efficient buffer management</li>
 * <li>Secure message handling</li>
 * <li>Protocol compliance</li>
 * <li>Connection lifecycle</li>
 * <li>Detailed logging</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@NonNullByDefault
public class HomekitHttpReceiver extends HttpReceiverOverHTTP implements HomekitHttpParser.ResponseHandler {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitHttpReceiver.class);

    protected static final boolean debug = logger.isDebugEnabled();

    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "HomeKit HTTP Receiver: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Initialization - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State Change - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Configuration - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "Accessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    private final HomekitHttpParser parser;
    private final HttpClient httpClient;
    private boolean shutdown;
    private boolean complete;
    @SuppressWarnings("unused")
    private @Nullable HomekitHttpVersion version;

    private byte @Nullable [] decryptionKey;
    private long inboundSequenceCount = 0;
    @Nullable
    private ByteBuffer decryptedInputBuffer;
    @Nullable
    private ByteBuffer encryptedInputBuffer;
    @SuppressWarnings("unused")
    @Nullable
    private ByteBuffer encryptedOutputBuffer;

    /**
     * Creates a new HomeKit HTTP receiver for the given channel.
     *
     * <p>
     * This constructor initializes a receiver with the specified HTTP channel,
     * setting up the foundation for secure HTTP response handling.
     * </p>
     *
     * <p>
     * <b>Implementation details:</b>
     * </p>
     * <ul>
     * <li>Initializes HTTP channel</li>
     * <li>Configures HTTP parser</li>
     * <li>Sets up buffer management</li>
     * <li>Configures HTTP client</li>
     * <li>Ensures thread safety</li>
     * </ul>
     *
     * @param channel The HTTP channel to use for communication
     */
    public HomekitHttpReceiver(HttpChannelOverHTTP channel) {
        super(channel);
        httpClient = channel.getHttpDestination().getHttpClient();
        parser = new HomekitHttpParser(this, -1, httpClient.getHttpCompliance());
        logger.debug("{}Initialized with HTTP channel", LOG_INIT);
    }

    /**
     * Gets the HTTP channel associated with this receiver.
     *
     * <p>
     * This method returns the specialized HomeKit HTTP channel instance
     * that manages this receiver's communication channel.
     * </p>
     *
     * <p>
     * <b>Implementation details:</b>
     * </p>
     * <ul>
     * <li>Retrieves channel instance</li>
     * <li>Performs type casting</li>
     * <li>Ensures thread safety</li>
     * </ul>
     *
     * @return The HomeKit HTTP channel instance
     */
    @Override
    public HomekitHttpChannel getHttpChannel() {
        return (HomekitHttpChannel) super.getHttpChannel();
    }

    /**
     * Gets the HTTP connection associated with this receiver.
     *
     * <p>
     * This method returns the specialized HomeKit HTTP connection instance
     * that manages the underlying network connection.
     * </p>
     *
     * <p>
     * <b>Implementation details:</b>
     * </p>
     * <ul>
     * <li>Retrieves connection instance</li>
     * <li>Performs type casting</li>
     * <li>Ensures thread safety</li>
     * </ul>
     *
     * @return The HomeKit HTTP connection instance
     */
    private HomekitHttpConnectionOverHTTP getHttpConnection() {
        return (HomekitHttpConnectionOverHTTP) getHttpChannel().getHttpConnection();
    }

    /**
     * Gets the response buffer for this receiver.
     *
     * <p>
     * This method returns the decrypted input buffer used for storing
     * decrypted response content.
     * </p>
     *
     * <p>
     * <b>Implementation details:</b>
     * </p>
     * <ul>
     * <li>Retrieves decrypted buffer</li>
     * <li>Ensures buffer availability</li>
     * <li>Maintains thread safety</li>
     * </ul>
     *
     * @return The decrypted input buffer
     */
    @Override
    protected ByteBuffer getResponseBuffer() {
        ByteBuffer buffer = decryptedInputBuffer;
        if (buffer == null) {
            throw new IllegalStateException("Decrypted input buffer is not initialized");
        }
        return buffer;
    }

    /**
     * Releases a buffer back to the pool.
     *
     * <p>
     * This method validates and releases a buffer back to the buffer pool
     * for reuse.
     * </p>
     *
     * <p>
     * <b>Implementation details:</b>
     * </p>
     * <ul>
     * <li>Validates buffer state</li>
     * <li>Releases buffer to pool</li>
     * <li>Ensures thread safety</li>
     * <li>Handles error cases</li>
     * </ul>
     *
     * @param buffer The buffer to release
     * @throws IllegalStateException if the buffer is null or contains content
     */
    private void releaseBuffer(@Nullable ByteBuffer buffer) {
        if (buffer == null) {
            logger.error("{}Cannot release null buffer", LOG_ERROR);
            throw new IllegalStateException("Buffer cannot be null");
        }
        if (BufferUtil.hasContent(buffer)) {
            logger.error("{}Cannot release buffer with content", LOG_ERROR);
            throw new IllegalStateException("Buffer contains content");
        }
        HttpClient client = getHttpDestination().getHttpClient();
        ByteBufferPool bufferPool = client.getByteBufferPool();
        bufferPool.release(buffer);
        logger.debug("{}Released buffer to pool", LOG_STATE);
    }

    /**
     * Handles protocol upgrade scenarios.
     *
     * <p>
     * This method is called when the connection is being upgraded to a different
     * protocol, ensuring proper handling of any remaining content.
     * </p>
     *
     * <p>
     * <b>Implementation details:</b>
     * </p>
     * <ul>
     * <li>Checks for remaining content</li>
     * <li>Creates upgrade buffer</li>
     * <li>Transfers content</li>
     * <li>Ensures thread safety</li>
     * <li>Logs upgrade process</li>
     * </ul>
     *
     * @return A new buffer containing any remaining content, or null if none
     */
    @Override
    protected ByteBuffer onUpgradeFrom() {
        if (decryptedInputBuffer != null && decryptedInputBuffer.hasRemaining()) {
            logger.debug("{}Handling protocol upgrade with remaining content", LOG_STATE);
            // Null Pointer Access Warning Checked
            ByteBuffer upgradeBuffer = ByteBuffer.allocate(Objects.requireNonNull(decryptedInputBuffer).remaining());
            upgradeBuffer.put(decryptedInputBuffer).flip();
            return upgradeBuffer;
        }
        logger.debug("{}Protocol upgrade with no remaining content", LOG_STATE);
        return ByteBuffer.allocate(0); // Return empty buffer instead of null
    }

    /**
     * Receives and processes incoming data.
     *
     * <p>
     * This method handles the core receive loop, managing both encrypted and
     * unencrypted communication. It works with:
     * </p>
     * <ul>
     * <li>{@link org.eclipse.jetty.io.EndPoint EndPoint} for network I/O</li>
     * <li>{@link org.openhab.io.homekit.protocol.crypto.HomekitEncryptionEngine HomekitEncryptionEngine} for
     * decryption</li>
     * <li>{@link HomekitHttpParser} for response parsing</li>
     * </ul>
     *
     * <p>
     * <b>Key implementation details:</b>
     * </p>
     * <ul>
     * <li>Manages buffer lifecycle</li>
     * <li>Handles connection upgrades</li>
     * <li>Processes encrypted/unencrypted data</li>
     * <li>Maintains sequence numbers</li>
     * <li>Handles connection closure</li>
     * </ul>
     */
    @Override
    public void receive() {
        try {
            HomekitHttpConnectionOverHTTP connection = getHttpConnection();
            EndPoint endPoint = connection.getEndPoint();
            while (true) {
                boolean upgraded = !connection.equals(endPoint.getConnection());

                // Connection may be closed or upgraded in a parser callback.
                if (connection.isClosed() || upgraded) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("{} {}", connection, upgraded ? "upgraded" : "closed");
                    }
                    releaseBuffer(decryptedInputBuffer);
                    decryptedInputBuffer = null;
                    return;
                }

                ByteBufferPool bufferPool = httpClient.getByteBufferPool();

                // if (parse()) {
                // return;
                // }

                int read = 0;

                if (!hasDecryptionKey()) {

                    if (decryptedInputBuffer == null) {
                        decryptedInputBuffer = bufferPool.acquire(httpClient.getResponseBufferSize(), true);
                        BufferUtil.clear(decryptedInputBuffer);
                    } else {
                        BufferUtil.compact(decryptedInputBuffer);
                        // BufferUtil.flipToFill(decryptedInputBuffer);
                    }

                    if (logger.isTraceEnabled()) {
                        logger.trace("[{}] Receive : Start : decryptedInputBuffer={}",
                                endPoint.getRemoteAddress().toString(),
                                BufferUtil.toDetailString(decryptedInputBuffer));
                    }

                    if (logger.isTraceEnabled()) {
                        logger.trace("[{}] Receive : Before fill : decryptedInputBuffer={}",
                                endPoint.getRemoteAddress().toString(),
                                BufferUtil.toDetailString(decryptedInputBuffer));
                    }

                    read = endPoint.fill(decryptedInputBuffer);
                } else {

                    if (decryptedInputBuffer == null) {
                        decryptedInputBuffer = bufferPool.acquire(httpClient.getResponseBufferSize(), true);
                        BufferUtil.clear(decryptedInputBuffer);
                    } else {
                        BufferUtil.compact(decryptedInputBuffer);
                        BufferUtil.flipToFill(decryptedInputBuffer);
                    }

                    if (encryptedInputBuffer == null) {
                        encryptedInputBuffer = bufferPool.acquire(httpClient.getResponseBufferSize(), true);
                        BufferUtil.clear(encryptedInputBuffer);
                    } else {
                        BufferUtil.compact(encryptedInputBuffer);
                        // BufferUtil.flipToFill(encryptedInputBuffer);
                    }

                    if (logger.isTraceEnabled()) {
                        logger.trace("[{}] Receive : Before fill : encryptedInputBuffer={}, decryptedInputBuffer={}",
                                endPoint.getRemoteAddress().toString(), BufferUtil.toDetailString(encryptedInputBuffer),
                                BufferUtil.toDetailString(decryptedInputBuffer));
                    }

                    int netFilled = endPoint.fill(encryptedInputBuffer);
                    if (logger.isTraceEnabled()) {
                        logger.trace("[{}] Receive : Read {} bytes into {} from the endpoint {}",
                                endPoint.getRemoteAddress().toString(), netFilled,
                                BufferUtil.toDetailString(encryptedInputBuffer), endPoint.toString());
                    }

                    if (encryptedInputBuffer != null && encryptedInputBuffer.hasRemaining()
                            && decryptedInputBuffer != null) {

                        if (logger.isTraceEnabled()) {
                            logger.trace(
                                    "[{}] Receive : Before decryption : encryptedInputBuffer={}, decryptedInputBuffer={}",
                                    endPoint.getRemoteAddress().toString(),
                                    BufferUtil.toDetailString(encryptedInputBuffer),
                                    BufferUtil.toDetailString(decryptedInputBuffer));
                        }

                        if (decryptionKey != null) {
                            // Null Pointer Access Warning Checked
                            // Explicitly check and assign to local variables to prevent NPE
                            ByteBuffer safeDecryptedBuffer = Objects.requireNonNull(decryptedInputBuffer);
                            ByteBuffer safeEncryptedBuffer = Objects.requireNonNull(encryptedInputBuffer);
                            byte[] safeDecryptionKey = Objects.requireNonNull(decryptionKey);

                            int position = safeDecryptedBuffer.position();
                            SequenceBuffer sBuffer = HomekitEncryptionEngine.decryptBuffer(safeDecryptedBuffer,
                                    safeEncryptedBuffer, safeDecryptionKey, inboundSequenceCount);
                            BufferUtil.flipToFlush(safeDecryptedBuffer, position);

                            if (logger.isTraceEnabled()) {
                                logger.trace(
                                        "[{}] Receive : After decryption : encryptedInputBuffer={}, decryptedInputBuffer={}, sBuffer={}}",
                                        endPoint.getRemoteAddress().toString(),
                                        BufferUtil.toDetailString(safeEncryptedBuffer),
                                        BufferUtil.toDetailString(safeDecryptedBuffer),
                                        BufferUtil.toDetailString(sBuffer.buffer));
                            }

                            // read = BufferUtil.append(decryptedInputBuffer, sBuffer.buffer);
                            // decryptedInputBuffer = sBuffer.buffer;
                            inboundSequenceCount = sBuffer.sequenceNumber;

                            if (logger.isTraceEnabled()) {
                                logger.trace(
                                        "[{}] Receive : Before parsing : encryptedInputBuffer={}, decryptedInputBuffer={}",
                                        endPoint.getRemoteAddress().toString(),
                                        BufferUtil.toDetailString(safeEncryptedBuffer),
                                        BufferUtil.toDetailString(safeDecryptedBuffer));
                            }
                        } else {
                            logger.warn("{}Cannot decrypt: decryption key is null", LOG_WARN);
                        }

                    } else {
                        read = netFilled;
                    }
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("{}Read {} bytes {} from {}", LOG_STATE, read,
                            BufferUtil.toDetailString(decryptedInputBuffer), endPoint);
                }

                if (read > 0) {
                    connection.addBytesIn(read);
                    if (parse()) {
                        return;
                    }
                } else if (read == 0) {
                    if (decryptedInputBuffer != null && decryptedInputBuffer.hasRemaining()) {
                        if (parse()) {
                            return;
                        }
                    }
                    // Null Pointer Access Warning Checked
                    releaseBuffer(decryptedInputBuffer);
                    decryptedInputBuffer = null;
                    fillInterested();
                    return;
                } else {
                    if (decryptedInputBuffer != null) {
                        releaseBuffer(decryptedInputBuffer);
                        decryptedInputBuffer = null;
                    }
                    if (encryptedInputBuffer != null) {
                        releaseBuffer(encryptedInputBuffer);
                        encryptedInputBuffer = null;
                    }
                    shutdown();
                    return;
                }
            }
        } catch (IOException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("{}Exception caught in receive", LOG_ERROR, e);
            }
            if (decryptedInputBuffer != null) {
                BufferUtil.clear(decryptedInputBuffer);
                releaseBuffer(decryptedInputBuffer);
                decryptedInputBuffer = null;
            }
            failAndClose(e);
        } catch (RuntimeException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("{}Exception caught in receive", LOG_ERROR, e);
            }
            if (decryptedInputBuffer != null) {
                BufferUtil.clear(decryptedInputBuffer);
                releaseBuffer(decryptedInputBuffer);
                decryptedInputBuffer = null;
            }
            failAndClose(e);
        } catch (Error e) {
            if (logger.isDebugEnabled()) {
                logger.debug("{}Exception caught in receive", LOG_ERROR, e);
            }
            if (decryptedInputBuffer != null) {
                BufferUtil.clear(decryptedInputBuffer);
                releaseBuffer(decryptedInputBuffer);
                decryptedInputBuffer = null;
            }
            failAndClose(e);
        }
    }

    /**
     * Parses HTTP responses in the receiver's buffer.
     *
     * This method processes the content in the decrypted input buffer using
     * the {@link HomekitHttpParser}. It handles:
     * - Response parsing
     * - Content validation
     * - Buffer management
     * - Error conditions
     *
     * @return true if parsing should be interrupted (will be resumed by another thread)
     */
    private boolean parse() {
        ByteBuffer buffer = decryptedInputBuffer;
        if (buffer == null) {
            return false;
        }

        while (true) {
            try {
                boolean handle = parser.parseNext(buffer);
                boolean complete = this.complete;
                this.complete = false;
                if (logger.isDebugEnabled()) {
                    logger.debug("{}Parsed {}, remaining {} {}", LOG_STATE, handle, buffer.remaining(), parser);
                }
                if (handle) {
                    return true;
                }
                if (!buffer.hasRemaining()) {
                    return false;
                }
                if (complete) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("{}Discarding unexpected content after response: {}", LOG_WARN,
                                BufferUtil.toDetailString(buffer));
                    }
                    BufferUtil.clear(buffer);
                    return false;
                }
            } catch (BadMessageException e) {
                // Handle bad message exceptions more gracefully
                int status = e.getCode();
                String reason = e.getReason() != null ? e.getReason() : "Bad Message";

                logger.warn("{}Parse error occurred - Status: {}, Reason: {}, Parser State: {}", LOG_WARN, status,
                        reason, parser.getState());

                // For 4xx client errors, treat as expected error responses
                if (status >= 400 && status < 500) {
                    logger.debug("{}Treating 4xx parse error as error response - Status: {}, Reason: {}", LOG_STATE,
                            status, reason);

                    // Try to create a synthetic error response
                    try {
                        HttpExchange exchange = getHttpExchange();
                        if (exchange != null) {
                            exchange.getResponse().status(status).reason(reason);
                            complete = true;
                            parser.setHeadResponse(true);
                            responseBegin(exchange);
                            responseSuccess(exchange);
                            return false;
                        }
                    } catch (Exception recoveryException) {
                        logger.debug("{}Failed to create synthetic error response: {}", LOG_ERROR,
                                recoveryException.getMessage());
                    }
                }

                // For other errors, try to reset and continue
                if (status >= 500 || status < 400) {
                    logger.debug("{}Attempting parser recovery for status: {}", LOG_STATE, status);
                    try {
                        if (parser.attemptRecovery()) {
                            BufferUtil.clear(buffer);
                            logger.debug("{}Parser recovery successful for status: {}", LOG_STATE, status);
                            return false;
                        } else {
                            logger.debug("{}Parser recovery failed for status: {}", LOG_STATE, status);
                        }
                    } catch (Exception resetException) {
                        logger.debug("{}Parser recovery threw exception: {}", LOG_ERROR, resetException.getMessage());
                    }
                }

                // If all recovery attempts fail, let the exception propagate
                logger.error("{}Parse error could not be recovered - Status: {}, Reason: {}", LOG_ERROR, status,
                        reason);
                throw e;
            } catch (Exception e) {
                // Handle unexpected exceptions during parsing
                logger.warn("{}Unexpected exception during parsing: {} - attempting recovery", LOG_WARN,
                        e.getMessage());

                try {
                    // Try to use the parser's recovery method first
                    if (parser.attemptRecovery()) {
                        BufferUtil.clear(buffer);
                        logger.debug("{}Parser recovery successful after unexpected exception", LOG_STATE);
                        return false;
                    } else {
                        // Fallback to basic reset
                        parser.reset();
                        BufferUtil.clear(buffer);
                        logger.debug("{}Fallback parser reset successful after unexpected exception", LOG_STATE);
                        return false;
                    }
                } catch (Exception resetException) {
                    logger.error("{}Failed to recover from parsing exception: {}", LOG_ERROR,
                            resetException.getMessage(), resetException);
                    // Re-throw as IOException to trigger connection closure
                    throw new RuntimeException("Failed to recover from parsing exception", e);
                }
            }
        }
    }

    /**
     * Registers interest in filling the buffer.
     *
     * This method notifies the connection that we're ready to receive more data.
     * It works with {@link HomekitHttpConnectionOverHTTP} to manage the
     * asynchronous I/O operations.
     */
    @Override
    protected void fillInterested() {
        getHttpConnection().fillInterested();
    }

    /**
     * Shuts down the receiver.
     *
     * This method marks the receiver as shutdown and closes the parser.
     * The connection will be closed when the current exchange terminates.
     * It works with:
     * - {@link HomekitHttpParser} for parser shutdown
     * - {@link org.eclipse.jetty.util.BufferUtil BufferUtil} for buffer management
     */
    private void shutdown() {
        shutdown = true;
        parser.atEOF();
        parser.parseNext(BufferUtil.EMPTY_BUFFER);
        logger.debug("{}Receiver shut down", LOG_STATE);
    }

    /**
     * Checks if the receiver is shutdown.
     *
     * @return true if the receiver is shutdown
     */
    @Override
    protected boolean isShutdown() {
        return shutdown;
    }

    /**
     * Handles the start of an HTTP response.
     *
     * This method processes the initial response line and sets up the response
     * handling. It works with:
     * - {@link org.eclipse.jetty.http.HttpMethod HttpMethod} for method validation
     * - {@link org.eclipse.jetty.http.HttpStatus HttpStatus} for status code handling
     *
     * @param version The HTTP version
     * @param status The response status code
     * @param reason The response reason phrase
     * @return true if processing should continue
     */
    @Override
    public boolean startResponse(HomekitHttpVersion version, int status, String reason) {
        this.version = version; // Store version for potential future use

        HttpExchange exchange = getHttpExchange();
        if (exchange == null) {
            return false;
        }

        String method = exchange.getRequest().getMethod();
        if (method == null) {
            return false;
        }

        // Handle 4xx error responses differently to prevent content processing issues
        if (status >= 400 && status < 500) {
            logger.debug("{}Received 4xx error response - Status: {}, Reason: {}", LOG_STATE, status, reason);
            exchange.getResponse().version(HomekitHttpVersion.convert(version)).status(status).reason(reason);

            // For 4xx errors, we don't expect content, so handle them immediately
            if (status != HttpStatus.CONTINUE_100) {
                complete = true;
            }

            // Mark as HEAD response to skip content processing
            parser.setHeadResponse(true);

            boolean proceed = responseBegin(exchange);
            if (!proceed) {
                return true;
            }

            // Complete the response immediately for 4xx errors
            responseSuccess(exchange);
            return false;
        }

        parser.setHeadResponse(
                HttpMethod.HEAD.is(method) || (HttpMethod.CONNECT.is(method) && status == HttpStatus.OK_200));
        exchange.getResponse().version(HomekitHttpVersion.convert(version)).status(status).reason(reason);

        if (version == HomekitHttpVersion.EVENT_1_0) {
            exchange.getResponse().getHeaders().add("X-HOMEKIT-EVENT", "True");
        }

        logger.debug("{}Starting response processing - Status: {}, Method: {}", LOG_STATE, status, method);
        return !responseBegin(exchange);
    }

    /**
     * Handles message completion.
     *
     * This method is called when a complete response message has been received
     * and processed. It works with:
     * - {@link org.eclipse.jetty.client.HttpExchange HttpExchange} for exchange management
     * - {@link org.eclipse.jetty.http.HttpStatus HttpStatus} for status code handling
     * - {@link org.eclipse.jetty.http.HttpMethod HttpMethod} for method validation
     *
     * @return true if processing should continue
     */
    @Override
    public boolean messageComplete() {
        HttpExchange exchange = getHttpExchange();
        if (exchange == null) {
            return false;
        }

        int status = exchange.getResponse().getStatus();

        if (status != HttpStatus.CONTINUE_100) {
            complete = true;
        }

        boolean proceed = responseSuccess(exchange);
        if (!proceed) {
            return true;
        }

        if (status == HttpStatus.SWITCHING_PROTOCOLS_101) {
            return true;
        }

        logger.debug("{}Message complete - Status: {}", LOG_STATE, status);
        return HttpMethod.CONNECT.is(exchange.getRequest().getMethod()) && status == HttpStatus.OK_200;
    }

    /**
     * Resets the receiver's state.
     *
     * This method resets both the parser and the parent class state.
     * It works with:
     * - {@link HomekitHttpParser} for parser reset
     * - {@link org.eclipse.jetty.client.http.HttpReceiverOverHTTP HttpReceiverOverHTTP} for parent state reset
     */
    @Override
    protected void reset() {
        super.reset();
        parser.reset();
        logger.debug("{}Receiver state reset", LOG_STATE);
    }

    /**
     * Disposes of the receiver's resources.
     *
     * This method closes the parser and cleans up any resources.
     * It works with:
     * - {@link HomekitHttpParser} for parser cleanup
     * - {@link org.eclipse.jetty.client.http.HttpReceiverOverHTTP HttpReceiverOverHTTP} for parent cleanup
     */
    @Override
    protected void dispose() {
        super.dispose();
        parser.close();
        logger.debug("{}Receiver resources disposed", LOG_STATE);
    }

    /**
     * Handles failure and closes the connection.
     *
     * This method manages failure scenarios and ensures proper connection closure.
     * It works with:
     * - {@link HomekitHttpConnectionOverHTTP} for connection management
     * - {@link org.eclipse.jetty.client.http.HttpReceiverOverHTTP HttpReceiverOverHTTP} for failure handling
     *
     * @param failure The failure that occurred
     */
    private void failAndClose(Throwable failure) {
        if (responseFailure(failure)) {
            getHttpConnection().close(failure);
        }
        logger.error("{}Connection failed: {}", LOG_ERROR, failure.getMessage());
    }

    /**
     * Handles bad message responses.
     *
     * This method processes invalid HTTP messages. It works with:
     * - {@link org.eclipse.jetty.http.HttpStatus HttpStatus} for status code handling
     * - {@link org.eclipse.jetty.client.http.HttpReceiverOverHTTP HttpReceiverOverHTTP} for base handling
     *
     * @param exception The bad message exception
     */
    @Override
    public void badMessage(@Nullable BadMessageException exception) {
        if (exception != null) {
            int status = exception.getCode();
            String reason = exception.getReason() != null ? exception.getReason() : "Bad Message";
            Throwable cause = exception.getCause();

            // Provide detailed logging with context
            if (cause != null) {
                logger.warn("{}Bad message received - Status: {}, Reason: {}, Cause: {}, Parser State: {}", LOG_WARN,
                        status, reason, cause.getClass().getSimpleName(), parser.getState());
                if (logger.isDebugEnabled()) {
                    logger.debug("{}Bad message exception details", LOG_ERROR, exception);
                }
            } else {
                logger.warn("{}Bad message received - Status: {}, Reason: {}, Parser State: {}", LOG_WARN, status,
                        reason, parser.getState());
            }

            // Handle 4xx responses more gracefully
            if (status >= 400 && status < 500) {
                logger.debug("{}Treating 4xx bad message as error response - Status: {}, Reason: {}", LOG_STATE, status,
                        reason);

                // Try to complete the exchange gracefully for 4xx errors
                try {
                    HttpExchange exchange = getHttpExchange();
                    if (exchange != null && !complete) {
                        exchange.getResponse().status(status).reason(reason);
                        complete = true;

                        // Attempt to complete the response gracefully
                        if (responseBegin(exchange)) {
                            responseSuccess(exchange);
                            logger.debug("{}Successfully handled 4xx error as response - Status: {}", LOG_STATE,
                                    status);
                            return;
                        }
                    }
                } catch (Exception recoveryException) {
                    logger.debug("{}Failed to handle 4xx error gracefully: {}", LOG_ERROR,
                            recoveryException.getMessage());
                }
            }

            // For non-4xx errors or failed recovery, use standard handling
            badMessage(status, reason);
        } else {
            logger.warn("{}Received null BadMessageException", LOG_WARN);
            badMessage(HttpStatus.BAD_REQUEST_400, "Unknown Bad Message");
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void badMessage(int status, @Nullable String reason) {
        logger.warn("{}Bad message received - Status: {}, Reason: {}", LOG_WARN, status, reason);
        super.badMessage(status, reason);
    }

    @Override
    public void parsedTrailer(@Nullable HttpField trailer) {
        if (trailer != null) {
            logger.debug("{}Parsed trailer: {}", LOG_STATE, trailer);
        }
        super.parsedTrailer(trailer);
    }

    @Override
    public void parsedHeader(@Nullable HttpField header) {
        if (header != null) {
            logger.debug("{}Parsed header: {}", LOG_STATE, header);
        }
        super.parsedHeader(header);
    }

    @Override
    public boolean content(@Nullable ByteBuffer buffer) {
        if (buffer != null) {
            logger.debug("{}Received content: {} bytes", LOG_STATE, buffer.remaining());
            try {
                return super.content(buffer);
            } catch (NullPointerException e) {
                // Handle case where parent class networkBuffer is null
                logger.error(
                        "{}NullPointerException in parent content handler - networkBuffer is null. "
                                + "This can happen during HTTP parsing errors. Buffer size: {}",
                        LOG_WARN, buffer.remaining(), e);

                // Try to handle the content directly without parent class
                try {
                    // Signal that we've handled the content - use a no-op callback
                    return responseContent(getHttpExchange(), buffer, Callback.NOOP);
                } catch (Exception fallbackException) {
                    logger.error("{}Failed to handle content after NullPointerException: {}", LOG_ERROR,
                            fallbackException.getMessage(), fallbackException);
                    return false;
                }
            }
        } else {
            // Handle null buffer gracefully - this can happen with 4xx error responses
            logger.debug("{}Received null content buffer (likely 4xx error response)", LOG_STATE);
            return false;
        }
    }

    /**
     * Sets the decryption key for this receiver.
     *
     * This method configures the key used for decrypting incoming messages.
     * The key is used by {@link org.openhab.io.homekit.protocol.crypto.HomekitEncryptionEngine HomekitEncryptionEngine}
     * for secure communication.
     *
     * @param decryptionKey The key to use for decryption
     */
    public void setDecryptionKey(byte[] decryptionKey) {
        this.decryptionKey = decryptionKey;
        logger.info("{}Decryption key set", LOG_CONFIG);
    }

    /**
     * Checks if this receiver has a decryption key set.
     *
     * @return true if a decryption key is set
     */
    public boolean hasDecryptionKey() {
        return (decryptionKey != null);
    }

    /**
     * Gets the decryption key used by this receiver.
     *
     * @return The decryption key, or null if not set
     */
    public byte @Nullable [] getDecryptionKey() {
        return decryptionKey;
    }
}
