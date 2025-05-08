package org.openhab.io.homekit.util;

import java.io.ByteArrayOutputStream;

public class HomekitResizableByteArrayOutputStream extends ByteArrayOutputStream {

    private static final int DEFAULT_INITIAL_CAPACITY = 256;

    /**
     * Create a new HomekitResizableByteArrayOutputStream
     * with the default initial capacity of 256 bytes.
     */
    public HomekitResizableByteArrayOutputStream() {
        super(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Create a new HomekitResizableByteArrayOutputStream
     * with the specified initial capacity.
     * 
     * @param initialCapacity the initial buffer size in bytes
     */
    public HomekitResizableByteArrayOutputStream(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Resize the internal buffer size to a specified capacity.
     * 
     * @param targetCapacity the desired size of the buffer
     * @throws IllegalArgumentException if the given capacity is smaller than
     *             the actual size of the content stored in the buffer already
     * @see HomekitResizableByteArrayOutputStream#size()
     */
    public synchronized void resize(int targetCapacity) {
        byte[] resizedBuffer = new byte[targetCapacity];
        System.arraycopy(this.buf, 0, resizedBuffer, 0, this.count);
        this.buf = resizedBuffer;
    }

    /**
     * Grow the internal buffer size.
     * 
     * @param additionalCapacity the number of bytes to add to the current buffer size
     * @see HomekitResizableByteArrayOutputStream#size()
     */
    public synchronized void grow(int additionalCapacity) {
        if (this.count + additionalCapacity > this.buf.length) {
            int newCapacity = Math.max(this.buf.length * 2, this.count + additionalCapacity);
            resize(newCapacity);
        }
    }

    /**
     * Return the current size of this stream's internal buffer.
     */
    public synchronized int capacity() {
        return this.buf.length;
    }
}
