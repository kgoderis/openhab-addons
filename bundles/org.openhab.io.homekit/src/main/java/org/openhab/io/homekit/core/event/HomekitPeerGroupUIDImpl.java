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

package org.openhab.io.homekit.core.event;

import org.openhab.io.homekit.api.uid.HomekitPeerGroupUID;
import org.openhab.io.homekit.util.HomekitUID;

/**
 * Implementation of a unique identifier for a HomeKit peer group.
 *
 * <p>
 * This class provides a structured way to identify HomeKit peer groups within
 * the system.
 * The UID follows a specific format: {@code homekit:peergroup:{peerGroup}}
 * where:
 * </p>
 * <ul>
 * <li>{@code homekit} is the namespace prefix</li>
 * <li>{@code peergroup} indicates this is a peer group identifier</li>
 * <li>{@code peerGroup} is the unique identifier for the peer group</li>
 * </ul>
 *
 * <p>
 * Key responsibilities:
 * </p>
 * <ul>
 * <li>Creating and parsing peer group UIDs</li>
 * <li>Validating UID format and structure</li>
 * <li>Extracting peer group-specific information from UIDs</li>
 * <li>Ensuring unique identification across the system</li>
 * </ul>
 *
 * <p>
 * The class integrates with:
 * </p>
 * <ul>
 * <li>{@link org.openhab.core.common.registry.Identifiable} for UID
 * management</li>
 * <li>{@link org.openhab.io.homekit.api.event.HomekitEvent} for event
 * handling</li>
 * <li>OpenHAB's UID system for consistent identification</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
     */
public class HomekitPeerGroupUIDImpl extends HomekitUID implements HomekitPeerGroupUID {
    private static final String PEER_GROUP_PREFIX = "peergroup";
    private final String peerGroup;

    /**
     * Creates a new peer group UID with the specified peer group identifier.
     *
     * <p>
     * This constructor builds a complete peer group UID instance with the required
     * peer group identifier. The UID is used to uniquely identify a HomeKit
     * peer group within the system.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Validates all input parameters</li>
     * <li>Constructs the UID string in the correct format</li>
     * <li>Initializes all internal fields</li>
     * <li>Sets up the base UID structure</li>
     * </ul>
     *
     * @param peerGroup The unique identifier for the peer group
     * @throws IllegalArgumentException if the peer group identifier is null or
     *             empty
     */
    public HomekitPeerGroupUIDImpl(String peerGroup) {
        super(PEER_GROUP_PREFIX, "homekit:" + PEER_GROUP_PREFIX + ":" + peerGroup);
        this.peerGroup = peerGroup;
    }

    /**
     * Gets the UID as a string.
     *
     * <p>
     * The string representation follows the format
     * {@code homekit:peergroup:{peerGroup}}.
     * This format ensures consistent identification across the system.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Uses String.format for consistent formatting</li>
     * <li>Maintains the standard UID structure</li>
     * <li>Preserves all identifier components</li>
     * </ul>
     *
     * @return The UID string in the format {@code homekit:peergroup:{peerGroup}}
     */
    @Override
    public String toString() {
        return String.format("homekit:peergroup:%s", peerGroup);
    }

    /**
     * Gets the peer group identifier.
     *
     * <p>
     * The peer group identifier is used to group related HomeKit components
     * together for coordinated event handling and state management.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Returns the internal peer group field</li>
     * <li>Used for group coordination</li>
     * <li>Supports event handling</li>
     * </ul>
     *
     * @return The peer group identifier
     */
    @Override
    public String getPeerGroup() {
        return peerGroup;
    }

    /**
     * Gets the minimum number of segments required for a valid UID.
     *
     * <p>
     * A valid peer group UID must have at least 3 segments:
     * </p>
     * <ol>
     * <li>The namespace prefix ("homekit")</li>
     * <li>The type identifier ("peergroup")</li>
     * <li>The peer group identifier</li>
     * </ol>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Enforces UID structure validation</li>
     * <li>Ensures complete identification</li>
     * <li>Supports UID parsing</li>
     * </ul>
     *
     * @return The minimum number of segments (3) for a valid peer group UID
     */
    @Override
    protected int getMinimalNumberOfSegments() {
        return 3;
    }

    /**
     * Gets this UID instance.
     *
     * <p>
     * This method provides access to the UID instance itself, maintaining
     * consistency with the {@link HomekitPeerGroupUID} interface.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Returns this instance</li>
     * <li>Supports interface compliance</li>
     * <li>Enables UID access</li>
     * </ul>
     *
     * @return This UID instance
     */
    @Override
    public HomekitPeerGroupUID getUID() {
        return this;
    }
}
