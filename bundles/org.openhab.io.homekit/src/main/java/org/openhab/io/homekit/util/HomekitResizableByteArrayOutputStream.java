package org.openhab.io.homekit.util;

import java.io.ByteArrayOutputStream;

/**
 * A resizable byte array output stream for efficient content buffering.
 *
 * This class extends {@link java.io.ByteArrayOutputStream} to provide dynamic
 * buffer resizing capabilities, which is essential for handling variable-sized
 * content in HomeKit HTTP responses. It allows for both manual resizing and
 * automatic growth of the internal buffer.
 *
 * The class integrates with:
 * - {@link org.openhab.io.homekit.network.http.HomekitResponseWrapper} for response content caching
 * - {@link javax.servlet.ServletOutputStream} for binary content handling
 * - {@link java.io.OutputStreamWriter} for character content handling
 *
 * Key responsibilities:
 * 1. Dynamic buffer resizing
 * 2. Content buffering
 * 3. Memory-efficient growth
 * 4. Thread-safe operations
 *
 * The implementation uses:
 * - {@link System#arraycopy} for efficient buffer copying
 * - Synchronized methods for thread safety
 * - Default initial capacity of 256 bytes
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0
 */
public class HomekitResizableByteArrayOutputStream extends ByteArrayOutputStream {

    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit ResizableByteArrayOutputStream: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";

    private static final int DEFAULT_INITIAL_CAPACITY = 256;

    /**
     * Creates a new resizable byte array output stream.
     *
     * This constructor initializes the stream with the default initial
     * capacity of 256 bytes.
     *
     * Key implementation details:
     * - Uses DEFAULT_INITIAL_CAPACITY for initial buffer size
     * - Inherits from ByteArrayOutputStream
     * - Thread-safe by default
     */
    public HomekitResizableByteArrayOutputStream() {
        super(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Creates a new resizable byte array output stream with specified capacity.
     *
     * This constructor initializes the stream with a custom initial capacity,
     * allowing for optimized memory usage based on expected content size.
     *
     * Key implementation details:
     * - Uses provided initialCapacity for buffer size
     * - Inherits from ByteArrayOutputStream
     * - Thread-safe by default
     *
     * @param initialCapacity The initial buffer size in bytes
     */
    public HomekitResizableByteArrayOutputStream(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Resizes the internal buffer to a specified capacity.
     *
     * This method creates a new buffer of the target size and copies
     * existing content to it. The operation is synchronized to ensure
     * thread safety.
     *
     * Key implementation details:
     * - Creates new buffer of target size
     * - Copies existing content using System.arraycopy
     * - Updates internal buffer reference
     * - Thread-safe operation
     *
     * @param targetCapacity The desired size of the buffer
     * @throws IllegalArgumentException if the target capacity is smaller than
     *             the current content size
     * @see #size()
     */
    public synchronized void resize(int targetCapacity) {
        if (targetCapacity < this.count) {
            throw new IllegalArgumentException("Target capacity " + targetCapacity + " is smaller than current content size " + this.count);
        }
        byte[] resizedBuffer = new byte[targetCapacity];
        System.arraycopy(this.buf, 0, resizedBuffer, 0, this.count);
        this.buf = resizedBuffer;
    }

    /**
     * Grows the internal buffer by a specified amount.
     *
     * This method increases the buffer size if needed to accommodate
     * additional content. The growth is optimized to minimize memory
     * reallocations.
     *
     * Key implementation details:
     * - Doubles current size if needed
     * - Ensures minimum growth to accommodate new content
     * - Uses resize() for actual buffer expansion
     * - Thread-safe operation
     *
     * @param additionalCapacity The number of bytes to add to the current buffer size
     * @see #size()
     */
    public synchronized void grow(int additionalCapacity) {
        if (this.count + additionalCapacity > this.buf.length) {
            int newCapacity = Math.max(this.buf.length * 2, this.count + additionalCapacity);
            resize(newCapacity);
        }
    }

    /**
     * Gets the current capacity of the internal buffer.
     *
     * This method returns the total size of the internal buffer,
     * which may be larger than the actual content size.
     *
     * Key implementation details:
     * - Returns actual buffer length
     * - Thread-safe operation
     * - Different from size() which returns content length
     *
     * @return The current capacity of the buffer in bytes
     */
    public synchronized int capacity() {
        return this.buf.length;
    }
}
