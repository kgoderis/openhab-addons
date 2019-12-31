package org.openhab.io.homekit.util;

import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.eclipse.jetty.util.BufferUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps a {@link ByteBuffer} so it can be used like an {@link OutputStream}. This is similar to a
 * {@link java.io.ByteArrayOutputStream}, just that this uses a {@code ByteBuffer} instead of a
 * {@code byte[]} as internal storage.
 */
public class ByteBufferOutputStream extends OutputStream {

    private static final Logger logger = LoggerFactory.getLogger(ByteBufferOutputStream.class);

    private ByteBuffer wrappedBuffer;
    private final boolean autoEnlarge;

    public ByteBufferOutputStream(final ByteBuffer wrappedBuffer, final boolean autoEnlarge) {

        this.wrappedBuffer = wrappedBuffer;
        this.autoEnlarge = autoEnlarge;
    }

    public ByteBuffer toByteBuffer() {

        final ByteBuffer byteBuffer = wrappedBuffer.duplicate();
        byteBuffer.flip();
        return byteBuffer;
    }

    /**
     * Resets the <code>count</code> field of this byte array output stream to zero, so that all
     * currently accumulated output in the output stream is discarded. The output stream can be used
     * again, reusing the already allocated buffer space.
     *
     * @see java.io.ByteArrayInputStream#count
     */
    public void reset() {
        wrappedBuffer.rewind();
    }

    /**
     * Increases the capacity to ensure that it can hold at least the number of elements specified
     * by the minimum capacity argument.
     *
     * @param minCapacity the desired minimum capacity
     */
    private void growTo(final int minCapacity) {

        // overflow-conscious code
        final int oldCapacity = wrappedBuffer.capacity();

        if (wrappedBuffer.limit() < oldCapacity) {
            wrappedBuffer.limit(oldCapacity);
            return;
        }

        logger.trace("Growing {} from {} to {}", BufferUtil.toSummaryString(wrappedBuffer), oldCapacity, minCapacity);

        int newCapacity = oldCapacity << 1;
        if (newCapacity - minCapacity < 0) {
            newCapacity = minCapacity;
        }
        if (newCapacity < 0) {
            if (minCapacity < 0) { // overflow
                throw new OutOfMemoryError();
            }
            newCapacity = Integer.MAX_VALUE;
        }
        final ByteBuffer oldWrappedBuffer = wrappedBuffer;
        // create the new buffer
        if (wrappedBuffer.isDirect()) {
            wrappedBuffer = ByteBuffer.allocateDirect(newCapacity);
        } else {
            wrappedBuffer = ByteBuffer.allocate(newCapacity);
        }
        // copy over the old content into the new buffer
        oldWrappedBuffer.flip();
        wrappedBuffer.put(oldWrappedBuffer);
    }

    @Override
    public void write(final int bty) {

        try {
            wrappedBuffer.put((byte) bty);
        } catch (final BufferOverflowException ex) {
            if (autoEnlarge) {
                final int newBufferSize = wrappedBuffer.capacity() * 2;
                growTo(newBufferSize);
                write(bty);
            } else {
                throw ex;
            }
        }
    }

    @Override
    public void write(final byte[] bytes) {

        int oldPosition = 0;
        try {
            oldPosition = wrappedBuffer.position();
            wrappedBuffer.put(bytes);
        } catch (final BufferOverflowException ex) {
            if (autoEnlarge) {
                final int newBufferSize = Math.max(wrappedBuffer.capacity() * 2, oldPosition + bytes.length);
                growTo(newBufferSize);
                write(bytes);
            } else {
                throw ex;
            }
        }
    }

    @Override
    public void write(final byte[] bytes, final int off, final int len) {

        int oldPosition = 0;
        try {
            oldPosition = wrappedBuffer.position();
            wrappedBuffer.put(bytes, off, len);
        } catch (final BufferOverflowException ex) {
            if (autoEnlarge) {
                final int newBufferSize = Math.max(wrappedBuffer.capacity() * 2, oldPosition + len);
                growTo(newBufferSize);
                write(bytes, off, len);
            } else {
                throw ex;
            }
        }
    }

}
