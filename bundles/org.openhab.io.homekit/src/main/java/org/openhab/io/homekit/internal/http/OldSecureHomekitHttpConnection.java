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
import org.eclipse.jetty.io.WriteFlusher;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.thread.Invocable;
import org.openhab.io.homekit.crypto.ChachaDecoder;
import org.openhab.io.homekit.crypto.ChachaEncoder;
import org.openhab.io.homekit.util.ByteBufferOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Connection that acts as an interceptor between an EndPoint providing SSL encrypted data
 * and another consumer of an EndPoint (typically an {@link Connection} like HttpConnection) that
 * wants unencrypted data.
 * <p>
 * The connector uses an {@link EndPoint} (typically SocketChannelEndPoint) as
 * it's source/sink of encrypted data. It then provides an endpoint via {@link #getDecryptedEndPoint()} to
 * expose a source/sink of unencrypted data to another connection (eg HttpConnection).
 * <p>
 * The design of this class is based on a clear separation between the passive methods, which do not block nor schedule
 * any
 * asynchronous callbacks, and active methods that do schedule asynchronous callbacks.
 * <p>
 * The passive methods are {@link DecryptedEndPoint#fill(ByteBuffer)} and
 * {@link DecryptedEndPoint#flush(ByteBuffer...)}. They make best
 * effort attempts to progress the connection using only calls to the encrypted {@link EndPoint#fill(ByteBuffer)} and
 * {@link EndPoint#flush(ByteBuffer...)}
 * methods. They will never block nor schedule any readInterest or write callbacks. If a fill/flush cannot progress
 * either because
 * of network congestion or waiting for an SSL handshake message, then the fill/flush will simply return with zero bytes
 * filled/flushed.
 * Specifically, if a flush cannot proceed because it needs to receive a handshake message, then the flush will attempt
 * to fill bytes from the
 * encrypted endpoint, but if insufficient bytes are read it will NOT call {@link EndPoint#fillInterested(Callback)}.
 * <p>
 * It is only the active methods : {@link DecryptedEndPoint#fillInterested(Callback)} and
 * {@link DecryptedEndPoint#write(Callback, ByteBuffer...)} that may schedule callbacks by calling the encrypted
 * {@link EndPoint#fillInterested(Callback)} and {@link EndPoint#write(Callback, ByteBuffer...)}
 * methods. For normal data handling, the decrypted fillInterest method will result in an encrypted fillInterest and a
 * decrypted
 * write will result in an encrypted write. However, due to SSL handshaking requirements, it is also possible for a
 * decrypted fill
 * to call the encrypted write and for the decrypted flush to call the encrypted fillInterested methods.
 * <p>
 * MOST IMPORTANTLY, the encrypted callbacks from the active methods (#onFillable() and WriteFlusher#completeWrite()) do
 * no filling or flushing
 * themselves. Instead they simple make the callbacks to the decrypted callbacks, so that the passive encrypted
 * fill/flush will
 * be called again and make another best effort attempt to progress the connection.
 */

public class OldSecureHomekitHttpConnection extends AbstractConnection implements Connection.UpgradeTo {

    protected static final Logger logger = LoggerFactory.getLogger(OldSecureHomekitHttpConnection.class);

    private enum FillState {
        IDLE, // Not Filling any data
        INTERESTED, // We have a pending read interest
        WAIT_FOR_FLUSH // Waiting for a flush to happen
    }

    private enum FlushState {
        IDLE, // Not flushing any data
        WRITING, // We have a pending write of encrypted data
        WAIT_FOR_FILL // Waiting for a fill to happen
    }

    private final ByteBufferPool _bufferPool;
    private final DecryptedEndPoint _decryptedEndPoint;
    private ByteBuffer decryptedInputBuffer;
    private ByteBuffer encryptedInputBuffer;
    private ByteBuffer encryptedOutputBuffer;
    private final boolean _encryptedDirectBuffers;
    private final boolean _decryptedDirectBuffers;
    private boolean _closedOutbound;
    private FlushState _flushState = FlushState.IDLE;
    private FillState _fillState = FillState.IDLE;
    private boolean _underflown;
    private final byte[] readKey;
    private final byte[] writeKey;
    private static int inboundBinaryMessageCount = 0;
    private static int outboundBinaryMessageCount = 0;

    private abstract class RunnableTask implements Runnable, Invocable {
        private final String _operation;

        protected RunnableTask(String op) {
            _operation = op;
        }

        @Override
        public String toString() {
            return String.format("HOMEKIT:%s:%s:%s", OldSecureHomekitHttpConnection.this, _operation, getInvocationType());
        }
    }

    private final Runnable runFillable = new RunnableTask("runFillable") {
        @Override
        public void run() {
            _decryptedEndPoint.getFillInterest().fillable();
        }

        @Override
        public InvocationType getInvocationType() {
            return _decryptedEndPoint.getFillInterest().getCallbackInvocationType();
        }
    };

    private final Callback readCallback = new Callback() {
        @Override
        public void succeeded() {
            onFillable();
        }

        @Override
        public void failed(final Throwable x) {
            onFillInterestedFailed(x);
        }

        @Override
        public InvocationType getInvocationType() {
            return getDecryptedEndPoint().getFillInterest().getCallbackInvocationType();
        }

        @Override
        public String toString() {
            return String.format("ReadCallback @%x{%s}", OldSecureHomekitHttpConnection.this.hashCode(),
                    OldSecureHomekitHttpConnection.this);
        }
    };

    public OldSecureHomekitHttpConnection(ByteBufferPool byteBufferPool, Executor executor, EndPoint endPoint,
            byte[] readKey, byte[] writeKey, boolean directBuffersForEncryption, boolean directBuffersForDecryption) {
        // This connection does not execute calls to onFillable(), so they will be called by the selector thread.
        // onFillable() does not block and will only wakeup another thread to do the actual reading and handling.
        super(endPoint, executor);
        this._bufferPool = byteBufferPool;
        this._decryptedEndPoint = newDecryptedEndPoint();
        this._encryptedDirectBuffers = directBuffersForEncryption;
        this._decryptedDirectBuffers = directBuffersForDecryption;
        this.readKey = readKey;
        this.writeKey = writeKey;

        // String testString = "Allegeo NV = Kalipedia BVBA";
        // ByteBuffer testBuffer = ByteBuffer.wrap(testString.getBytes());
        // ByteBuffer cipher = ByteBuffer.allocate(1024);
        //
        // try {
        // this.encryptBuffer(cipher, testBuffer, readKey);
        // } catch (IOException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        //
        // logger.debug("Encrypted is {}", byteToHexString(cipher.array()));
        //
        // BufferUtil.flipToFlush(cipher, 0);
        //
        // ByteBuffer plain = ByteBuffer.allocate(1024);
        //
        // this.decryptBuffer(plain, cipher, readKey);
        //
        // BufferUtil.flipToFlush(plain, 0);
        //
        // logger.debug("Roundtrip result is {}", BufferUtil.toUTF8String(plain));
    }

    protected DecryptedEndPoint newDecryptedEndPoint() {
        return new DecryptedEndPoint();
    }

    public DecryptedEndPoint getDecryptedEndPoint() {
        return _decryptedEndPoint;
    }

    private void acquireEncryptedInput() {
        if (encryptedInputBuffer == null) {
            encryptedInputBuffer = _bufferPool.acquire(getInputBufferSize(), _encryptedDirectBuffers);
        }
    }

    @Override
    public void onUpgradeTo(ByteBuffer buffer) {
        if (BufferUtil.hasContent(buffer)) {
            acquireEncryptedInput();
            BufferUtil.append(encryptedInputBuffer, buffer);
        }
    }

    @Override
    public void onOpen() {
        super.onOpen();
        getDecryptedEndPoint().getConnection().onOpen();
    }

    @Override
    public void onClose() {
        _decryptedEndPoint.getConnection().onClose();
        super.onClose();
    }

    @Override
    public void close() {
        getDecryptedEndPoint().getConnection().close();
    }

    @Override
    public boolean onIdleExpired() {
        return getDecryptedEndPoint().getConnection().onIdleExpired();
    }

    @Override
    public void onFillable() {
        // onFillable means that there are encrypted bytes ready to be filled.
        // however we do not fill them here on this callback, but instead wakeup
        // the decrypted readInterest and/or writeFlusher so that they will attempt
        // to do the fill and/or flush again and these calls will do the actually
        // filling.

        if (logger.isDebugEnabled()) {
            logger.debug(">c.onFillable {}", OldSecureHomekitHttpConnection.this);
        }

        // We have received a close handshake, close the end point to send FIN.
        if (_decryptedEndPoint.isInputShutdown()) {
            _decryptedEndPoint.close();
        }

        _decryptedEndPoint.onFillable();

        if (logger.isDebugEnabled()) {
            logger.debug("<c.onFillable {}", OldSecureHomekitHttpConnection.this);
        }
    }

    @Override
    public void onFillInterestedFailed(Throwable cause) {
        _decryptedEndPoint.onFillableFail(cause == null ? new IOException() : cause);
    }

    @Override
    public String toConnectionString() {
        ByteBuffer b = encryptedInputBuffer;
        int ei = b == null ? -1 : b.remaining();
        b = encryptedOutputBuffer;
        int eo = b == null ? -1 : b.remaining();
        b = decryptedInputBuffer;
        int di = b == null ? -1 : b.remaining();

        Connection connection = _decryptedEndPoint.getConnection();
        return String.format("%s@%x{eio=%d/%d,di=%d,fill=%s,flush=%s}~>%s=>%s", getClass().getSimpleName(), hashCode(),
                ei, eo, di, _fillState, _flushState, _decryptedEndPoint.toEndPointString(),
                connection instanceof AbstractConnection ? ((AbstractConnection) connection).toConnectionString()
                        : connection);
    }

    private void releaseEncryptedOutputBuffer() {
        if (!Thread.holdsLock(_decryptedEndPoint)) {
            throw new IllegalStateException();
        }
        if (encryptedOutputBuffer != null && !encryptedOutputBuffer.hasRemaining()) {
            _bufferPool.release(encryptedOutputBuffer);
            encryptedOutputBuffer = null;
        }
    }

    protected int networkFill(ByteBuffer input) throws IOException {
        return getEndPoint().fill(input);
    }

    protected boolean networkFlush(ByteBuffer output) throws IOException {
        return getEndPoint().flush(output);
    }

    public class DecryptedEndPoint extends AbstractEndPoint {
        private final Callback _incompleteWriteCallback = new IncompleteWriteCallback();
        private Throwable _failure;

        public DecryptedEndPoint() {
            // Disable idle timeout checking: no scheduler and -1 timeout for this instance.
            super(null);
            super.setIdleTimeout(-1);
        }

        @Override
        public long getIdleTimeout() {
            return getEndPoint().getIdleTimeout();
        }

        @Override
        public void setIdleTimeout(long idleTimeout) {
            getEndPoint().setIdleTimeout(idleTimeout);
        }

        @Override
        public boolean isOpen() {
            return getEndPoint().isOpen();
        }

        @Override
        public InetSocketAddress getLocalAddress() {
            return getEndPoint().getLocalAddress();
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return getEndPoint().getRemoteAddress();
        }

        @Override
        public WriteFlusher getWriteFlusher() {
            return super.getWriteFlusher();
        }

        protected void onFillable() {
            try {
                boolean waitingForFill;
                synchronized (_decryptedEndPoint) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("onFillable {}", OldSecureHomekitHttpConnection.this);
                    }

                    _fillState = FillState.IDLE;
                    waitingForFill = _flushState == FlushState.WAIT_FOR_FILL;
                }

                getFillInterest().fillable();

                if (waitingForFill) {
                    synchronized (_decryptedEndPoint) {
                        waitingForFill = _flushState == FlushState.WAIT_FOR_FILL;
                    }
                    if (waitingForFill) {
                        fill(BufferUtil.EMPTY_BUFFER);
                    }
                }
            } catch (Throwable e) {
                close(e);
            }
        }

        protected void onFillableFail(Throwable failure) {
            boolean fail = false;
            synchronized (_decryptedEndPoint) {
                if (logger.isDebugEnabled()) {
                    logger.debug("onFillableFail {}", OldSecureHomekitHttpConnection.this, failure);
                }

                _fillState = FillState.IDLE;
                if (_flushState == FlushState.WAIT_FOR_FILL) {
                    _flushState = FlushState.IDLE;
                    fail = true;
                }
            }

            // wake up whoever is doing the fill
            getFillInterest().onFail(failure);

            // Try to complete the write
            if (fail) {
                if (!getWriteFlusher().onFail(failure)) {
                    close(failure);
                }
            }
        }

        @Override
        public void setConnection(Connection connection) {
            if (connection instanceof AbstractConnection) {
                AbstractConnection a = (AbstractConnection) connection;
                if (a.getInputBufferSize() < getInputBufferSize()) {
                    a.setInputBufferSize(getInputBufferSize());
                }
            }
            super.setConnection(connection);
        }

        public OldSecureHomekitHttpConnection getSecureHomekitHttpConnection() {
            return OldSecureHomekitHttpConnection.this;
        }

        @Override
        public int fill(ByteBuffer buffer) throws IOException {

            int bufferPosition = buffer.position();

            try {
                synchronized (_decryptedEndPoint) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(">fill {}", OldSecureHomekitHttpConnection.this);
                    }

                    int filled = -2;
                    try {
                        if (_fillState != FillState.IDLE) {
                            return filled = 0;
                        }

                        // Do we already have some decrypted data?
                        if (BufferUtil.hasContent(decryptedInputBuffer)) {
                            return filled = BufferUtil.append(buffer, decryptedInputBuffer);
                        }

                        // loop filling and unwrapping until we have something
                        while (true) {
                            acquireEncryptedInput();

                            // can we use the passed buffer if it is big enough
                            ByteBuffer decryptedBuffer;
                            if (decryptedInputBuffer == null) {
                                if (BufferUtil.space(buffer) > getInputBufferSize()) {
                                    decryptedBuffer = buffer;
                                } else {
                                    decryptedBuffer = decryptedInputBuffer = _bufferPool.acquire(getInputBufferSize(),
                                            _decryptedDirectBuffers);
                                }
                            } else {
                                decryptedBuffer = decryptedInputBuffer;
                                BufferUtil.compact(encryptedInputBuffer);
                            }

                            // Let's try reading some encrypted data... even if we have some already.
                            int netFilled = networkFill(encryptedInputBuffer);
                            if (logger.isDebugEnabled()) {
                                logger.debug("net filled={}", netFilled);
                            }

                            int pos = BufferUtil.flipToFill(decryptedBuffer);
                            try {
                                _underflown = false;
                                decryptBuffer(decryptedBuffer, encryptedInputBuffer, writeKey);
                            } finally {
                                BufferUtil.flipToFlush(decryptedBuffer, pos);
                            }

                            if (logger.isDebugEnabled()) {
                                logger.debug(
                                        "decrypt net_filled={} encryptedInputBuffer={} decryptedBuffer={} buffer={}",
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
                        if (_flushState == FlushState.WAIT_FOR_FILL) {
                            _flushState = FlushState.IDLE;
                            getExecutor().execute(() -> _decryptedEndPoint.getWriteFlusher().onFail(failure));
                        }
                        throw failure;
                    } finally {
                        if (encryptedInputBuffer != null && !encryptedInputBuffer.hasRemaining()) {
                            _bufferPool.release(encryptedInputBuffer);
                            encryptedInputBuffer = null;
                        }

                        if (decryptedInputBuffer != null && !decryptedInputBuffer.hasRemaining()) {
                            _bufferPool.release(decryptedInputBuffer);
                            decryptedInputBuffer = null;
                        }

                        if (_flushState == FlushState.WAIT_FOR_FILL) {
                            _flushState = FlushState.IDLE;
                            getExecutor().execute(() -> _decryptedEndPoint.getWriteFlusher().completeWrite());
                        }

                        if (logger.isDebugEnabled()) {
                            logger.debug("<fill f={} uf={} {}", filled, _underflown, OldSecureHomekitHttpConnection.this);
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
                ByteBuffer write = null;
                boolean interest = false;
                synchronized (_decryptedEndPoint) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(">needFillInterest s={}/{} uf={} ei={} di={} {}", _flushState, _fillState,
                                _underflown, BufferUtil.toDetailString(encryptedInputBuffer),
                                BufferUtil.toDetailString(decryptedInputBuffer), OldSecureHomekitHttpConnection.this);
                    }

                    if (_fillState != FillState.IDLE) {
                        return;
                    }

                    // Fillable if we have decrypted input OR enough encrypted input.
                    fillable = BufferUtil.hasContent(decryptedInputBuffer)
                            || (BufferUtil.hasContent(encryptedInputBuffer) && !_underflown);

                    if (!fillable) {
                        interest = true;
                        _fillState = FillState.INTERESTED;
                    }

                    if (logger.isDebugEnabled()) {
                        logger.debug("<needFillInterest s={}/{} f={} i={} w={}", _flushState, _fillState, fillable,
                                interest, BufferUtil.toDetailString(write));
                    }
                }

                if (fillable) {
                    getExecutor().execute(runFillable);
                } else if (interest) {
                    ensureFillInterested();
                }
            } catch (Throwable x) {
                if (logger.isDebugEnabled()) {
                    logger.debug(OldSecureHomekitHttpConnection.this.toString(), x);
                }
                close(x);
                throw x;
            }
        }

        @Override
        public boolean flush(ByteBuffer... buffers) throws IOException {
            try {
                synchronized (_decryptedEndPoint) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(">flush {}", OldSecureHomekitHttpConnection.this);
                        int i = 0;
                        for (ByteBuffer b : buffers) {
                            logger.debug(">flush b[{}]={}", i++, BufferUtil.toDetailString(b));
                        }
                    }

                    int totalRemaining = 0;
                    for (ByteBuffer b : buffers) {
                        totalRemaining += b.remaining();
                    }

                    ByteBuffer flushBuffer = _bufferPool.acquire(totalRemaining, _encryptedDirectBuffers);
                    BufferUtil.flipToFill(flushBuffer);

                    for (ByteBuffer b : buffers) {
                        BufferUtil.put(b, flushBuffer);
                    }

                    BufferUtil.flipToFlush(flushBuffer, 0);

                    logger.debug(">flush flushBuffer={}", BufferUtil.toDetailString(flushBuffer));

                    // finish of any previous flushes
                    if (BufferUtil.hasContent(encryptedOutputBuffer) && !networkFlush(encryptedOutputBuffer)) {
                        return false;
                    }

                    boolean isEmpty = BufferUtil.isEmpty(flushBuffer);

                    Boolean result = null;
                    try {
                        if (_flushState != FlushState.IDLE) {
                            return result = false;
                        }

                        // Keep going while we can make progress or until we are done
                        while (true) {

                            if (encryptedOutputBuffer == null) {
                                encryptedOutputBuffer = _bufferPool.acquire(getInputBufferSize(),
                                        _encryptedDirectBuffers);
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
                                logger.debug("encrypt {}", BufferUtil.toSummaryString(encryptedOutputBuffer));
                            }

                            // Was all the data consumed?
                            isEmpty = BufferUtil.isEmpty(flushBuffer);

                            // if we have net bytes, let's try to flush them
                            boolean flushed = true;
                            if (BufferUtil.hasContent(encryptedOutputBuffer)) {
                                flushed = networkFlush(encryptedOutputBuffer);
                            }

                            if (logger.isDebugEnabled()) {
                                logger.debug("net flushed={}, ac={}", flushed, isEmpty);
                            }

                            if (!flushed) {
                                return result = false;
                            }

                            if (isEmpty) {
                                return result = true;
                            }

                            if (getEndPoint().isOutputShutdown()) {
                                return false;
                            }
                        }
                    } catch (Throwable x) {
                        Throwable failure = handleException(x, "flush");
                        throw failure;
                    } finally {
                        releaseEncryptedOutputBuffer();
                        if (logger.isDebugEnabled()) {
                            logger.debug("<flush {} {}", result, OldSecureHomekitHttpConnection.this);
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
        protected void onIncompleteFlush() {
            try {
                boolean fillInterest = false;
                ByteBuffer write = null;
                synchronized (_decryptedEndPoint) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(">onIncompleteFlush {} {}", OldSecureHomekitHttpConnection.this,
                                BufferUtil.toDetailString(encryptedOutputBuffer));
                    }

                    if (_flushState != FlushState.IDLE) {
                        return;
                    }

                    write = BufferUtil.hasContent(encryptedOutputBuffer) ? encryptedOutputBuffer
                            : BufferUtil.EMPTY_BUFFER;
                    _flushState = FlushState.WRITING;

                    if (logger.isDebugEnabled()) {
                        logger.debug("<onIncompleteFlush s={}/{} fi={} w={}", _flushState, _fillState, fillInterest,
                                BufferUtil.toDetailString(write));
                    }
                }

                if (write != null) {
                    getEndPoint().write(_incompleteWriteCallback, write);
                } else if (fillInterest) {
                    ensureFillInterested();
                }
            } catch (Throwable x) {
                if (logger.isDebugEnabled()) {
                    logger.debug(OldSecureHomekitHttpConnection.this.toString(), x);
                }
                close(x);
                throw x;
            }
        }

        @Override
        public void doShutdownOutput() {
            EndPoint endPoint = getEndPoint();
            try {
                boolean close;
                boolean flush = false;
                synchronized (_decryptedEndPoint) {
                    boolean ishut = endPoint.isInputShutdown();
                    boolean oshut = endPoint.isOutputShutdown();
                    if (logger.isDebugEnabled()) {
                        logger.debug("shutdownOutput: {} oshut={}, ishut={}", OldSecureHomekitHttpConnection.this, oshut,
                                ishut);
                    }

                    if (!_closedOutbound) {
                        _closedOutbound = true;
                        // Flush only once.
                        flush = !oshut;
                    }

                    close = ishut;
                }

                if (flush) {
                    if (!flush(BufferUtil.EMPTY_BUFFER) && !close) {
                        // If we still can't flush, but we are not closing the endpoint,
                        // let's just flush the encrypted output in the background.
                        ByteBuffer write = encryptedOutputBuffer;
                        if (BufferUtil.hasContent(write)) {
                            endPoint.write(Callback.from(Callback.NOOP::succeeded, t -> endPoint.close()), write);
                        }
                    }
                }

                if (close) {
                    endPoint.close();
                } else {
                    ensureFillInterested();
                }
            } catch (Throwable x) {
                endPoint.close();
            }
        }

        private void ensureFillInterested() {
            if (logger.isDebugEnabled()) {
                logger.debug("ensureFillInterested {}", OldSecureHomekitHttpConnection.this);
            }
            OldSecureHomekitHttpConnection.this.tryFillInterested(readCallback);
        }

        @Override
        public boolean isOutputShutdown() {
            return getEndPoint().isOutputShutdown();
        }

        @Override
        public void doClose() {
            doShutdownOutput();
            getEndPoint().close();
            super.doClose();
        }

        @Override
        public Object getTransport() {
            return getEndPoint();
        }

        @Override
        public boolean isInputShutdown() {
            return BufferUtil.isEmpty(decryptedInputBuffer) && (getEndPoint().isInputShutdown());
        }

        private Throwable handleException(Throwable x, String context) {
            synchronized (_decryptedEndPoint) {
                if (_failure == null) {
                    _failure = x;
                    if (logger.isDebugEnabled()) {
                        logger.debug(this + " stored " + context + " exception", x);
                    }
                } else if (x != _failure) {
                    _failure.addSuppressed(x);
                    if (logger.isDebugEnabled()) {
                        logger.debug(this + " suppressed " + context + " exception", x);
                    }
                }
                return _failure;
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
            return super.toEndPointString();
        }

        private final class IncompleteWriteCallback implements Callback, Invocable {
            @Override
            public void succeeded() {
                boolean fillable;
                synchronized (_decryptedEndPoint) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("IncompleteWriteCB succeeded {}", OldSecureHomekitHttpConnection.this);
                    }

                    releaseEncryptedOutputBuffer();
                    _flushState = FlushState.IDLE;
                    fillable = _fillState == FillState.WAIT_FOR_FLUSH;
                    if (fillable) {
                        _fillState = FillState.IDLE;
                    }
                }

                if (fillable) {
                    _decryptedEndPoint.getFillInterest().fillable();
                }

                _decryptedEndPoint.getWriteFlusher().completeWrite();
            }

            @Override
            public void failed(final Throwable x) {
                boolean failFillInterest;
                synchronized (_decryptedEndPoint) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("IncompleteWriteCB failed {}", OldSecureHomekitHttpConnection.this, x);
                    }

                    BufferUtil.clear(encryptedOutputBuffer);
                    releaseEncryptedOutputBuffer();

                    _flushState = FlushState.IDLE;
                    failFillInterest = _fillState == FillState.WAIT_FOR_FLUSH;
                    if (failFillInterest) {
                        _fillState = FillState.IDLE;
                    }
                }

                getExecutor().execute(() -> {
                    if (failFillInterest) {
                        _decryptedEndPoint.getFillInterest().onFail(x);
                    }
                    _decryptedEndPoint.getWriteFlusher().onFail(x);
                });
            }

            @Override
            public InvocationType getInvocationType() {
                return _decryptedEndPoint.getWriteFlusher().getCallbackInvocationType();
            }

            @Override
            public String toString() {
                return String.format("SSL@%h.DEP.writeCallback", OldSecureHomekitHttpConnection.this);
            }
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
                break;
            }
        }

        if (!results.isEmpty()) {
            try (ByteBufferOutputStream decrypted = new ByteBufferOutputStream(decryptedBuffer, true)) {
                results.stream().map(msg -> decrypt(msg, writeKey)).forEach(bytes -> {
                    try {
                        decrypted.write(bytes);
                    } catch (Exception e) {
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
        BufferUtil.flipToFlush(dummy, 0);
        logger.debug("EncryptBuffer : Encrypting '{}'", BufferUtil.toUTF8String(dummy));

        try (ByteBufferOutputStream encrypted = new ByteBufferOutputStream(encryptedBuffer, true)) {
            while (plainTextBuffer.hasRemaining()) {
                short length = (short) Math.min(plainTextBuffer.remaining(), 0x400);
                logger.debug("EncryptBuffer : Encrypting {} bytes out of {} in the input buffer", length,
                        plainTextBuffer.remaining());
                byte[] lengthBytes = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(length).array();

                encrypted.write(lengthBytes);
                logger.debug("EncryptBuffer : Writing LengthBytes (output={})",
                        BufferUtil.toSummaryString(encryptedBuffer));

                byte[] nonce = Pack.longToLittleEndian(outboundBinaryMessageCount++);
                byte[] plaintext = new byte[length];
                plainTextBuffer.get(plaintext, 0, length);

                byte[] ciphertext = new ChachaEncoder(readKey, nonce).encodeCiphertext(plaintext, lengthBytes);

                encrypted.write(ciphertext);
                logger.debug("EncryptBuffer : Writing CipherText ({} bytes) (output={})", ciphertext.length,
                        BufferUtil.toSummaryString(encryptedBuffer));
            }
        }

        logger.debug("EncryptBuffer : Output = {}", BufferUtil.toSummaryString(encryptedBuffer));
    }

    private byte[] decrypt(byte[] msg, byte[] readKey) {
        byte[] mac = new byte[16];
        byte[] ciphertext = new byte[msg.length - 16];
        System.arraycopy(msg, 0, ciphertext, 0, msg.length - 16);
        System.arraycopy(msg, msg.length - 16, mac, 0, 16);
        byte[] additionalData = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
                .putShort((short) (msg.length - 16)).array();
        try {
            byte[] nonce = Pack.longToLittleEndian(inboundBinaryMessageCount++);
            return new ChachaDecoder(readKey, nonce).decodeCiphertext(mac, additionalData, ciphertext);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    protected static String byteToHexString(byte[] input) {
        StringBuilder sb = new StringBuilder();
        for (byte b : input) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }
}
