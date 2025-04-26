package org.openhab.io.homekit.internal.accessory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.AccessoryServer;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.api.listener.AccessoryChangeListener;
import org.openhab.io.homekit.exception.AccessoryOperationException;
import org.openhab.io.homekit.exception.HomekitFactoryException;
import org.openhab.io.homekit.internal.events.AccessoryEvent;
import org.openhab.io.homekit.internal.service.GenericService;
import org.openhab.io.homekit.library.service.AccessoryInformationService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericAccessory implements Accessory {

    private static final Logger logger = LoggerFactory.getLogger(GenericAccessory.class);
    private static ServiceTracker<@NonNull HomekitFactory, @NonNull HomekitFactory> homekitFactoryTracker;

    protected static final String LOG_PREFIX = "HomeKit Accessory: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "Accessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    private final long instanceId = 0;
    private long accessoryId = 0;
    private final Object instanceIdLock = new Object();
    private final Set<Long> usedInstanceIds = new HashSet<>();
    private long nextInstanceId = 1;
    // private final AccessoryServer server;
    private final Collection<AccessoryChangeListener> listeners = new CopyOnWriteArraySet<>();
    private Collection<Service> services = new HashSet<>();
    private AccessoryUID accessoryUID;

    /**
     * Creates a new GenericAccessory with a unique instance ID.
     * The instance ID is automatically assigned and managed to avoid conflicts.
     *
     * @param server The accessory server this accessory belongs to
     */
    public GenericAccessory(AccessoryServer server) {
        // this.server = server;
        try {
            this.accessoryId = server.getNextAvailableAccessoryId();
        } catch (AccessoryOperationException e) {
            logger.error("{}Error getting next available accessory ID: {}", LOG_ERROR, e.getMessage(), e);
            this.accessoryId = 0;
        }
        logger.debug("{}Created new accessory with instance ID: {}", LOG_INIT, instanceId);

        this.accessoryUID = new AccessoryUID(server.getUID().getPairingId(), this.accessoryId);
        initializeServices();
    }

    private void initializeServices() {
        if (isExtensible()) {
            addServices();
        }
    }

    /**
     * Creates a new GenericAccessory from a JSON value.
     * The instance ID is taken from the JSON data.
     *
     * @param server The accessory server this accessory belongs to
     * @param value The JSON value containing the accessory data
     */
    public GenericAccessory(JsonValue value) {
        // this.server = server;
        this.accessoryId = ((JsonObject) value).getInt("aid");
        logger.debug("{}Created accessory from JSON with accessory ID: {}", LOG_INIT, accessoryId);

        initializeServices(value);
    }

    private void initializeServices(JsonValue value) {
        JsonArray servicesArray = ((JsonObject) value).getJsonArray("services");
        for (JsonValue serviceValue : servicesArray) {
            Service service = createService(serviceValue);
            if (service != null) {
                addService(service);
            }
        }
    }

    private Service createService(JsonValue value) {
        if (homekitFactoryTracker == null) {
            BundleContext context = FrameworkUtil.getBundle(GenericAccessory.class).getBundleContext();
            homekitFactoryTracker = new ServiceTracker<>(context, HomekitFactory.class, null);
            homekitFactoryTracker.open();
        }

        Object[] factories = homekitFactoryTracker.getServices();
        if (factories != null) {
            for (Object factory : factories) {
                if (factory instanceof HomekitFactory homekitFactory) {
                    String serviceType = ((JsonObject) value).getString("type");
                    if (homekitFactory.supportsServiceType(serviceType)) {
                        try {
                            Service service = homekitFactory.createService(this, value);
                            if (service != null) {
                                return service;
                            }
                        } catch (HomekitFactoryException e) {
                            logger.error("{}Error creating service: {}", LOG_ERROR, e.getMessage(), e);
                        }
                    }
                }
            }
        }
        logger.warn("{}No HomekitFactory found to create service from JSON value", LOG_WARN);
        return null;
    }

    @Override
    public void addService(@Nullable Service service) {
        if (service != null && isExtensible()) {
            if (getService(service.getInstanceType()) == null) {
                services.add(service);
                logger.debug("{}Added Service '{}' (Type: {}) to Accessory '{}' (Type: {})", LOG_ACCESSORY, 
                    service.getName(), service.getInstanceType(), this.getLabel(), this.getClass().getSimpleName());
                notifyServiceAdded(service);

                // Listen for service changes
                if (service instanceof GenericService genericService) {
                    genericService.addChangeListener(event -> notifyServiceStateChanged(service));
                }
            } else {
                logger.debug("{}Accessory '{}' (Type: {}) already contains Service '{}' (Type: {})", LOG_ACCESSORY, 
                    this.getLabel(), this.getClass().getSimpleName(), service.getName(), service.getInstanceType());
            }
        }
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
                logger.warn("{}Attempted to release unused instance ID: {} from accessory: {}", LOG_WARN, id, instanceId);
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
        listeners.clear();
        logger.debug("{}Cleaned up accessory with instance ID: {}", LOG_STATE, instanceId);
    }

    /**
     * Adds default services to the accessory. Subclasses can override this method
     * to provide additional services.
     */
    @Override
    public void addServices() {
        addService(new AccessoryInformationService(this, getNextAvailableInstanceId(), true, getLabel()));
    }

    @Override
    @NonNull
    public AccessoryUID getUID() {
        return accessoryUID;
    }

    @Override
    public long getAccessoryId() {
        return accessoryId;
    }

    @Override
    @NonNull
    public String getSerialNumber() {
        return getUID().getAsString();
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
    // public @NonNull AccessoryServer getServer() {
    // return server;
    // }

    @Override
    @NonNull
    public Collection<@NonNull Service> getServices() {
        return services;
    }

    @Override
    public Service getService(@NonNull String serviceType) {
        return services.stream().filter(s -> s.isType(serviceType) == true).findAny().orElse(null);
    }

    @Override
    public @NonNull Service getPrimaryService() {
        return services.stream().filter(s -> s.isPrimary()).findAny().get();
    }

    @Override
    public boolean isExtensible() {
        return true;
    }

    @Override
    public void addChangeListener(@NonNull AccessoryChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeChangeListener(@NonNull AccessoryChangeListener listener) {
        listeners.remove(listener);
    }

    protected void notifyServiceAdded(Service service) {
        logger.debug("{}Notifying listeners of Service '{}' (Type: {}) addition to Accessory '{}'", LOG_ACCESSORY, 
            service.getName(), service.getInstanceType(), this.getLabel());
        AccessoryEvent event = new AccessoryEvent(this, service, AccessoryEvent.AccessoryEventType.SERVICE_ADDED);
        notifyListeners(event);
    }

    protected void notifyServiceRemoved(Service service) {
        logger.debug("{}Notifying listeners of Service '{}' (Type: {}) removal from Accessory '{}'", LOG_ACCESSORY, 
            service.getName(), service.getInstanceType(), this.getLabel());
        AccessoryEvent event = new AccessoryEvent(this, service, AccessoryEvent.AccessoryEventType.SERVICE_REMOVED);
        notifyListeners(event);
    }

    protected void notifyServiceStateChanged(Service service) {
        logger.debug("{}Notifying listeners of Service '{}' (Type: {}) state change in Accessory '{}'", LOG_ACCESSORY, 
            service.getName(), service.getInstanceType(), this.getLabel());
        AccessoryEvent event = new AccessoryEvent(this, service,
                AccessoryEvent.AccessoryEventType.SERVICE_STATE_CHANGED);
        notifyListeners(event);
    }

    private void notifyListeners(AccessoryEvent event) {
        for (AccessoryChangeListener listener : listeners) {
            try {
                listener.onAccessoryEvent(event);
            } catch (Exception e) {
                logger.error("Error notifying listener of accessory event", e);
            }
        }
    }

    @Override
    @NonNull
    public JsonObject toJson() {
        JsonArrayBuilder jsonServices = Json.createArrayBuilder();

        for (Service service : getServices()) {
            jsonServices.add(service.toJson());
        }

        JsonObjectBuilder builder = Json.createObjectBuilder().add("aid", accessoryId).add("services", jsonServices);

        return builder.build();
    }

    @Override
    @NonNull
    public JsonObject toReducedJson() {
        JsonArrayBuilder jsonServices = Json.createArrayBuilder();

        for (Service service : getServices()) {
            jsonServices.add(service.toReducedJson());
        }

        JsonObjectBuilder builder = Json.createObjectBuilder().add("aid", accessoryId).add("services", jsonServices);

        return builder.build();
    }

    @Override
    public void identify() {
        // No op for virtual accessories
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        GenericAccessory that = (GenericAccessory) o;

        // Compare accessory ID
        if (accessoryId != that.accessoryId)
            return false;

        // Compare services
        List<Service> thisServices = new ArrayList<>(this.services);
        List<Service> thatServices = new ArrayList<>(that.services);

        // Sort both lists by instance ID for consistent comparison
        thisServices.sort((s1, s2) -> Long.compare(s1.getInstanceId(), s2.getInstanceId()));
        thatServices.sort((s1, s2) -> Long.compare(s1.getInstanceId(), s2.getInstanceId()));

        // Compare sizes
        if (thisServices.size() != thatServices.size())
            return false;

        // Compare each service
        for (int i = 0; i < thisServices.size(); i++) {
            if (!thisServices.get(i).equals(thatServices.get(i)))
                return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessoryId, services);
    }

    @Override
    public int compareTo(@NonNull Accessory other) {
        if (this == other)
            return 0;

        // First compare by accessory ID
        int idCompare = Long.compare(this.accessoryId, other.getAccessoryId());
        if (idCompare != 0)
            return idCompare;

        // Compare services
        GenericAccessory that = (GenericAccessory) other;
        List<Service> thisServices = new ArrayList<>(this.services);
        List<Service> thatServices = new ArrayList<>(that.services);

        // Sort both lists by instance ID for consistent comparison
        thisServices.sort((s1, s2) -> Long.compare(s1.getInstanceId(), s2.getInstanceId()));
        thatServices.sort((s1, s2) -> Long.compare(s1.getInstanceId(), s2.getInstanceId()));

        // Compare sizes first
        int sizeCompare = Integer.compare(thisServices.size(), thatServices.size());
        if (sizeCompare != 0)
            return sizeCompare;

        // Compare each service
        for (int i = 0; i < thisServices.size(); i++) {
            int serviceCompare = thisServices.get(i).compareTo(thatServices.get(i));
            if (serviceCompare != 0)
                return serviceCompare;
        }

        return 0;
    }

    @Override
    public void removeService(@NonNull Service service) {
        if (services.remove(service)) {
            logger.debug("{}Removed Service '{}' (Type: {}) from Accessory '{}' (Type: {})", LOG_ACCESSORY, 
                service.getName(), service.getInstanceType(), this.getLabel(), this.getClass().getSimpleName());
            notifyServiceRemoved(service);
        }
    }
}
