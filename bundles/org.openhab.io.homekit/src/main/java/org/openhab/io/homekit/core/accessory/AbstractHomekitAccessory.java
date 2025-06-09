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

package org.openhab.io.homekit.core.accessory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.event.HomekitEvent;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.factory.HomekitServiceFactory;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.api.uid.HomekitAccessoryUID;
import org.openhab.io.homekit.event.core.HomekitEventSubscription;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.event.manager.HomekitEventManager.HomekitEventHandler;
import org.openhab.io.homekit.event.model.accessory.HomekitAccessoryEvent;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.openhab.io.homekit.library.service.HomekitAccessoryInformationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for HomeKit accessories that provides core functionality
 * and lifecycle management.
 *
 * <p>
 * This class implements the fundamental accessory behavior required by all
 * HomeKit accessories, including:
 * </p>
 * <ul>
 * <li>Unique identification and instance management</li>
 * <li>Service management and lifecycle</li>
 * <li>Event handling and subscriptions</li>
 * <li>JSON serialization</li>
 * <li>Builder pattern for configuration</li>
 * </ul>
 *
 * <p>
 * Key responsibilities:
 * </p>
 * <ul>
 * <li>Managing service lifecycle and relationships</li>
 * <li>Handling event subscriptions and notifications</li>
 * <li>Providing builder pattern for easy configuration</li>
 * <li>Implementing JSON serialization for HomeKit protocol</li>
 * <li>Managing instance IDs and unique identification</li>
 * <li>Coordinating with HomeKit servers</li>
 * </ul>
 *
 * <p>
 * The class integrates with:
 * </p>
 * <ul>
 * <li>{@link HomekitService} for service lifecycle management</li>
 * <li>{@link HomekitEventManager} for event handling</li>
 * <li>{@link HomekitServiceFactory} for service creation</li>
 * <li>{@link HomekitCharacteristicFactory} for characteristic creation</li>
 * <li>{@link HomekitAccessoryServer} for server coordination</li>
 * <li>{@link org.openhab.core.thing.UID OpenHAB's UID system} for unique
 * identification</li>
 * <li>{@link org.openhab.io.homekit.api.event.HomekitEvent OpenHAB's event
 * system} for event handling</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@NonNullByDefault
public abstract class AbstractHomekitAccessory implements HomekitAccessory {

    private static final Logger logger = LoggerFactory.getLogger(AbstractHomekitAccessory.class);

    protected static final String LOG_PREFIX = "Homekit HomekitAccessory: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "HomekitAccessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    private final long instanceId = 0;
    private @Nullable Long accessoryId = null;
    private final Object instanceIdLock = new Object();
    private final Set<Long> usedInstanceIds = new HashSet<>();
    private long nextInstanceId = 1;
    // private final HomekitAccessoryServer server;
    private @Nullable HomekitAccessoryUID accessoryUID;
    private final HomekitAccessoryUID tempUID = new HomekitAccessoryUIDImpl(UUID.randomUUID().toString());

    private final HomekitEventManager eventManager;
    private final HomekitServiceFactory serviceFactory;
    private final HomekitCharacteristicFactory characteristicFactory;

    private final Set<HomekitEventSubscription> eventSubscriptions = new HashSet<>();
    private Collection<HomekitService> services = new HashSet<>();

    // Builder pattern fields
    private String label = "";
    private String serialNumber = "";
    private String model = "";
    private String manufacturer = "openHAB";
    private boolean extensible = true;
    private boolean orphaned = false;

    /**
     * Creates a new Accessory with a unique instance ID.
     * The instance ID is automatically assigned and managed to avoid conflicts.
     *
     * @param eventManager The event manager for handling events
     * @param serviceFactory The factory for creating services
     * @param characteristicFactory The factory for creating characteristics
     * @since 1.0
     */
    public AbstractHomekitAccessory(HomekitEventManager eventManager, HomekitServiceFactory serviceFactory,
            HomekitCharacteristicFactory characteristicFactory) {
        this.eventManager = eventManager;
        this.serviceFactory = serviceFactory;
        this.characteristicFactory = characteristicFactory;
        logger.debug("{}Created new accessory with instance ID: {}", LOG_INIT, instanceId);

        initializeServices();
    }

    /**
     * Creates a new Accessory from a JSON value.
     * The instance ID is taken from the JSON data.
     * AID is restored from JSON to maintain consistency across reboots.
     *
     * @param eventManager The event manager for handling events
     * @param serviceFactory The factory for creating services
     * @param characteristicFactory The factory for creating characteristics
     * @param value The JSON value containing the accessory data
     * @since 1.0
     */
    public AbstractHomekitAccessory(HomekitEventManager eventManager, HomekitServiceFactory serviceFactory,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        this.eventManager = eventManager;
        this.serviceFactory = serviceFactory;
        this.characteristicFactory = characteristicFactory;

        JsonObject jsonObject = (JsonObject) value;
        if (jsonObject.containsKey("aid")) {
            this.accessoryId = (long) jsonObject.getInt("aid");
            // UID will be set when assigned to a server
            logger.debug("{}Restored accessory from JSON with AID: {}", LOG_INIT, accessoryId);
        } else {
            logger.debug("{}Created new accessory from JSON (no AID)", LOG_INIT);
        }

        initializeServices(value);
    }

    private void initializeServices() {
        if (isExtensible()) {
            addServices();
        }
    }

    private void initializeServices(JsonValue value) {
        JsonArray servicesArray = ((JsonObject) value).getJsonArray("services");
        for (JsonValue serviceValue : servicesArray) {
            createService(serviceValue).ifPresent(this::addService);
        }
    }

    /**
     * Gets the unique identifier for this accessory.
     * If the accessory is not yet assigned to a server, returns a temporary UID.
     *
     * @return the unique identifier for this accessory
     */
    @Override
    @NonNull
    public HomekitAccessoryUID getUID() {
        HomekitAccessoryUID uid = this.accessoryUID;
        if (uid == null) {
            return tempUID;
        }
        return uid;
    }

    /**
     * Gets the accessory instance ID.
     * This ID is assigned from a global pool across the entire HomeKit server.
     *
     * @return the accessory instance ID
     * @throws IllegalStateException if the accessory ID has not been set
     */
    @Override
    public long getAccessoryId() {
        Long aid = this.accessoryId;
        if (aid == null) {
            throw new IllegalStateException("HomekitAccessory ID has not been set");
        }
        return aid;
    }

    /**
     * Gets the display label for this accessory.
     * If no label is set, returns the UID as a string.
     *
     * @return the display label
     */
    @Override
    @NonNull
    public String getLabel() {
        return label.isEmpty() ? getUID().toString() : label;
    }

    /**
     * Gets the serial number for this accessory.
     * If no serial number is set, returns the UID as a string.
     *
     * @return the serial number
     */
    @Override
    @NonNull
    public String getSerialNumber() {
        return serialNumber.isEmpty() ? getUID().toString() : serialNumber;
    }

    /**
     * Gets the model name for this accessory.
     * If no model is set, returns the simple class name.
     *
     * @return the model name
     */
    @Override
    @NonNull
    public String getModel() {
        return model.isEmpty() ? this.getClass().getSimpleName() : model;
    }

    /**
     * Gets the manufacturer name for this accessory.
     * Defaults to "openHAB" if not set.
     *
     * @return the manufacturer name
     */
    @Override
    @NonNull
    public String getManufacturer() {
        return manufacturer;
    }

    /**
     * Checks if this accessory can be extended with additional services.
     *
     * @return true if the accessory is extensible, false otherwise
     */
    @Override
    public boolean isExtensible() {
        return extensible;
    }

    /**
     * Checks if this accessory is assigned to a server.
     * An accessory is considered assigned when it has both an AID and UID.
     *
     * @return true if assigned, false otherwise
     */
    public boolean isAssigned() {
        return accessoryId != null && accessoryUID != null;
    }

    /**
     * Adds a service to this accessory.
     * The service is only added if the accessory is extensible and doesn't already
     * have a service of the same type.
     * When a service is added, it is also subscribed to state change events.
     *
     * @param service the service to add
     * @since 1.0
     */
    @Override
    public void addService(@Nullable HomekitService service) {
        if (service != null && isExtensible()) {
            if (getService(service.getType()).isEmpty()) {
                services.add(service);
                logger.debug("{}Added HomekitService '{}' (Type: {}) to HomekitAccessory '{}' (Type: {})",
                        LOG_ACCESSORY, service.getName(), service.getType(), this.getLabel(),
                        this.getClass().getSimpleName());

                // Send event via HomekitEventManager using current UID (temporary or real)
                eventManager
                        .publishEvent(new HomekitAccessoryEvent(HomekitEventType.SERVICE_ADDED, this, service, null));

                // Subscribe to service state change events using current UID
                eventSubscriptions.add(eventManager.subscribe(HomekitEventType.SERVICE_STATE_CHANGED,
                        (UID) service.getUID(), (UID) getUID(), (HomekitEventHandler) event -> onEvent(event)));
            } else {
                logger.debug("{}HomekitAccessory '{}' (Type: {}) already contains HomekitService '{}' (Type: {})",
                        LOG_ACCESSORY, this.getLabel(), this.getClass().getSimpleName(), service.getName(),
                        service.getType());
            }
        }
    }

    /**
     * Adds the default set of services to this accessory.
     * This includes the required accessory information service.
     * Subclasses can override this method to provide additional services.
     *
     * @since 1.0
     */
    @Override
    public void addServices() {
        HomekitService accessoryInformationService = new HomekitAccessoryInformationService(this, eventManager,
                characteristicFactory).withName(getLabel()).withInstanceId(getNextAvailableInstanceId())
                .withExtensible(true);
        addService(accessoryInformationService);
    }

    /**
     * Removes a service from this accessory.
     * Also cleans up any event subscriptions associated with the service.
     *
     * @param service the service to remove
     * @since 1.0
     */
    @Override
    public void removeService(HomekitService service) {
        if (services.remove(service)) {
            logger.debug("{}Removed HomekitService '{}' (Type: {}) from HomekitAccessory '{}' (Type: {})",
                    LOG_ACCESSORY, service.getName(), service.getType(), this.getLabel(),
                    this.getClass().getSimpleName());
            // Send event via HomekitEventManager
            eventManager.publishEvent(new HomekitAccessoryEvent(HomekitEventType.SERVICE_REMOVED, this, service, null));

            Set<HomekitEventSubscription> subscriptions = eventSubscriptions.stream()
                    .filter(subscription -> subscription.getPublisherUID().equals((UID) service.getUID()))
                    .collect(Collectors.toSet());
            subscriptions.forEach(subscription -> eventManager.unsubscribe(subscription));
            eventSubscriptions.removeAll(subscriptions);
        }
    }

    /**
     * Creates a service from a JSON value.
     * Uses the service factory to create the appropriate service type.
     *
     * @param value The JSON value containing service data
     * @return Optional containing the created service if successful
     * @since 1.0
     */
    private Optional<HomekitService> createService(JsonValue value) {
        String serviceType = ((JsonObject) value).getString("type");
        if (serviceFactory.supportsServiceType(serviceType)) {
            try {
                HomekitService service = serviceFactory.createService(serviceType, this);
                if (service != null) {
                    return Optional.of(service);
                }
            } catch (Exception e) {
                logger.error("{}Error creating service: {}", LOG_ERROR, e.getMessage(), e);
            }
        }
        logger.warn("{}No HomekitServiceFactory found to create service from JSON value", LOG_WARN);
        return Optional.empty();
    }

    /**
     * Gets all services supported by this accessory.
     * Services are the primary way to interact with the accessory via the HomeKit
     * protocol.
     *
     * @return the collection of services
     * @since 1.0
     */
    @Override
    @NonNull
    public Collection<HomekitService> getServices() {
        return services;
    }

    /**
     * Gets a service by its type.
     * The service type must match exactly.
     *
     * @param serviceType the type of service to find
     * @return an Optional containing the service if found, empty otherwise
     * @since 1.0
     */
    @Override
    public Optional<HomekitService> getService(String serviceType) {
        return services.stream().filter(s -> s.isType(serviceType)).findAny();
    }

    /**
     * Gets the primary service for this accessory.
     * The primary service represents the main functionality of the accessory.
     *
     * @return an Optional containing the primary service if found, empty otherwise
     * @since 1.0
     */
    @Override
    public Optional<HomekitService> getPrimaryService() {
        return services.stream().filter(s -> s.isPrimary()).findAny();
    }

    /**
     * Assigns this accessory to a server.
     * This method:
     * 1. Verifies the accessory is not already assigned
     * 2. Preserves or obtains an accessory ID
     * 3. Creates a new UID based on the server's pairing ID
     * 4. Notifies the event manager of the UID change
     * 5. Updates the accessory's UID
     *
     * @param server The server to assign to
     * @throws HomekitAccessoryOperationException if the accessory is already
     *             assigned
     */
    @Override
    public void assignToServer(HomekitAccessoryServer server) throws HomekitAccessoryOperationException {
        if (isAssigned()) {
            throw new HomekitAccessoryOperationException("HomekitAccessory is already assigned to a server");
        }

        // If we don't have an AID, get one from the server
        if (accessoryId == null) {
            this.accessoryId = server.getNextAvailableAccessoryId();
        }

        // Create UID using the AID (either restored or newly assigned)
        Long aid = this.accessoryId;
        if (aid == null) {
            throw new IllegalStateException("HomekitAccessory ID should be set at this point");
        }
        HomekitAccessoryUID newUID = new HomekitAccessoryUIDImpl(server.getUID().getPairingId(), aid.longValue());

        // Notify event manager of UID change to migrate subscriptions
        eventManager.notifyUIDChange(tempUID, newUID);

        this.accessoryUID = newUID;
        logger.debug("{}Assigned accessory to server with AID: {}", LOG_STATE, accessoryId);
    }

    /**
     * Creates the JSON representation of the accessory.
     * This includes:
     * - All services and their characteristics
     * - The accessory ID if set
     * 
     * The JSON format follows the HomeKit protocol specification.
     *
     * @return the JSON representation
     */
    @Override
    @NonNull
    public JsonObject toJson() {
        JsonArrayBuilder jsonServices = Json.createArrayBuilder();

        for (HomekitService service : getServices()) {
            jsonServices.add(service.toJson());
        }

        JsonObjectBuilder builder = Json.createObjectBuilder().add("services", jsonServices);

        // Always include AID in JSON to maintain consistency across reboots
        if (accessoryId != null) {
            builder.add("aid", accessoryId.longValue());
        }

        return builder.build();
    }

    /**
     * Creates a reduced JSON representation of the accessory.
     * This includes:
     * - Only essential services and their characteristics
     * - The accessory ID if set
     * 
     * The reduced format is used for basic operations and discovery.
     *
     * @return the reduced JSON representation
     */
    @Override
    @NonNull
    public JsonObject toReducedJson() {
        JsonArrayBuilder jsonServices = Json.createArrayBuilder();

        for (HomekitService service : getServices()) {
            jsonServices.add(service.toReducedJson());
        }

        JsonObjectBuilder builder = Json.createObjectBuilder().add("services", jsonServices);

        // Always include AID in reduced JSON to maintain consistency
        if (accessoryId != null) {
            builder.add("aid", accessoryId.longValue());
        }

        return builder.build();
    }

    /**
     * Performs an operation to identify the accessory.
     * This is a no-op for virtual accessories.
     * Physical accessories should override this method to provide visual or audio
     * feedback.
     */
    @Override
    public void identify() {
        // No op for virtual accessories
    }

    /**
     * Gets the next available instance ID from this accessory's pool.
     * This method is thread-safe and ensures unique IDs within this accessory.
     * The method first tries to recycle unused IDs before creating new ones.
     *
     * @return The next available instance ID
     */
    @Override
    public long getNextAvailableInstanceId() {
        synchronized (instanceIdLock) {
            // First try to find a recycled ID
            for (long id = 1; id < nextInstanceId; id++) {
                if (!usedInstanceIds.contains(id)) {
                    usedInstanceIds.add(id);
                    logger.debug("{}Recycled instance ID: {} for accessory: {}", LOG_STATE, id, instanceId);
                    return id;
                }
            }

            // If no recycled IDs available, use the next new ID
            long newId = nextInstanceId++;
            usedInstanceIds.add(newId);
            logger.debug("{}Assigned new instance ID: {} for accessory: {}", LOG_STATE, newId, instanceId);
            return newId;
        }
    }

    /**
     * Releases an instance ID back to this accessory's pool.
     * This method is thread-safe and should be called when an ID is no longer
     * needed.
     * The released ID can be reused by future calls to getNextAvailableInstanceId.
     *
     * @param id The instance ID to release
     */
    public void releaseInstanceId(long id) {
        synchronized (instanceIdLock) {
            if (usedInstanceIds.remove(id)) {
                logger.debug("{}Released instance ID: {} from accessory: {}", LOG_STATE, id, instanceId);
            } else {
                logger.warn("{}Attempted to release unused instance ID: {} from accessory: {}", LOG_WARN, id,
                        instanceId);
            }
        }
    }

    /**
     * Checks if an instance ID is currently in use in this accessory's pool.
     * This method is thread-safe and can be used to verify ID availability.
     *
     * @param id The instance ID to check
     * @return true if the ID is in use, false otherwise
     */
    public boolean isInstanceIdInUse(long id) {
        synchronized (instanceIdLock) {
            return usedInstanceIds.contains(id);
        }
    }

    /**
     * Cleans up resources associated with this accessory.
     * This method:
     * 1. Releases the instance ID
     * 2. Clears all services
     * 
     * This method should be called when the accessory is no longer needed.
     */
    public void cleanup() {
        releaseInstanceId(instanceId);
        services.clear();
        logger.debug("{}Cleaned up accessory with instance ID: {}", LOG_STATE, instanceId);
    }

    /**
     * Sets the display label for this accessory.
     * The label is used to identify the accessory in the Home app.
     *
     * @param label The new label
     * @return this accessory for method chaining
     */
    @Override
    public HomekitAccessory withLabel(String label) {
        this.label = label;
        return this;
    }

    /**
     * Sets the serial number for this accessory.
     * The serial number should be unique for each physical device.
     *
     * @param serialNumber The new serial number
     * @return this accessory for method chaining
     */
    @Override
    public HomekitAccessory withSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
        return this;
    }

    /**
     * Sets the model name for this accessory.
     * The model name should identify the specific model of the device.
     *
     * @param model The new model name
     * @return this accessory for method chaining
     */
    @Override
    public HomekitAccessory withModel(String model) {
        this.model = model;
        return this;
    }

    /**
     * Sets the manufacturer name for this accessory.
     * The manufacturer name should identify the company that made the device.
     *
     * @param manufacturer The new manufacturer name
     * @return this accessory for method chaining
     */
    @Override
    public HomekitAccessory withManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
        return this;
    }

    /**
     * Sets whether this accessory is extensible.
     * If true, new services can be added to the accessory after creation.
     *
     * @param isExtensible true if the accessory should be extensible, false
     *            otherwise
     * @return this accessory for method chaining
     */
    @Override
    public HomekitAccessory withExtensible(boolean isExtensible) {
        this.extensible = isExtensible;
        return this;
    }

    /**
     * Sets whether this accessory is orphaned (its source item/thing has been
     * removed).
     * Orphaned accessories are kept in the registry to prevent HomeKit controllers
     * from deleting them.
     *
     * @param orphaned true if the accessory is orphaned, false otherwise
     */
    @Override
    public void setOrphaned(boolean orphaned) {
        this.orphaned = orphaned;
    }

    /**
     * Checks if this accessory is orphaned.
     * Orphaned accessories are those whose source items/things have been removed.
     *
     * @return true if the accessory is orphaned, false otherwise
     */
    @Override
    public boolean isOrphaned() {
        return orphaned;
    }

    /**
     * Handles events from the HomekitEventManager.
     * This is a no-op in the base implementation.
     * Subclasses can override this method to handle specific events.
     *
     * @param event The event to handle
     */
    public void onEvent(HomekitEvent event) {
        // No Op?
    }

    /**
     * Checks if this accessory is equal to another object.
     * Two accessories are considered equal if they have:
     * 1. The same accessory ID
     * 2. The same services in the same order
     *
     * @param o The object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        AbstractHomekitAccessory that = (AbstractHomekitAccessory) o;

        // Compare accessory ID
        if (accessoryId != that.accessoryId)
            return false;

        // Compare services
        List<HomekitService> thisServices = new ArrayList<>(this.services);
        List<HomekitService> thatServices = new ArrayList<>(that.services);

        // Sort both lists by instance ID for consistent comparison
        thisServices.sort((s1, s2) -> Long.compare(s1.getInstanceId(), s2.getInstanceId()));
        thatServices.sort((s1, s2) -> Long.compare(s1.getInstanceId(), s2.getInstanceId()));

        // Compare sizes
        if (thisServices.size() != thatServices.size())
            return false;

        // Compare each service
        for (int i = 0; i < thisServices.size(); i++) {
            @Nullable
            HomekitService thisService = thisServices.get(i);
            @Nullable
            HomekitService thatService = thatServices.get(i);
            if (thisService != null && thatService != null && !thisService.equals(thatService))
                return false;
        }

        return true;
    }

    /**
     * Generates a hash code for this accessory.
     * The hash code is based on the accessory ID and services.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(accessoryId, services);
    }

    /**
     * Compares this accessory with another.
     * The comparison is based on:
     * 1. Accessory ID
     * 2. Number of services
     * 3. Service order and equality
     *
     * @param other The accessory to compare with
     * @return a negative integer, zero, or a positive integer as this accessory
     *         is less than, equal to, or greater than the other
     */
    @Override
    public int compareTo(HomekitAccessory other) {
        if (this == other)
            return 0;

        // First compare by accessory ID
        Long thisId = this.accessoryId;
        if (thisId == null) {
            throw new IllegalStateException("Cannot compare accessory without ID");
        }
        int idCompare = Long.compare(thisId, other.getAccessoryId());
        if (idCompare != 0)
            return idCompare;

        // Compare services
        AbstractHomekitAccessory that = (AbstractHomekitAccessory) other;
        List<HomekitService> thisServices = new ArrayList<>(this.services);
        List<HomekitService> thatServices = new ArrayList<>(that.services);

        // Sort both lists by instance ID for consistent comparison
        thisServices.sort((s1, s2) -> Long.compare(s1.getInstanceId(), s2.getInstanceId()));
        thatServices.sort((s1, s2) -> Long.compare(s1.getInstanceId(), s2.getInstanceId()));

        // Compare sizes first
        int sizeCompare = Integer.compare(thisServices.size(), thatServices.size());
        if (sizeCompare != 0)
            return sizeCompare;

        // Compare each service
        for (int i = 0; i < thisServices.size(); i++) {
            @Nullable
            HomekitService thisService = thisServices.get(i);
            if (thisService != null) {
                int serviceCompare = thisService.compareTo(thatServices.get(i));
                if (serviceCompare != 0)
                    return serviceCompare;
            }
        }

        return 0;
    }
}
