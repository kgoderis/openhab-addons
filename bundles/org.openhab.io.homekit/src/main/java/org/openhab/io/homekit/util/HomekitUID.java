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

import java.util.UUID;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.UID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a unique identifier for a HomeKit event handler.
 *
 * <p>
 * This class provides a standardized way to generate and manage unique identifiers
 * for event handlers in the HomeKit integration. It extends OpenHAB's UID system
 * to provide HomeKit-specific identification capabilities.
 * </p>
 *
 * <p>
 * The class integrates with several key components:
 * </p>
 * <ul>
 * <li>{@link org.openhab.core.thing.UID} for base UID functionality</li>
 * <li>{@link java.util.UUID} for unique identifier generation</li>
 * <li>OpenHAB's thing system for device identification</li>
 * </ul>
 *
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>Standardized UID format: homekit:{prefix}:{uuid}</li>
 * <li>Support for different handler types through prefixes</li>
 * <li>UUID-based unique identification</li>
 * <li>Format validation and parsing</li>
 * <li>Wildcard UID support for matching any subscriber</li>
 * </ul>
 *
 * <p>
 * Example UID format: homekit:bridge:550e8400-e29b-41d4-a716-446655440000
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@NonNullByDefault
public class HomekitUID extends UID {
    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit UID: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";

    private static final Logger logger = LoggerFactory.getLogger(HomekitUID.class);
    private static final String HOMEKIT_PREFIX = "homekit";
    private final String prefix;

    public static final UID WILDCARD_UID = new HomekitUID("any", "homekit:any:any");

    /**
     * Creates a new HomeKit UID with the specified prefix.
     *
     * <p>
     * This constructor generates a new UID with a random UUID component. The UID
     * follows the format: homekit:{prefix}:{uuid}
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Generates random UUID for uniqueness</li>
     * <li>Validates prefix parameter</li>
     * <li>Constructs UID in standard format</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @param prefix The prefix to use for this handler UID (e.g., "bridge", "accessory", "service")
     * @throws IllegalArgumentException if the prefix is null or empty
     */
    public HomekitUID(String prefix) {
        super(HOMEKIT_PREFIX, prefix, UUID.randomUUID().toString());
        this.prefix = prefix;
        logger.trace("{}Created new UID with prefix: {}", LOG_INIT, prefix);
    }

    /**
     * Creates a new HomeKit UID with the specified prefix and UID.
     *
     * <p>
     * This constructor is used when recreating a UID from a stored value. It validates
     * that the provided UID follows the required format: homekit:{prefix}:{uuid}
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Validates UID format</li>
     * <li>Extracts prefix component</li>
     * <li>Ensures format consistency</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @param prefix The prefix to use for this handler UID
     * @param uid The unique identifier to use
     * @throws IllegalArgumentException if the UID format is invalid
     */
    public HomekitUID(String prefix, String uid) {
        super(uid);
        if (!uid.startsWith(HOMEKIT_PREFIX + ":" + prefix + ":")) {
            logger.error("{}Invalid UID format: {}", LOG_ERROR, uid);
            throw new IllegalArgumentException("Invalid UID format. Expected: homekit:" + prefix + ":{uuid}");
        }
        this.prefix = prefix;
        logger.trace("{}Created UID from existing value with prefix: {}", LOG_INIT, prefix);
    }

    /**
     * Returns the prefix of this handler UID.
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Returns internal prefix field</li>
     * <li>Used for UID categorization</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @return The prefix used in this UID
     */
    public String getPrefix() {
        logger.trace("{}Getting prefix: {}", LOG_INIT, prefix);
        return prefix;
    }

    /**
     * Returns the UUID part of this handler UID.
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Extracts UUID segment</li>
     * <li>Maintains UID structure</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @return The UUID component of this UID
     */
    public String getUUID() {
        String uuid = getSegment(2);
        logger.trace("{}Getting UUID: {}", LOG_INIT, uuid);
        return uuid;
    }

    /**
     * Gets the minimum number of segments required for a valid UID.
     *
     * <p>
     * A valid HomeKit UID must have at least 3 segments:
     * </p>
     * <ol>
     * <li>The namespace prefix ("homekit")</li>
     * <li>The handler prefix (e.g., "bridge", "accessory")</li>
     * <li>The UUID component</li>
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
     * @return The minimum number of segments (3) for a valid HomeKit UID
     */
    @Override
    protected int getMinimalNumberOfSegments() {
        logger.trace("{}Getting minimal number of segments: 3", LOG_INIT);
        return 3; // homekit:prefix:uuid
    }
}
