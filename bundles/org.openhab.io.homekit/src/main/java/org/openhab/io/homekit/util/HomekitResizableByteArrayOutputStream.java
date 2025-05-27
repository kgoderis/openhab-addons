package org.openhab.io.homekit.util;

import java.io.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A resizable byte array output stream for efficient content buffering.
 *
 * <p>
 * This class extends {@link java.io.ByteArrayOutputStream} to provide dynamic
 * buffer resizing capabilities, which is essential for handling variable-sized
 * content in HomeKit HTTP responses. It allows for both manual resizing and
 * automatic growth of the internal buffer.
 * </p>
 *
 * <p>
 * The class integrates with several key components:
 * </p>
 * <ul>
 *   <li>{@link org.openhab.io.homekit.network.http.HomekitResponseWrapper} for response content caching</li>
 *   <li>{@link javax.servlet.ServletOutputStream} for binary content handling</li>
 *   <li>{@link java.io.OutputStreamWriter} for character content handling</li>
 *   <li>{@link java.io.ByteArrayOutputStream} for base stream functionality</li>
 * </ul>
 *
 * <p>
 * Key features:
 * </p>
 * <ul>
 *   <li>Dynamic buffer resizing with configurable initial capacity</li>
 *   <li>Efficient content buffering with minimal memory overhead</li>
 *   <li>Thread-safe operations through synchronized methods</li>
 *   <li>Optimized buffer growth strategy</li>
 *   <li>Memory-efficient content copying</li>
 * </ul>
 *
 * <p>
 * The implementation uses:
 * </p>
 * <ul>
 *   <li>{@link System#arraycopy} for efficient buffer copying</li>
 *   <li>Synchronized methods for thread safety</li>
 *   <li>Default initial capacity of 256 bytes</li>
 *   <li>Exponential growth strategy for optimal performance</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
public class HomekitResizableByteArrayOutputStream extends ByteArrayOutputStream {
    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit ResizableByteArrayOutputStream: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_BUFFER = LOG_PREFIX + "Buffer - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";

    private static final Logger logger = LoggerFactory.getLogger(HomekitResizableByteArrayOutputStream.class);
    private static final int DEFAULT_INITIAL_CAPACITY = 256;

    /**
     * Creates a new resizable byte array output stream.
     *
     * <p>
     * This constructor initializes the stream with the default initial
     * capacity of 256 bytes.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     *   <li>Uses DEFAULT_INITIAL_CAPACITY for initial buffer size</li>
     *   <li>Inherits from ByteArrayOutputStream</li>
     *   <li>Thread-safe by default</li>
     *   <li>Provides trace-level logging</li>
     * </ul>
     */
    public HomekitResizableByteArrayOutputStream() {
        super(DEFAULT_INITIAL_CAPACITY);
        logger.trace("{}Created new stream with default capacity: {}", LOG_INIT, DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Creates a new resizable byte array output stream with specified capacity.
     *
     * <p>
     * This constructor initializes the stream with a custom initial capacity,
     * allowing for optimized memory usage based on expected content size.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     *   <li>Uses provided initialCapacity for buffer size</li>
     *   <li>Inherits from ByteArrayOutputStream</li>
     *   <li>Thread-safe by default</li>
     *   <li>Provides trace-level logging</li>
     * </ul>
     *
     * @param initialCapacity The initial buffer size in bytes
     * @throws IllegalArgumentException if initialCapacity is negative
     */
    public HomekitResizableByteArrayOutputStream(int initialCapacity) {
        super(initialCapacity);
        logger.trace("{}Created new stream with custom capacity: {}", LOG_INIT, initialCapacity);
    }

    /**
     * Resizes the internal buffer to a specified capacity.
     *
     * <p>
     * This method creates a new buffer of the target size and copies
     * existing content to it. The operation is synchronized to ensure
     * thread safety.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     *   <li>Creates new buffer of target size</li>
     *   <li>Copies existing content using System.arraycopy</li>
     *   <li>Updates internal buffer reference</li>
     *   <li>Thread-safe operation</li>
     *   <li>Provides trace-level logging</li>
     * </ul>
     *
     * @param targetCapacity The desired size of the buffer
     * @throws IllegalArgumentException if the target capacity is smaller than
     *             the current content size
     * @see #size()
     */
    public synchronized void resize(int targetCapacity) {
        if (targetCapacity < this.count) {
            logger.error("{}Target capacity {} is smaller than current content size {}", LOG_ERROR, targetCapacity, this.count);
            throw new IllegalArgumentException("Target capacity " + targetCapacity + " is smaller than current content size " + this.count);
        }
        byte[] resizedBuffer = new byte[targetCapacity];
        System.arraycopy(this.buf, 0, resizedBuffer, 0, this.count);
        this.buf = resizedBuffer;
        logger.trace("{}Resized buffer from {} to {} bytes", LOG_BUFFER, this.buf.length, targetCapacity);
    }

    /**
     * Grows the internal buffer by a specified amount.
     *
     * <p>
     * This method increases the buffer size if needed to accommodate
     * additional content. The growth is optimized to minimize memory
     * reallocations.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     *   <li>Doubles current size if needed</li>
     *   <li>Ensures minimum growth to accommodate new content</li>
     *   <li>Uses resize() for actual buffer expansion</li>
     *   <li>Thread-safe operation</li>
     *   <li>Provides trace-level logging</li>
     * </ul>
     *
     * @param additionalCapacity The number of bytes to add to the current buffer size
     * @see #size()
     */
    public synchronized void grow(int additionalCapacity) {
        if (this.count + additionalCapacity > this.buf.length) {
            int newCapacity = Math.max(this.buf.length * 2, this.count + additionalCapacity);
            logger.trace("{}Growing buffer from {} to {} bytes", LOG_BUFFER, this.buf.length, newCapacity);
            resize(newCapacity);
        }
    }

    /**
     * Gets the current capacity of the internal buffer.
     *
     * <p>
     * This method returns the total size of the internal buffer,
     * which may be larger than the actual content size.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     *   <li>Returns actual buffer length</li>
     *   <li>Thread-safe operation</li>
     *   <li>Different from size() which returns content length</li>
     *   <li>Provides trace-level logging</li>
     * </ul>
     *
     * @return The current capacity of the buffer in bytes
     */
    public synchronized int capacity() {
        logger.trace("{}Getting buffer capacity: {}", LOG_BUFFER, this.buf.length);
        return this.buf.length;
    }
}
