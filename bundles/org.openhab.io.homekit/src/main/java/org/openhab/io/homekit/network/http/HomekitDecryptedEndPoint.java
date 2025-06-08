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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadPendingException;
import java.nio.channels.WritePendingException;
import java.util.concurrent.Executor;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.io.AbstractConnection;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.openhab.io.homekit.protocol.crypto.HomekitEncryptionEngine;
import org.openhab.io.homekit.protocol.crypto.HomekitEncryptionEngine.SequenceBuffer;
import org.openhab.io.homekit.util.HomekitByte;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomeKit decrypted endpoint implementation that provides encryption/decryption
 * capabilities for HomeKit communication.
 * 
 * This class wraps an encrypted endpoint and provides transparent encryption and
 * decryption of data using HomeKit's encryption protocol. It implements the Jetty
 * EndPoint interface to integrate seamlessly with the HTTP server infrastructure.
 * 
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public class HomekitDecryptedEndPoint implements EndPoint {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitDecryptedEndPoint.class);
    protected static final String LOG_PREFIX = "Homekit DecryptedHomekitEndPoint: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "HomekitAccessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    protected static final String LOG_EVENT = LOG_PREFIX + "Event - ";
    protected static final String LOG_SERVER = LOG_PREFIX + "Server - ";
    protected static final String LOG_PAIRING = LOG_PREFIX + "HomekitPairing - ";

    private @Nullable Throwable failure;

    private final long created = System.currentTimeMillis();

    @SuppressWarnings("unused")
    private Executor executor;
    private @Nullable Connection connection;
    @SuppressWarnings("unused")
    private @Nullable Callback connectionCallback;

    private final ByteBufferPool bufferPool;
    private int inputBufferSize = 2048;
    private boolean encryptedInputBufferUnderflown;
    private boolean useDirectBuffers = false;
    private @Nullable ByteBuffer decryptedInputBuffer;
    private @Nullable ByteBuffer encryptedInputBuffer;
    private @Nullable ByteBuffer encryptedOutputBuffer;

    private long inboundSequenceCount = 0;
    private long outboundSequenceCount = 0;

    private final byte[] encryptionKey;
    private final byte[] decryptionKey;

    private final EndPoint encryptedEndPoint;

    public HomekitDecryptedEndPoint(EndPoint encryptedEndpoint, Executor executor, ByteBufferPool byteBufferPool,
            boolean useDirectBuffers, byte[] encryptionKey, byte[] decryptionKey) {
        this.encryptedEndPoint = encryptedEndpoint;
        this.bufferPool = byteBufferPool;
        this.useDirectBuffers = useDirectBuffers;
        this.executor = executor;
        this.encryptionKey = encryptionKey;
        this.decryptionKey = decryptionKey;

        if (logger.isTraceEnabled()) {
            logger.trace("[{}] DecryptedHomekitEndPoint : Setting Encryption Key {}", getRemoteAddress().toString(),
                    HomekitByte.toHexString(this.decryptionKey));
            logger.trace("[{}] DecryptedHomekitEndPoint : Setting Decryption Key {}", getRemoteAddress().toString(),
                    HomekitByte.toHexString(this.encryptionKey));

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
    @SuppressWarnings("null") // Parent EndPoint interface doesn't constrain this parameter
    public void setConnection(@Nullable Connection connection) {
        if (connection instanceof AbstractConnection) {
            AbstractConnection a = (AbstractConnection) connection;
            if (a.getInputBufferSize() < inputBufferSize) {
                a.setInputBufferSize(inputBufferSize);
            }
        }
        this.connection = connection;
    }

    @Override
    @SuppressWarnings("null") // Parent EndPoint interface doesn't constrain this parameter
    public int fill(@Nullable ByteBuffer buffer) throws IOException {
        if (buffer == null) {
            return -1;
        }

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
                        @SuppressWarnings("null") // hasContent check ensures decryptedInputBuffer is not null
                        int appendResult = BufferUtil.append(buffer, decryptedInputBuffer);
                        return filled = appendResult;
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
                                ByteBuffer logBuffer = encryptedInputBuffer;
                                if (logBuffer != null) {
                                    HomekitByte.logBuffer(logger, "Fill", getRemoteAddress().toString(), logBuffer);
                                }
                            }

                            encryptedInputBufferUnderflown = false;
                            // Ensure buffers are non-null before decryption
                            ByteBuffer nonNullDecryptedBuffer = decryptedInputBuffer;
                            ByteBuffer nonNullEncryptedBuffer = encryptedInputBuffer;
                            if (nonNullDecryptedBuffer == null || nonNullEncryptedBuffer == null) {
                                throw new IllegalStateException("Buffers must be allocated before decryption");
                            }
                            SequenceBuffer sBuffer = HomekitEncryptionEngine.decryptBuffer(nonNullDecryptedBuffer,
                                    nonNullEncryptedBuffer, decryptionKey, inboundSequenceCount);
                            decryptedInputBuffer = sBuffer.buffer;
                            inboundSequenceCount = sBuffer.sequenceNumber;

                            if (logger.isTraceEnabled()) {
                                logger.trace(
                                        "[{}] Fill : Buffers : decrypted={}, encryptedInputBuffer={}, decryptedInputBuffer={}, buffer={}",
                                        getRemoteAddress().toString(), netFilled,
                                        encryptedInputBuffer != null ? BufferUtil.toSummaryString(encryptedInputBuffer)
                                                : "null",
                                        decryptedInputBuffer != null ? BufferUtil.toDetailString(decryptedInputBuffer)
                                                : "null",
                                        BufferUtil.toDetailString(buffer));
                            }

                            if (logger.isTraceEnabled()) {
                                logger.trace("[{}] Fill : Appending {} to {}", getRemoteAddress().toString(),
                                        decryptedInputBuffer != null ? BufferUtil.toDetailString(decryptedInputBuffer)
                                                : "null",
                                        BufferUtil.toDetailString(buffer));
                            }
                            // Ensure decryptedInputBuffer is non-null after decryption
                            ByteBuffer nonNullAppendBuffer = decryptedInputBuffer;
                            if (nonNullAppendBuffer == null) {
                                throw new IllegalStateException("Decrypted buffer should not be null after decryption");
                            }
                            int appendResult = BufferUtil.append(buffer, nonNullAppendBuffer);
                            return filled = appendResult;
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
                        ByteBuffer nonNullBuffer = buffer;
                        if (nonNullBuffer != null) {
                            HomekitByte.logBuffer(logger, "Decrypt", getRemoteAddress().toString(), nonNullBuffer);
                        }
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
    @SuppressWarnings("null") // Parent EndPoint interface doesn't constrain this parameter
    public boolean flush(ByteBuffer @Nullable... buffers) throws IOException {
        if (buffers == null) {
            return true;
        }

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
                            HomekitByte.logBuffer(logger, "Flush", getRemoteAddress().toString(), flushBuffer);
                        }

                        // Ensure encryptedOutputBuffer is non-null before encryption
                        ByteBuffer nonNullEncryptedOutputBuffer = encryptedOutputBuffer;
                        if (nonNullEncryptedOutputBuffer == null) {
                            throw new IllegalStateException(
                                    "Encrypted output buffer should be allocated before encryption");
                        }
                        SequenceBuffer sBuffer = HomekitEncryptionEngine.encryptBuffer(nonNullEncryptedOutputBuffer,
                                flushBuffer, encryptionKey, outboundSequenceCount);
                        encryptedOutputBuffer = sBuffer.buffer;
                        outboundSequenceCount = sBuffer.sequenceNumber;

                        if (logger.isTraceEnabled()) {
                            logger.trace("[{}] Flush : Encrypted : encryptedOutputBuffer={}",
                                    getRemoteAddress().toString(),
                                    encryptedOutputBuffer != null ? BufferUtil.toSummaryString(encryptedOutputBuffer)
                                            : "null");
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
    @SuppressWarnings("null") // Parent EndPoint interface doesn't constrain this parameter
    public void fillInterested(@Nullable Callback callback) throws ReadPendingException {
        if (callback != null) {
            encryptedEndPoint.fillInterested(callback);
        }
        // TODO Verify we need this in the server implementation.
    }

    @Override
    @SuppressWarnings("null") // Parent EndPoint interface doesn't constrain this parameter
    public boolean tryFillInterested(@Nullable Callback callback) {
        if (callback == null) {
            return false;
        }
        return encryptedEndPoint.tryFillInterested(callback);
    }

    @Override
    public boolean isFillInterested() {
        return encryptedEndPoint.isFillInterested();
    }

    @Override
    @SuppressWarnings("null") // Parent EndPoint interface doesn't constrain these parameters
    public void write(@Nullable Callback callback, ByteBuffer @Nullable... buffers) throws WritePendingException {
        if (logger.isTraceEnabled()) {
            logger.trace("[{}] Write : Start [{}]", getRemoteAddress().toString(), this.toString());
        }

        try {
            if (buffers != null && this.flush(buffers)) {
                if (callback != null) {
                    callback.succeeded();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (callback != null) {
                callback.failed(e);
            }
        }

        if (logger.isTraceEnabled()) {
            logger.trace("[{}] Write : End [{}]", getRemoteAddress().toString(), this.toString());
        }
    }

    @Override
    public Connection getConnection() {
        Connection nonNullConnection = connection;
        if (nonNullConnection == null) {
            throw new IllegalStateException("Connection not set");
        }
        return nonNullConnection;
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
    public void upgrade(@Nullable Connection newConnection) {
        if (newConnection == null) {
            return;
        }

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
                Throwable nonNullFailure = failure;
                if (nonNullFailure != null) {
                    nonNullFailure.addSuppressed(x);
                    if (logger.isTraceEnabled()) {
                        logger.trace(this + " suppressed " + context + " exception", x);
                    }
                }
            }
            Throwable result = failure;
            return result != null ? result : x;
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
