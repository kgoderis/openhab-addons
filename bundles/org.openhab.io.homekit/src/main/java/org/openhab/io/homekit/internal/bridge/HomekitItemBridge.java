package org.openhab.io.homekit.internal.bridge;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.ItemRegistryChangeListener;
import org.openhab.core.items.StateChangeListener;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.AccessoryServerRegistry;
import org.openhab.io.homekit.api.Characteristic;
import org.openhab.io.homekit.api.HomekitFactory;
import org.openhab.io.homekit.api.LocalAccessoryServer;
import org.openhab.io.homekit.internal.accessory.AccessoryRegistryImpl;
import org.openhab.io.homekit.internal.accessory.GenericAccessory;
import org.openhab.io.homekit.internal.characteristic.GenericCharacteristic;
import org.openhab.io.homekit.internal.service.GenericService;
import org.openhab.io.homekit.v1.internal.HomekitTaggedItem;
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
    private final Map<String, Characteristic<?>> characteristicMap = new ConcurrentHashMap<>();
    private final Map<String, GenericAccessory> accessoryMap = new ConcurrentHashMap<>();

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
        Characteristic<?> characteristic = characteristicMap.get(item.getName());
        if (characteristic != null) {
            updateCharacteristicValue(characteristic, newState);
        }
    }

    @Override
    public void stateUpdated(Item item, State state) {
        Characteristic<?> characteristic = characteristicMap.get(item.getName());
        if (characteristic != null) {
            updateCharacteristicValue(characteristic, state);
        }
    }

    private void createAccessoryForItem(HomekitTaggedItem taggedItem) {
        try {
            // Find compatible factory
            Optional<HomekitFactory> compatibleFactory = homekitFactories.values().stream()
                    .filter(factory -> factory.supportsServiceType(taggedItem.getAccessoryType().getTag()) &&
                            factory.supportsCharacteristicsType(taggedItem.getCharacteristicType().getTag()))
                    .findFirst();

            if (compatibleFactory.isPresent()) {
                HomekitFactory factory = compatibleFactory.get();
                LocalAccessoryServer server = accessoryServerRegistry.getAvailableBridgeAccessoryServer();
                if (server != null) {
                    // Create a Thing from the Item using ThingBuilder
                    Thing thing = ThingBuilder.create(new ThingTypeUID("homekit", "item"), taggedItem.getName())
                            .withLabel(taggedItem.getItem().getLabel())
                            .build();
                    
                    GenericAccessory accessory = (GenericAccessory) factory.createAccessory(thing, server);
                    
                    if (accessory != null) {
                        // Register the accessory
                        accessoryRegistry.add(accessory);
                        accessoryMap.put(taggedItem.getName(), accessory);
                        
                        // Set up characteristics
                        for (org.openhab.io.homekit.api.Service service : accessory.getServices()) {
                            if (service instanceof GenericService) {
                                GenericService genericService = (GenericService) service;
                                for (Characteristic<?> characteristic : genericService.getCharacteristics()) {
                                    characteristicMap.put(taggedItem.getName(), characteristic);
                                    characteristic.addListener(event -> {
                                        // Convert HomeKit value to openHAB state and publish
                                        State state = characteristic.toState(event.getNewValue());
                                        if (state != null) {
                                            eventPublisher.post(ItemEventFactory.createStateEvent(taggedItem.getName(), state));
                                        }
                                    });
                                }
                            }
                        }
                        
                        logger.debug("Created HomeKit accessory for item {} and added it to server {}", 
                            taggedItem.getName(), server.getUID());
                    }
                } else {
                    logger.warn("No available bridge accessory server found for item {}", taggedItem.getName());
                }
            } else {
                logger.debug("No compatible HomeKit factory found for item {}", taggedItem.getName());
            }
        } catch (Exception e) {
            logger.warn("Error creating HomeKit accessory for item {}", taggedItem.getName(), e);
        }
    }

    private void removeAccessoryForItem(Item item) {
        GenericAccessory accessory = accessoryMap.remove(item.getName());
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
            // Find the factory that created this characteristic
            Optional<HomekitFactory> factory = homekitFactories.values().stream()
                    .filter(f -> f.supportsCharacteristicsType(characteristic.getInstanceType()))
                    .findFirst();
            
            if (factory.isPresent()) {
                if (characteristic instanceof GenericCharacteristic) {
                    @SuppressWarnings("unchecked")
                    GenericCharacteristic<Object> genericCharacteristic = (GenericCharacteristic<Object>) characteristic;
                    Object value = genericCharacteristic.toValue(state);
                    if (value != null) {
                        genericCharacteristic.setValue(value);
                        logger.debug("Updated characteristic {} with value {}", characteristic.getInstanceType(), state);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error updating characteristic value", e);
        }
    }
} 