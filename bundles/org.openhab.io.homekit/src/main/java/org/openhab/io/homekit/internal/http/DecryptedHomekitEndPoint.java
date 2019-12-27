package org.openhab.io.homekit.internal.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.Executor;

import org.bouncycastle.util.Pack;
import org.eclipse.jetty.io.AbstractConnection;
import org.eclipse.jetty.io.AbstractEndPoint;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.thread.Invocable;
import org.openhab.io.homekit.crypto.ChachaDecoder;
import org.openhab.io.homekit.crypto.ChachaEncoder;
import org.openhab.io.homekit.util.ByteBufferOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DecryptedHomekitEndPoint extends AbstractEndPoint {

    protected static final Logger logger = LoggerFactory.getLogger(DecryptedHomekitEndPoint.class);

    private final Callback encryptedEndPointWriteCallback = new EncryptedEndPointWriteCallback();
    private final Callback encryptedEndPointReadCallback = new EncryptedEndPointReadCallback();
    private Throwable failure;

    private Executor executor;

    private final ByteBufferPool bufferPool;
    private int inputBufferSize = 2048;
    private boolean encryptedInputBufferUnderflown;
    private boolean useDirectBuffers = false;
    private ByteBuffer decryptedInputBuffer;
    private ByteBuffer encryptedInputBuffer;
    private ByteBuffer encryptedOutputBuffer;

    private final byte[] readKey;
    private final byte[] writeKey;
    private static int inboundBinaryMessageCount = 0;
    private static int outboundBinaryMessageCount = 0;

    private final EndPoint encryptedEndPoint;

    public DecryptedHomekitEndPoint(EndPoint encryptedEndpoint, Executor executor, ByteBufferPool byteBufferPool,
            boolean useDirectBuffers, byte[] readKey, byte[] writeKey) {
        // Disable idle timeout checking: no scheduler and -1 timeout for this instance.
        super(null);
        this.encryptedEndPoint = encryptedEndpoint;
        this.bufferPool = byteBufferPool;
        this.useDirectBuffers = useDirectBuffers;
        this.executor = executor;
        this.readKey = readKey;
        this.writeKey = writeKey;

        super.setIdleTimeout(-1);
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

    // @Override
    // public WriteFlusher getWriteFlusher() {
    // return super.getWriteFlusher();
    // }

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
        super.setConnection(connection);
    }

    public void onUpgradeTo(ByteBuffer buffer) {
        if (BufferUtil.hasContent(buffer)) {
            if (encryptedInputBuffer == null) {
                encryptedInputBuffer = bufferPool.acquire(inputBufferSize, useDirectBuffers);
            }
            BufferUtil.append(encryptedInputBuffer, buffer);
        }
    }

    @Override
    public int fill(ByteBuffer buffer) throws IOException {

        int bufferPosition = buffer.position();

        try {
            synchronized (this) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Fill : Start [{}]", this.toString());
                }

                int filled = -2;
                try {

                    // Do we already have some decrypted data?
                    if (BufferUtil.hasContent(decryptedInputBuffer)) {
                        return filled = BufferUtil.append(buffer, decryptedInputBuffer);
                    }

                    // loop filling and unwrapping until we have something
                    while (true) {
                        if (encryptedInputBuffer == null) {
                            encryptedInputBuffer = bufferPool.acquire(inputBufferSize, useDirectBuffers);
                        }

                        // can we use the passed buffer if it is big enough
                        ByteBuffer decryptedBuffer;
                        if (decryptedInputBuffer == null) {
                            if (BufferUtil.space(buffer) > inputBufferSize) {
                                decryptedBuffer = buffer;
                            } else {
                                decryptedBuffer = decryptedInputBuffer = bufferPool.acquire(inputBufferSize,
                                        useDirectBuffers);
                            }
                        } else {
                            decryptedBuffer = decryptedInputBuffer;
                            BufferUtil.compact(encryptedInputBuffer);
                        }

                        // Let's try reading some encrypted data... even if we have some already.
                        int netFilled = encryptedEndPoint.fill(encryptedInputBuffer);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Fill : Read {} bytes from the encrypted endpoint {}", netFilled,
                                    encryptedEndPoint.toString());
                        }

                        int pos = BufferUtil.flipToFill(decryptedBuffer);
                        try {
                            encryptedInputBufferUnderflown = false;
                            decryptBuffer(decryptedBuffer, encryptedInputBuffer, writeKey);
                        } finally {
                            BufferUtil.flipToFlush(decryptedBuffer, pos);
                        }

                        if (logger.isDebugEnabled()) {
                            logger.debug(
                                    "Fill : Buffers : decrypted={}, encryptedInputBuffer={}, decryptedBuffer={}, buffer={}",
                                    netFilled, BufferUtil.toSummaryString(encryptedInputBuffer),
                                    BufferUtil.toDetailString(decryptedBuffer), BufferUtil.toDetailString(buffer));
                        }

                        if (decryptedBuffer == buffer) {
                            return filled = buffer.position() - bufferPosition;
                        }

                        return filled = BufferUtil.append(buffer, decryptedInputBuffer);
                    }
                } catch (Throwable x) {
                    Throwable failure = handleException(x, "fill");
                    executor.execute(() -> getWriteFlusher().onFail(failure));
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

                    if (logger.isDebugEnabled()) {
                        logger.debug("Fill : End : filled={}, encryptedInputBufferUnderflown={} [{}]", filled,
                                encryptedInputBufferUnderflown, this.toString());
                    }
                }
            }
        } catch (Throwable x) {
            close(x);
            rethrow(x);
            // Never reached.
            throw new AssertionError();
        }
    }

    @Override
    protected void needsFillInterest() {
        try {
            boolean fillable;
            synchronized (this) {
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "NeedFillInterest : Start : encryptedInputBufferUnderflown={} encryptedInputBuffer={} decryptedInputBuffer={} encryptedIsFillInterested={} [{}]",
                            encryptedInputBufferUnderflown, BufferUtil.toDetailString(encryptedInputBuffer),
                            BufferUtil.toDetailString(decryptedInputBuffer), encryptedEndPoint.isFillInterested(),
                            this);
                }

                if (!encryptedEndPoint.isFillInterested()) {
                    ensureFillInterested();
                }

                // Fillable if we have decrypted input OR enough encrypted input.
                fillable = BufferUtil.hasContent(decryptedInputBuffer)
                        || (BufferUtil.hasContent(encryptedInputBuffer) && !encryptedInputBufferUnderflown);

                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "NeedFillInterest : End : encryptedInputBufferUnderflown={} encryptedInputBuffer={} decryptedInputBuffer={} encryptedIsFillInterested={} [{}]",
                            encryptedInputBufferUnderflown, BufferUtil.toDetailString(encryptedInputBuffer),
                            BufferUtil.toDetailString(decryptedInputBuffer), encryptedEndPoint.isFillInterested(),
                            this);
                }
            }

            if (fillable) {
                executor.execute(() -> getFillInterest().fillable());
            }

        } catch (Throwable x) {
            if (logger.isDebugEnabled()) {
                logger.debug(this.toString(), x);
            }
            close(x);
            throw x;
        }
    }

    private void ensureFillInterested() {
        if (logger.isDebugEnabled()) {
            logger.debug("EnsureFillInterested : Start [{}]", this.toString());
        }

        encryptedEndPoint.tryFillInterested(encryptedEndPointReadCallback);

        if (logger.isDebugEnabled()) {
            logger.debug("EnsureFillInterested : End [{}]", this.toString());
        }
    }

    @Override
    public boolean flush(ByteBuffer... buffers) throws IOException {
        try {
            synchronized (this) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Flush : Start [{}]", this.toString());
                }

                if (logger.isDebugEnabled()) {
                    int i = 0;
                    for (ByteBuffer b : buffers) {
                        logger.debug("Flush : buffer[{}]={}", i++, BufferUtil.toDetailString(b));
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

                logger.debug("Flush : flushBuffer={}", BufferUtil.toDetailString(flushBuffer));

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
                        }

                        BufferUtil.compact(encryptedOutputBuffer);
                        int pos = BufferUtil.flipToFill(encryptedOutputBuffer);
                        try {
                            encryptBuffer(encryptedOutputBuffer, flushBuffer, readKey);
                        } finally {
                            BufferUtil.flipToFlush(encryptedOutputBuffer, pos);

                            // byte[] dummy = new byte[totalRemaining];
                            // for (ByteBuffer b : buffers) {
                            // b.get(dummy);
                            // }
                        }
                        if (logger.isDebugEnabled()) {
                            logger.debug("Flush : Encrypted : encryptedOutputBuffer={}",
                                    BufferUtil.toSummaryString(encryptedOutputBuffer));
                        }

                        // Was all the data consumed?
                        isEmpty = BufferUtil.isEmpty(flushBuffer);

                        // if we have net bytes, let's try to flush them
                        boolean flushed = true;
                        if (BufferUtil.hasContent(encryptedOutputBuffer)) {
                            flushed = encryptedEndPoint.flush(encryptedOutputBuffer);
                        }

                        if (logger.isDebugEnabled()) {
                            logger.debug("Flush : Flushed : flushed={}, isEmpty={} to the encrypted endpoint  {}",
                                    flushed, isEmpty, encryptedEndPoint.toString());
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
                    releaseEncryptedOutputBuffer();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Flush : End : result={} [{}]", result, this.toString());
                    }
                }
            }
        } catch (Throwable x) {
            close(x);
            rethrow(x);
            // Never reached.
            throw new AssertionError();
        }
    }

    public void decryptBuffer(ByteBuffer decryptedBuffer, ByteBuffer cipherTextBuffer, byte[] writeKey) {

        logger.debug("DecryptBuffer : Processing a buffer of length {}", cipherTextBuffer.remaining());
        int currentPosition = cipherTextBuffer.position();

        Collection<byte[]> results = new LinkedList<>();
        while (cipherTextBuffer.remaining() > 18) {
            int targetLength = (cipherTextBuffer.get() & 0xFF) + (cipherTextBuffer.get() & 0xFF) * 256 + 16;
            logger.debug("DecryptBuffer : Attempting to read a message of length {}", targetLength);
            if (cipherTextBuffer.remaining() >= targetLength) {
                byte[] b = new byte[targetLength];
                cipherTextBuffer.get(b, 0, targetLength);
                results.add(b);
                logger.debug("DecryptBuffer : Read a complete message ({} bytes)", targetLength);
            } else {
                cipherTextBuffer.position(currentPosition);
                logger.debug("DecryptBuffer : Not enough data available ({} bytes) for a complete message ({} bytes)",
                        cipherTextBuffer.remaining(), targetLength);
                encryptedInputBufferUnderflown = true;
                break;
            }
        }

        if (!results.isEmpty()) {
            try (ByteBufferOutputStream decrypted = new ByteBufferOutputStream(decryptedBuffer, true)) {
                results.stream().map(msg -> decrypt(msg, writeKey)).forEach(bytes -> {
                    try {
                        decrypted.write(bytes);
                    } catch (Exception e) {
                        logger.error("DecryptBuffer : Exception while decrypting with key {}", writeKey);
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        ByteBuffer dummy = decryptedBuffer.duplicate();
        BufferUtil.flipToFlush(dummy, 0);
        logger.debug("DecryptBuffer : Decrypted '{}'", BufferUtil.toUTF8String(dummy));

    }

    public void encryptBuffer(ByteBuffer encryptedBuffer, ByteBuffer plainTextBuffer, byte[] readKey)
            throws IOException {

        logger.debug("EncryptBuffer : Input = {}", BufferUtil.toSummaryString(plainTextBuffer));
        logger.debug("EncryptBuffer : Output = {}", BufferUtil.toSummaryString(encryptedBuffer));

        ByteBuffer dummy = plainTextBuffer.duplicate();
        logger.debug("EncryptBuffer : Encrypting '{}'", BufferUtil.toUTF8String(dummy));

        try (ByteBufferOutputStream encrypted = new ByteBufferOutputStream(encryptedBuffer, true)) {
            while (plainTextBuffer.hasRemaining()) {
                short length = (short) Math.min(plainTextBuffer.remaining(), 0x400);
                logger.debug("EncryptBuffer : Encrypting {} bytes out of {} remaining in the input buffer", length,
                        plainTextBuffer.remaining());
                byte[] lengthBytes = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(length).array();

                encrypted.write(lengthBytes);
                logger.debug("EncryptBuffer : Writing LengthBytes (Output={})",
                        BufferUtil.toSummaryString(encryptedBuffer));

                byte[] nonce = Pack.longToLittleEndian(outboundBinaryMessageCount++);
                byte[] plaintext = new byte[length];
                plainTextBuffer.get(plaintext, 0, length);

                byte[] ciphertext = new ChachaEncoder(readKey, nonce).encodeCiphertext(plaintext, lengthBytes);

                encrypted.write(ciphertext);
                logger.debug("EncryptBuffer : Writing CipherText ({} bytes) (Output={})", ciphertext.length,
                        BufferUtil.toSummaryString(encryptedBuffer));
            }
        }

        logger.debug("EncryptBuffer : Output = {}", BufferUtil.toSummaryString(encryptedBuffer));
    }

    private byte[] decrypt(byte[] msg, byte[] writeKey) {
        byte[] mac = new byte[16];
        byte[] ciphertext = new byte[msg.length - 16];
        System.arraycopy(msg, 0, ciphertext, 0, msg.length - 16);
        System.arraycopy(msg, msg.length - 16, mac, 0, 16);
        byte[] additionalData = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
                .putShort((short) (msg.length - 16)).array();
        try {
            byte[] nonce = Pack.longToLittleEndian(inboundBinaryMessageCount++);
            return new ChachaDecoder(writeKey, nonce).decodeCiphertext(mac, additionalData, ciphertext);
        } catch (IOException e) {
            logger.error("Decrypt : Exception while decrypting {} with key {}", msg, writeKey);
            throw new RuntimeException(e);
        }
    }

    protected void onFillable() {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("OnFillable : Start [{}]", this.toString());
            }

            if (isInputShutdown()) {
                close();
            }

            getFillInterest().fillable();

            if (logger.isDebugEnabled()) {
                logger.debug("OnFillable : End [{}]", this.toString());
            }
        } catch (Throwable e) {
            close(e);
        }
    }

    protected void onFillInterestedFailed(Throwable failure) {
        synchronized (this) {
            if (logger.isDebugEnabled()) {
                logger.debug("OnFillableFail : Failed because of {} [{}]", failure.getCause(), this.toString());
            }

        }

        // wake up whoever is doing the fill
        getFillInterest().onFail(failure);

        // Try to complete the write
        if (!getWriteFlusher().onFail(failure)) {
            close(failure);
        }

    }

    @Override
    protected void onIncompleteFlush() {
        try {
            ByteBuffer write = null;
            synchronized (this) {
                if (logger.isDebugEnabled()) {
                    logger.debug("OnIncompleteFlush : encryptedOutputBuffer={} [{}]",
                            BufferUtil.toDetailString(encryptedOutputBuffer), this);
                }

                write = BufferUtil.hasContent(encryptedOutputBuffer) ? encryptedOutputBuffer : BufferUtil.EMPTY_BUFFER;
            }

            if (write != null) {
                encryptedEndPoint.write(encryptedEndPointWriteCallback, write);
            }

        } catch (Throwable x) {
            if (logger.isDebugEnabled()) {
                logger.debug(this.toString(), x);
            }
            close(x);
            throw x;
        }
    }

    @Override
    public void doClose() {
        doShutdownOutput();
        encryptedEndPoint.close();
        super.doClose();
    }

    @Override
    public void doShutdownOutput() {
        try {
            boolean ishut;
            boolean oshut;
            synchronized (this) {
                ishut = encryptedEndPoint.isInputShutdown();
                oshut = encryptedEndPoint.isOutputShutdown();
                if (logger.isDebugEnabled()) {
                    logger.debug("ShutdownOutput: oshut={}, ishut={} [{}]", oshut, ishut, this.toString());
                }
            }

            if (!oshut) {
                // If we still can't flush, but we are not closing the endpoint,
                // let's just flush the encrypted output in the background.
                ByteBuffer write = encryptedOutputBuffer;
                if (BufferUtil.hasContent(write)) {
                    encryptedEndPoint.write(Callback.from(Callback.NOOP::succeeded, t -> encryptedEndPoint.close()),
                            write);
                }
            }

            if (ishut) {
                encryptedEndPoint.close();
            } else {
                ensureFillInterested();
            }
        } catch (Throwable x) {
            encryptedEndPoint.close();
        }
    }

    private final class EncryptedEndPointReadCallback implements Callback, Invocable {
        @Override
        public void succeeded() {
            if (logger.isDebugEnabled()) {
                logger.debug("EncryptedEndPointReadCallback : Succeeded [{}]", this.toString());
            }
            onFillable();
        }

        @Override
        public void failed(final Throwable x) {
            if (logger.isDebugEnabled()) {
                logger.debug("EncryptedEndPointReadCallback : Failed [{}]", this.toString(), x);
            }
            onFillInterestedFailed(x == null ? new IOException() : x);
        }

        @Override
        public InvocationType getInvocationType() {
            return getFillInterest().getCallbackInvocationType();
        }

        @Override
        public String toString() {
            Class<?> c = getClass();
            String name = c.getSimpleName();
            while (name.length() == 0 && c.getSuperclass() != null) {
                c = c.getSuperclass();
                name = c.getSimpleName();
            }

            return String.format("%s@%h", name, this);
        }
    };

    private final class EncryptedEndPointWriteCallback implements Callback, Invocable {
        @Override
        public void succeeded() {
            synchronized (this) {
                if (logger.isDebugEnabled()) {
                    logger.debug("EncryptedEndPointWriteCallback : Succeeded [{}]", this);
                }

                releaseEncryptedOutputBuffer();
            }

            executor.execute(() -> {
                getFillInterest().fillable();
                getWriteFlusher().completeWrite();
            });

        }

        @Override
        public void failed(final Throwable x) {
            synchronized (this) {
                if (logger.isDebugEnabled()) {
                    logger.debug("EncryptedEndPointWriteCallback : Failed [{}]", this, x);
                }

                BufferUtil.clear(encryptedOutputBuffer);
                releaseEncryptedOutputBuffer();
            }

            executor.execute(() -> {
                getFillInterest().onFail(x);
                getWriteFlusher().onFail(x);
            });
        }

        @Override
        public InvocationType getInvocationType() {
            return getWriteFlusher().getCallbackInvocationType();
        }

        @Override
        public String toString() {
            Class<?> c = getClass();
            String name = c.getSimpleName();
            while (name.length() == 0 && c.getSuperclass() != null) {
                c = c.getSuperclass();
                name = c.getSimpleName();
            }

            return String.format("%s@%h", name, this);
        }
    }

    private void releaseEncryptedOutputBuffer() {
        if (!Thread.holdsLock(this)) {
            throw new IllegalStateException();
        }
        if (encryptedOutputBuffer != null && !encryptedOutputBuffer.hasRemaining()) {
            bufferPool.release(encryptedOutputBuffer);
            encryptedOutputBuffer = null;
        }
    }

    private Throwable handleException(Throwable x, String context) {
        synchronized (this) {
            if (failure == null) {
                failure = x;
                if (logger.isDebugEnabled()) {
                    logger.debug(this + " stored " + context + " exception", x);
                }
            } else if (x != failure) {
                failure.addSuppressed(x);
                if (logger.isDebugEnabled()) {
                    logger.debug(this + " suppressed " + context + " exception", x);
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

        ByteBuffer b = encryptedInputBuffer;
        int ei = b == null ? -1 : b.remaining();
        b = encryptedOutputBuffer;
        int eo = b == null ? -1 : b.remaining();
        b = decryptedInputBuffer;
        int di = b == null ? -1 : b.remaining();

        return String.format("%s@%x{encryptedInputBuffer=%d,encryptedOutputBuffer=%d,decryptedInputBuffer=%d}~>%s",
                getClass().getSimpleName(), hashCode(), ei, eo, di, encryptedEndPoint.toString());

    }

}
