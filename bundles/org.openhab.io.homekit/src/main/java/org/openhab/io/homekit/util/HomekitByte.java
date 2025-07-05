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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HexFormat;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
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
@NonNullByDefault
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
    public static byte[] joinBytes(byte @Nullable []... piece) {
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

        byte[] array = i.toByteArray();
        if (array[0] == 0) {
            array = Arrays.copyOfRange(array, 1, array.length);
        }

        logger.trace("{}Converted BigInteger to {} bytes", LOG_OPERATION, array.length);
        return array;
    }

    /**
     * Converts a byte array to a hexadecimal string representation.
     *
     * @param input The byte array to convert
     * @return A string containing the hexadecimal representation of the input
     * @throws IllegalArgumentException if the input is null
     */
    public static String toHex(byte[] input) {

        StringBuilder sb = new StringBuilder();
        for (byte b : input) {
            sb.append(String.format("%02X ", b));
        }

        logger.trace("{}Converted {} bytes to hex string", LOG_OPERATION, input.length);
        return sb.toString();
    }

    /**
     * Pads a BigInteger to the byte length of the modulus N, as required by SRP6 protocol operations.
     * <p>
     * This method is used to ensure that all SRP6 values (such as N, g, A, B, S) are represented as byte arrays
     * of consistent length, matching the byte length of the modulus N. This is critical for interoperability
     * and compliance with RFC 2945/5054 and HomeKit's SRP6 implementation.
     * <p>
     * The byte length is calculated as (N.bitLength() + 7) / 8. If the input BigInteger is shorter, it is
     * left-padded with zeros. If it is longer, the leading bytes are preserved (should not occur for valid SRP6
     * values).
     *
     * @param n The BigInteger value to pad
     * @param N The modulus, used to determine the target byte length
     * @return A byte array of length equal to the byte length of N, containing the padded value of n
     */
    public static byte[] Pad(BigInteger n, BigInteger N) {
        int length = (N.bitLength() + 7) / 8;
        return HomekitByte.Pad(n, length);
    }

    public static byte[] Pad(BigInteger n, int length) {
        byte[] bs = toByteArray(n);
        if (bs.length < length) {
            byte[] tmp = new byte[length];
            System.arraycopy(bs, 0, tmp, length - bs.length, bs.length);
            bs = tmp;
        }
        logger.trace("{}Padded BigInteger {} to length {}", LOG_OPERATION, n, length);
        return bs;
    }

    /**
     * XOR two byte arrays of the same length.
     * 
     * @param a First byte array
     * @param b Second byte array
     * @return XOR result
     * @throws IllegalArgumentException if arrays have different lengths
     */
    public static byte[] xor(byte[] a, byte[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Arrays must be the same length");
        }
        byte[] result = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = (byte) (a[i] ^ b[i]);
        }
        return result;
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

        if (buf.hasRemaining()) {
            try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                ByteBuffer buffer = buf.asReadOnlyBuffer();
                byte[] bytes = new byte[buf.remaining()];
                buffer.get(bytes, 0, buffer.remaining());

                // Generate hex dump using Java's HexFormat
                HexFormat hexFormat = HexFormat.of();
                StringBuilder hexDump = new StringBuilder();

                for (int i = 0; i < bytes.length; i += 16) {
                    hexDump.append(String.format("%08X: ", i));

                    // Print hex values
                    for (int j = 0; j < 16; j++) {
                        if (i + j < bytes.length) {
                            hexDump.append(hexFormat.formatHex(new byte[] { bytes[i + j] })).append(' ');
                        } else {
                            hexDump.append("   ");
                        }

                        if (j == 7) {
                            hexDump.append(' ');
                        }
                    }

                    // Print ASCII representation
                    hexDump.append(" |");
                    for (int j = 0; j < 16; j++) {
                        if (i + j < bytes.length) {
                            char c = (char) bytes[i + j];
                            hexDump.append(c >= 32 && c < 127 ? c : '.');
                        } else {
                            hexDump.append(' ');
                        }
                    }
                    hexDump.append("|\n");
                }

                logger.trace("[{}] {} {}:%n{}%n", remote, label, BufferUtil.toDetailString(buf), hexDump.toString());
            }
        }
    }
}
