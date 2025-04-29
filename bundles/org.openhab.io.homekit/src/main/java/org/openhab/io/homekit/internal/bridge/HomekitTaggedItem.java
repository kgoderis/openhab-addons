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
package org.openhab.io.homekit.internal.bridge;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps an Item with data derived from supported tags defined.
 * This class represents an openHAB item that has been tagged for HomeKit integration.
 * It manages the mapping between openHAB items and their corresponding HomeKit accessories or characteristics.
 *
 * <p>
 * The class handles two main types of items:
 * <ul>
 * <li>Accessory items: Items that represent complete HomeKit accessories (e.g., lights, switches)</li>
 * <li>Characteristic items: Items that represent specific characteristics of an accessory (e.g., brightness,
 * color)</li>
 * </ul>
 * </p>
 *
 * <p>
 * Items can be tagged in two ways:
 * <ul>
 * <li>Direct tagging: The item itself is tagged as a HomeKit accessory</li>
 * <li>Group membership: The item is a member of a group that is tagged as a HomeKit accessory</li>
 * </ul>
 * </p>
 *
 * @author Andy Lintner - Initial contribution
 */
public class HomekitTaggedItem {
    /**
     * Exception thrown when an item's configuration is invalid for HomeKit integration.
     * This includes cases where items are incorrectly tagged or grouped.
     */
    class BadItemConfigurationException extends Exception {
        private static final long serialVersionUID = 2199765638404197193L;

        public BadItemConfigurationException(String reason) {
            super(reason);
        }
    }

    private static ServiceTracker<@NonNull HomekitFactory, @NonNull HomekitFactory> homekitFactoryTracker;
    private static final Map<Integer, String> CREATED_ACCESSORY_IDS = new ConcurrentHashMap<>();

    /**
     * The type of HomekitDevice we've decided this was. If the item is question is the member of a group which is a
     * HomekitDevice, then this is null.
     */
    private String serviceType;
    private String characteristicType;
    private final Item item;
    private Logger logger = LoggerFactory.getLogger(HomekitTaggedItem.class);
    private final int id;
    private GroupItem parentGroupItem;
    private final Collection<String> homekitTags;
    private final MetadataRegistry metadataRegistry;
    private final ItemRegistry itemRegistry;

    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "HomeKit TaggedItem: ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    /**
     * Constructs a new HomekitTaggedItem instance for the given item.
     * Determines the item's role in HomeKit integration based on its tags and group membership.
     *
     * @param item The openHAB item to wrap
     * @param itemRegistry The item registry to use for group lookups
     * @param metadataRegistry The metadata registry to use for metadata lookups
     * @throws BadItemConfigurationException if the item's configuration is invalid
     */
    public HomekitTaggedItem(Item item, ItemRegistry itemRegistry, MetadataRegistry metadataRegistry) {
        this.item = item;
        this.metadataRegistry = metadataRegistry;
        this.itemRegistry = itemRegistry;
        this.homekitTags = getHomekitTags(item);

        try {
            serviceType = getFactoryServiceType();
            characteristicType = getFactoryCharacteristicType();
            if (serviceType != null && characteristicType != null) {
                throw new BadItemConfigurationException(
                        "Items cannot be tagged as both a characteristic and an accessory type");
            }
            List<GroupItem> matchingGroupItems = findMyAccessoryGroupsInternal();

            switch (matchingGroupItems.size()) {
                case 0 -> { // Does not belong to a accessory group
                    if (characteristicType != null) {
                        throw new BadItemConfigurationException(
                                "Item is tagged as a characteristic, but does not belong to a root accessory group");
                    }

                    parentGroupItem = null;
                }
                case 1 -> { // Belongs to exactly one accessory group
                    if (item instanceof GroupItem) {
                        throw new BadItemConfigurationException("Nested Accessory Groups are not supported");
                    }

                    parentGroupItem = matchingGroupItems.get(0);
                }
                default -> { // Belongs to more than one accessory group
                    throw new BadItemConfigurationException(
                            "Item belongs to multiple Groups which are tagged as Homekit devices.");
                }
            }

        } catch (BadItemConfigurationException e) {
            logger.warn("{}Item {} was misconfigured: {}. Excluding item from homekit.", LOG_WARN, item.getName(), e.getMessage());
            serviceType = null;
            characteristicType = null;
            parentGroupItem = null;
        }
        if (serviceType != null) {
            this.id = calculateId(item);
        } else {
            this.id = 0;
        }
    }

    /**
     * Retrieves the HomeKit service type from the item's tags.
     * This method checks all available HomeKit factories to find a matching service type.
     *
     * @return The HomeKit service type if found, null otherwise
     */
    private String getFactoryServiceType() {
        if (homekitFactoryTracker == null) {
            BundleContext context = FrameworkUtil.getBundle(HomekitTaggedItem.class).getBundleContext();
            homekitFactoryTracker = new ServiceTracker<>(context, HomekitFactory.class, null);
            homekitFactoryTracker.open();
        }

        Object[] factories = homekitFactoryTracker.getServices();
        if (factories != null) {
            if (!homekitTags.isEmpty()) {
                String firstTag = homekitTags.iterator().next();
                for (Object factory : factories) {
                    if (factory instanceof HomekitFactory homekitFactory) {
                        String factoryServiceType = homekitFactory.getServiceTypeFromTag(firstTag);
                        if (factoryServiceType != null) {
                            return factoryServiceType;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Retrieves the HomeKit characteristic type from the item's tags.
     * This method checks all available HomeKit factories to find a matching characteristic type.
     *
     * @return The HomeKit characteristic type if found, null otherwise
     */
    private String getFactoryCharacteristicType() {
        if (homekitFactoryTracker == null) {
            BundleContext context = FrameworkUtil.getBundle(HomekitTaggedItem.class).getBundleContext();
            homekitFactoryTracker = new ServiceTracker<>(context, HomekitFactory.class, null);
            homekitFactoryTracker.open();
        }

        Object[] factories = homekitFactoryTracker.getServices();
        if (factories != null) {
            if (!homekitTags.isEmpty()) {
                String firstTag = homekitTags.iterator().next();
                for (Object factory : factories) {
                    if (factory instanceof HomekitFactory homekitFactory) {
                        String factoryCharacteristicType = homekitFactory.getCharacteristicTypeFromTag(firstTag);
                        if (factoryCharacteristicType != null) {
                            return factoryCharacteristicType;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Checks if the item is tagged for HomeKit integration.
     * An item is considered tagged if it has either a service type or a characteristic type.
     *
     * @return true if the item is tagged for HomeKit integration, false otherwise
     */
    public boolean isTagged() {
        return (serviceType != null && id != 0) || characteristicType != null;
    }

    /**
     * Checks if the item is a group item that represents a HomeKit accessory.
     *
     * @return true if the item is a group and represents a HomeKit accessory, false otherwise
     */
    public boolean isGroup() {
        return (isAccessory() && (this.item instanceof GroupItem));
    }

    /**
     * Gets the HomeKit service type associated with this item.
     * This represents the type of HomeKit accessory (e.g., Light, Switch, Thermostat).
     *
     * @return The HomeKit service type, or null if not applicable
     */
    public String getServiceType() {
        return serviceType;
    }

    /**
     * Gets the HomeKit characteristic type associated with this item.
     * This represents a specific property of a HomeKit accessory (e.g., On, Brightness, Temperature).
     *
     * @return The HomeKit characteristic type, or null if not applicable
     */
    public String getCharacteristicType() {
        return characteristicType;
    }

    /**
     * Checks if this item represents a complete HomeKit accessory.
     * An item is considered an accessory if it has a service type defined.
     * Accessory items must belong to a root accessory group.
     *
     * @return true if the item represents a HomeKit accessory, false otherwise
     */
    public boolean isAccessory() {
        return serviceType != null;
    }

    /**
     * Checks if this item represents a HomeKit characteristic.
     * An item is considered a characteristic if it has a characteristic type defined.
     * Characteristic items must belong to a root accessory group.
     *
     * @return true if the item represents a HomeKit characteristic, false otherwise
     */
    public boolean isCharacteristic() {
        return characteristicType != null;
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
     * Gets the unique identifier for this HomeKit accessory.
     * The ID is calculated based on the item's name and is used to identify the accessory in HomeKit.
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
     * This is the group that represents the complete HomeKit accessory.
     *
     * @return The root device group item, or null if the item is not in a group
     */
    public GroupItem getRootDeviceGroupItem() {
        return parentGroupItem;
    }

    /**
     * Checks if this item belongs to a HomeKit accessory group.
     * Characteristic items must belong to an accessory group.
     *
     * @return true if the item belongs to a HomeKit accessory group, false otherwise
     */
    public boolean isMemberOfAccessoryGroup() {
        return parentGroupItem != null;
    }

    /**
     * Calculates a unique identifier for the HomeKit accessory.
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

    /**
     * Finds all accessory groups that contain the given item.
     * An accessory group is a group item that is tagged as a HomeKit accessory.
     *
     * @param item The item to find groups for
     * @param itemRegistry The item registry to use for group lookups
     * @return A list of group items that are tagged as HomeKit accessories
     */
    public  List<GroupItem> findMyAccessoryGroups() {
        if (homekitFactoryTracker == null) {
            BundleContext context = FrameworkUtil.getBundle(HomekitTaggedItem.class).getBundleContext();
            homekitFactoryTracker = new ServiceTracker<>(context, HomekitFactory.class, null);
            homekitFactoryTracker.open();
        }

        return item.getGroupNames().stream().flatMap(name -> {
            Item groupItem = itemRegistry.get(name);
            if ((groupItem != null) && (groupItem instanceof GroupItem)) {
                return Stream.of((GroupItem) groupItem);
            } else {
                return Stream.empty();
            }
        }).filter(groupItem -> {
            Collection<String> groupHomekitTags = getHomekitTags(groupItem);

            return groupHomekitTags.stream().anyMatch(tag -> {
                Object[] factories = homekitFactoryTracker.getServices();
                if (factories != null) {
                    return Stream.of(factories).filter(factory -> factory instanceof HomekitFactory)
                            .map(factory -> (HomekitFactory) factory)
                            .anyMatch(homekitFactory -> homekitFactory.getServiceTypeFromTag(tag) != null);
                }
                return false;
            });
        }).collect(Collectors.toList());
    }

    private Collection<String> getHomekitTags(Item item) {
        MetadataKey key = new MetadataKey("homekit", item.getName());
        Metadata metadata = metadataRegistry.get(key);
        return metadata != null ? Arrays.asList(metadata.getValue().split(",")) : Collections.emptyList();
    }

    private List<GroupItem> findMyAccessoryGroupsInternal() {
        return findMyAccessoryGroups();
    }
}
