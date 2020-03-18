package org.openhab.io.homekit.internal.http.jetty;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadPendingException;
import java.nio.channels.WritePendingException;
import java.util.concurrent.Executor;

import org.eclipse.jetty.io.AbstractConnection;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.openhab.io.homekit.crypto.HomekitEncryptionEngine;
import org.openhab.io.homekit.crypto.HomekitEncryptionEngine.SequenceBuffer;
import org.openhab.io.homekit.util.Byte;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DecryptedHomekitEndPoint implements EndPoint {

    protected static final Logger logger = LoggerFactory.getLogger(DecryptedHomekitEndPoint.class);

    private Throwable failure;

    private final long created = System.currentTimeMillis();

    private Executor executor;
    private Connection connection;
    private Callback connectionCallback;

    private final ByteBufferPool bufferPool;
    private int inputBufferSize = 2048;
    private boolean encryptedInputBufferUnderflown;
    private boolean useDirectBuffers = false;
    private ByteBuffer decryptedInputBuffer;
    private ByteBuffer encryptedInputBuffer;
    private ByteBuffer encryptedOutputBuffer;

    private long inboundSequenceCount = 0;
    private long outboundSequenceCount = 0;

    private final byte[] encryptionKey;
    private final byte[] decryptionKey;

    private final EndPoint encryptedEndPoint;

    public DecryptedHomekitEndPoint(EndPoint encryptedEndpoint, Executor executor, ByteBufferPool byteBufferPool,
            boolean useDirectBuffers, byte[] encryptionKey, byte[] decryptionKey) {
        this.encryptedEndPoint = encryptedEndpoint;
        this.bufferPool = byteBufferPool;
        this.useDirectBuffers = useDirectBuffers;
        this.executor = executor;
        this.encryptionKey = encryptionKey;
        this.decryptionKey = decryptionKey;

        if (logger.isTraceEnabled()) {
            logger.trace("[{}] DecryptedHomekitEndPoint : Setting Encryption Key {}", getRemoteAddress().toString(),
                    Byte.toHexString(this.decryptionKey));
            logger.trace("[{}] DecryptedHomekitEndPoint : Setting Decryption Key {}", getRemoteAddress().toString(),
                    Byte.toHexString(this.encryptionKey));

        }
    }

    @Override
    public long getIdleTimeout() {
        return encryptedEndPoint.getIdleTimeout();
    }

    @Override
    public void setIdleTimeout(long idleTimeout) {
        encryptedEndPoint.setIdleTimeout(idleTimeout);
    }

    @Override
    public boolean isOpen() {
        return encryptedEndPoint.isOpen();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return encryptedEndPoint.getLocalAddress();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return encryptedEndPoint.getRemoteAddress();
    }

    @Override
    public Object getTransport() {
        return encryptedEndPoint;
    }

    @Override
    public boolean isOutputShutdown() {
        return encryptedEndPoint.isOutputShutdown();
    }

    @Override
    public boolean isInputShutdown() {
        return BufferUtil.isEmpty(decryptedInputBuffer) && (encryptedEndPoint.isInputShutdown());
    }

    @Override
    public void setConnection(Connection connection) {
        if (connection instanceof AbstractConnection) {
            AbstractConnection a = (AbstractConnection) connection;
            if (a.getInputBufferSize() < inputBufferSize) {
                a.setInputBufferSize(inputBufferSize);
            }
        }
        this.connection = connection;
    }

    @Override
    public int fill(ByteBuffer buffer) throws IOException {

        // int bufferPosition = buffer.position();

        try {
            synchronized (this) {
                if (logger.isTraceEnabled()) {
                    logger.trace("[{}] Fill : Start [{}]", getRemoteAddress().toString(), this.toString());
                }

                int filled = -2;
                try {

                    // Do we already have some decrypted data?
                    if (BufferUtil.hasContent(decryptedInputBuffer)) {
                        if (logger.isTraceEnabled()) {
                            logger.trace("[{}] Fill : Appending already decrypted data {} to {}",
                                    getRemoteAddress().toString(), BufferUtil.toDetailString(decryptedInputBuffer),
                                    BufferUtil.toDetailString(buffer));
                        }
                        return filled = BufferUtil.append(buffer, decryptedInputBuffer);
                    }

                    // loop filling and unwrapping until we have something
                    while (true) {
                        if (encryptedInputBuffer == null) {
                            encryptedInputBuffer = bufferPool.acquire(inputBufferSize, useDirectBuffers);
                        } else {
                            BufferUtil.compact(encryptedInputBuffer);
                            BufferUtil.flipToFill(encryptedInputBuffer);
                        }

                        if (decryptedInputBuffer == null) {
                            decryptedInputBuffer = bufferPool.acquire(inputBufferSize, useDirectBuffers);
                        } else {
                            BufferUtil.compact(decryptedInputBuffer);
                            BufferUtil.flipToFill(decryptedInputBuffer);
                        }

                        // Let's try reading some encrypted data... even if we have some already.
                        int netFilled = encryptedEndPoint.fill(encryptedInputBuffer);
                        if (logger.isTraceEnabled()) {
                            logger.trace("[{}] Fill : Read {} bytes into {} from the encrypted endpoint {}",
                                    getRemoteAddress().toString(), netFilled,
                                    BufferUtil.toSummaryString(encryptedInputBuffer), encryptedEndPoint.toString());
                        }

                        if (encryptedInputBuffer.hasRemaining()) {

                            if (logger.isTraceEnabled()) {
                                Byte.logBuffer(logger, "Fill", getRemoteAddress().toString(), encryptedInputBuffer);
                            }

                            encryptedInputBufferUnderflown = false;
                            SequenceBuffer sBuffer = HomekitEncryptionEngine.decryptBuffer(decryptedInputBuffer,
                                    encryptedInputBuffer, decryptionKey, inboundSequenceCount);
                            decryptedInputBuffer = sBuffer.buffer;
                            inboundSequenceCount = sBuffer.sequenceNumber;

                            if (logger.isTraceEnabled()) {
                                logger.trace(
                                        "[{}] Fill : Buffers : decrypted={}, encryptedInputBuffer={}, decryptedInputBuffer={}, buffer={}",
                                        getRemoteAddress().toString(), netFilled,
                                        BufferUtil.toSummaryString(encryptedInputBuffer),
                                        BufferUtil.toDetailString(decryptedInputBuffer),
                                        BufferUtil.toDetailString(buffer));
                            }

                            if (logger.isTraceEnabled()) {
                                logger.trace("[{}] Fill : Appending {} to {}", getRemoteAddress().toString(),
                                        BufferUtil.toDetailString(decryptedInputBuffer),
                                        BufferUtil.toDetailString(buffer));
                            }
                            return filled = BufferUtil.append(buffer, decryptedInputBuffer);
                        } else {
                            filled = netFilled;
                            return filled;
                        }
                    }
                } catch (Throwable x) {
                    Throwable failure = handleException(x, "fill");
                    throw failure;
                } finally {
                    if (encryptedInputBuffer != null && !encryptedInputBuffer.hasRemaining()) {
                        bufferPool.release(encryptedInputBuffer);
                        encryptedInputBuffer = null;
                    }

                    if (decryptedInputBuffer != null && !decryptedInputBuffer.hasRemaining()) {
                        bufferPool.release(decryptedInputBuffer);
                        decryptedInputBuffer = null;
                    }

                    if (logger.isTraceEnabled()) {
                        Byte.logBuffer(logger, "Decrypt", getRemoteAddress().toString(), buffer);
                    }

                    if (logger.isTraceEnabled()) {
                        logger.trace("[{}] Fill : End : filled={}, encryptedInputBufferUnderflown={} [{}]",
                                getRemoteAddress().toString(), filled, encryptedInputBufferUnderflown, this.toString());
                    }

                    if (filled == -2) {
                        close();
                    }
                }
            }
        } catch (Throwable x) {
            rethrow(x);
            // Never reached.
            throw new AssertionError();
        }

    }

    @Override
    public boolean flush(ByteBuffer... buffers) throws IOException {
        try {
            synchronized (this) {
                if (logger.isTraceEnabled()) {
                    logger.trace("[{}] Flush : Start [{}]", getRemoteAddress().toString(), this.toString());
                }

                if (logger.isTraceEnabled()) {
                    int i = 0;
                    for (ByteBuffer b : buffers) {
                        logger.trace("[{}] Flush : buffer[{}]={}", getRemoteAddress().toString(), i++,
                                BufferUtil.toDetailString(b));
                    }
                }

                int totalRemaining = 0;
                for (ByteBuffer b : buffers) {
                    totalRemaining += b.remaining();
                }

                ByteBuffer flushBuffer = bufferPool.acquire(totalRemaining, useDirectBuffers);
                BufferUtil.flipToFill(flushBuffer);

                for (ByteBuffer b : buffers) {
                    BufferUtil.put(b, flushBuffer);
                }

                BufferUtil.flipToFlush(flushBuffer, 0);

                logger.trace("[{}] Flush : flushBuffer={}", getRemoteAddress().toString(),
                        BufferUtil.toDetailString(flushBuffer));

                // finish of any previous flushes
                if (BufferUtil.hasContent(encryptedOutputBuffer) && !encryptedEndPoint.flush(encryptedOutputBuffer)) {
                    return false;
                }

                boolean isEmpty = BufferUtil.isEmpty(flushBuffer);

                Boolean result = null;
                try {

                    // Keep going while we can make progress or until we are done
                    while (true) {
                        if (encryptedOutputBuffer == null) {
                            encryptedOutputBuffer = bufferPool.acquire(inputBufferSize, useDirectBuffers);
                        } else {
                            BufferUtil.compact(encryptedOutputBuffer);
                            BufferUtil.flipToFill(encryptedOutputBuffer);
                        }

                        if (logger.isTraceEnabled()) {
                            Byte.logBuffer(logger, "Flush", getRemoteAddress().toString(), flushBuffer);
                        }

                        SequenceBuffer sBuffer = HomekitEncryptionEngine.encryptBuffer(encryptedOutputBuffer,
                                flushBuffer, encryptionKey, outboundSequenceCount);
                        encryptedOutputBuffer = sBuffer.buffer;
                        outboundSequenceCount = sBuffer.sequenceNumber;

                        if (logger.isTraceEnabled()) {
                            logger.trace("[{}] Flush : Encrypted : encryptedOutputBuffer={}",
                                    getRemoteAddress().toString(), BufferUtil.toSummaryString(encryptedOutputBuffer));
                        }

                        // Was all the data consumed?
                        isEmpty = BufferUtil.isEmpty(flushBuffer);

                        // if we have net bytes, let's try to flush them
                        boolean flushed = true;
                        if (BufferUtil.hasContent(encryptedOutputBuffer)) {
                            flushed = encryptedEndPoint.flush(encryptedOutputBuffer);
                        }

                        if (logger.isTraceEnabled()) {
                            logger.trace("[{}] Flush : Flushed : flushed={}, isEmpty={} to the encrypted endpoint  {}",
                                    getRemoteAddress().toString(), flushed, isEmpty, encryptedEndPoint.toString());
                        }

                        if (!flushed) {
                            return result = false;
                        }

                        if (isEmpty) {
                            return result = true;
                        }

                        if (encryptedEndPoint.isOutputShutdown()) {
                            return false;
                        }
                    }
                } catch (Throwable x) {
                    Throwable failure = handleException(x, "flush");
                    throw failure;
                } finally {
                    if (!Thread.holdsLock(this)) {
                        throw new IllegalStateException();
                    }

                    if (encryptedOutputBuffer != null && !encryptedOutputBuffer.hasRemaining()) {
                        bufferPool.release(encryptedOutputBuffer);
                        encryptedOutputBuffer = null;
                    }

                    if (logger.isTraceEnabled()) {
                        logger.trace("[{}] Flush : End : result={} [{}]", getRemoteAddress().toString(), result,
                                this.toString());
                    }
                }
            }
        } catch (Throwable x) {
            rethrow(x);
            // Never reached.
            throw new AssertionError();
        }
    }

    @Override
    public long getCreatedTimeStamp() {
        return created;
    }

    @Override
    public void shutdownOutput() {
        encryptedEndPoint.shutdownOutput();
    }

    @Override
    public void close() {
        encryptedEndPoint.close();
    }

    @Override
    public void fillInterested(Callback callback) throws ReadPendingException {
        encryptedEndPoint.fillInterested(callback);
    }

    @Override
    public boolean tryFillInterested(Callback callback) {
        return encryptedEndPoint.tryFillInterested(callback);
    }

    @Override
    public boolean isFillInterested() {
        return encryptedEndPoint.isFillInterested();
    }

    @Override
    public void write(Callback callback, ByteBuffer... buffers) throws WritePendingException {
        if (logger.isTraceEnabled()) {
            logger.trace("[{}] Write : Start [{}]", getRemoteAddress().toString(), this.toString());
        }

        try {
            if (this.flush(buffers)) {
                callback.succeeded();
            }
        } catch (IOException e) {
            e.printStackTrace();
            callback.failed(e);
        }

        if (logger.isTraceEnabled()) {
            logger.trace("[{}] Write : End [{}]", getRemoteAddress().toString(), this.toString());
        }
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public void onOpen() {
        encryptedEndPoint.onOpen();
    }

    @Override
    public void onClose() {
        encryptedEndPoint.onClose();
    }

    @Override
    public boolean isOptimizedForDirectBuffers() {
        return encryptedEndPoint.isOptimizedForDirectBuffers();
    }

    @Override
    public void upgrade(Connection newConnection) {

        if (logger.isTraceEnabled()) {
            logger.trace("[{}] Upgrade : Start [{}]", getRemoteAddress().toString(), this.toString());
        }

        Connection oldConnection = getConnection();

        if (logger.isTraceEnabled()) {
            logger.trace("[{}] {} upgrading from {} to {}", getRemoteAddress().toString(), this, oldConnection,
                    newConnection);
        }

        ByteBuffer buffer = (oldConnection instanceof Connection.UpgradeFrom)
                ? ((Connection.UpgradeFrom) oldConnection).onUpgradeFrom()
                : null;
        oldConnection.onClose();
        oldConnection.getEndPoint().setConnection(newConnection);

        if (newConnection instanceof Connection.UpgradeTo) {
            ((Connection.UpgradeTo) newConnection).onUpgradeTo(buffer);
        } else if (BufferUtil.hasContent(buffer)) {
            throw new IllegalStateException(
                    "Cannot upgrade: " + newConnection + " does not implement " + Connection.UpgradeTo.class.getName());
        }

        newConnection.onOpen();

        if (logger.isTraceEnabled()) {
            logger.trace("[{}] Upgrade : End [{}]", getRemoteAddress().toString(), this.toString());
        }
    }

    private Throwable handleException(Throwable x, String context) {
        synchronized (this) {
            if (failure == null) {
                failure = x;
                if (logger.isTraceEnabled()) {
                    logger.trace(this + " stored " + context + " exception", x);
                }
            } else if (x != failure) {
                failure.addSuppressed(x);
                if (logger.isTraceEnabled()) {
                    logger.trace(this + " suppressed " + context + " exception", x);
                }
            }
            return failure;
        }
    }

    private void rethrow(Throwable x) throws IOException {
        if (x instanceof RuntimeException) {
            throw (RuntimeException) x;
        }
        if (x instanceof Error) {
            throw (Error) x;
        }
        if (x instanceof IOException) {
            throw (IOException) x;
        }
        throw new IOException(x);
    }

    @Override
    public String toString() {
        return String.format("%s->%s", toEndPointString(), toConnectionString());
    }

    public String toEndPointString() {

        ByteBuffer b = encryptedInputBuffer;
        int ei = b == null ? -1 : b.remaining();
        b = encryptedOutputBuffer;
        int eo = b == null ? -1 : b.remaining();
        b = decryptedInputBuffer;
        int di = b == null ? -1 : b.remaining();

        Class<?> c = getClass();
        String name = c.getSimpleName();
        while (name.length() == 0 && c.getSuperclass() != null) {
            c = c.getSuperclass();
            name = c.getSimpleName();
        }

        return String.format("%s~>%s@%h{encryptedInputBuffer=%d,encryptedOutputBuffer=%d,decryptedInputBuffer=%d}",
                encryptedEndPoint.toString(), name, this, ei, eo, di);
    }

    public String toConnectionString() {
        Connection connection = getConnection();
        if (connection == null) {
            return "<null>";
        }
        if (connection instanceof AbstractConnection) {
            return ((AbstractConnection) connection).toConnectionString();
        }
        return String.format("%s@%x", connection.getClass().getSimpleName(), connection.hashCode());
    }
}
