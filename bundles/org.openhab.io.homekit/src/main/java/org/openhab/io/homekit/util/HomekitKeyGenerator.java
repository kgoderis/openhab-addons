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

import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;

/**
 * Utility class for generating Homekit keys and identifiers.
 *
 * @author Your Name - Initial contribution
 */
@NonNullByDefault
public class HomekitKeyGenerator {

    /**
     * Generates a secret key for Homekit encryption.
     *
     * @return the generated secret key
     */
    public static byte[] generateSecretKey() {
        EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("ed25519-sha-512");
        byte[] seed = new byte[spec.getCurve().getField().getb() / 8];
        HomekitEncryptionEngine.getSecureRandom().nextBytes(seed);
        return seed;
    }

    /**
     * Generates a pairing ID for Homekit accessories.
     *
     * @return the generated pairing ID
     */
    public static byte[] generateHexidecimalId() {
        int byte1 = ((HomekitEncryptionEngine.getSecureRandom().nextInt(255) + 1) | 2) & 0xFE; // Unicast locally
                                                                                               // administered MAC;
        return (Integer.toHexString(byte1).toUpperCase() + ":"
                + Stream.generate(() -> HomekitEncryptionEngine.getSecureRandom().nextInt(255) + 1).limit(5)
                        .map(i -> Integer.toHexString(i).toUpperCase()).collect(Collectors.joining(":")))
                .getBytes(StandardCharsets.UTF_8);
    }
}
