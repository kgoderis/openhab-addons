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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Wraps an Item with data derived from supported tags defined.
 *
 * @author Andy Lintner - Initial contribution
 */
public class HomekitTaggedItem {
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

    public HomekitTaggedItem(Item item, ItemRegistry itemRegistry) {
        this.item = item;

        try {
            serviceType = getFactoryServiceType();
            characteristicType = getFactoryCharacteristicType();
            if (serviceType != null && characteristicType != null) {
                throw new BadItemConfigurationException(
                        "Items cannot be tagged as both a characteristic and an accessory type");
            }
            List<GroupItem> matchingGroupItems = findMyAccessoryGroups(item, itemRegistry);

            switch (matchingGroupItems.size()) {
                case 0: // Does not belong to a accessory group
                    if (characteristicType != null) {
                        throw new BadItemConfigurationException(
                                "Item is tagged as a characteristic, but does not belong to a root accessory group");
                    }

                    parentGroupItem = null;
                    break;
                case 1: // Belongs to exactly one accessory group
                    if (item instanceof GroupItem) {
                        throw new BadItemConfigurationException("Nested Accessory Groups are not supported");
                    }

                    parentGroupItem = matchingGroupItems.get(0);
                    break;
                default: // Belongs to more than one accessory group
                    throw new BadItemConfigurationException(
                            "Item belongs to multiple Groups which are tagged as Homekit devices.");
            }

        } catch (BadItemConfigurationException e) {
            logger.warn("Item {} was misconfigured: {}. Excluding item from homekit.", item.getName(), e.getMessage());
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



    public String getFactoryServiceType() {
        if (homekitFactoryTracker == null) {
            BundleContext context = FrameworkUtil.getBundle(HomekitTaggedItem.class).getBundleContext();
            homekitFactoryTracker = new ServiceTracker<>(context, HomekitFactory.class, null);
            homekitFactoryTracker.open();
        }

        Object[] factories = homekitFactoryTracker.getServices();
        if (factories != null) {
            if (!item.getTags().isEmpty()) {
                String firstTag = item.getTags().iterator().next();
                for (Object factory : factories) {
                    if (factory instanceof HomekitFactory homekitFactory) {
                        String serviceType = homekitFactory.getServiceTypeFromTag(firstTag);
                        if (serviceType != null) {
                            return serviceType;
                        }
                    }
                }
            }
        }
        return null;
    }

    public String getFactoryCharacteristicType() {
        if (homekitFactoryTracker == null) {
            BundleContext context = FrameworkUtil.getBundle(HomekitTaggedItem.class).getBundleContext();
            homekitFactoryTracker = new ServiceTracker<>(context, HomekitFactory.class, null);
            homekitFactoryTracker.open();
        }

        Object[] factories = homekitFactoryTracker.getServices();
        if (factories != null) {
            if (!item.getTags().isEmpty()) {
                String firstTag = item.getTags().iterator().next();
                for (Object factory : factories) {
                    if (factory instanceof HomekitFactory homekitFactory) {
                        String characteristicType = homekitFactory.getCharacteristicTypeFromTag(firstTag);
                        if (characteristicType != null) {
                            return characteristicType;
                        }
                    }
                }
            }
        }
        return null;
    }

    public boolean isTagged() {
        return (serviceType != null && id != 0) || characteristicType != null;
    }

    public boolean isGroup() {
        return (isAccessory() && (this.item instanceof GroupItem));
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getCharacteristicType() {
        return characteristicType;
    }

    /**
     * Returns whether or not this item refers to an item that fully specifies a Homekit accessory. Mutually
     * exclusive
     * to isCharacteristic(). Primary devices must belong to a root accessory group.
     */
    public boolean isAccessory() {
        return serviceType != null;
    }

    /**
     * Returns whether or not this item is in a group that specifies a Homekit accessory. It is not possible to be a
     * characteristic and an accessory. Further, all characteristics belong to a
     * root deviceGroup.
     */
    public boolean isCharacteristic() {
        return characteristicType != null;
    }

    public Item getItem() {
        return item;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return item.getName();
    }

    /**
     * Returns the RootDevice GroupItem to which this item belongs.
     * Returns null if not in a group.
     */
    public GroupItem getRootDeviceGroupItem() {
        return parentGroupItem;
    }

    /**
     * Returns whether or not this item belongs to a Homekit accessory group.
     *
     * Characteristic devices must belong to a Homekit accessory group.
     */
    public boolean isMemberOfAccessoryGroup() {
        return parentGroupItem != null;
    }

    private int calculateId(Item item) {
        int id = new HashCodeBuilder().append(item.getName()).hashCode();
        if (id < 0) {
            id += Integer.MAX_VALUE;
        }
        if (id < 2) {
            id = 2; // 0 and 1 are reserved
        }
        if (CREATED_ACCESSORY_IDS.containsKey(id)) {
            if (!CREATED_ACCESSORY_IDS.get(id).equals(item.getName())) {
                logger.warn(
                        "Could not create homekit accessory {} because its hash conflicts with {}. This is a 1:1,000,000 chance occurrence. Change one of the names and consider playing the lottery. See https://github.com/openhab/openhab2-addons/issues/257#issuecomment-125886562",
                        item.getName(), CREATED_ACCESSORY_IDS.get(id));
                return 0;
            }
        } else {
            CREATED_ACCESSORY_IDS.put(id, item.getName());
        }
        return id;
    }

    public static List<GroupItem> findMyAccessoryGroups(Item item, ItemRegistry itemRegistry) {
        // return item.getGroupNames().stream().flatMap(name -> {
        //     Item groupItem = itemRegistry.get(name);
        //     if ((groupItem != null) && (groupItem instanceof GroupItem)) {
        //         return Stream.of((GroupItem) groupItem);
        //     } else {
        //         return Stream.empty();
        //     }
        // }).filter(groupItem -> {
        //     return groupItem.getTags().stream().filter(gt -> HomekitAccessoryType.valueOfTag(gt) != null).count() > 0;
        // }).collect(Collectors.toList());
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
            return groupItem.getTags().stream().anyMatch(tag -> {
                Object[] factories = homekitFactoryTracker.getServices();
                if (factories != null) {
                    return Stream.of(factories)
                        .filter(factory -> factory instanceof HomekitFactory)
                        .map(factory -> (HomekitFactory) factory)
                        .anyMatch(homekitFactory -> homekitFactory.getServiceTypeFromTag(tag) != null);
                }
                return false;
            });
        }).collect(Collectors.toList());
    }
}
