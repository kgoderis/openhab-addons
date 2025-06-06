/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.io.homekit.bridge;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.factory.HomekitServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps an Item with data derived from supported tags defined.
 * This class represents an openHAB item that has been tagged for Homekit
 * integration.
 * It manages the mapping between openHAB items and their corresponding Homekit
 * accessories or characteristics.
 *
 * The class integrates with:
 * - {@link org.openhab.core.items.ItemRegistry} for item management
 * - {@link org.openhab.core.items.MetadataRegistry} for metadata handling
 * - {@link org.openhab.io.homekit.api.factory.HomekitAccessoryFactory} for
 * accessory creation
 * - {@link org.openhab.io.homekit.api.factory.HomekitServiceFactory} for
 * service creation
 * - {@link org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory} for
 * characteristic creation
 *
 * <p>
 * The class handles two main types of items:
 * <ul>
 * <li>HomekitAccessory items: Items that represent complete Homekit accessories
 * (e.g., lights, switches)</li>
 * <li>HomekitCharacteristic items: Items that represent specific
 * characteristics of an accessory (e.g., brightness,
 * color)</li>
 * </ul>
 * </p>
 *
 * <p>
 * Items can be tagged in two ways:
 * <ul>
 * <li>Direct tagging: The item itself is tagged as a Homekit accessory</li>
 * <li>Group membership: The item is a member of a group that is tagged as a
 * Homekit accessory</li>
 * </ul>
 * </p>
 *
 * Key Features:
 * - Tag-based item identification
 * - Group hierarchy management
 * - Service and characteristic mapping
 * - Metadata-based configuration
 * - Unique ID generation
 * - Configuration validation
 *
 * Usage Patterns:
 * - Item tagging: Use metadata or direct tags to mark items for HomeKit
 * integration
 * - Group organization: Create accessory groups to organize related items
 * - Service mapping: Map items to specific HomeKit services
 * - Characteristic mapping: Map items to specific HomeKit characteristics
 *
 * Configuration Validation:
 * - Prevents nested accessory groups
 * - Ensures items belong to at most one accessory group
 * - Validates service and characteristic tags
 * - Enforces proper group hierarchy
 *
 * @author Karel Goderis - Initial contribution
 */
public class HomekitTaggedItem {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit TaggedItem: ";
    private static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    private static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    // 1. Constants and static fields
    private static final Map<Integer, String> CREATED_ACCESSORY_IDS = new ConcurrentHashMap<>();

    // 2. Instance fields
    private final Item item;
    private final ItemRegistry itemRegistry;
    private final MetadataRegistry metadataRegistry;
    private final Collection<@NonNull String> homekitTags;
    private final int id;
    private String serviceTag;
    private String characteristicTag;
    private GroupItem parentGroupItem;
    private final Logger logger = LoggerFactory.getLogger(HomekitTaggedItem.class);
    private final HomekitServiceFactory serviceFactory;
    private final HomekitCharacteristicFactory characteristicFactory;
    private final boolean useMetadataTags;

    // 3. Inner classes
    class BadItemConfigurationException extends Exception {
        private static final long serialVersionUID = 2199765638404197193L;

        public BadItemConfigurationException(String reason) {
            super(reason);
        }
    }

    /**
     * Constructs a new HomekitTaggedItem instance for the given item.
     * Determines the item's role in Homekit integration based on its tags and group
     * membership.
     *
     * @param item The openHAB item to wrap
     * @param itemRegistry The item registry to use for group lookups
     * @param metadataRegistry The metadata registry to use for metadata
     *            lookups
     * @param accessoryFactory The factory for creating Homekit accessories
     * @param serviceFactory The factory for creating Homekit services
     * @param characteristicFactory The factory for creating Homekit characteristics
     * @param useMetadataTags Whether to use metadata-based tags (true) or
     *            real tags (false)
     * @throws BadItemConfigurationException if the item's configuration is invalid
     */
    public HomekitTaggedItem(Item item, ItemRegistry itemRegistry, MetadataRegistry metadataRegistry,
            HomekitServiceFactory serviceFactory, HomekitCharacteristicFactory characteristicFactory) {
        this.item = item;
        this.metadataRegistry = metadataRegistry;
        this.itemRegistry = itemRegistry;
        this.useMetadataTags = true;
        this.homekitTags = getHomekitTags(item);
        this.serviceFactory = serviceFactory;
        this.characteristicFactory = characteristicFactory;

        logger.debug("{}Initializing tagged item: {}", LOG_PREFIX, item.getName());

        try {
            serviceTag = determineServiceTag();
            characteristicTag = determineCharacteristicTag();
            if (serviceTag != null && characteristicTag != null) {
                throw new BadItemConfigurationException(
                        "Items cannot be tagged as both a characteristic and an accessory type");
            }
            List<GroupItem> matchingGroupItems = findMyAccessoryGroupsInternal();

            switch (matchingGroupItems.size()) {
                case 0 -> { // Does not belong to a accessory group
                    if (characteristicTag != null) {
                        throw new BadItemConfigurationException(
                                "Item is tagged as a characteristic, but does not belong to a root accessory group");
                    }

                    parentGroupItem = null;
                    logger.debug("{}Item {} is not part of any accessory group", LOG_CONFIG, item.getName());
                }
                case 1 -> { // Belongs to exactly one accessory group
                    if (item instanceof GroupItem) {
                        throw new BadItemConfigurationException("Nested HomekitAccessory Groups are not supported");
                    }

                    parentGroupItem = matchingGroupItems.get(0);
                    logger.debug("{}Item {} belongs to accessory group {}", LOG_CONFIG, item.getName(),
                            parentGroupItem.getName());
                }
                default -> { // Belongs to more than one accessory group
                    throw new BadItemConfigurationException(
                            "Item belongs to multiple Groups which are tagged as Homekit devices.");
                }
            }

        } catch (BadItemConfigurationException e) {
            logger.warn("{}Item {} was misconfigured: {}. Excluding item from homekit.", LOG_WARN, item.getName(),
                    e.getMessage());
            serviceTag = null;
            characteristicTag = null;
            parentGroupItem = null;
        }
        if (serviceTag != null) {
            this.id = calculateId(item);
            logger.debug("{}Assigned ID {} to item {} with service tag {}", LOG_CONFIG, id, item.getName(), serviceTag);
        } else {
            this.id = 0;
        }
    }

    /**
     * Determines the Homekit service type from the item's tags.
     *
     * @return The Homekit service type if found, null otherwise
     */
    @SuppressWarnings("null")
    private String determineServiceTag() {
        if (!homekitTags.isEmpty()) {
            String firstTag = homekitTags.iterator().next();
            if (serviceFactory.supportsTag(firstTag)) {
                logger.debug("{}Found service tag {} for item {}", LOG_CONFIG, firstTag, item.getName());
                return firstTag;
            }
        }
        return null;
    }

    /**
     * Determines the Homekit characteristic type from the item's tags.
     *
     * @return The Homekit characteristic type if found, null otherwise
     */
    @SuppressWarnings("null")
    private String determineCharacteristicTag() {
        if (!homekitTags.isEmpty()) {
            String firstTag = homekitTags.iterator().next();
            if (characteristicFactory.supportsTag(firstTag)) {
                logger.debug("{}Found characteristic tag {} for item {}", LOG_CONFIG, firstTag, item.getName());
                return firstTag;
            }
        }
        return null;
    }

    /**
     * Checks if the item is tagged for Homekit integration.
     * An item is considered tagged if it has either a service type or a
     * characteristic type.
     *
     * @return true if the item is tagged for Homekit integration, false otherwise
     */
    public boolean isTagged() {
        boolean tagged = (serviceTag != null && id != 0) || characteristicTag != null;
        logger.debug("{}Item {} is {}tagged for HomeKit", LOG_CONFIG, item.getName(), tagged ? "" : "not ");
        return tagged;
    }

    /**
     * Checks if the item is a group item that represents a Homekit accessory.
     *
     * @return true if the item is a group and represents a Homekit accessory, false
     *         otherwise
     */
    public boolean isGroup() {
        boolean isGroup = (isAccessory() && (this.item instanceof GroupItem));
        logger.debug("{}Item {} is {}a group", LOG_CONFIG, item.getName(), isGroup ? "" : "not ");
        return isGroup;
    }

    /**
     * Gets the Homekit service type associated with this item.
     * This represents the type of Homekit accessory (e.g., Light, Switch,
     * Thermostat).
     *
     * @return The Homekit service type, or null if not applicable
     */
    public String getServiceTag() {
        logger.debug("{}Getting service tag for item {}: {}", LOG_CONFIG, item.getName(), serviceTag);
        return serviceTag;
    }

    /**
     * Gets the Homekit characteristic type associated with this item.
     * This represents a specific property of a Homekit accessory (e.g., On,
     * Brightness, Temperature).
     *
     * @return The Homekit characteristic type, or null if not applicable
     */
    public String getCharacteristicTag() {
        logger.debug("{}Getting characteristic tag for item {}: {}", LOG_CONFIG, item.getName(), characteristicTag);
        return characteristicTag;
    }

    /**
     * Checks if this item represents a complete Homekit accessory.
     * An item is considered an accessory if it has a service type defined.
     * HomekitAccessory items must belong to a root accessory group.
     *
     * @return true if the item represents a Homekit accessory, false otherwise
     */
    public boolean isAccessory() {
        boolean isAccessory = serviceTag != null && id != 0;
        logger.debug("{}Item {} is {}an accessory", LOG_CONFIG, item.getName(), isAccessory ? "" : "not ");
        return isAccessory;
    }

    /**
     * Checks if this item represents a Homekit characteristic.
     * An item is considered a characteristic if it has a characteristic type
     * defined.
     * HomekitCharacteristic items must belong to a root accessory group.
     *
     * @return true if the item represents a Homekit characteristic, false otherwise
     */
    public boolean isCharacteristic() {
        boolean isCharacteristic = characteristicTag != null;
        logger.debug("{}Item {} is {}a characteristic", LOG_CONFIG, item.getName(), isCharacteristic ? "" : "not ");
        return isCharacteristic;
    }

    /**
     * Gets the underlying openHAB item.
     *
     * @return The wrapped openHAB item
     */
    public Item getItem() {
        return item;
    }

    /**
     * Gets the unique identifier for this Homekit accessory.
     * The ID is calculated based on the item's name and is used to identify the
     * accessory in Homekit.
     *
     * @return The unique identifier for the accessory
     */
    public int getId() {
        logger.debug("{}Getting ID for item {}: {}", LOG_CONFIG, item.getName(), id);
        return id;
    }

    /**
     * Gets the name of the underlying openHAB item.
     *
     * @return The name of the item
     */
    public String getName() {
        return item.getName();
    }

    /**
     * Gets the root device group item to which this item belongs.
     * This is the group that represents the complete Homekit accessory.
     *
     * @return The root device group item, or null if the item is not in a group
     */
    public GroupItem getRootDeviceGroupItem() {
        logger.debug("{}Getting root device group for item {}: {}", LOG_CONFIG, item.getName(),
                parentGroupItem != null ? parentGroupItem.getName() : "none");
        return parentGroupItem;
    }

    /**
     * Checks if this item belongs to a Homekit accessory group.
     * HomekitCharacteristic items must belong to an accessory group.
     *
     * @return true if the item belongs to a Homekit accessory group, false
     *         otherwise
     */
    public boolean isMemberOfAccessoryGroup() {
        boolean isMember = parentGroupItem != null;
        logger.debug("{}Item {} is {}a member of an accessory group", LOG_CONFIG, item.getName(),
                isMember ? "" : "not ");
        return isMember;
    }

    /**
     * Finds all accessory groups that contain the given item.
     * An accessory group is a group item that is tagged as a Homekit accessory.
     *
     * @param item The item to find groups for
     * @param itemRegistry The item registry to use for group lookups
     * @return A list of group items that are tagged as Homekit accessories
     */
    public List<GroupItem> findMyAccessoryGroups() {
        List<GroupItem> groups = findMyAccessoryGroupsInternal();
        logger.debug("{}Found {} accessory groups for item {}", LOG_CONFIG, groups.size(), item.getName());
        return groups;
    }

    public Collection<@NonNull String> getHomekitTags() {
        logger.debug("{}Getting HomeKit tags for item {}: {}", LOG_CONFIG, item.getName(), homekitTags);
        return homekitTags;
    }

    /**
     * Gets the Homekit tags for the given item based on the configured tag source.
     *
     * @param item The item to get tags for
     * @return Collection of Homekit tags
     */
    private Collection<@NonNull String> getHomekitTags(Item item) {
        Collection<@NonNull String> tags = useMetadataTags ? getHomekitTagsFromMetaRegistry(item)
                : getHomekitTagsFromItem(item);
        logger.debug("{}Retrieved {} HomeKit tags for item {}", LOG_CONFIG, tags.size(), item.getName());
        return tags;
    }

    /**
     * Gets the Homekit tags directly from the item's tags.
     *
     * @param item The item to get tags for
     * @return Collection of Homekit tags
     */
    private Collection<@NonNull String> getHomekitTagsFromItem(Item item) {
        return item.getTags();
    }

    private Collection<@NonNull String> getHomekitTagsFromMetaRegistry(Item item) {
        MetadataKey key = new MetadataKey("homekit", item.getName());
        Metadata metadata = metadataRegistry.get(key);
        if (metadata != null) {
            logger.debug("{}Found metadata for item {}: {}", LOG_CONFIG, item.getName(), metadata.getValue());
            return Collections.singletonList(metadata.getValue());
        }
        return Collections.emptyList();
    }

    private List<GroupItem> findMyAccessoryGroupsInternal() {
        return item.getGroupNames().stream().map(name -> itemRegistry.get(name))
                .filter(item -> item instanceof GroupItem).map(item -> (GroupItem) item).filter(group -> {
                    Collection<@NonNull String> groupTags = getHomekitTags(group);
                    boolean isAccessory = !groupTags.isEmpty()
                            && serviceFactory.supportsTag(groupTags.iterator().next());
                    logger.debug("{}Group {} is {}an accessory group", LOG_CONFIG, group.getName(),
                            isAccessory ? "" : "not ");
                    return isAccessory;
                }).collect(Collectors.toList());
    }

    /**
     * Calculates a unique identifier for the Homekit accessory.
     * The ID is based on the item's name and is guaranteed to be unique within the
     * system.
     * IDs 0 and 1 are reserved for special purposes.
     *
     * @param item The item to calculate the ID for
     * @return A unique identifier for the accessory
     */
    private int calculateId(Item item) {
        int id = new HashCodeBuilder().append(item.getName()).toHashCode();
        if (CREATED_ACCESSORY_IDS.containsKey(id)) {
            logger.warn("{}ID collision detected for item {} with existing item {}", LOG_WARN, item.getName(),
                    CREATED_ACCESSORY_IDS.get(id));
        }
        CREATED_ACCESSORY_IDS.put(id, item.getName());
        logger.debug("{}Calculated ID {} for item {}", LOG_CONFIG, id, item.getName());
        return id;
    }
}
