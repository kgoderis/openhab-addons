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

package org.openhab.io.homekit.api.uid;

import org.openhab.core.common.registry.Identifiable;

/**
 * Interface for HomeKit pairing unique identifiers.
 * <p>
 * This interface defines the contract for pairing UIDs in the HomeKit system.
 * It provides
 * methods for accessing and managing unique identifiers for HomeKit pairings,
 * ensuring
 * proper identification and security throughout the system.
 * </p>
 * <p>
 * The interface provides:
 * <ul>
 * <li>String representation of the UID</li>
 * <li>Destination pairing ID management</li>
 * <li>Source pairing ID management</li>
 * <li>Unique identification</li>
 * </ul>
 * </p>
 * <p>
 * Key implementation details:
 * <ul>
 * <li>Thread-safe UID generation</li>
 * <li>Unique ID validation</li>
 * <li>Pairing ID association</li>
 * <li>String format consistency</li>
 * <li>Byte array handling</li>
 * </ul>
 * </p>
 * <p>
 * The interface integrates with:
 * <ul>
 * <li>{@link org.openhab.core.common.registry.Identifiable} for registry
 * integration</li>
 * <li>{@link org.openhab.io.homekit.api.server.HomekitAccessoryServer} for
 * server identification</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0.0
 */
public interface HomekitPairingUID extends Identifiable<HomekitPairingUID> {
    /**
     * Gets the unique identifier string for this pairing.
     * This method provides a string representation of the pairing's unique
     * identifier
     * that can be used for display, logging, and identification purposes.
     *
     * @return The unique identifier string
     * @since 1.0.0
     */
    String toString();

    /**
     * Gets the destination pairing ID.
     * This method retrieves the unique identifier used for the destination device
     * in the pairing relationship.
     *
     * @return The destination pairing ID as a byte array
     * @since 1.0.0
     */
    byte[] getId();

    /**
     * Gets the source pairing ID.
     * This method retrieves the unique identifier used for the source device
     * in the pairing relationship.
     *
     * @return The source pairing ID as a byte array
     * @since 1.0.0
     */
    byte[] getSourcePairingId();
}
