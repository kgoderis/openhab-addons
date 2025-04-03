package org.openhab.io.homekit.internal.bridge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.ItemRegistryChangeListener;
import org.openhab.core.items.StateChangeListener;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.Characteristic;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.api.registry.AccessoryServerRegistry;
import org.openhab.io.homekit.api.server.LocalAccessoryServer;
import org.openhab.io.homekit.internal.accessory.AccessoryRegistryImpl;
import org.openhab.io.homekit.internal.accessory.GenericAccessory;
import org.openhab.io.homekit.internal.characteristic.GenericCharacteristic;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HomekitItemBridge} listens for new Items that are added to openHAB and creates HomeKit accessories
 * for tagged items. It also handles state changes and updates characteristics accordingly.
 *
 * @author Your Name - Initial contribution
 */
@Component(service = HomekitItemBridge.class, immediate = true)
@NonNullByDefault
public class HomekitItemBridge implements ItemRegistryChangeListener, StateChangeListener {

    private final Logger logger = LoggerFactory.getLogger(HomekitItemBridge.class);

    private final ItemRegistry itemRegistry;
    private final EventPublisher eventPublisher;
    private final AccessoryRegistryImpl accessoryRegistry;
    private final AccessoryServerRegistry accessoryServerRegistry;
    private final Map<String, HomekitFactory> homekitFactories = new ConcurrentHashMap<>();
    private final Map<String, Accessory> accessoryMap = new ConcurrentHashMap<>();
    private final Map<String, Collection<Characteristic<?>>> characteristicMap = new ConcurrentHashMap<>();


    @Activate
    public HomekitItemBridge(
            @Reference ItemRegistry itemRegistry,
            @Reference EventPublisher eventPublisher,
            @Reference AccessoryRegistryImpl accessoryRegistry,
            @Reference AccessoryServerRegistry accessoryServerRegistry) {
        this.itemRegistry = itemRegistry;
        this.eventPublisher = eventPublisher;
        this.accessoryRegistry = accessoryRegistry;
        this.accessoryServerRegistry = accessoryServerRegistry;
        
        itemRegistry.addRegistryChangeListener(this);
        
        // Initialize existing HomeKit tagged items
        for (Item item : itemRegistry.getItems()) {
            HomekitTaggedItem taggedItem = new HomekitTaggedItem(item, itemRegistry);
            if (taggedItem.isTagged()) {
                createAccessoryForItem(taggedItem);
            }
        }
    }

    @Deactivate
    public void deactivate() {
        itemRegistry.removeRegistryChangeListener(this);
        
        // Clean up all accessories and characteristics
        accessoryMap.values().forEach(accessory -> {
            try {
                accessoryRegistry.remove(accessory.getUID());
            } catch (Exception e) {
                logger.warn("Error removing accessory during deactivation", e);
            }
        });
        
        accessoryMap.clear();
        characteristicMap.clear();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addHomekitFactory(HomekitFactory homekitFactory) {
        homekitFactories.put(homekitFactory.getClass().getName(), homekitFactory);
        
        // Check existing items for compatibility with new factory
        itemRegistry.getItems().stream()
                .map(item -> new HomekitTaggedItem(item, itemRegistry))
                .filter(HomekitTaggedItem::isTagged)
                .filter(taggedItem -> !accessoryMap.containsKey(taggedItem.getName()))
                .forEach(this::createAccessoryForItem);
    }

    protected void removeHomekitFactory(HomekitFactory homekitFactory) {
        homekitFactories.remove(homekitFactory.getClass().getName());
    }

    @Override
    public void added(Item item) {
        HomekitTaggedItem taggedItem = new HomekitTaggedItem(item, itemRegistry);
        if (taggedItem.isTagged()) {
            createAccessoryForItem(taggedItem);
        }
    }

    @Override
    public void removed(Item item) {
        removeAccessoryForItem(item);
    }

    @Override
    public void updated(Item oldItem, Item item) {
        removeAccessoryForItem(oldItem);
        HomekitTaggedItem taggedItem = new HomekitTaggedItem(item, itemRegistry);
        if (taggedItem.isTagged()) {
            createAccessoryForItem(taggedItem);
        }
    }

    @Override
    public void allItemsChanged(Collection<String> oldItemNames) {
        // Remove all old accessories
        oldItemNames.forEach(itemName -> {
            Item item = itemRegistry.get(itemName);
            if (item != null) {
                removeAccessoryForItem(item);
            }
        });

        // Create new accessories for all tagged items
        itemRegistry.getItems().stream()
                .map(item -> new HomekitTaggedItem(item, itemRegistry))
                .filter(HomekitTaggedItem::isTagged)
                .forEach(this::createAccessoryForItem);
    }

    @Override
    public void stateChanged(Item item, State oldState, State newState) {
        Optional.ofNullable(characteristicMap.get(item.getName()))
            .ifPresent(characteristics -> characteristics.forEach(c -> updateCharacteristicValue(c, newState)));
    }

    @Override
    public void stateUpdated(Item item, State state) {
        Optional.ofNullable(characteristicMap.get(item.getName()))
            .ifPresent(characteristics -> characteristics.forEach(c -> updateCharacteristicValue(c, state)));
    }

    private void createAccessoryForItem(HomekitTaggedItem taggedItem) {
        try {
            // Find compatible factory
            Optional<HomekitFactory> compatibleFactory = homekitFactories.values().stream()
                    .filter(factory -> factory.supportsServiceType(taggedItem.getServiceType())).findFirst();

            compatibleFactory.ifPresent(serviceFactory -> {
                LocalAccessoryServer server = accessoryServerRegistry.getAvailableBridgeAccessoryServer();
                if (server != null) {
                    HomekitTaggedItem primaryAccessory = getPrimaryAccessory(taggedItem, taggedItem.getServiceType(),
                            itemRegistry)
                            .map(item -> item)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Primary accessory not found for item " + taggedItem.getName()));
                    Map<String, Item> characteristicItems = getCharacteristicTypeItemMap(taggedItem);

                    if (primaryAccessory != null) {
                        Accessory accessory = new GenericAccessory(server, 1);
                        Service primaryService = serviceFactory.createService(primaryAccessory.getServiceType(),
                                accessory, accessory.getNewInstanceId(), true,
                                "Primary Service for " + taggedItem.getItem().getName());
                        if (primaryService != null) {
                            accessory.addService(primaryService);

                            accessoryRegistry.add(accessory);
                            accessoryMap.put(taggedItem.getName(), accessory);

                            // travers characteristicItems and add characteristics to primaryService
                            for (Map.Entry<String, Item> entry : characteristicItems.entrySet()) {
                                String characteristicType = entry.getKey();
                                Item item = entry.getValue();

                                Optional<HomekitFactory> compatibleCharacteristicFactory = homekitFactories.values()
                                        .stream()
                                        .filter(factory -> factory
                                                .supportsCharacteristicsType(characteristicType))
                                        .findFirst();

                                if (primaryService.isExtensible()
                                        && !primaryService.getCharacteristics().stream()
                                                .anyMatch(c -> c.getInstanceType().equals(characteristicType))
                                        && compatibleCharacteristicFactory.isPresent()) {
                                    HomekitFactory characteristicFactory = compatibleCharacteristicFactory.get();
                                    Characteristic<?> characteristic = characteristicFactory.createCharacteristic(
                                            characteristicType, primaryService, accessory.getNewInstanceId());
                                    if (characteristic != null) {
                                        primaryService.addCharacteristic(characteristic);

                                        // add the characteristic to the characteristicMap, add to the list if it already exists
                                        characteristicMap.computeIfAbsent(item.getName(), k -> new ArrayList<>()).add(characteristic);
                                        characteristic.addListener(event -> {
                                            // Convert HomeKit value to openHAB state and publish
                                            State state = characteristic.toState(event.getNewValue());
                                            if (state != null) {
                                                eventPublisher.post(
                                                        ItemEventFactory.createStateEvent(item.getName(), state));
                                            }
                                        });
                                    }
                                }
                                logger.debug("Created HomeKit accessory for item {} and added it to server {}",
                                        taggedItem.getName(), server.getUID());
                            }
                        }
                    } else {
                        logger.warn("No available bridge accessory server found for item {}", taggedItem.getName());
                    }
                }
            });
        } catch (Exception e) {
            logger.warn("Error creating HomeKit accessory for item {}", taggedItem.getName(), e);
        }
    }

    private void removeAccessoryForItem(Item item) {
        Accessory accessory = accessoryMap.remove(item.getName());
        if (accessory != null) {
            try {
                accessoryRegistry.remove(accessory.getUID());
                characteristicMap.remove(item.getName());
                logger.debug("Removed HomeKit accessory for item {}", item.getName());
            } catch (Exception e) {
                logger.warn("Error removing HomeKit accessory for item {}", item.getName(), e);
            }
        }
    }

    private void updateCharacteristicValue(Characteristic<?> characteristic, State state) {
        try {

                if (characteristic instanceof GenericCharacteristic) {
                    @SuppressWarnings("unchecked")
                    GenericCharacteristic<Object> genericCharacteristic = (GenericCharacteristic<Object>) characteristic;
                    Object value = genericCharacteristic.toValue(state);
                    if (value != null) {
                        genericCharacteristic.setValue(value);
                        logger.debug("Updated characteristic {} with value {}", characteristic.getInstanceType(), state);
                    }
                }
            
        } catch (Exception e) {
            logger.warn("Error updating characteristic value", e);
        }
    }

        /**
     * Given an accessory group, return the item in the group tagged as an accessory.
     *
     * @param taggedItem The group item containing our item, or, the accessory item.
     * @param serviceType The accessory type for which we're looking
     * @return
     */
    private  Optional<HomekitTaggedItem> getPrimaryAccessory(HomekitTaggedItem taggedItem,
            String serviceType, ItemRegistry itemRegistry) {
        logger.debug("{}: isGroup? {}, isMember? {}", taggedItem.getName(), taggedItem.isGroup(),
                taggedItem.isMemberOfAccessoryGroup());
        if (taggedItem.isGroup()) {
            GroupItem groupItem = (GroupItem) taggedItem.getItem();
            Object[] factories = homekitFactories.values().toArray();
            if (factories != null) {
                for (Object factory : factories) {
                    if (factory instanceof HomekitFactory) {
                        String tag = ((HomekitFactory) factory).getTagFromServiceType(serviceType);
                        if (tag != null) {
                            return groupItem.getMembers().stream().filter(item -> item.hasTag(tag)).findFirst()
                                .map(item -> new HomekitTaggedItem(item, itemRegistry));
                        }
                    }
                }
            }
        } else if (taggedItem.getServiceType() == serviceType) {
            return Optional.of(taggedItem);
        } 
            return Optional.empty();

    }

    private  Map<String, Item> getCharacteristicTypeItemMap(HomekitTaggedItem taggedItem) {
        if (taggedItem.isGroup()) {
            GroupItem groupItem = (GroupItem) taggedItem.getItem();
            Map<String, Item> characteristicItems = new HashMap<>();
            groupItem.getMembers().forEach(item -> {
                String type = item.getTags().stream()
                    .map(tag -> {
                        Object[] factories = homekitFactories.values().toArray();
                        if (factories != null) {
                            return Stream.of(factories)
                                .filter(factory -> factory instanceof HomekitFactory)
                                .map(factory -> (HomekitFactory) factory)
                                .map(factory -> factory.getCharacteristicTypeFromTag(tag))
                                .filter(t -> t != null)
                                .findFirst()
                                .orElse(null);
                        }
                        return null;
                    })
                    .filter(t -> t != null)
                    .findFirst()
                    .orElse(null);
                if (type != null) {
                    if (characteristicItems.containsKey(type)) {
                        logger.warn("incorrect configuration for {} detected: {} and {} are tagged as {}, skipping {}",
                                taggedItem.getItem().getUID(), characteristicItems.get(type).getUID(), item.getUID(),
                                type, item.getUID());
                    } else {
                        characteristicItems.put(type, item);
                    }
                }
            });
            return Collections.unmodifiableMap(characteristicItems);
        } else {
            // do nothing; only accessory groups have characteristic items
            return Collections.emptyMap();
        }
    }
} 