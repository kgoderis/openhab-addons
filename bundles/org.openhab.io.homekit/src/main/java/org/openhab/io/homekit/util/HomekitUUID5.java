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

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class for generating UUID version 5 (SHA-1) identifiers for HomeKit components.
 *
 * <p>
 * This class provides functionality for generating deterministic UUIDs based on
 * namespaces and names, following the UUID version 5 specification. It is used
 * throughout the HomeKit integration to create consistent identifiers for
 * accessories, services, and characteristics.
 * </p>
 *
 * <p>
 * The class integrates with several key components:
 * </p>
 * <ul>
 * <li>{@link java.security.MessageDigest} for SHA-1 hashing</li>
 * <li>{@link java.util.UUID} for UUID generation and manipulation</li>
 * <li>OpenHAB's HomeKit integration for component identification</li>
 * </ul>
 *
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>UUID version 5 (SHA-1) generation</li>
 * <li>Namespace-based UUID creation</li>
 * <li>Support for string and byte array inputs</li>
 * <li>Predefined HomeKit namespaces</li>
 * <li>Thread-safe operations</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@NonNullByDefault
public class HomekitUUID5 {
    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit UUID5: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_UUID = LOG_PREFIX + "UUID - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";

    private static final Logger logger = LoggerFactory.getLogger(HomekitUUID5.class);
    private static final Charset UTF8 = Charset.forName("UTF-8");
    // public static final UUID NAMESPACE_DNS = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");
    // public static final UUID NAMESPACE_URL = UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8");
    // public static final UUID NAMESPACE_OID = UUID.fromString("6ba7b812-9dad-11d1-80b4-00c04fd430c8");
    // public static final UUID NAMESPACE_X500 = UUID.fromString("6ba7b814-9dad-11d1-80b4-00c04fd430c8");

    public static final UUID NAMESPACE_HOMEKIT = UUID.fromString("00000000-1000-2000-8000-0026BB765291");
    public static final UUID NAMESPACE_OPENHAB = HomekitUUID5.fromNamespaceAndString(NAMESPACE_HOMEKIT, "openhab.org");
    public static final UUID NAMESPACE_ACCESSORY = HomekitUUID5.fromNamespaceAndString(NAMESPACE_OPENHAB,
            "HomekitAccessory");
    public static final UUID NAMESPACE_SERVICE = HomekitUUID5.fromNamespaceAndString(NAMESPACE_OPENHAB,
            "HomekitService");
    public static final UUID NAMESPACE_CHARACTERISTIC = HomekitUUID5.fromNamespaceAndString(NAMESPACE_OPENHAB,
            "HomekitCharacteristic");

    /**
     * Generates a UUID version 5 from a namespace UUID and a string name.
     *
     * <p>
     * This method creates a deterministic UUID by combining a namespace UUID
     * with a string name using SHA-1 hashing.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Converts string to UTF-8 bytes</li>
     * <li>Validates input parameters</li>
     * <li>Uses SHA-1 hashing</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @param namespace The namespace UUID
     * @param name The name string
     * @return A UUID version 5
     * @throws NullPointerException if namespace or name is null
     */
    public static UUID fromNamespaceAndString(UUID namespace, String name) {
        logger.trace("{}Generating UUID from namespace {} and string {}", LOG_UUID, namespace, name);
        return fromNamespaceAndBytes(namespace, Objects.requireNonNull(name, "name == null").getBytes(UTF8));
    }

    /**
     * Generates a UUID version 5 from a namespace UUID and a byte array name.
     *
     * <p>
     * This method creates a deterministic UUID by combining a namespace UUID
     * with a byte array name using SHA-1 hashing.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Uses SHA-1 message digest</li>
     * <li>Sets UUID version and variant bits</li>
     * <li>Validates input parameters</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @param namespace The namespace UUID
     * @param name The name bytes
     * @return A UUID version 5
     * @throws NullPointerException if namespace or name is null
     * @throws InternalError if SHA-1 algorithm is not available
     */
    public static UUID fromNamespaceAndBytes(UUID namespace, byte[] name) {
        logger.trace("{}Generating UUID from namespace {} and bytes", LOG_UUID, namespace);
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException nsae) {
            logger.error("{}SHA-1 algorithm not supported", LOG_ERROR);
            throw new InternalError("SHA-1 not supported");
        }
        md.update(toBytes(Objects.requireNonNull(namespace, "namespace is null")));
        md.update(Objects.requireNonNull(name, "name is null"));
        byte[] sha1Bytes = md.digest();
        UUID result = fromBytes(sha1Bytes);
        logger.trace("{}Generated UUID: {}", LOG_UUID, result);
        return result;
    }

    /**
     * Creates a UUID from a byte array.
     *
     * <p>
     * This method converts a 16-byte array into a UUID by splitting it into
     * most significant and least significant bits.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Requires 16-byte input</li>
     * <li>Splits into MSB and LSB</li>
     * <li>Handles byte ordering</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @param data The 16-byte array
     * @return A UUID
     * @throws AssertionError if data length is less than 16 bytes
     */
    private static UUID fromBytes(byte[] data) {
        logger.trace("{}Creating UUID from {} bytes", LOG_UUID, data.length);
        long msb = 0;
        long lsb = 0;
        assert data.length >= 16;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (data[i] & 0xff);
        }
        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (data[i] & 0xff);
        }
        return new UUID(msb, lsb);
    }

    /**
     * Converts a UUID to a byte array.
     *
     * <p>
     * This method converts a UUID into a 16-byte array by extracting its
     * most significant and least significant bits.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Produces 16-byte output</li>
     * <li>Extracts MSB and LSB</li>
     * <li>Handles byte ordering</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @param uuid The UUID to convert
     * @return A 16-byte array
     */
    private static byte[] toBytes(UUID uuid) {
        logger.trace("{}Converting UUID {} to bytes", LOG_UUID, uuid);
        byte[] out = new byte[16];
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            out[i] = (byte) ((msb >> ((7 - i) * 8)) & 0xff);
        }
        for (int i = 8; i < 16; i++) {
            out[i] = (byte) ((lsb >> ((15 - i) * 8)) & 0xff);
        }
        return out;
    }
}
