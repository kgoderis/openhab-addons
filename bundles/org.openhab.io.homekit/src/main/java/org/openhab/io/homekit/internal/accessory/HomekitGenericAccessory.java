package org.openhab.io.homekit.internal.accessory;

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
import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.openhab.io.homekit.api.hap.HomekitAccessory;
import org.openhab.io.homekit.api.hap.HomekitAccessoryServer;
import org.openhab.io.homekit.api.hap.HomekitService;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.openhab.io.homekit.exception.HomekitFactoryException;
import org.openhab.io.homekit.internal.events.HomekitAccessoryEvent;
import org.openhab.io.homekit.internal.events.HomekitEvent;
import org.openhab.io.homekit.internal.events.HomekitEventManager;
import org.openhab.io.homekit.internal.events.HomekitEventSubscription;
import org.openhab.io.homekit.internal.events.HomekitEventType;
import org.openhab.io.homekit.library.service.HomekitAccessoryInformationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class HomekitGenericAccessory implements HomekitAccessory {

    private static final Logger logger = LoggerFactory.getLogger(HomekitGenericAccessory.class);

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
    private Collection<HomekitService> services = new HashSet<>();
    private @Nullable HomekitAccessoryUID accessoryUID;
    private final HomekitAccessoryUID tempUID = new HomekitAccessoryUID(UUID.randomUUID().toString());
    private final HomekitEventManager eventManager;
    private final Collection<HomekitFactory> homekitFactories;

    private final Set<HomekitEventSubscription> eventSubscriptions = new HashSet<>();

    /**
     * Creates a new HomekitGenericAccessory with a unique instance ID.
     * The instance ID is automatically assigned and managed to avoid conflicts.
     *
     * @param eventManager The event manager for handling events
     * @param homekitFactories The factories for creating services
     */
    public HomekitGenericAccessory(HomekitEventManager eventManager, Collection<HomekitFactory> homekitFactories) {
        this.eventManager = eventManager;
        this.homekitFactories = homekitFactories;
        logger.debug("{}Created new accessory with instance ID: {}", LOG_INIT, instanceId);

        initializeServices();
    }

    /**
     * Creates a new HomekitGenericAccessory from a JSON value.
     * The instance ID is taken from the JSON data.
     * AID is restored from JSON to maintain consistency across reboots.
     *
     * @param value The JSON value containing the accessory data
     * @param eventManager The event manager for handling events
     * @param homekitFactories The factories for creating services
     */
    public HomekitGenericAccessory(JsonValue value, HomekitEventManager eventManager,
            Collection<HomekitFactory> homekitFactories) {
        this.eventManager = eventManager;
        this.homekitFactories = homekitFactories;

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

    /**
     * Assigns this accessory to a server, setting its UID.
     * If the accessory already has an AID, it will be preserved.
     * This method should be called by the server when adding the accessory.
     *
     * @param server The server to assign this accessory to
     * @throws HomekitAccessoryOperationException if the accessory is already assigned
     */
    public void assignToServer(HomekitAccessoryServer server) throws HomekitAccessoryOperationException {
        if (isAssigned()) {
            throw new HomekitAccessoryOperationException("HomekitAccessory is already assigned to a server");
        }

        // If we don't have an AID, get one from the server
        if (accessoryId == null) {
            this.accessoryId = server.getNextAvailableAccessoryId();
        }

        // Create UID using the AID (either restored or newly assigned)
        HomekitAccessoryUID newUID = new HomekitAccessoryUID(server.getUID().getPairingId(), this.accessoryId);

        // Notify event manager of UID change to migrate subscriptions
        eventManager.notifyUIDChange(tempUID, newUID);

        this.accessoryUID = newUID;
        logger.debug("{}Assigned accessory to server with AID: {}", LOG_STATE, accessoryId);
    }

    /**
     * Checks if this accessory is assigned to a server.
     *
     * @return true if the accessory has an AID and UID, false otherwise
     */
    public boolean isAssigned() {
        return accessoryId != null && accessoryUID != null;
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

    private Optional<HomekitService> createService(JsonValue value) {
        for (HomekitFactory factory : homekitFactories) {
            if (factory instanceof HomekitFactory homekitFactory) {
                String serviceType = ((JsonObject) value).getString("type");
                if (homekitFactory.supportsServiceType(serviceType)) {
                    try {
                        HomekitService service = homekitFactory.createService(this, value);
                        if (service != null) {
                            return Optional.of(service);
                        }
                    } catch (HomekitFactoryException e) {
                        logger.error("{}Error creating service: {}", LOG_ERROR, e.getMessage(), e);
                    }
                }
            }
        }
        logger.warn("{}No HomekitFactory found to create service from JSON value", LOG_WARN);
        return Optional.empty();
    }

    @Override
    public void addService(@Nullable HomekitService service) {
        if (service != null && isExtensible()) {
            if (getService(service.getInstanceType()).isEmpty()) {
                services.add(service);
                logger.debug("{}Added HomekitService '{}' (Type: {}) to HomekitAccessory '{}' (Type: {})", LOG_ACCESSORY,
                        service.getName(), service.getInstanceType(), this.getLabel(), this.getClass().getSimpleName());

                // Send event via HomekitEventManager using current UID (temporary or real)
                eventManager.publishEvent(new HomekitAccessoryEvent(HomekitEventType.SERVICE_ADDED, this, service, null));

                // Subscribe to service state change events using current UID
                eventSubscriptions.add(eventManager.subscribe(HomekitEventType.SERVICE_STATE_CHANGED, service.getUID(),
                        getUID(), event -> HomekitGenericAccessory.this.onEvent(event)));
            } else {
                logger.debug("{}HomekitAccessory '{}' (Type: {}) already contains HomekitService '{}' (Type: {})", LOG_ACCESSORY,
                        this.getLabel(), this.getClass().getSimpleName(), service.getName(), service.getInstanceType());
            }
        }
    }

    @Override
    public void removeService(HomekitService service) {
        if (services.remove(service)) {
            logger.debug("{}Removed HomekitService '{}' (Type: {}) from HomekitAccessory '{}' (Type: {})", LOG_ACCESSORY,
                    service.getName(), service.getInstanceType(), this.getLabel(), this.getClass().getSimpleName());
            // Send event via HomekitEventManager
            eventManager.publishEvent(new HomekitAccessoryEvent(HomekitEventType.SERVICE_REMOVED, this, service, null));

            Set<HomekitEventSubscription> subscriptions = eventSubscriptions.stream()
                    .filter(subscription -> subscription.getPublisherUID().equals(service.getUID().toString()))
                    .collect(Collectors.toSet());
            subscriptions.forEach(subscription -> eventManager.unsubscribe(subscription));
            eventSubscriptions.removeAll(subscriptions);
        }
    }

    // Handle events from HomekitEventManager
    public void onEvent(HomekitEvent event) {
        // No Op?
    }

    /**
     * Gets the next available instance ID from this accessory's pool.
     * This method is thread-safe and ensures unique IDs within this accessory.
     * Services and Characteristics associated with this accessory can use this method to get unique IDs.
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
     * This method is thread-safe and should be called when an ID is no longer needed.
     * Services and Characteristics associated with this accessory can use this method to release their IDs.
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
     * This method should be called when the accessory is no longer needed.
     */
    public void cleanup() {
        releaseInstanceId(instanceId);
        services.clear();
        logger.debug("{}Cleaned up accessory with instance ID: {}", LOG_STATE, instanceId);
    }

    /**
     * Adds default services to the accessory. Subclasses can override this method
     * to provide additional services.
     * 
     * @throws Exception
     */
    @Override
    public void addServices() {
        addService(new HomekitAccessoryInformationService(this, getNextAvailableInstanceId(), true, getLabel(), eventManager,
                homekitFactories));
    }

    @Override
    @NonNull
    public HomekitAccessoryUID getUID() {
        HomekitAccessoryUID uid = this.accessoryUID;
        if (uid == null) {
            return tempUID;
        }
        return uid;
    }

    @Override
    public long getAccessoryId() {
        Long aid = this.accessoryId;
        if (aid == null) {
            throw new IllegalStateException("HomekitAccessory ID has not been set");
        }
        return aid;
    }

    @Override
    @NonNull
    public String getSerialNumber() {
        return getUID().toString();
    }

    @Override
    public @NonNull String getLabel() {
        return getUID().toString();
    }

    @Override
    public @NonNull String getManufacturer() {
        return "openHAB";
    }

    @Override
    @NonNull
    public String getModel() {
        return this.getClass().getSimpleName();
    }

    // @Override
    // public @NonNull HomekitAccessoryServer getServer() {
    // return server;
    // }

    @Override
    @NonNull
    public Collection<HomekitService> getServices() {
        return services;
    }

    @Override
    public Optional<HomekitService> getService(String serviceType) {
        return services.stream().filter(s -> s.isType(serviceType)).findAny();
    }

    @Override
    public Optional<HomekitService> getPrimaryService() {
        return services.stream().filter(s -> s.isPrimary()).findAny();
    }

    @Override
    public boolean isExtensible() {
        return true;
    }

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
            builder.add("aid", accessoryId);
        }

        return builder.build();
    }

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
            builder.add("aid", accessoryId);
        }

        return builder.build();
    }

    @Override
    public void identify() {
        // No op for virtual accessories
    }

    @Override
    @SuppressWarnings("null")
    public boolean equals(@Nullable Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        HomekitGenericAccessory that = (HomekitGenericAccessory) o;

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
            if (thatServices.get(i) != null && thatServices.get(i) != null
                    && !thisServices.get(i).equals(thatServices.get(i)))
                return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessoryId, services);
    }

    @Override
    public int compareTo(HomekitAccessory other) {
        if (this == other)
            return 0;

        // First compare by accessory ID
        int idCompare = Long.compare(this.accessoryId, other.getAccessoryId());
        if (idCompare != 0)
            return idCompare;

        // Compare services
        HomekitGenericAccessory that = (HomekitGenericAccessory) other;
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

    /**
     * Sets the accessory ID and updates the UID accordingly.
     * 
     * @param accessoryId The new accessory ID
     * @param pairingId The pairing ID to use for the UID
     */
    public void setAccessoryId(long accessoryId, String pairingId) {
        this.accessoryId = accessoryId;
        this.accessoryUID = new HomekitAccessoryUID(pairingId, accessoryId);
        logger.debug("{}Set accessory ID to {} with pairing ID {}", LOG_STATE, accessoryId, pairingId);
    }

    /**
     * Sets the accessory UID directly.
     * 
     * @param accessoryUID The new accessory UID
     */
    public void setAccessoryUID(HomekitAccessoryUID accessoryUID) {
        this.accessoryUID = accessoryUID;
        this.accessoryId = Long.parseLong(accessoryUID.toString().split(":")[3]);
        logger.debug("{}Set accessory UID to {}", LOG_STATE, accessoryUID);
    }

    /**
     * Checks if the accessory has a valid ID and UID.
     * 
     * @return true if both ID and UID are set, false otherwise
     */
    public boolean hasValidId() {
        return accessoryId != null && accessoryUID != null;
    }
}
