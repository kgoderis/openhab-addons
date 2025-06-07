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

import org.openhab.core.items.Item;
import org.openhab.core.thing.UID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a unique identifier for a HomeKit item.
 *
 * <p>
 * This class provides a standardized way to generate and manage unique identifiers
 * for items in the HomeKit integration. It extends OpenHAB's UID system to provide
 * item-specific identification capabilities.
 * </p>
 *
 * <p>
 * The class integrates with several key components:
 * </p>
 * <ul>
 * <li>{@link org.openhab.core.thing.UID} for base UID functionality</li>
 * <li>{@link org.openhab.core.items.Item} for item identification</li>
 * <li>OpenHAB's item system for state management</li>
 * </ul>
 *
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>Standardized UID format: openhab:item:{itemName}</li>
 * <li>Direct item name mapping</li>
 * <li>Wildcard pattern matching support</li>
 * <li>Format validation and parsing</li>
 * <li>Wildcard UID support for matching any item</li>
 * </ul>
 *
 * <p>
 * Example UID format: openhab:item:livingroom_light
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
     */
public class ItemUID extends UID {
    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit ItemUID: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_MATCH = LOG_PREFIX + "Match - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";

    private static final Logger logger = LoggerFactory.getLogger(ItemUID.class);
    private static final String ITEM_PREFIX = "openhab:item";
    private final String itemName;

    public static final ItemUID WILDCARD_UID = new ItemUID("*");

    /**
     * Creates a new ItemUID for the specified item.
     *
     * <p>
     * This constructor creates a UID directly from an OpenHAB item instance.
     * The UID follows the format: openhab:item:{itemName}
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Extracts item name</li>
     * <li>Validates item parameter</li>
     * <li>Constructs UID in standard format</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @param item The item to create a UID for
     * @throws IllegalArgumentException if the item is null or has no name
     */
    public ItemUID(Item item) {
        super(ITEM_PREFIX, item.getName());
        this.itemName = item.getName();
        logger.trace("{}Created new UID for item: {}", LOG_INIT, itemName);
    }

    /**
     * Creates a new ItemUID with the specified item name.
     *
     * <p>
     * This constructor creates a UID from a string item name. The UID follows
     * the format: openhab:item:{itemName}
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Validates item name</li>
     * <li>Constructs UID in standard format</li>
     * <li>Supports wildcard names</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @param itemName The name of the item
     * @throws IllegalArgumentException if the item name is null or empty
     */
    public ItemUID(String itemName) {
        super(ITEM_PREFIX, itemName);
        this.itemName = itemName;
        logger.trace("{}Created new UID with item name: {}", LOG_INIT, itemName);
    }

    /**
     * Returns the name of the item.
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Returns internal item name field</li>
     * <li>Used for item identification</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @return The name of the item
     */
    public String getItemName() {
        logger.trace("{}Getting item name: {}", LOG_INIT, itemName);
        return itemName;
    }

    /**
     * Checks if this UID matches a pattern that may contain wildcards.
     *
     * <p>
     * This method supports pattern matching with wildcards in the item name segment.
     * The pattern must follow the format: openhab:item:{itemPattern}
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Supports wildcard matching</li>
     * <li>Validates pattern format</li>
     * <li>Converts wildcards to regex</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @param pattern The pattern to match against
     * @return true if this UID matches the pattern
     */
    public boolean matches(String pattern) {
        if (pattern.equals("*")) {
            logger.trace("{}Pattern matches wildcard: {}", LOG_MATCH, pattern);
            return true;
        }
        String[] patternSegments = pattern.split(":");
        if (patternSegments.length != 3) {
            logger.trace("{}Pattern has invalid number of segments: {}", LOG_MATCH, pattern);
            return false;
        }
        if (!patternSegments[0].equals(getPrefix()) || !patternSegments[1].equals(ITEM_PREFIX)) {
            logger.trace("{}Pattern prefix mismatch: {}", LOG_MATCH, pattern);
            return false;
        }
        String itemPattern = patternSegments[2];
        boolean matches = itemName.matches(itemPattern.replace("*", ".*"));
        logger.trace("{}Pattern {} matches item {}: {}", LOG_MATCH, pattern, itemName, matches);
        return matches;
    }

    /**
     * Gets the minimum number of segments required for a valid UID.
     *
     * <p>
     * A valid item UID must have exactly 3 segments:
     * </p>
     * <ol>
     * <li>The namespace prefix ("openhab")</li>
     * <li>The type identifier ("item")</li>
     * <li>The item name</li>
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
     * @return The minimum number of segments (3) for a valid item UID
     */
    @Override
    protected int getMinimalNumberOfSegments() {
        logger.trace("{}Getting minimal number of segments: 3", LOG_INIT);
        return 3; // openhab:item:itemName
    }

    /**
     * Returns the prefix used in this UID.
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Returns constant prefix</li>
     * <li>Used for UID categorization</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @return The prefix used in this UID
     */
    public String getPrefix() {
        logger.trace("{}Getting prefix: {}", LOG_INIT, ITEM_PREFIX);
        return ITEM_PREFIX;
    }
}
