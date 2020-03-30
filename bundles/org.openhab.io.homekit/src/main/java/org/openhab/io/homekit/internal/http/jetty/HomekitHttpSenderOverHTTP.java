package org.openhab.io.homekit.internal.http.jetty;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpContent;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.client.HttpRequest;
import org.eclipse.jetty.client.HttpRequestException;
import org.eclipse.jetty.client.api.ContentProvider;
import org.eclipse.jetty.client.http.HttpChannelOverHTTP;
import org.eclipse.jetty.client.http.HttpSenderOverHTTP;
import org.eclipse.jetty.http.HttpGenerator;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.IteratingCallback;
import org.openhab.io.homekit.crypto.HomekitEncryptionEngine;
import org.openhab.io.homekit.crypto.HomekitEncryptionEngine.SequenceBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomekitHttpSenderOverHTTP extends HttpSenderOverHTTP {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitHttpSenderOverHTTP.class);

    private final HttpGenerator generator = new HttpGenerator();
    private final HttpClient httpClient;
    private boolean shutdown;

    private byte[] encryptionKey;
    private long outboundSequenceCount = 0;

    public HomekitHttpSenderOverHTTP(HomekitHttpChannelOverHTTP channel) {
        super(channel);
        httpClient = channel.getHttpDestination().getHttpClient();
    }

    @Override
    public HttpChannelOverHTTP getHttpChannel() {
        return super.getHttpChannel();
    }

    @Override
    protected void sendHeaders(HttpExchange exchange, HttpContent content, Callback callback) {
        try {
            new HeadersCallback(exchange, content, callback).iterate();
        } catch (Throwable x) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(x);
            }
            callback.failed(x);
        }
    }

    @Override
    protected void sendContent(HttpExchange exchange, HttpContent content, Callback callback) {
        try {
            ByteBufferPool bufferPool = httpClient.getByteBufferPool();
            ByteBuffer chunk = null;
            while (true) {
                ByteBuffer contentBuffer = content.getByteBuffer();
                boolean lastContent = content.isLast();
                HttpGenerator.Result result = generator.generateRequest(null, null, chunk, contentBuffer, lastContent);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Generated content ({} bytes) - {}/{}",
                            contentBuffer == null ? -1 : contentBuffer.remaining(), result, generator);
                }
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
            if (LOG.isDebugEnabled()) {
                LOG.debug(x);
            }
            callback.failed(x);
        }
    }

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

        logger.debug("[{}] encryptBuffers={}", endpoint.getRemoteAddress().toString(),
                BufferUtil.toDetailString(flushBuffer));

        ByteBuffer encryptedBuffer = bufferPool.acquire(httpClient.getResponseBufferSize(), true);

        SequenceBuffer sBuffer = null;
        try {
            sBuffer = HomekitEncryptionEngine.encryptBuffer(encryptedBuffer, flushBuffer, encryptionKey,
                    outboundSequenceCount);
            encryptedBuffer = sBuffer.buffer;
            outboundSequenceCount = sBuffer.sequenceNumber;

            logger.debug("[{}] outboundSequenceCount={}", endpoint.getRemoteAddress().toString(),
                    outboundSequenceCount);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return encryptedBuffer;
    }

    @Override
    protected void reset() {
        generator.reset();
        super.reset();
    }

    @Override
    protected void dispose() {
        generator.abort();
        super.dispose();
        shutdownOutput();
    }

    private void shutdownOutput() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Request shutdown output {}", getHttpExchange().getRequest());
        }
        shutdown = true;
    }

    @Override
    protected boolean isShutdown() {
        return shutdown;
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", super.toString(), generator);
    }

    private class HeadersCallback extends IteratingCallback {
        private final HttpExchange exchange;
        private final Callback callback;
        private final MetaData.Request metaData;
        private ByteBuffer headerBuffer;
        private ByteBuffer chunkBuffer;
        private ByteBuffer contentBuffer;
        private boolean lastContent;
        private boolean generated;

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

        @Override
        protected Action process() throws Exception {
            while (true) {
                HttpGenerator.Result result = generator.generateRequest(metaData, headerBuffer, chunkBuffer,
                        contentBuffer, lastContent);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Generated headers ({} bytes), chunk ({} bytes), content ({} bytes) - {}/{}",
                            headerBuffer == null ? -1 : headerBuffer.remaining(),
                            chunkBuffer == null ? -1 : chunkBuffer.remaining(),
                            contentBuffer == null ? -1 : contentBuffer.remaining(), result, generator);
                }
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
                        long bytes = headerBuffer.remaining() + chunkBuffer.remaining() + contentBuffer.remaining();
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

        @Override
        public void succeeded() {
            release();
            super.succeeded();
        }

        @Override
        public void failed(Throwable x) {
            release();
            callback.failed(x);
            super.failed(x);
        }

        @Override
        protected void onCompleteSuccess() {
            super.onCompleteSuccess();
            callback.succeeded();
        }

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

    private class ByteBufferRecyclerCallback extends Callback.Nested {
        private final ByteBufferPool pool;
        private final ByteBuffer[] buffers;

        private ByteBufferRecyclerCallback(Callback callback, ByteBufferPool pool, ByteBuffer... buffers) {
            super(callback);
            this.pool = pool;
            this.buffers = buffers;
        }

        @Override
        public void succeeded() {
            for (ByteBuffer buffer : buffers) {
                assert !buffer.hasRemaining();
                pool.release(buffer);
            }
            super.succeeded();
        }

        @Override
        public void failed(Throwable x) {
            for (ByteBuffer buffer : buffers) {
                pool.release(buffer);
            }
            super.failed(x);
        }
    }

    public void setEncryptionKey(byte[] encryptionKey) {
        this.encryptionKey = encryptionKey;

        logger.info("Setting Encryption Key on {}", this);

    }

    public boolean hasEncryptionKey() {
        return (encryptionKey != null);
    }

    public byte[] getEncryptionKey() {
        return encryptionKey;
    }

}
