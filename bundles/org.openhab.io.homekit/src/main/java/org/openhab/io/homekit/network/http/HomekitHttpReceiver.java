package org.openhab.io.homekit.network.http;

import java.nio.ByteBuffer;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.client.http.HttpChannelOverHTTP;
import org.eclipse.jetty.client.http.HttpReceiverOverHTTP;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.util.BufferUtil;
import org.openhab.io.homekit.protocol.crypto.HomekitEncryptionEngine;
import org.openhab.io.homekit.protocol.crypto.HomekitEncryptionEngine.SequenceBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A specialized HTTP receiver for HomeKit communication with decryption support.
 *
 * This class extends {@link org.eclipse.jetty.client.http.HttpReceiverOverHTTP HttpReceiverOverHTTP} to provide specialized HTTP response
 * handling for HomeKit accessories, including decryption of response payloads and
 * proper sequence number management for secure communication.
 *
 * The receiver works in conjunction with:
 * - {@link HomekitHttpChannel} for channel management
 * - {@link HomekitHttpParser} for response parsing
 * - {@link org.openhab.io.homekit.protocol.crypto.HomekitEncryptionEngine HomekitEncryptionEngine} for payload decryption
 * - {@link HomekitHttpConnectionOverHTTP} for connection management
 *
 * Key responsibilities:
 * 1. Receiving and decrypting HTTP responses
 * 2. Managing sequence numbers for secure communication
 * 3. Handling response parsing and validation
 * 4. Providing buffer management and recycling
 * 5. Supporting both encrypted and unencrypted communication
 *
 * The implementation uses:
 * - {@link HomekitHttpParser} for response parsing
 * - {@link org.eclipse.jetty.io.ByteBufferPool ByteBufferPool} for efficient buffer management
 * - {@link org.openhab.io.homekit.protocol.crypto.HomekitEncryptionEngine HomekitEncryptionEngine} for secure communication
 * - {@link org.eclipse.jetty.client.HttpClient HttpClient} for HTTP client functionality
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0
 */
public class HomekitHttpReceiver extends HttpReceiverOverHTTP implements HomekitHttpParser.ResponseHandler {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitHttpReceiver.class);

    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit HttpReceiver: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "HomekitAccessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    private final HomekitHttpParser parser;
    private final HttpClient httpClient;
    private boolean shutdown;
    private boolean complete;
    @SuppressWarnings("unused")
    private HomekitHttpVersion version;

    private byte[] decryptionKey;
    private long inboundSequenceCount = 0;
    private ByteBuffer decryptedInputBuffer;
    private ByteBuffer encryptedInputBuffer;
    @SuppressWarnings("unused")
    private ByteBuffer encryptedOutputBuffer;

    /**
     * Creates a new HomeKit HTTP receiver for the given channel.
     *
     * This constructor initializes a receiver with the specified HTTP channel.
     *
     * Key implementation details:
     * - Uses provided {@link org.eclipse.jetty.client.http.HttpChannelOverHTTP HttpChannelOverHTTP}
     * - Initializes {@link HomekitHttpParser} with HTTP compliance settings
     * - Sets up buffer management using {@link org.eclipse.jetty.io.ByteBufferPool ByteBufferPool}
     * - Configures {@link org.eclipse.jetty.client.HttpClient HttpClient} for HTTP operations
     *
     * @param channel The HTTP channel to use for communication
     */
    public HomekitHttpReceiver(HttpChannelOverHTTP channel) {
        super(channel);
        httpClient = channel.getHttpDestination().getHttpClient();
        parser = new HomekitHttpParser(this, -1, httpClient.getHttpCompliance());
    }

    /**
     * Gets the HTTP channel associated with this receiver.
     *
     * This method returns the specialized {@link HomekitHttpChannel} instance
     * that manages this receiver's communication channel.
     *
     * @return The {@link HomekitHttpChannel} instance
     */
    @Override
    public HomekitHttpChannel getHttpChannel() {
        return (HomekitHttpChannel) super.getHttpChannel();
    }

    /**
     * Gets the HTTP connection associated with this receiver.
     *
     * This method returns the specialized {@link HomekitHttpConnectionOverHTTP} instance
     * that manages the underlying network connection.
     *
     * @return The {@link HomekitHttpConnectionOverHTTP} instance
     */
    private HomekitHttpConnectionOverHTTP getHttpConnection() {
        return (HomekitHttpConnectionOverHTTP) getHttpChannel().getHttpConnection();
    }

    /**
     * Gets the response buffer for this receiver.
     *
     * This method returns the decrypted input buffer used for storing
     * decrypted response content. The buffer is managed by the
     * {@link org.eclipse.jetty.io.ByteBufferPool ByteBufferPool}.
     *
     * @return The decrypted input {@link java.nio.ByteBuffer ByteBuffer}
     */
    @Override
    protected ByteBuffer getResponseBuffer() {
        return decryptedInputBuffer;
    }

    /**
     * Releases a buffer back to the pool.
     *
     * This method validates and releases a buffer back to the
     * {@link org.eclipse.jetty.io.ByteBufferPool ByteBufferPool}.
     *
     * @param buffer The buffer to release
     * @throws IllegalStateException if the buffer is null or contains content
     */
    private void releaseBuffer(ByteBuffer buffer) {
        if (buffer == null) {
            throw new IllegalStateException();
        }
        if (BufferUtil.hasContent(buffer)) {
            throw new IllegalStateException();
        }
        HttpClient client = getHttpDestination().getHttpClient();
        ByteBufferPool bufferPool = client.getByteBufferPool();
        bufferPool.release(buffer);
    }

    /**
     * Handles protocol upgrade scenarios.
     *
     * This method is called when the connection is being upgraded to a different
     * protocol. It ensures any remaining content in the decrypted buffer is
     * properly handled.
     *
     * @return A new {@link java.nio.ByteBuffer ByteBuffer} containing any remaining content, or null if none
     */
    @Override
    protected ByteBuffer onUpgradeFrom() {
        if (BufferUtil.hasContent(decryptedInputBuffer)) {
            ByteBuffer upgradeBuffer = ByteBuffer.allocate(decryptedInputBuffer.remaining());
            upgradeBuffer.put(decryptedInputBuffer).flip();
            return upgradeBuffer;
        }
        return null;
    }

    /**
     * Receives and processes incoming data.
     *
     * This method handles the core receive loop, managing both encrypted and
     * unencrypted communication. It works with:
     * - {@link org.eclipse.jetty.io.EndPoint EndPoint} for network I/O
     * - {@link org.openhab.io.homekit.protocol.crypto.HomekitEncryptionEngine HomekitEncryptionEngine} for decryption
     * - {@link HomekitHttpParser} for response parsing
     *
     * Key implementation details:
     * - Manages buffer lifecycle
     * - Handles connection upgrades
     * - Processes encrypted/unencrypted data
     * - Maintains sequence numbers
     * - Handles connection closure
     */
    @Override
    public void receive() {
        try {
            HomekitHttpConnectionOverHTTP connection = getHttpConnection();
            EndPoint endPoint = connection.getEndPoint();
            while (true) {
                boolean upgraded = connection != endPoint.getConnection();

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

                    if (encryptedInputBuffer.hasRemaining()) {

                        if (logger.isTraceEnabled()) {
                            logger.trace(
                                    "[{}] Receive : Before decryption : encryptedInputBuffer={}, decryptedInputBuffer={}",
                                    endPoint.getRemoteAddress().toString(),
                                    BufferUtil.toDetailString(encryptedInputBuffer),
                                    BufferUtil.toDetailString(decryptedInputBuffer));
                        }

                        int position = decryptedInputBuffer.position();
                        SequenceBuffer sBuffer = HomekitEncryptionEngine.decryptBuffer(decryptedInputBuffer,
                                encryptedInputBuffer, decryptionKey, inboundSequenceCount);
                        BufferUtil.flipToFlush(decryptedInputBuffer, position);

                        if (logger.isTraceEnabled()) {
                            logger.trace(
                                    "[{}] Receive : After decryption : encryptedInputBuffer={}, decryptedInputBuffer={}, sBuffer={}}",
                                    endPoint.getRemoteAddress().toString(),
                                    BufferUtil.toDetailString(encryptedInputBuffer),
                                    BufferUtil.toDetailString(decryptedInputBuffer),
                                    BufferUtil.toDetailString(sBuffer.buffer));
                        }

                        // read = BufferUtil.append(decryptedInputBuffer, sBuffer.buffer);
                        // decryptedInputBuffer = sBuffer.buffer;
                        inboundSequenceCount = sBuffer.sequenceNumber;

                        if (logger.isTraceEnabled()) {
                            logger.trace(
                                    "[{}] Receive : Before parsing : encryptedInputBuffer={}, decryptedInputBuffer={}",
                                    endPoint.getRemoteAddress().toString(),
                                    BufferUtil.toDetailString(encryptedInputBuffer),
                                    BufferUtil.toDetailString(decryptedInputBuffer));
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
                    if (decryptedInputBuffer.hasRemaining()) {
                        if (parse()) {
                            return;
                        }
                    }
                    releaseBuffer(decryptedInputBuffer);
                    decryptedInputBuffer = null;
                    fillInterested();
                    return;
                } else {
                    releaseBuffer(decryptedInputBuffer);
                    decryptedInputBuffer = null;
                    releaseBuffer(encryptedInputBuffer);
                    encryptedInputBuffer = null;
                    shutdown();
                    return;
                }
            }
        } catch (Throwable x) {
            if (logger.isDebugEnabled()) {
                logger.debug("{}Exception caught in receive", LOG_ERROR, x);
            }
            BufferUtil.clear(decryptedInputBuffer);
            if (decryptedInputBuffer != null) {
                releaseBuffer(decryptedInputBuffer);
                decryptedInputBuffer = null;
            }
            failAndClose(x);
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
        while (true) {
            boolean handle = parser.parseNext(decryptedInputBuffer);
            boolean complete = this.complete;
            this.complete = false;
            if (logger.isDebugEnabled()) {
                logger.debug("{}Parsed {}, remaining {} {}", LOG_STATE, handle, decryptedInputBuffer.remaining(),
                        parser);
            }
            if (handle) {
                return true;
            }
            if (!decryptedInputBuffer.hasRemaining()) {
                return false;
            }
            if (complete) {
                if (logger.isDebugEnabled()) {
                    logger.debug("{}Discarding unexpected content after response: {}", LOG_WARN,
                            BufferUtil.toDetailString(decryptedInputBuffer));
                }
                BufferUtil.clear(decryptedInputBuffer);
                return false;
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
     * Starts processing a response.
     *
     * This method handles the initial processing of an HTTP response,
     * including version checking and header handling. It works with:
     * - {@link org.eclipse.jetty.client.HttpExchange HttpExchange} for request/response management
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
        HttpExchange exchange = getHttpExchange();
        if (exchange == null) {
            return false;
        }

        String method = exchange.getRequest().getMethod();
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
     * @param status The response status code
     * @param reason The response reason phrase
     */
    @SuppressWarnings("deprecation")
    @Override
    public void badMessage(int status, String reason) {
        super.badMessage(status, reason);
        logger.warn("{}Bad message received - Status: {}, Reason: {}", LOG_WARN, status, reason);
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
    public byte[] getDecryptionKey() {
        return decryptionKey;
    }
}
