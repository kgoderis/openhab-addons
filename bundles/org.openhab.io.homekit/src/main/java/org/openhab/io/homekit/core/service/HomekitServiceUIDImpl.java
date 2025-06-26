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

package org.openhab.io.homekit.core.service;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.uid.HomekitServiceUID;
import org.openhab.io.homekit.util.HomekitUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a unique identifier for a HomeKit service.
 *
 * <p>
 * This class provides a structured way to identify HomeKit services within the
 * system.
 * The UID follows a specific format:
 * {@code homekit:service:{serverId}:{accessoryId}:{serviceId}} where:
 * </p>
 * <ul>
 * <li>{@code homekit} is the namespace prefix</li>
 * <li>{@code service} indicates this is a service identifier</li>
 * <li>{@code serverId} is the unique identifier for the server</li>
 * <li>{@code accessoryId} is the unique identifier for the accessory</li>
 * <li>{@code serviceId} is the unique identifier for the service</li>
 * </ul>
 *
 * <p>
 * Key responsibilities:
 * </p>
 * <ul>
 * <li>Creating and parsing service UIDs</li>
 * <li>Validating UID format and structure</li>
 * <li>Extracting service-specific information from UIDs</li>
 * <li>Ensuring unique identification across the system</li>
 * </ul>
 *
 * <p>
 * The class integrates with:
 * </p>
 * <ul>
 * <li>{@link org.openhab.core.common.registry.Identifiable} for UID
 * management</li>
 * <li>{@link org.openhab.io.homekit.api.service.HomekitService} for service
 * identification</li>
 * <li>OpenHAB's UID system for consistent identification</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@NonNullByDefault
public class HomekitServiceUIDImpl extends HomekitUID implements HomekitServiceUID {
    private static final String SERVICE_PREFIX = "service";
    private static final Logger logger = LoggerFactory.getLogger(HomekitServiceUIDImpl.class);
    private static final String LOG_UID = "Homekit ServiceUID: UID - ";

    /**
     * Creates a new service UID with the specified components.
     *
     * <p>
     * This constructor builds a complete service UID instance with all required
     * identifiers. The UID is used to uniquely identify a HomeKit service
     * within the system.
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
     * @param serverId The unique identifier for the server
     * @param accessoryId The unique identifier for the accessory
     * @param serviceId The unique identifier for the service
     * @throws IllegalArgumentException if any of the IDs are null or empty
     */
    public HomekitServiceUIDImpl(String serverId, long accessoryId, long serviceId) {
        super(SERVICE_PREFIX,
                HOMEKIT_PREFIX + ":" + SERVICE_PREFIX + ":" + serverId + ":" + accessoryId + ":" + serviceId);
    }

    /**
     * Creates a new service UID from a string key.
     *
     * <p>
     * This constructor parses an existing UID string into a service identifier.
     * It is used when reconstructing a UID from its string representation.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Validates the input string format</li>
     * <li>Extracts individual components</li>
     * <li>Initializes internal fields</li>
     * </ul>
     *
     * @param uid The string representation of the UID
     * @throws IllegalArgumentException if the UID format is invalid
     */
    public HomekitServiceUIDImpl(String uid) {
        super(SERVICE_PREFIX, uid);
        String[] segments = uid.split(":");
        if (segments.length != 5 || !HOMEKIT_PREFIX.equals(segments[0]) || !SERVICE_PREFIX.equals(segments[1])) {
            throw new IllegalArgumentException("Invalid HomeKit service UID format: " + uid);
        }
    }

    /**
     * Gets the UID as a string.
     *
     * <p>
     * The string representation follows the format
     * {@code homekit:service:{serverId}:{accessoryId}:{serviceId}}.
     * This format ensures consistent identification across the system.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Uses super.toString() for consistent formatting</li>
     * <li>Maintains the standard UID structure</li>
     * <li>Preserves all identifier components</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @return The UID string in the format
     *         {@code homekit:service:{serverId}:{accessoryId}:{serviceId}}
     */
    @Override
    public String toString() {
        String result = super.toString();
        logger.trace("{}Getting UID string: {}", LOG_UID, result);
        return result;
    }

    /**
     * Gets the instance ID of this service.
     *
     * <p>
     * The instance ID is a unique identifier used to distinguish between
     * multiple services of the same type within an accessory.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Returns the internal instance ID field</li>
     * <li>Used for service differentiation</li>
     * <li>Supports multiple instances of the same type</li>
     * </ul>
     *
     * @return The service instance ID
     */
    @Override
    public long getInstanceId() {
        return Long.parseLong(getSegment(4));
    }

    /**
     * Gets the minimum number of segments required for a valid UID.
     *
     * <p>
     * A valid service UID must have at least 5 segments:
     * </p>
     * <ol>
     * <li>The namespace prefix ("homekit")</li>
     * <li>The type identifier ("service")</li>
     * <li>The server ID</li>
     * <li>The accessory ID</li>
     * <li>The service ID</li>
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
     * @return The minimum number of segments (5) for a valid service UID
     */
    @Override
    protected int getMinimalNumberOfSegments() {
        return 5;
    }

    /**
     * Gets this UID instance.
     *
     * <p>
     * This method provides access to the UID instance itself, maintaining
     * consistency with the {@link HomekitServiceUID} interface.
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
    public HomekitServiceUID getUID() {
        return this;
    }
}
