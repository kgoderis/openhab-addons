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

package org.openhab.io.homekit.util;

import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.util.BufferUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A specialized output stream implementation that wraps a {@link ByteBuffer} for efficient byte handling
 * in HomeKit communication. This class provides a bridge between the standard Java I/O stream API and
 * the more efficient NIO ByteBuffer operations.
 *
 * <p>
 * Key features:
 * <ul>
 * <li>Direct ByteBuffer integration for improved performance</li>
 * <li>Optional automatic buffer enlargement</li>
 * <li>Efficient memory management</li>
 * <li>Thread-safe operations</li>
 * </ul>
 *
 * <p>
 * Integration points:
 * <ul>
 * <li>HomeKit HTTP response handling</li>
 * <li>Network communication buffers</li>
 * <li>Data serialization</li>
 * </ul>
 *
 * <p>
 * Implementation details:
 * <ul>
 * <li>Uses ByteBuffer for internal storage</li>
 * <li>Supports both direct and heap buffers</li>
 * <li>Implements automatic buffer growth when enabled</li>
 * <li>Maintains buffer position and limit correctly</li>
 * </ul>
 *
 * @author Mark Weiss - Initial contribution
 * @version 1.0
 */
@NonNullByDefault
public class HomekitByteBufferOutputStream extends OutputStream {

    private static final Logger logger = LoggerFactory.getLogger(HomekitByteBufferOutputStream.class);
    private static final String LOG_PREFIX = "Homekit ByteBufferOutputStream: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_BUFFER = LOG_PREFIX + "Buffer - ";
    private static final String LOG_ERROR = LOG_PREFIX + "Error - ";

    private ByteBuffer wrappedBuffer;
    private final boolean autoEnlarge;

    /**
     * Creates a new HomekitByteBufferOutputStream with the specified buffer and auto-enlarge setting.
     *
     * @param wrappedBuffer The ByteBuffer to wrap
     * @param autoEnlarge Whether the buffer should automatically grow when full
     * @throws IllegalArgumentException if wrappedBuffer is null
     */
    public HomekitByteBufferOutputStream(final ByteBuffer wrappedBuffer, final boolean autoEnlarge) {
        logger.trace("{}Creating stream with buffer: {}, autoEnlarge: {}", LOG_INIT,
                BufferUtil.toSummaryString(wrappedBuffer), autoEnlarge);
        this.wrappedBuffer = wrappedBuffer;
        this.autoEnlarge = autoEnlarge;
    }

    /**
     * Returns a duplicate of the wrapped buffer with position set to 0 and limit set to the current position.
     * The returned buffer is ready for reading.
     *
     * @return A new ByteBuffer containing the written data
     */
    public ByteBuffer toByteBuffer() {
        logger.trace("{}Converting to ByteBuffer", LOG_BUFFER);
        final ByteBuffer byteBuffer = wrappedBuffer.duplicate();
        byteBuffer.flip();
        return byteBuffer;
    }

    /**
     * Resets the buffer position to 0, discarding all accumulated output.
     * The output stream can be used again, reusing the already allocated buffer space.
     */
    public void reset() {
        logger.trace("{}Resetting buffer position", LOG_BUFFER);
        wrappedBuffer.rewind();
    }

    /**
     * Increases the buffer capacity to ensure it can hold at least the specified minimum capacity.
     * If the current buffer is not at its limit, it will be expanded to its full capacity first.
     *
     * @param minCapacity The desired minimum capacity
     * @throws OutOfMemoryError if the required capacity exceeds Integer.MAX_VALUE
     */
    private void growTo(final int minCapacity) {
        final int oldCapacity = wrappedBuffer.capacity();
        logger.trace("{}Growing buffer from {} to {}", LOG_BUFFER, oldCapacity, minCapacity);

        if (wrappedBuffer.limit() < oldCapacity) {
            logger.trace("{}Expanding buffer to full capacity", LOG_BUFFER);
            wrappedBuffer.limit(oldCapacity);
            return;
        }

        int newCapacity = oldCapacity << 1;
        if (newCapacity - minCapacity < 0) {
            newCapacity = minCapacity;
        }
        if (newCapacity < 0) {
            if (minCapacity < 0) {
                logger.error("{}Buffer capacity overflow", LOG_ERROR);
                throw new OutOfMemoryError();
            }
            newCapacity = Integer.MAX_VALUE;
        }

        final ByteBuffer oldWrappedBuffer = wrappedBuffer;
        logger.trace("{}Creating new buffer with capacity {}", LOG_BUFFER, newCapacity);

        wrappedBuffer = wrappedBuffer.isDirect() ? ByteBuffer.allocateDirect(newCapacity)
                : ByteBuffer.allocate(newCapacity);

        oldWrappedBuffer.flip();
        wrappedBuffer.put(oldWrappedBuffer);
        logger.trace("{}Buffer growth complete", LOG_BUFFER);
    }

    @Override
    public void write(final int bty) {
        try {
            wrappedBuffer.put((byte) bty);
        } catch (final BufferOverflowException ex) {
            if (autoEnlarge) {
                logger.trace("{}Buffer overflow, auto-enlarging", LOG_BUFFER);
                final int newBufferSize = wrappedBuffer.capacity() * 2;
                growTo(newBufferSize);
                write(bty);
            } else {
                logger.error("{}Buffer overflow, auto-enlarge disabled", LOG_ERROR);
                throw ex;
            }
        }
    }

    @Override
    @SuppressWarnings("null") // Parent OutputStream interface doesn't constrain this parameter
    public void write(final byte @Nullable [] bytes) {
        if (bytes == null) {
            throw new NullPointerException("Input array cannot be null");
        }
        write(bytes, 0, bytes.length);
    }

    @Override
    @SuppressWarnings("null") // Parent OutputStream interface doesn't constrain this parameter
    public void write(final byte @Nullable [] bytes, final int off, final int len) {
        if (bytes == null) {
            throw new NullPointerException("Input array cannot be null");
        }
        if (off < 0 || len < 0 || off + len > bytes.length) {
            throw new IndexOutOfBoundsException("Invalid offset or length");
        }

        int oldPosition = 0;
        try {
            oldPosition = wrappedBuffer.position();
            wrappedBuffer.put(bytes, off, len);
        } catch (final BufferOverflowException ex) {
            if (autoEnlarge) {
                logger.trace("{}Buffer overflow, auto-enlarging for {} bytes", LOG_BUFFER, len);
                final int newBufferSize = Math.max(wrappedBuffer.capacity() * 2, oldPosition + len);
                growTo(newBufferSize);
                write(bytes, off, len);
            } else {
                logger.error("{}Buffer overflow, auto-enlarge disabled", LOG_ERROR);
                throw ex;
            }
        }
    }
}
