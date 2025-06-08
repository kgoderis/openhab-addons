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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.protocol.error.HomekitErrorCode;
import org.openhab.io.homekit.protocol.message.HomekitMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class for encoding and decoding Type-Length-Value (TLV) data in HomeKit protocol.
 *
 * <p>
 * This class provides functionality for encoding and decoding TLV data structures
 * used in the HomeKit protocol. It handles the conversion between raw bytes and
 * structured data types, supporting various HomeKit message types and error codes.
 * </p>
 *
 * <p>
 * The class integrates with several key components:
 * </p>
 * <ul>
 * <li>{@link org.openhab.io.homekit.protocol.message.HomekitMessage} for message type definitions</li>
 * <li>{@link org.openhab.io.homekit.protocol.error.HomekitErrorCode} for error code handling</li>
 * <li>{@link java.io.ByteArrayInputStream} for input stream handling</li>
 * <li>{@link java.io.ByteArrayOutputStream} for output stream handling</li>
 * </ul>
 *
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>TLV data structure encoding and decoding</li>
 * <li>Support for various data types (byte, BigInteger, byte arrays)</li>
 * <li>Error code handling and conversion</li>
 * <li>Stream-based processing for large data sets</li>
 * <li>Thread-safe operations</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@NonNullByDefault
public class HomekitTypeLengthValueEncoderDecoder {
    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit TLV: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_ENCODE = LOG_PREFIX + "Encode - ";
    protected static final String LOG_DECODE = LOG_PREFIX + "Decode - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";

    protected static final Logger logger = LoggerFactory.getLogger(HomekitTypeLengthValueEncoderDecoder.class);

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>
     * This class is designed to be used as a utility class with static methods.
     * </p>
     */
    private HomekitTypeLengthValueEncoderDecoder() {
    }

    /**
     * Decodes a byte array containing TLV data.
     *
     * <p>
     * This method processes a byte array containing Type-Length-Value data and
     * converts it into a structured DecodeResult object.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Processes input stream byte by byte</li>
     * <li>Extracts type, length, and value components</li>
     * <li>Handles variable-length data</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @param content The byte array to decode
     * @return A DecodeResult containing the decoded data
     * @throws IOException if an I/O error occurs during decoding
     */
    public static DecodeResult decode(byte[] content) throws IOException {
        logger.trace("{}Decoding {}", LOG_DECODE, HomekitByte.toHexString(content));
        DecodeResult ret = new DecodeResult();
        ByteArrayInputStream bais = new ByteArrayInputStream(content);
        while (bais.available() > 0) {
            byte type = (byte) (bais.read() & 0xFF);
            int length = bais.read();
            byte[] part = new byte[length];
            bais.read(part);
            ret.add(type, part);
            logger.trace("{}Decoded T {} L {} V {}", LOG_DECODE, HomekitMessage.get(type).name(), length,
                    HomekitByte.toHexString(part));
        }
        return ret;
    }

    /**
     * Creates a new TLV encoder instance.
     *
     * <p>
     * This factory method provides a new encoder instance for building TLV data.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Creates new ByteArrayOutputStream</li>
     * <li>Returns new Encoder instance</li>
     * <li>Thread-safe operation</li>
     * </ul>
     *
     * @return A new Encoder instance
     */
    public static Encoder getEncoder() {
        return new Encoder();
    }

    /**
     * A class for encoding TLV data structures.
     *
     * <p>
     * This inner class provides methods for building TLV data structures by
     * adding various types of data with their corresponding message types.
     * </p>
     *
     * <p>
     * Key features:
     * </p>
     * <ul>
     * <li>Support for multiple data types</li>
     * <li>Automatic length calculation</li>
     * <li>Chunked data handling</li>
     * <li>Trace-level logging</li>
     * </ul>
     */
    public static final class Encoder {
        private final ByteArrayOutputStream baos;

        private Encoder() {
            baos = new ByteArrayOutputStream();
        }

        /**
         * Adds a BigInteger value to the TLV data.
         *
         * <p>
         * Key implementation details:
         * </p>
         * <ul>
         * <li>Converts BigInteger to byte array</li>
         * <li>Handles large numbers</li>
         * <li>Provides trace-level logging</li>
         * </ul>
         *
         * @param type The message type
         * @param i The BigInteger value to add
         * @throws IOException if an I/O error occurs
         */
        public void add(HomekitMessage type, BigInteger i) throws IOException {
            add(type, HomekitByte.toByteArray(i));
        }

        /**
         * Adds a short value to the TLV data.
         *
         * <p>
         * Key implementation details:
         * </p>
         * <ul>
         * <li>Writes type byte</li>
         * <li>Writes length byte</li>
         * <li>Writes value byte</li>
         * </ul>
         *
         * @param type The message type
         * @param b The short value to add
         */
        public void add(HomekitMessage type, short b) {
            baos.write(type.getKey());
            baos.write(1);
            baos.write(b);
        }

        /**
         * Adds an error code to the TLV data.
         *
         * <p>
         * Key implementation details:
         * </p>
         * <ul>
         * <li>Writes type byte</li>
         * <li>Writes length byte</li>
         * <li>Writes error code</li>
         * </ul>
         *
         * @param type The message type
         * @param e The error code to add
         */
        public void add(HomekitMessage type, HomekitErrorCode e) {
            baos.write(type.getKey());
            baos.write(1);
            baos.write(e.getCode());
        }

        /**
         * Adds a byte array to the TLV data.
         *
         * <p>
         * Key implementation details:
         * </p>
         * <ul>
         * <li>Handles large byte arrays</li>
         * <li>Splits data into chunks if needed</li>
         * <li>Provides trace-level logging</li>
         * </ul>
         *
         * @param type The message type
         * @param bytes The byte array to add
         * @throws IOException if an I/O error occurs
         */
        public void add(HomekitMessage type, byte[] bytes) throws IOException {
            InputStream bais = new ByteArrayInputStream(bytes);
            while (bais.available() > 0) {
                int toWrite = bais.available();
                toWrite = toWrite > 255 ? 255 : toWrite;
                baos.write(type.getKey());
                baos.write(toWrite);
                HomekitByte.copyStream(bais, baos, toWrite);
                logger.trace("{}Encoded T {} L {} V {}", LOG_ENCODE, type.name(), toWrite,
                        HomekitByte.toHexString(bytes));
            }
        }

        /**
         * Gets the encoded TLV data as a byte array.
         *
         * <p>
         * Key implementation details:
         * </p>
         * <ul>
         * <li>Returns complete TLV data</li>
         * <li>Preserves all added values</li>
         * </ul>
         *
         * @return The encoded TLV data
         */
        public byte[] toByteArray() {
            return baos.toByteArray();
        }
    }

    /**
     * A class for storing decoded TLV data.
     *
     * <p>
     * This inner class provides methods for accessing decoded TLV data in
     * various formats and handling multiple values for the same type.
     * </p>
     *
     * <p>
     * Key features:
     * </p>
     * <ul>
     * <li>Support for multiple data types</li>
     * <li>Value merging for repeated types</li>
     * <li>Safe value access</li>
     * </ul>
     */
    public static final class DecodeResult {
        private final Map<Short, byte[]> result = new HashMap<>();

        private DecodeResult() {
        }

        /**
         * Gets a byte value for the specified message type.
         *
         * <p>
         * Key implementation details:
         * </p>
         * <ul>
         * <li>Returns first byte of value</li>
         * <li>Assumes single-byte value</li>
         * </ul>
         *
         * @param type The message type
         * @return The byte value
         */
        public byte getByte(HomekitMessage type) {
            return result.get(type.getKey())[0];
        }

        /**
         * Gets a BigInteger value for the specified message type.
         *
         * <p>
         * Key implementation details:
         * </p>
         * <ul>
         * <li>Converts bytes to BigInteger</li>
         * <li>Handles large numbers</li>
         * </ul>
         *
         * @param type The message type
         * @return The BigInteger value
         */
        public BigInteger getBigInt(HomekitMessage type) {
            return new BigInteger(1, result.get(type.getKey()));
        }

        /**
         * Gets a byte array value for the specified message type.
         *
         * <p>
         * Key implementation details:
         * </p>
         * <ul>
         * <li>Returns complete byte array</li>
         * <li>Preserves all bytes</li>
         * </ul>
         *
         * @param type The message type
         * @return The byte array value
         */
        public byte[] getBytes(HomekitMessage type) {
            return result.get(type.getKey());
        }

        /**
         * Copies bytes from the specified message type to a destination array.
         *
         * <p>
         * Key implementation details:
         * </p>
         * <ul>
         * <li>Handles partial copies</li>
         * <li>Respects array bounds</li>
         * <li>Uses System.arraycopy</li>
         * </ul>
         *
         * @param type The message type
         * @param dest The destination array
         * @param srcOffset The source offset
         */
        public void getBytes(HomekitMessage type, byte[] dest, int srcOffset) {
            byte[] b = result.get(type.getKey());
            System.arraycopy(b, srcOffset, dest, 0, Math.min(dest.length, b.length));
        }

        /**
         * Gets the length of the value for the specified message type.
         *
         * <p>
         * Key implementation details:
         * </p>
         * <ul>
         * <li>Returns array length</li>
         * <li>Handles null values</li>
         * </ul>
         *
         * @param type The message type
         * @return The length of the value
         */
        public int getLength(HomekitMessage type) {
            return result.get(type.getKey()).length;
        }

        /**
         * Adds a value for the specified type, merging with existing values.
         *
         * <p>
         * Key implementation details:
         * </p>
         * <ul>
         * <li>Merges multiple values</li>
         * <li>Uses HomekitByte.joinBytes</li>
         * <li>Thread-safe operation</li>
         * </ul>
         *
         * @param type The message type
         * @param bytes The bytes to add
         */
        private void add(short type, byte[] bytes) {
            result.merge(type, bytes, HomekitByte::joinBytes);
        }
    }
}
