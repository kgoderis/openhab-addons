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
 * <p>This class implements both {@link ItemRegistryChangeListener} and {@link StateChangeListener} to handle
 * item lifecycle events and state changes. It maintains thread-safe collections of accessories,
 * characteristics, and factories.</p>
 *
 * @author Your Name - Initial contribution
 */
@Component(service = HomekitItemBridge.class, immediate = true)
@NonNullByDefault
public class HomekitItemBridge implements ItemRegistryChangeListener, StateChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(HomekitItemBridge.class);
    
    // Error messages
    private static final String ERROR_CREATING_ACCESSORY = "Error creating HomeKit accessory for item {}: {}";
    private static final String ERROR_UPDATING_CHARACTERISTIC = "Error updating characteristic {}: {}";
    private static final String ERROR_REMOVING_ACCESSORY = "Error removing accessory {}: {}";
    private static final String NO_COMPATIBLE_FACTORY = "No compatible HomeKit factory found for item {}";
    private static final String NO_AVAILABLE_SERVER = "No available bridge accessory server found for item {}";
    private static final String PRIMARY_ACCESSORY_NOT_FOUND = "Primary accessory not found for item {}";
    private static final String CHARACTERISTIC_CREATION_FAILED = "Failed to create characteristic {} for item {}";
    private static final String SERVICE_CREATION_FAILED = "Failed to create service {} for item {}";
    
    // Debug messages
    private static final String DEBUG_CREATING_ACCESSORY = "Creating HomeKit accessory for item {}";
    private static final String DEBUG_ACCESSORY_CREATED = "Successfully created HomeKit accessory for item {}";
    private static final String DEBUG_UPDATING_CHARACTERISTIC = "Updating characteristic {} for item {} with value {}";
    private static final String DEBUG_REMOVING_ACCESSORY = "Removing HomeKit accessory for item {}";
    private static final String DEBUG_ACCESSORY_REMOVED = "Successfully removed HomeKit accessory for item {}";
    private static final String DEBUG_FOUND_COMPATIBLE_FACTORY = "Found compatible factory {} for item {}";
    private static final String DEBUG_FOUND_AVAILABLE_SERVER = "Found available server {} for item {}";

    // Thread safety
    private final Object accessoryLock = new Object();
    private final Object characteristicLock = new Object();
    private final Object factoryLock = new Object();

    private final ItemRegistry itemRegistry;
    private final EventPublisher eventPublisher;
    private final AccessoryRegistryImpl accessoryRegistry;
    private final AccessoryServerRegistry accessoryServerRegistry;
    private final Map<String, HomekitFactory> homekitFactories = new ConcurrentHashMap<>();
    private final Map<String, Collection<Characteristic<?>>> characteristicMap = new ConcurrentHashMap<>();
    private final Map<String, Accessory> accessoryMap = new ConcurrentHashMap<>();

    /**
     * Activates the bridge component and initializes necessary resources.
     * 
     * @param itemRegistry The item registry service
     * @param eventPublisher The event publisher service
     * @param accessoryRegistry The accessory registry service
     * @param accessoryServerRegistry The accessory server registry service
     */
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

    /**
     * Deactivates the bridge component and cleans up resources.
     */
    @Deactivate
    protected void deactivate() {
        itemRegistry.removeRegistryChangeListener(this);
        cleanup();
    }

    /**
     * Cleans up all resources and removes all accessories.
     * This method is synchronized to ensure thread-safe cleanup.
     */
    private void cleanup() {
        synchronized (accessoryLock) {
            accessoryMap.values().forEach(accessory -> {
                try {
                    accessoryRegistry.remove(accessory.getUID());
                } catch (Exception e) {
                    logger.error(ERROR_REMOVING_ACCESSORY, accessory.getUID(), e.getMessage(), e);
                }
            });
            accessoryMap.clear();
        }

        synchronized (characteristicLock) {
            characteristicMap.clear();
        }

        synchronized (factoryLock) {
            homekitFactories.clear();
        }
    }

    /**
     * Safely adds a characteristic to the map.
     * This method is synchronized to ensure thread-safe characteristic management.
     * 
     * @param itemName The name of the item
     * @param characteristic The characteristic to add
     */
    private void addCharacteristic(String itemName, Characteristic<?> characteristic) {
        synchronized (characteristicMap) {
            characteristicMap.computeIfAbsent(itemName, k -> new ArrayList<>()).add(characteristic);
        }
    }

    /**
     * Safely removes a characteristic from the map.
     * This method is synchronized to ensure thread-safe characteristic management.
     * 
     * @param itemName The name of the item
     * @param characteristic The characteristic to remove
     */
    private void removeCharacteristic(String itemName, Characteristic<?> characteristic) {
        synchronized (characteristicMap) {
            Collection<Characteristic<?>> characteristics = characteristicMap.get(itemName);
            if (characteristics != null) {
                characteristics.remove(characteristic);
                if (characteristics.isEmpty()) {
                    characteristicMap.remove(itemName);
                }
            }
        }
    }

    /**
     * Adds a new HomeKit factory to the bridge.
     * This method is synchronized to ensure thread-safe factory management.
     * 
     * @param homekitFactory The factory to add
     */
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addHomekitFactory(HomekitFactory homekitFactory) {
        synchronized (factoryLock) {
            homekitFactories.put(homekitFactory.getClass().getName(), homekitFactory);
        }
        
        // Check existing items for compatibility with new factory
        itemRegistry.getItems().stream()
                .map(item -> new HomekitTaggedItem(item, itemRegistry))
                .filter(HomekitTaggedItem::isTagged)
                .filter(taggedItem -> {
                    synchronized (accessoryLock) {
                        return !accessoryMap.containsKey(taggedItem.getName());
                    }
                })
                .forEach(this::createAccessoryForItem);
    }

    /**
     * Removes a HomeKit factory from the bridge.
     * This method is synchronized to ensure thread-safe factory management.
     * 
     * @param homekitFactory The factory to remove
     */
    protected void removeHomekitFactory(HomekitFactory homekitFactory) {
        synchronized (factoryLock) {
            homekitFactories.remove(homekitFactory.getClass().getName());
        }
    }

    /**
     * Finds a compatible factory for the given tagged item.
     * 
     * @param taggedItem The item to find a compatible factory for
     * @return Optional containing the compatible factory if found
     */
    private Optional<HomekitFactory> findCompatibleFactory(HomekitTaggedItem taggedItem) {
        if (taggedItem == null || taggedItem.getServiceType() == null) {
            return Optional.empty();
        }
        synchronized (factoryLock) {
            return homekitFactories.values().stream()
                    .filter(factory -> factory.supportsServiceType(taggedItem.getServiceType()))
                    .findFirst();
        }
    }

    /**
     * Finds a compatible factory for a characteristic type.
     * 
     * @param characteristicType The type of characteristic to find a factory for
     * @return Optional containing the compatible factory if found
     */
    private Optional<HomekitFactory> findCompatibleCharacteristicFactory(String characteristicType) {
        return homekitFactories.values().stream()
                .filter(factory -> factory.supportsCharacteristicsType(characteristicType))
                .findFirst();
    }

    /**
     * Creates a new accessory for the given tagged item.
     * This method handles the complete accessory creation process including service and characteristic setup.
     * 
     * @param taggedItem The item to create an accessory for
     */
    private void createAccessoryForItem(HomekitTaggedItem taggedItem) {
        try {
            logger.debug(DEBUG_CREATING_ACCESSORY, taggedItem.getName());
            
            Optional<HomekitFactory> compatibleFactory = findCompatibleFactory(taggedItem);
            if (!compatibleFactory.isPresent()) {
                logger.warn(NO_COMPATIBLE_FACTORY, taggedItem.getName());
                return;
            }
            
            compatibleFactory.ifPresent(serviceFactory -> {
                logger.debug(DEBUG_FOUND_COMPATIBLE_FACTORY, serviceFactory.getClass().getSimpleName(), taggedItem.getName());
                
                LocalAccessoryServer server = accessoryServerRegistry.getAvailableBridgeAccessoryServer();
                if (server != null) {
                    logger.debug(DEBUG_FOUND_AVAILABLE_SERVER, server.getUID(), taggedItem.getName());
                    createAndRegisterAccessory(taggedItem, serviceFactory, server);
                } else {
                    logger.warn(NO_AVAILABLE_SERVER, taggedItem.getName());
                }
            });
        } catch (Exception e) {
            logger.error(ERROR_CREATING_ACCESSORY, taggedItem.getName(), e.getMessage(), e);
            // Consider adding recovery logic here
        }
    }

    /**
     * Creates and registers an accessory for the given tagged item.
     * This method is synchronized to ensure thread-safe accessory management.
     * 
     * @param taggedItem The item to create an accessory for
     * @param serviceFactory The factory to create the accessory
     * @param server The server to register the accessory with
     */
    private void createAndRegisterAccessory(HomekitTaggedItem taggedItem, HomekitFactory serviceFactory,
            LocalAccessoryServer server) {
        try {
            Accessory accessory = createAccessory(taggedItem, serviceFactory, server);
            if (accessory != null) {
                synchronized (accessoryLock) {
                    registerAccessory(taggedItem, accessory);
                }
                logger.debug(DEBUG_ACCESSORY_CREATED, taggedItem.getName());
            }
        } catch (Exception e) {
            logger.error(ERROR_CREATING_ACCESSORY, taggedItem.getName(), e.getMessage(), e);
        }
    }

    /**
     * Registers the created accessory with the registry.
     * This method is synchronized to ensure thread-safe registration.
     * 
     * @param taggedItem The item associated with the accessory
     * @param accessory The accessory to register
     */
    private void registerAccessory(HomekitTaggedItem taggedItem, Accessory accessory) {
        try {
            synchronized (accessoryLock) {
                accessoryRegistry.add(accessory);
                accessoryMap.put(taggedItem.getName(), accessory);
            }
            logger.debug("Registered HomeKit accessory for item {} with UID {}", 
                taggedItem.getName(), accessory.getUID());
        } catch (Exception e) {
            logger.error(ERROR_CREATING_ACCESSORY, taggedItem.getName(), e.getMessage(), e);
        }
    }

    /**
     * Removes an accessory for the given item.
     * This method is synchronized to ensure thread-safe removal.
     * 
     * @param item The item whose accessory should be removed
     */
    private void removeAccessoryForItem(Item item) {
        logger.debug(DEBUG_REMOVING_ACCESSORY, item.getName());
        
        synchronized (accessoryLock) {
            Accessory accessory = accessoryMap.remove(item.getName());
            if (accessory != null) {
                try {
                    accessoryRegistry.remove(accessory.getUID());
                    synchronized (characteristicLock) {
                        removeCharacteristic(item.getName(), null);
                    }
                    logger.debug(DEBUG_ACCESSORY_REMOVED, item.getName());
                } catch (Exception e) {
                    logger.error(ERROR_REMOVING_ACCESSORY, item.getName(), e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Creates an accessory for the given tagged item.
     * 
     * @param taggedItem The item to create an accessory for
     * @param serviceFactory The factory to create the accessory
     * @param server The server to add the accessory to
     * @return The created accessory, or null if creation failed
     */
    private Accessory createAccessory(HomekitTaggedItem taggedItem,
            HomekitFactory serviceFactory, LocalAccessoryServer server) {
        try {
            HomekitTaggedItem primaryAccessory = getPrimaryAccessory(taggedItem, taggedItem.getServiceType(),
                    itemRegistry)
                    .map(item -> item)
                    .orElseThrow(() -> new IllegalStateException(
                            String.format(PRIMARY_ACCESSORY_NOT_FOUND, taggedItem.getName())));
            Map<String, Item> characteristicItems = getCharacteristicTypeItemMap(taggedItem);

            if (primaryAccessory != null) {
                Accessory accessory = new GenericAccessory(server);
                Service primaryService = createPrimaryService(serviceFactory, primaryAccessory, accessory, taggedItem);
                if (primaryService != null) {
                    accessory.addService(primaryService);
                    addCharacteristics(primaryService, characteristicItems, accessory);
                    return accessory;
                } else {
                    logger.warn(SERVICE_CREATION_FAILED, primaryAccessory.getServiceType(), taggedItem.getName());
                }
            }
        } catch (Exception e) {
            logger.warn(ERROR_CREATING_ACCESSORY, taggedItem.getName(), e.getMessage());
        }
        return null;
    }

    /**
     * Creates the primary service for an accessory.
     * 
     * @param serviceFactory The factory to create the service
     * @param primaryAccessory The primary accessory item
     * @param accessory The accessory to add the service to
     * @param taggedItem The tagged item
     * @return The created service, or null if creation failed
     */
    private Service createPrimaryService(HomekitFactory serviceFactory, HomekitTaggedItem primaryAccessory,
            Accessory accessory, HomekitTaggedItem taggedItem) {
        return serviceFactory.createService(primaryAccessory.getServiceType(), accessory,
                accessory.getNextAvailableInstanceId(), true, "Primary Service for " + taggedItem.getItem().getName());
    }

    /**
     * Adds characteristics to the primary service.
     * 
     * @param primaryService The service to add characteristics to
     * @param characteristicItems Map of characteristic types to items
     * @param accessory The accessory containing the service
     */
    private void addCharacteristics(Service primaryService, Map<String, Item> characteristicItems, Accessory accessory) {
        for (Map.Entry<String, Item> entry : characteristicItems.entrySet()) {
            String characteristicType = entry.getKey();
            Item item = entry.getValue();

            Optional<HomekitFactory> compatibleCharacteristicFactory = findCompatibleCharacteristicFactory(characteristicType);
            if (shouldAddCharacteristic(primaryService, characteristicType, compatibleCharacteristicFactory)) {
                addCharacteristicToService(primaryService, characteristicType, item, accessory,
                        compatibleCharacteristicFactory.get());
            }
        }
    }

    /**
     * Determines if a characteristic should be added to a service.
     * 
     * @param primaryService The service to check
     * @param characteristicType The type of characteristic
     * @param compatibleFactory Optional containing the compatible factory
     * @return true if the characteristic should be added, false otherwise
     */
    private boolean shouldAddCharacteristic(Service primaryService, String characteristicType,
            Optional<HomekitFactory> compatibleFactory) {
        return primaryService.isExtensible()
                && !primaryService.getCharacteristics().stream()
                        .anyMatch(c -> c.getInstanceType().equals(characteristicType))
                && compatibleFactory.isPresent();
    }

    /**
     * Adds a characteristic to a service and sets up its listener.
     * 
     * @param primaryService The service to add the characteristic to
     * @param characteristicType The type of characteristic to add
     * @param item The item associated with the characteristic
     * @param accessory The accessory containing the service
     * @param characteristicFactory The factory to create the characteristic
     */
    private void addCharacteristicToService(Service primaryService, String characteristicType, Item item,
            Accessory accessory, HomekitFactory characteristicFactory) {
        Characteristic<?> characteristic = characteristicFactory.createCharacteristic(characteristicType, primaryService,
                accessory.getNextAvailableInstanceId());
        if (characteristic != null) {
            primaryService.addCharacteristic(characteristic);
            addCharacteristic(item.getName(), characteristic);
            setupCharacteristicListener(characteristic, item);
        }
    }

    /**
     * Sets up the listener for a characteristic.
     * 
     * @param characteristic The characteristic to set up the listener for
     * @param item The item associated with the characteristic
     */
    private void setupCharacteristicListener(Characteristic<?> characteristic, Item item) {
        characteristic.addListener(event -> {
            State state = characteristic.toState(event.getNewValue());
            if (state != null) {
                eventPublisher.post(ItemEventFactory.createStateEvent(item.getName(), state));
            }
        });
    }


    /**
     * Updates the value of a characteristic based on the new state.
     * This method is synchronized to ensure thread-safe updates.
     * 
     * @param characteristic The characteristic to update
     * @param state The new state value
     */
    private void updateCharacteristicValue(Characteristic<?> characteristic, State state) {
        try {
            if (characteristic instanceof GenericCharacteristic) {
                @SuppressWarnings("unchecked")
                GenericCharacteristic<Object> genericCharacteristic = (GenericCharacteristic<Object>) characteristic;
                Object value = genericCharacteristic.toValue(state);
                if (value != null) {
                    synchronized (characteristicLock) {
                        genericCharacteristic.setValue(value);
                    }
                    logger.debug(DEBUG_UPDATING_CHARACTERISTIC, 
                        characteristic.getInstanceType(), 
                        characteristic.getService().getAccessory().getUID(), 
                        state);
                }
            }
        } catch (Exception e) {
            logger.error(ERROR_UPDATING_CHARACTERISTIC, characteristic.getInstanceType(), e.getMessage(), e);
        }
    }

    /**
     * Given an accessory group, return the item in the group tagged as an accessory.
     *
     * @param taggedItem The group item containing our item, or, the accessory item
     * @param serviceType The accessory type for which we're looking
     * @param itemRegistry The item registry to use
     * @return Optional containing the primary accessory if found
     */
    private Optional<HomekitTaggedItem> getPrimaryAccessory(HomekitTaggedItem taggedItem,
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

    /**
     * Gets the map of characteristic types to items for a tagged item.
     *
     * @param taggedItem The tagged item to get characteristics for
     * @return Map of characteristic types to items
     */
    private Map<String, Item> getCharacteristicTypeItemMap(HomekitTaggedItem taggedItem) {
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

    // ========== Item Registry Change Listener Methods ==========
    /**
     * Handles the addition of a new item to the registry.
     * If the item is tagged for HomeKit integration, creates a new accessory for it.
     * 
     * @param item The item that was added to the registry
     */
    @Override
    public void added(Item item) {
        HomekitTaggedItem taggedItem = new HomekitTaggedItem(item, itemRegistry);
        if (taggedItem.isTagged()) {
            createAccessoryForItem(taggedItem);
        }
    }

    /**
     * Handles the removal of an item from the registry.
     * Removes the associated HomeKit accessory if it exists.
     * 
     * @param item The item that was removed from the registry
     */
    @Override
    public void removed(Item item) {
        removeAccessoryForItem(item);
    }

    /**
     * Handles the update of an existing item in the registry.
     * Removes the old accessory and creates a new one if the updated item is tagged for HomeKit integration.
     * 
     * @param oldItem The previous version of the item
     * @param item The updated version of the item
     */
    @Override
    public void updated(Item oldItem, Item item) {
        removeAccessoryForItem(oldItem);
        HomekitTaggedItem taggedItem = new HomekitTaggedItem(item, itemRegistry);
        if (taggedItem.isTagged()) {
            createAccessoryForItem(taggedItem);
        }
    }

    /**
     * Called when all items in the registry have changed.
     * This method handles removing all old accessories and creating new ones for tagged items.
     * 
     * @param oldItemNames Collection of names of items that were previously in the registry
     */
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

    // ========== State Change Listener Methods ==========
    /**
     * Handles state changes for items.
     * Updates the corresponding HomeKit characteristic values when an item's state changes.
     * 
     * @param item The item whose state changed
     * @param oldState The previous state of the item
     * @param newState The new state of the item
     */
    @Override
    public void stateChanged(Item item, State oldState, State newState) {
        Optional.ofNullable(characteristicMap.get(item.getName()))
            .ifPresent(characteristics -> characteristics.forEach(c -> updateCharacteristicValue(c, newState)));
    }

    /**
     * Handles state updates for items.
     * Updates the corresponding HomeKit characteristic values when an item's state is updated.
     * 
     * @param item The item whose state was updated
     * @param state The new state of the item
     */
    @Override
    public void stateUpdated(Item item, State state) {
        Optional.ofNullable(characteristicMap.get(item.getName()))
            .ifPresent(characteristics -> characteristics.forEach(c -> updateCharacteristicValue(c, state)));
    }
} 