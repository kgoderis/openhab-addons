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

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.protocol.crypto.HomekitEncryptionEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;

/**
 * A utility class for generating cryptographic keys and identifiers used in HomeKit security operations.
 * This class provides methods for generating secure random keys and unique identifiers that comply
 * with HomeKit security requirements.
 *
 * <p>
 * Key features:
 * <ul>
 * <li>Secure key generation using EdDSA (Ed25519)</li>
 * <li>Cryptographically secure random number generation</li>
 * <li>MAC address format identifier generation</li>
 * <li>Thread-safe operations</li>
 * </ul>
 *
 * <p>
 * Integration points:
 * <ul>
 * <li>HomeKit encryption engine</li>
 * <li>Accessory pairing process</li>
 * <li>Secure communication setup</li>
 * </ul>
 *
 * <p>
 * Implementation details:
 * <ul>
 * <li>Uses Ed25519 curve for key generation</li>
 * <li>Employs secure random number generation</li>
 * <li>Generates locally administered MAC addresses</li>
 * <li>UTF-8 encoding for identifier strings</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 */
@NonNullByDefault
public class HomekitKeyGenerator {

    private static final Logger logger = LoggerFactory.getLogger(HomekitKeyGenerator.class);
    private static final String LOG_PREFIX = "Homekit KeyGenerator: ";
    private static final String LOG_KEY = LOG_PREFIX + "Key - ";
    private static final String LOG_ID = LOG_PREFIX + "ID - ";

    /**
     * Generates a secret key for HomeKit encryption using the Ed25519 curve.
     * The generated key is suitable for use in HomeKit's encryption engine.
     *
     * <p>
     * The key generation process:
     * <ol>
     * <li>Retrieves the Ed25519 curve parameters</li>
     * <li>Creates a seed buffer of appropriate size</li>
     * <li>Fills the buffer with cryptographically secure random bytes</li>
     * </ol>
     *
     * @return A byte array containing the generated secret key
     * @throws IllegalStateException if the Ed25519 curve parameters cannot be retrieved
     */
    public static byte[] generateSecretKey() {
        logger.debug("{}Generating secret key using Ed25519 curve", LOG_KEY);

        EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("ed25519-sha-512");
        if (spec == null) {
            logger.error("{}Failed to retrieve Ed25519 curve parameters", LOG_KEY);
            throw new IllegalStateException("Ed25519 curve parameters not available");
        }

        byte[] seed = new byte[spec.getCurve().getField().getb() / 8];
        HomekitEncryptionEngine.getSecureRandom().nextBytes(seed);

        logger.debug("{}Generated secret key of length {} bytes", LOG_KEY, seed.length);
        return seed;
    }

    /**
     * Generates a unique identifier in MAC address format for HomeKit accessories.
     * The generated ID follows the locally administered unicast MAC address format.
     *
     * <p>
     * The ID generation process:
     * <ol>
     * <li>Generates a random byte for the first octet with specific bits set</li>
     * <li>Generates 5 additional random bytes</li>
     * <li>Formats the bytes as a colon-separated hexadecimal string</li>
     * </ol>
     *
     * <p>
     * The first byte is generated with the following constraints:
     * <ul>
     * <li>Least significant bit is set to 0 (unicast)</li>
     * <li>Second least significant bit is set to 1 (locally administered)</li>
     * <li>Value is non-zero</li>
     * </ul>
     *
     * @return A byte array containing the generated identifier in UTF-8 encoding
     */
    public static byte[] generateHexidecimalId() {
        logger.debug("{}Generating hexadecimal identifier", LOG_ID);

        int byte1 = ((HomekitEncryptionEngine.getSecureRandom().nextInt(255) + 1) | 2) & 0xFE;
        String id = Integer.toHexString(byte1).toUpperCase() + ":"
                + Stream.generate(() -> HomekitEncryptionEngine.getSecureRandom().nextInt(255) + 1).limit(5)
                        .map(i -> Integer.toHexString(i).toUpperCase()).collect(Collectors.joining(":"));

        logger.debug("{}Generated identifier: {}", LOG_ID, id);
        return id.getBytes(StandardCharsets.UTF_8);
    }
}
