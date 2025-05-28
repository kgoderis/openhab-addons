package org.openhab.io.homekit.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.commons.io.HexDump;
import org.eclipse.jetty.util.BufferUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class providing byte manipulation and conversion operations for HomeKit communication.
 * This class offers methods for handling binary data, stream operations, and hexadecimal conversions
 * commonly used in HomeKit protocol implementations.
 *
 * <p>
 * Key features:
 * <ul>
 * <li>Byte array concatenation</li>
 * <li>BigInteger to byte array conversion</li>
 * <li>Stream copying with length control</li>
 * <li>Hexadecimal string conversion</li>
 * <li>Buffer logging with hex dump</li>
 * </ul>
 *
 * <p>
 * Integration points:
 * <ul>
 * <li>HomeKit protocol message handling</li>
 * <li>Binary data processing</li>
 * <li>Network communication</li>
 * <li>Debug logging</li>
 * </ul>
 *
 * <p>
 * Implementation details:
 * <ul>
 * <li>Efficient byte array operations</li>
 * <li>Thread-safe methods</li>
 * <li>Memory-efficient stream handling</li>
 * <li>UTF-8 encoding support</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 */
public class HomekitByte {

    private static final Logger logger = LoggerFactory.getLogger(HomekitByte.class);
    private static final String LOG_PREFIX = "Homekit Byte: ";
    private static final String LOG_OPERATION = LOG_PREFIX + "Operation - ";
    private static final String LOG_ERROR = LOG_PREFIX + "Error - ";

    /**
     * Concatenates multiple byte arrays into a single array.
     *
     * @param piece The byte arrays to concatenate
     * @return A new byte array containing all input arrays concatenated
     * @throws IllegalArgumentException if any input array is null
     */
    public static byte[] joinBytes(byte[]... piece) {
        if (piece == null) {
            throw new IllegalArgumentException("Input arrays cannot be null");
        }

        int length = 0;
        for (byte[] bytes : piece) {
            if (bytes == null) {
                throw new IllegalArgumentException("Individual byte arrays cannot be null");
            }
            length += bytes.length;
        }

        byte[] ret = new byte[length];
        int pos = 0;
        for (byte[] bytes : piece) {
            System.arraycopy(bytes, 0, ret, pos, bytes.length);
            pos += bytes.length;
        }

        logger.trace("{}Concatenated {} byte arrays into {} bytes", LOG_OPERATION, piece.length, length);
        return ret;
    }

    /**
     * Converts a BigInteger to a byte array, removing any leading zero byte.
     *
     * @param i The BigInteger to convert
     * @return A byte array representation of the BigInteger
     * @throws IllegalArgumentException if the input is null
     */
    public static byte[] toByteArray(BigInteger i) {
        if (i == null) {
            throw new IllegalArgumentException("BigInteger cannot be null");
        }

        byte[] array = i.toByteArray();
        if (array[0] == 0) {
            array = Arrays.copyOfRange(array, 1, array.length);
        }

        logger.trace("{}Converted BigInteger to {} bytes", LOG_OPERATION, array.length);
        return array;
    }

    /**
     * Copies a specified number of bytes from an input stream to an output stream.
     *
     * @param input The source input stream
     * @param output The target output stream
     * @param length The number of bytes to copy
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if any parameter is null or length is negative
     */
    public static void copyStream(InputStream input, OutputStream output, int length) throws IOException {
        if (input == null || output == null) {
            throw new IllegalArgumentException("Streams cannot be null");
        }
        if (length < 0) {
            throw new IllegalArgumentException("Length cannot be negative");
        }

        logger.trace("{}Copying {} bytes between streams", LOG_OPERATION, length);

        byte[] buffer = new byte[length];
        int remaining = length;
        int bytesRead;

        while ((bytesRead = input.read(buffer, 0, remaining)) != -1 && remaining > 0) {
            output.write(buffer, 0, bytesRead);
            remaining -= bytesRead;
        }

        if (remaining > 0) {
            logger.warn("{}Incomplete copy: {} bytes remaining", LOG_ERROR, remaining);
        }
    }

    /**
     * Converts a byte array to a hexadecimal string representation.
     *
     * @param input The byte array to convert
     * @return A string containing the hexadecimal representation of the input
     * @throws IllegalArgumentException if the input is null
     */
    public static String toHexString(byte[] input) {
        if (input == null) {
            throw new IllegalArgumentException("Input array cannot be null");
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : input) {
            sb.append(String.format("%02X ", b));
        }

        logger.trace("{}Converted {} bytes to hex string", LOG_OPERATION, input.length);
        return sb.toString();
    }

    /**
     * Logs the contents of a ByteBuffer in hexadecimal format.
     *
     * @param logger The logger to use
     * @param label A label for the log entry
     * @param remote The remote endpoint identifier
     * @param buf The buffer to log
     * @throws IOException if an I/O error occurs during hex dump
     * @throws IllegalArgumentException if any parameter is null
     */
    public static void logBuffer(Logger logger, String label, String remote, ByteBuffer buf) throws IOException {
        if (logger == null || label == null || remote == null || buf == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }

        if (buf.hasRemaining()) {
            try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                ByteBuffer buffer = buf.asReadOnlyBuffer();
                byte[] bytes = new byte[buf.remaining()];
                buffer.get(bytes, 0, buffer.remaining());

                HexDump.dump(bytes, 0, stream, 0);
                stream.flush();

                logger.trace("[{}] {} {}:%n{}%n", remote, label, BufferUtil.toDetailString(buf),
                        stream.toString(StandardCharsets.UTF_8.name()));
            }
        }
    }
}
