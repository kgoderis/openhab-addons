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

package org.openhab.io.homekit.network.http;

import java.nio.ByteBuffer;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.util.ArrayTrie;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.Trie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines the supported HTTP versions for HomeKit communication.
 *
 * <p>
 * This enum provides a set of HTTP version constants used in HomeKit network
 * communication. It extends the standard HTTP versions with HomeKit-specific
 * protocol versions and provides utilities for version parsing and conversion.
 * </p>
 *
 * <p>
 * <b>Component Integration:</b>
 * </p>
 * <ul>
 * <li>{@link HomekitHttpParser} for protocol parsing</li>
 * <li>{@link HomekitHttpGenerator} for protocol generation</li>
 * <li>{@link HomekitHttpConnection} for connection handling</li>
 * <li>{@link HomekitHttpChannel} for channel management</li>
 * </ul>
 *
 * <p>
 * <b>Key responsibilities:</b>
 * </p>
 * <ul>
 * <li>Defining supported HTTP versions</li>
 * <li>Providing version parsing utilities</li>
 * <li>Supporting version conversion</li>
 * <li>Managing version caching</li>
 * </ul>
 *
 * <p>
 * <b>Implementation details:</b>
 * </p>
 * <ul>
 * <li>Uses a Trie-based cache for efficient version lookups</li>
 * <li>Provides optimized byte-level parsing for performance</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@NonNullByDefault
public enum HomekitHttpVersion {
    HTTP_0_9("HTTP/0.9", 9),
    HTTP_1_0("HTTP/1.0", 10),
    HTTP_1_1("HTTP/1.1", 11),
    HTTP_2("HTTP/2.0", 20),
    EVENT_1_0("EVENT/1.0", 30);

    private static final Logger logger = LoggerFactory.getLogger(HomekitHttpVersion.class);

    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit HttpVersion: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_STATE = LOG_PREFIX + "State - ";
    private static final String LOG_ERROR = LOG_PREFIX + "Error - ";

    /**
     * Cache for efficient version lookups.
     * Uses a Trie data structure for optimal performance.
     */
    public static final Trie<HomekitHttpVersion> CACHE = new ArrayTrie<HomekitHttpVersion>();

    static {
        for (HomekitHttpVersion version : HomekitHttpVersion.values()) {
            CACHE.put(version.toString(), version);
            logger.trace("{}Created version: {} ({})", LOG_INIT, version._string, version._version);
        }
    }

    /**
     * Gets the HTTP version from a string representation.
     *
     * <p>
     * This method looks up a version in the cache using its string representation.
     * The lookup is case-insensitive and optimized using a Trie data structure.
     * </p>
     *
     * @param version The version string to parse
     * @return The corresponding HomekitHttpVersion or empty if not found
     */
    public static Optional<HomekitHttpVersion> get(String version) {
        return Optional.ofNullable(CACHE.get(version));
    }

    /**
     * Optimized lookup to find an HTTP Version and whitespace in a byte array.
     *
     * <p>
     * This method performs a fast, byte-level parsing of HTTP version strings
     * in a byte array, looking for specific patterns that indicate valid
     * HTTP version declarations.
     * </p>
     *
     * <p>
     * <b>Key implementation details:</b>
     * </p>
     * <ul>
     * <li>Performs byte-level pattern matching</li>
     * <li>Handles both HTTP and EVENT protocols</li>
     * <li>Supports case-insensitive matching</li>
     * <li>Optimized for performance</li>
     * </ul>
     *
     * @param bytes Array containing ISO-8859-1 characters
     * @param position The first valid index
     * @param limit The first non-valid index
     * @return A HomekitHttpVersion if a match is found, empty otherwise
     */
    public static Optional<HomekitHttpVersion> lookAheadGet(byte[] bytes, int position, int limit) {
        if (position < 0 || limit < position) {
            return Optional.empty();
        }
        int length = limit - position;
        if (length < 9) {
            logger.trace("{}Buffer too short for version lookup: {}", LOG_STATE, length);
            return Optional.empty();
        }

        if (bytes[position + 4] == '/' && bytes[position + 6] == '.'
                && Character.isWhitespace((char) bytes[position + 8])
                && ((bytes[position] == 'H' && bytes[position + 1] == 'T' && bytes[position + 2] == 'T'
                        && bytes[position + 3] == 'P')
                        || (bytes[position] == 'h' && bytes[position + 1] == 't' && bytes[position + 2] == 't'
                                && bytes[position + 3] == 'p'))) {
            switch (bytes[position + 5]) {
                case '1':
                    switch (bytes[position + 7]) {
                        case '0':
                            return Optional.of(HomekitHttpVersion.HTTP_1_0);
                        case '1':
                            return Optional.of(HomekitHttpVersion.HTTP_1_1);
                        default:
                            logger.trace("{}Invalid HTTP/1.x version: {}", LOG_STATE, bytes[position + 7]);
                            return Optional.empty();
                    }
                case '2':
                    switch (bytes[position + 7]) {
                        case '0':
                            return Optional.of(HomekitHttpVersion.HTTP_2);
                        default:
                            logger.trace("{}Invalid HTTP/2.x version: {}", LOG_STATE, bytes[position + 7]);
                            return Optional.empty();
                    }
                default:
                    logger.trace("{}Invalid HTTP major version: {}", LOG_STATE, bytes[position + 5]);
                    return Optional.empty();
            }
        }

        if (bytes[position + 5] == '/' && bytes[position + 7] == '.'
                && Character.isWhitespace((char) bytes[position + 9])
                && ((bytes[position] == 'E' && bytes[position + 1] == 'V' && bytes[position + 2] == 'E'
                        && bytes[position + 3] == 'N' && bytes[position + 4] == 'T')
                        || (bytes[position] == 'e' && bytes[position + 1] == 'v' && bytes[position + 2] == 'e'
                                && bytes[position + 3] == 'n') && bytes[position + 4] == 't')) {
            switch (bytes[position + 6]) {
                case '1':
                    switch (bytes[position + 8]) {
                        case '0':
                            return Optional.of(HomekitHttpVersion.EVENT_1_0);
                        default:
                            logger.trace("{}Invalid EVENT/1.x version: {}", LOG_STATE, bytes[position + 8]);
                            return Optional.empty();
                    }
                default:
                    logger.trace("{}Invalid EVENT major version: {}", LOG_STATE, bytes[position + 6]);
                    return Optional.empty();
            }
        }

        logger.trace("{}No valid version pattern found", LOG_STATE);
        return Optional.empty();
    }

    /**
     * Optimized lookup to find an HTTP Version in a ByteBuffer.
     *
     * <p>
     * This method provides a convenient wrapper for looking up HTTP versions
     * in ByteBuffer objects, delegating to the byte array implementation.
     * </p>
     *
     * @param buffer Buffer containing ISO-8859-1 characters
     * @return A HomekitHttpVersion if a match is found, empty otherwise
     */
    public static Optional<HomekitHttpVersion> lookAheadGet(ByteBuffer buffer) {
        if (buffer.hasArray()) {
            return lookAheadGet(buffer.array(), buffer.arrayOffset() + buffer.position(),
                    buffer.arrayOffset() + buffer.limit());
        }
        logger.trace("{}Buffer does not have array backing", LOG_STATE);
        return Optional.empty();
    }

    private final String _string;
    private final byte[] _bytes;
    private final ByteBuffer _buffer;
    private final int _version;

    /**
     * Creates a new HTTP version constant.
     *
     * <p>
     * This constructor initializes a version with its string representation
     * and numeric identifier, preparing the byte array and buffer for
     * efficient lookups.
     * </p>
     *
     * @param s The string representation of the version
     * @param version The numeric version identifier
     */
    HomekitHttpVersion(String s, int version) {
        _string = s;
        _bytes = StringUtil.getBytes(s);
        _buffer = ByteBuffer.wrap(_bytes);
        _version = version;
    }

    /**
     * Gets the byte array representation of this version.
     *
     * @return The version as a byte array
     */
    public byte[] toBytes() {
        return _bytes;
    }

    /**
     * Gets the ByteBuffer representation of this version.
     *
     * @return A read-only ByteBuffer containing the version
     */
    public ByteBuffer toBuffer() {
        return _buffer.asReadOnlyBuffer();
    }

    /**
     * Gets the numeric version identifier.
     *
     * @return The version number
     */
    public int getVersion() {
        return _version;
    }

    /**
     * Checks if this version matches a given string.
     *
     * @param s The string to compare against
     * @return true if the versions match (case-insensitive)
     */
    public boolean is(String s) {
        return _string.equalsIgnoreCase(s);
    }

    /**
     * Gets the string representation of this version.
     *
     * @return The version string
     */
    public String asString() {
        return _string;
    }

    @Override
    public String toString() {
        return _string;
    }

    /**
     * Converts a string to a HomekitHttpVersion.
     *
     * <p>
     * This method looks up a version in the cache using its string representation.
     * The lookup is case-insensitive and optimized using a Trie data structure.
     * </p>
     *
     * @param version The version string to convert
     * @return The corresponding HomekitHttpVersion or empty if not found
     */
    public static Optional<HomekitHttpVersion> fromString(String version) {
        return Optional.ofNullable(CACHE.get(version));
    }

    /**
     * Converts a numeric version to a HomekitHttpVersion.
     *
     * <p>
     * This method maps numeric version identifiers to their corresponding
     * HomekitHttpVersion enum values, with special handling for unsupported versions.
     * </p>
     *
     * @param version The numeric version to convert
     * @return The corresponding HomekitHttpVersion
     * @throws IllegalArgumentException if the version is not supported
     */
    public static HomekitHttpVersion fromVersion(int version) {
        try {
            switch (version) {
                case 9:
                    return HomekitHttpVersion.HTTP_0_9;
                case 10:
                    return HomekitHttpVersion.HTTP_1_0;
                case 11:
                    return HomekitHttpVersion.HTTP_1_1;
                case 20:
                    return HomekitHttpVersion.HTTP_2;
                case 30:
                    return HomekitHttpVersion.EVENT_1_0;
                default:
                    logger.error("{}Unsupported version number: {}", LOG_ERROR, version);
                    throw new IllegalArgumentException("Unsupported version: " + version);
            }
        } catch (IllegalArgumentException e) {
            logger.error("{}Failed to convert version number {}: {}", LOG_ERROR, version, e.getMessage());
            throw e;
        }
    }

    /**
     * Gets the best matching version from a buffer.
     *
     * <p>
     * This method uses the Trie cache to find the best matching version
     * in the given buffer range.
     * </p>
     *
     * @param buffer The buffer to search in
     * @param i The starting position
     * @param remaining The number of bytes to consider
     * @return The best matching HomekitHttpVersion or empty if none found
     */
    public static Optional<HomekitHttpVersion> getBest(ByteBuffer buffer, int i, int remaining) {
        // Buffer is marked as @NonNull in method signature, but we'll keep parameter validation for i and remaining
        if (i < 0 || remaining < 0) {
            return Optional.empty();
        }
        return Optional.ofNullable(CACHE.getBest(buffer, i, remaining));
    }

    /**
     * Converts a HomekitHttpVersion to a standard HttpVersion.
     *
     * <p>
     * This method maps HomeKit-specific versions to standard HTTP versions,
     * with special handling for EVENT protocol versions.
     * </p>
     *
     * @param version The HomekitHttpVersion to convert
     * @return The corresponding HttpVersion
     */
    public static HttpVersion convert(HomekitHttpVersion version) {
        int versionNumber = version.getVersion();
        HttpVersion result;
        switch (versionNumber) {
            case 30: {
                result = HttpVersion.fromVersion(11);
                break;
            }
            default: {
                result = HttpVersion.fromVersion(versionNumber);
                break;
            }
        }
        logger.trace("{}Converted {} to {}", LOG_STATE, version, result);
        return result;
    }
}
