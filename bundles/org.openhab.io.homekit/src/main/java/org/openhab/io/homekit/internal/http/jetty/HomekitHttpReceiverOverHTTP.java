
package org.openhab.io.homekit.internal.http.jetty;

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
import org.openhab.io.homekit.crypto.HomekitEncryptionEngine;
import org.openhab.io.homekit.crypto.HomekitEncryptionEngine.SequenceBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomekitHttpReceiverOverHTTP extends HttpReceiverOverHTTP implements HomekitHttpParser.ResponseHandler {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitHttpReceiverOverHTTP.class);

    private final HomekitHttpParser parser;
    private final HttpClient httpClient;
    private boolean shutdown;
    private boolean complete;
    private HomekitHttpVersion version;

    private byte[] decryptionKey;
    private long inboundSequenceCount = 0;
    private ByteBuffer decryptedInputBuffer;
    private ByteBuffer encryptedInputBuffer;
    private ByteBuffer encryptedOutputBuffer;

    public HomekitHttpReceiverOverHTTP(HttpChannelOverHTTP channel) {
        super(channel);
        httpClient = channel.getHttpDestination().getHttpClient();
        parser = new HomekitHttpParser(this, -1, httpClient.getHttpCompliance());
    }

    @Override
    public HomekitHttpChannelOverHTTP getHttpChannel() {
        return (HomekitHttpChannelOverHTTP) super.getHttpChannel();
    }

    private HomekitHttpConnectionOverHTTP getHttpConnection() {
        return (HomekitHttpConnectionOverHTTP) getHttpChannel().getHttpConnection();
    }

    @Override
    protected ByteBuffer getResponseBuffer() {
        return decryptedInputBuffer;
    }

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

    @Override
    protected ByteBuffer onUpgradeFrom() {
        if (BufferUtil.hasContent(decryptedInputBuffer)) {
            ByteBuffer upgradeBuffer = ByteBuffer.allocate(decryptedInputBuffer.remaining());
            upgradeBuffer.put(decryptedInputBuffer).flip();
            return upgradeBuffer;
        }
        return null;
    }

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
                        BufferUtil.flipToFill(encryptedInputBuffer);
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

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Read {} bytes {} from {}", read, BufferUtil.toDetailString(decryptedInputBuffer),
                            endPoint);
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
            if (LOG.isDebugEnabled()) {
                LOG.debug(x);
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
     * Parses a HTTP response in the receivers buffer.
     *
     * @return true to indicate that parsing should be interrupted (and will be resumed by another thread).
     */
    private boolean parse() {
        while (true) {
            boolean handle = parser.parseNext(decryptedInputBuffer);
            boolean complete = this.complete;
            this.complete = false;
            if (logger.isDebugEnabled()) {
                logger.debug("Parsed {}, remaining {} {}", handle, decryptedInputBuffer.remaining(), parser);
            }
            if (handle) {
                return true;
            }
            if (!decryptedInputBuffer.hasRemaining()) {
                return false;
            }
            if (complete) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Discarding unexpected content after response: {}",
                            BufferUtil.toDetailString(decryptedInputBuffer));
                }
                BufferUtil.clear(decryptedInputBuffer);
                return false;
            }
        }
    }

    @Override
    protected void fillInterested() {
        getHttpConnection().fillInterested();
    }

    private void shutdown() {
        // Mark this receiver as shutdown, so that we can
        // close the connection when the exchange terminates.
        // We cannot close the connection from here because
        // the request may still be in process.
        shutdown = true;

        // Shutting down the parser may invoke messageComplete() or earlyEOF().
        // In case of content delimited by EOF, without a Connection: close
        // header, the connection will be closed at exchange termination
        // thanks to the flag we have set above.
        parser.atEOF();
        parser.parseNext(BufferUtil.EMPTY_BUFFER);
    }

    @Override
    protected boolean isShutdown() {
        return shutdown;
    }

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

        return !responseBegin(exchange);
    }

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

        return HttpMethod.CONNECT.is(exchange.getRequest().getMethod()) && status == HttpStatus.OK_200;
    }

    @Override
    protected void reset() {
        super.reset();
        parser.reset();
    }

    @Override
    protected void dispose() {
        super.dispose();
        parser.close();
    }

    private void failAndClose(Throwable failure) {
        if (responseFailure(failure)) {
            getHttpConnection().close(failure);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void badMessage(int status, String reason) {
        // TODO Auto-generated method stub
        super.badMessage(status, reason);
    }

    public void setDecryptionKey(byte[] decryptionKey) {
        this.decryptionKey = decryptionKey;

        logger.info("Setting Decryption Key on {}", this);
    }

    public boolean hasDecryptionKey() {
        return (decryptionKey != null);
    }

    public byte[] getDecryptionKey() {
        return decryptionKey;
    }

}
