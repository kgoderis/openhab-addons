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
 * Interface for HomeKit peer group unique identifiers.
 * <p>
 * This interface defines the contract for peer group UIDs in the HomeKit
 * system. It provides
 * methods for accessing and managing unique identifiers for HomeKit peer
 * groups, ensuring
 * proper identification and grouping of related accessories.
 * </p>
 * <p>
 * The interface provides:
 * <ul>
 * <li>String representation of the UID</li>
 * <li>Peer group name management</li>
 * <li>Unique identification</li>
 * </ul>
 * </p>
 * <p>
 * Key implementation details:
 * <ul>
 * <li>Thread-safe UID generation</li>
 * <li>Unique ID validation</li>
 * <li>Group name association</li>
 * <li>String format consistency</li>
 * </ul>
 * </p>
 * <p>
 * The interface integrates with:
 * <ul>
 * <li>{@link org.openhab.core.common.registry.Identifiable} for registry
 * integration</li>
 * <li>{@link org.openhab.io.homekit.api.accessory.HomekitAccessory} for
 * accessory grouping</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0.0
 */
public interface HomekitPeerGroupUID extends Identifiable<HomekitPeerGroupUID> {
    /**
     * Gets the unique identifier string for this peer group.
     * This method provides a string representation of the peer group's unique
     * identifier
     * that can be used for display, logging, and identification purposes.
     *
     * @return The unique identifier string
     * @since 1.0.0
     */
    String toString();

    /**
     * Gets the peer group name.
     * This method retrieves the name assigned to the peer group,
     * which is used for grouping and organizing related accessories.
     *
     * @return The peer group name
     * @since 1.0.0
     */
    String getPeerGroup();
}
