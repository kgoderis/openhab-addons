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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.io.homekit.api.factory.HomekitAccessoryFactory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.factory.HomekitServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps an Item with data derived from supported tags defined.
 * This class represents an openHAB item that has been tagged for Homekit integration.
 * It manages the mapping between openHAB items and their corresponding Homekit accessories or characteristics.
 *
 * <p>
 * The class handles two main types of items:
 * <ul>
 * <li>HomekitAccessory items: Items that represent complete Homekit accessories (e.g., lights, switches)</li>
 * <li>HomekitCharacteristic items: Items that represent specific characteristics of an accessory (e.g., brightness,
 * color)</li>
 * </ul>
 * </p>
 *
 * <p>
 * Items can be tagged in two ways:
 * <ul>
 * <li>Direct tagging: The item itself is tagged as a Homekit accessory</li>
 * <li>Group membership: The item is a member of a group that is tagged as a Homekit accessory</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 */
public class HomekitTaggedItem {
    // 1. Constants and static fields
    private static final Map<Integer, String> CREATED_ACCESSORY_IDS = new ConcurrentHashMap<>();
    protected static final String LOG_PREFIX = "Homekit TaggedItem: ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    // 2. Instance fields
    private final Item item;
    private final ItemRegistry itemRegistry;
    private final MetadataRegistry metadataRegistry;
    private final Collection<String> homekitTags;
    private final int id;
    private String serviceTag;
    private String characteristicTag;
    private GroupItem parentGroupItem;
    private final Logger logger = LoggerFactory.getLogger(HomekitTaggedItem.class);
    private final HomekitServiceFactory serviceFactory;
    private final HomekitCharacteristicFactory characteristicFactory;
    private final HomekitAccessoryFactory accessoryFactory;
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
     * Determines the item's role in Homekit integration based on its tags and group membership.
     *
     * @param item The openHAB item to wrap
     * @param itemRegistry The item registry to use for group lookups
     * @param metadataRegistry The metadata registry to use for metadata lookups
     * @param accessoryFactory The factory for creating Homekit accessories
     * @param serviceFactory The factory for creating Homekit services
     * @param characteristicFactory The factory for creating Homekit characteristics
     * @param useMetadataTags Whether to use metadata-based tags (true) or real tags (false)
     * @throws BadItemConfigurationException if the item's configuration is invalid
     */
    public HomekitTaggedItem(Item item, ItemRegistry itemRegistry, MetadataRegistry metadataRegistry,
            HomekitAccessoryFactory accessoryFactory, HomekitServiceFactory serviceFactory,
            HomekitCharacteristicFactory characteristicFactory) {
        this.item = item;
        this.metadataRegistry = metadataRegistry;
        this.itemRegistry = itemRegistry;
        this.useMetadataTags = true;
        this.homekitTags = getHomekitTags(item);
        this.accessoryFactory = accessoryFactory;
        this.serviceFactory = serviceFactory;
        this.characteristicFactory = characteristicFactory;

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
                }
                case 1 -> { // Belongs to exactly one accessory group
                    if (item instanceof GroupItem) {
                        throw new BadItemConfigurationException("Nested HomekitAccessory Groups are not supported");
                    }

                    parentGroupItem = matchingGroupItems.get(0);
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
        } else {
            this.id = 0;
        }
    }

    /**
     * Determines the Homekit service type from the item's tags.
     *
     * @return The Homekit service type if found, null otherwise
     */
    private String determineServiceTag() {
        if (!homekitTags.isEmpty()) {
            String firstTag = homekitTags.iterator().next();
            if (serviceFactory.supportsTag(firstTag)) {
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
    private String determineCharacteristicTag() {
        if (!homekitTags.isEmpty()) {
            String firstTag = homekitTags.iterator().next();
            if (characteristicFactory.supportsTag(firstTag)) {
                return firstTag;
            }
        }
        return null;
    }

    /**
     * Checks if the item is tagged for Homekit integration.
     * An item is considered tagged if it has either a service type or a characteristic type.
     *
     * @return true if the item is tagged for Homekit integration, false otherwise
     */
    public boolean isTagged() {
        return (serviceTag != null && id != 0) || characteristicTag != null;
    }

    /**
     * Checks if the item is a group item that represents a Homekit accessory.
     *
     * @return true if the item is a group and represents a Homekit accessory, false otherwise
     */
    public boolean isGroup() {
        return (isAccessory() && (this.item instanceof GroupItem));
    }

    /**
     * Gets the Homekit service type associated with this item.
     * This represents the type of Homekit accessory (e.g., Light, Switch, Thermostat).
     *
     * @return The Homekit service type, or null if not applicable
     */
    public String getServiceTag() {
        return serviceTag;
    }

    /**
     * Gets the Homekit characteristic type associated with this item.
     * This represents a specific property of a Homekit accessory (e.g., On, Brightness, Temperature).
     *
     * @return The Homekit characteristic type, or null if not applicable
     */
    public String getCharacteristicTag() {
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
        return serviceTag != null;
    }

    /**
     * Checks if this item represents a Homekit characteristic.
     * An item is considered a characteristic if it has a characteristic type defined.
     * HomekitCharacteristic items must belong to a root accessory group.
     *
     * @return true if the item represents a Homekit characteristic, false otherwise
     */
    public boolean isCharacteristic() {
        return characteristicTag != null;
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
     * The ID is calculated based on the item's name and is used to identify the accessory in Homekit.
     *
     * @return The unique identifier for the accessory
     */
    public int getId() {
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
        return parentGroupItem;
    }

    /**
     * Checks if this item belongs to a Homekit accessory group.
     * HomekitCharacteristic items must belong to an accessory group.
     *
     * @return true if the item belongs to a Homekit accessory group, false otherwise
     */
    public boolean isMemberOfAccessoryGroup() {
        return parentGroupItem != null;
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
        return item.getGroupNames().stream().flatMap(name -> {
            Item groupItem = itemRegistry.get(name);
            if ((groupItem != null) && (groupItem instanceof GroupItem)) {
                return Stream.of((GroupItem) groupItem);
            } else {
                return Stream.empty();
            }
        }).filter(groupItem -> {
            Collection<String> groupHomekitTags = getHomekitTags(groupItem);
            return groupHomekitTags.stream().anyMatch(tag -> serviceFactory.supportsTag(tag));
        }).collect(Collectors.toList());
    }

    public Collection<String> getHomekitTags() {
        return homekitTags;
    }

    /**
     * Gets the Homekit tags for the given item based on the configured tag source.
     *
     * @param item The item to get tags for
     * @return Collection of Homekit tags
     */
    private Collection<String> getHomekitTags(Item item) {
        return useMetadataTags ? getHomekitTagsFromMetaRegistry(item) : getHomekitTagsFromItem(item);
    }

    /**
     * Gets the Homekit tags directly from the item's tags.
     *
     * @param item The item to get tags for
     * @return Collection of Homekit tags
     */
    private Collection<String> getHomekitTagsFromItem(Item item) {
        return item.getTags().stream()
                .filter(tag -> serviceFactory.supportsTag(tag) || characteristicFactory.supportsTag(tag))
                .collect(Collectors.toList());
    }

    private Collection<String> getHomekitTagsFromMetaRegistry(Item item) {
        MetadataKey key = new MetadataKey("homekit", item.getName());
        Metadata metadata = metadataRegistry.get(key);
        return metadata != null ? Arrays.asList(metadata.getValue().split(",")) : Collections.emptyList();
    }

    private List<GroupItem> findMyAccessoryGroupsInternal() {
        return findMyAccessoryGroups();
    }

    /**
     * Calculates a unique identifier for the Homekit accessory.
     * The ID is based on the item's name and is guaranteed to be unique within the system.
     * IDs 0 and 1 are reserved for special purposes.
     *
     * @param item The item to calculate the ID for
     * @return A unique identifier for the accessory
     */
    private int calculateId(Item item) {
        int calculatedId = new HashCodeBuilder().append(item.getName()).hashCode();
        if (calculatedId < 0) {
            calculatedId += Integer.MAX_VALUE;
        }
        if (calculatedId < 2) {
            calculatedId = 2; // 0 and 1 are reserved
        }
        if (CREATED_ACCESSORY_IDS.containsKey(calculatedId)) {
            if (!CREATED_ACCESSORY_IDS.get(calculatedId).equals(item.getName())) {
                logger.warn(
                        "Could not create homekit accessory {} because its hash conflicts with {}. This is a 1:1,000,000 chance occurrence. Change one of the names and consider playing the lottery. See https://github.com/openhab/openhab2-addons/issues/257#issuecomment-125886562",
                        item.getName(), CREATED_ACCESSORY_IDS.get(calculatedId));
                return 0;
            }
        } else {
            CREATED_ACCESSORY_IDS.put(id, item.getName());
        }
        return calculatedId;
    }
}
