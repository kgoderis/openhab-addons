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

package org.openhab.io.homekit.core.server;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.uid.HomekitAccessoryServerUID;
import org.openhab.io.homekit.util.HomekitUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a unique identifier for a HomeKit accessory server.
 *
 * <p>
 * This class provides a structured way to identify HomeKit accessory servers
 * within the system.
 * The UID follows a specific format: {@code homekit:server:{Id}} where:
 * </p>
 * <ul>
 * <li>{@code homekit} is the namespace prefix</li>
 * <li>{@code server} indicates this is a server identifier</li>
 * <li>{@code Id} is the unique identifier for the server</li>
 * </ul>
 *
 * <p>
 * Key responsibilities:
 * </p>
 * <ul>
 * <li>Creating and parsing server UIDs</li>
 * <li>Validating UID format and structure</li>
 * <li>Extracting server-specific information from UIDs</li>
 * <li>Ensuring unique identification across the system</li>
 * </ul>
 *
 * <p>
 * The class integrates with:
 * </p>
 * <ul>
 * <li>{@link org.openhab.core.common.registry.Identifiable} for UID
 * management</li>
 * <li>{@link org.openhab.io.homekit.api.server.HomekitAccessoryServer} for
 * server identification</li>
 * <li>OpenHAB's UID system for consistent identification</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@NonNullByDefault
public class HomekitAccessoryServerUIDImpl extends HomekitUID implements HomekitAccessoryServerUID {
    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit ServerUID: ";
    protected static final String LOG_UID = LOG_PREFIX + "UID - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";

    private static final Logger logger = LoggerFactory.getLogger(HomekitAccessoryServerUIDImpl.class);
    private static final String SERVER_PREFIX = "server";
    private static final String HOMEKIT_PREFIX = "homekit";

    /**
     * Creates a new server UID with default values.
     *
     * <p>
     * This constructor is package-private and intended for use by reflection only.
     * Not meant for normal instantiation.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Sets up basic UID structure</li>
     * <li>Initializes with empty values</li>
     * <li>Used internally by reflection</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     */
    HomekitAccessoryServerUIDImpl() {
        super(SERVER_PREFIX);
        logger.trace("{}Created default server UID instance", LOG_UID);
    }

    /**
     * Creates a new server UID with the specified identifier.
     *
     * <p>
     * This constructor builds a complete server UID instance with the required
     * identifier. The UID is used to uniquely identify a HomeKit
     * accessory server within the system.
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
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @param uid The UID string. If it contains ":", it should be a fully qualified UID in format
     *            "homekit:server:{id}".
     *            If it doesn't contain ":", it should be just the server ID and the full UID will be constructed.
     * @throws IllegalArgumentException if the UID format is invalid or the identifier is empty
     */
    public HomekitAccessoryServerUIDImpl(String uid) {
        super(SERVER_PREFIX, uid.contains(":") ? uid : HOMEKIT_PREFIX + ":" + SERVER_PREFIX + ":" + uid);
        String[] segments = uid.contains(":") ? uid.split(":")
                : (HOMEKIT_PREFIX + ":" + SERVER_PREFIX + ":" + uid).split(":");
        if (segments.length != 3 || !HOMEKIT_PREFIX.equals(segments[0]) || !SERVER_PREFIX.equals(segments[1])) {
            logger.error("{}Invalid UID format: {}", LOG_ERROR, uid);
            throw new IllegalArgumentException("Invalid HomeKit server UID format: " + uid);
        }
        String id = segments[2];
        if (id.isEmpty()) {
            logger.error("{}identifier cannot be empty", LOG_ERROR);
            throw new IllegalArgumentException("identifier cannot be empty");
        }
        logger.trace("{}Created server UID with identifier: {}", LOG_UID, id);
    }

    /**
     * Gets the UID as a string.
     *
     * <p>
     * The string representation follows the format
     * {@code homekit:server:{Id}}.
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
     * @return The UID string in the format {@code homekit:server:{Id}}
     */
    @Override
    public String toString() {
        String result = super.toString();
        logger.trace("{}Getting UID string: {}", LOG_UID, result);
        return result;
    }

    /**
     * Gets the identifier for this server.
     *
     * <p>
     * The identifier is a unique identifier used during the HomeKit pairing
     * process.
     * It helps maintain the connection between the server and its paired devices.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Returns the internal identifier field</li>
     * <li>Used for server connection management</li>
     * <li>Supports device pairing</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @return Theidentifier for this server
     */
    @Override
    public String getId() {
        String id = getSegment(2);
        logger.trace("{}Getting Id: {}", LOG_UID, id);
        return id;
    }

    /**
     * Gets the minimum number of segments required for a valid UID.
     *
     * <p>
     * A valid server UID must have at least 3 segments:
     * </p>
     * <ol>
     * <li>The namespace prefix ("homekit")</li>
     * <li>The type identifier ("server")</li>
     * <li>The identifier</li>
     * </ol>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Enforces UID structure validation</li>
     * <li>Ensures complete identification</li>
     * <li>Supports UID parsing</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @return The minimum number of segments (3) for a valid server UID
     */
    @Override
    protected int getMinimalNumberOfSegments() {
        logger.trace("{}Getting minimal number of segments: 3", LOG_UID);
        return 3;
    }

    /**
     * Gets this UID instance.
     *
     * <p>
     * This method provides access to the UID instance itself, maintaining
     * consistency with the {@link HomekitAccessoryServerUID} interface.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Returns this instance</li>
     * <li>Supports interface compliance</li>
     * <li>Enables UID access</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @return This UID instance
     */
    @Override
    public HomekitAccessoryServerUID getUID() {
        logger.trace("{}Getting UID instance", LOG_UID);
        return this;
    }
}
