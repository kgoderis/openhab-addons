package org.openhab.io.homekit.internal.accessory;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
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
import org.openhab.io.homekit.api.listener.ServiceChangeListener;
import org.openhab.io.homekit.internal.events.AccessoryEvent;
import org.openhab.io.homekit.internal.events.ServiceEvent;
import org.openhab.io.homekit.internal.service.GenericService;
import org.openhab.io.homekit.library.service.AccessoryInformationService;
import org.openhab.io.homekit.util.HomekitKeyGenerator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericAccessory implements Accessory {

    private static final Logger logger = LoggerFactory.getLogger(GenericAccessory.class);
    private static ServiceTracker<org.openhab.io.homekit.api.factory.HomekitFactory, org.openhab.io.homekit.api.factory.HomekitFactory> homekitFactoryTracker;

    private final long instanceId;
    private final long accessoryId;
    private final Object instanceIdLock = new Object();
    private final Set<Long> usedInstanceIds = new HashSet<>();
    private long nextInstanceId = 1;
    private final AccessoryServer server;
    private final Collection<AccessoryChangeListener> listeners = new CopyOnWriteArraySet<>();
    private Collection<Service> services = new HashSet<Service>();
        private @NonNull AccessoryUID accessoryUID;
    
        /**
         * Creates a new GenericAccessory with a unique instance ID.
         * The instance ID is automatically assigned and managed to avoid conflicts.
         *
         * @param server The accessory server this accessory belongs to
         */
        public GenericAccessory(AccessoryServer server) {
            this.server = server;
            this.instanceId = getNextAvailableInstanceId();
            this.accessoryId = server.getNextAvailableAccessoryId();
            logger.debug("Created new accessory with instance ID: {}", instanceId);
            
            if (isExtensible()) {
                addServices();
            }
    
            this.accessoryUID = new AccessoryUID(new String(HomekitKeyGenerator.generateHexidecimalId(), StandardCharsets.UTF_8).replace(":", ""), getAccessoryId());
    }

    /**
     * Creates a new GenericAccessory from a JSON value.
     * The instance ID is taken from the JSON data.
     *
     * @param server The accessory server this accessory belongs to
     * @param value The JSON value containing the accessory data
     */
    public GenericAccessory(AccessoryServer server, JsonValue value) {
        this.server = server;
        this.instanceId = getNextAvailableInstanceId();
        this.accessoryId = ((JsonObject) value).getInt("aid");
        logger.debug("Created accessory from JSON with accessory ID: {}", accessoryId);

        JsonArray servicesArray = ((JsonObject) value).getJsonArray("services");
        for (JsonValue serviceValue : servicesArray) {
            Service service = createService(serviceValue);
            if (service != null) {
                services.add(service);
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
    public long getNextAvailableInstanceId() {
        synchronized (instanceIdLock) {
            // First try to find a recycled ID
            for (long id = 1; id < nextInstanceId; id++) {
                if (!usedInstanceIds.contains(id)) {
                    usedInstanceIds.add(id);
                    logger.debug("Recycled instance ID: {} for accessory: {}", id, instanceId);
                    return id;
                }
            }
            
            // If no recycled IDs available, use the next new ID
            long newId = nextInstanceId++;
            usedInstanceIds.add(newId);
            logger.debug("Assigned new instance ID: {} for accessory: {}", newId, instanceId);
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
                logger.debug("Released instance ID: {} from accessory: {}", id, instanceId);
            } else {
                logger.warn("Attempted to release unused instance ID: {} from accessory: {}", id, instanceId);
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
        logger.debug("Cleaned up accessory with instance ID: {}", instanceId);
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
                    if (homekitFactory.isServiceSupported(serviceType)) {
                        Service service = homekitFactory.createService(this, value);
                        if (service != null) {
                            return service;
                        }
                    }
                }
            }
        }
        logger.warn("No HomekitFactory found to create service from JSON value");
        return null;
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
    @NonNull public AccessoryUID getUID() {
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

    @Override
    public @NonNull AccessoryServer getServer() {
        return server;
    }

    @Override
    @NonNull
    public Collection<@NonNull Service> getServices() {
        return services;
    }

    @Override
    public Service getService(String serviceType) {
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
    public void addService(@Nullable Service service) {
        if (service != null && isExtensible()) {
            if (getService(service.getInstanceType()) == null) {
                services.add(service);
                logger.debug("Added Service '{}' (Type: {}) to Accessory '{}' (Type: {})", 
                    service.getName(), service.getInstanceType(), 
                    this.getLabel(), this.getClass().getSimpleName());
                notifyServiceAdded(service);
                
                // Listen for service changes
                if (service instanceof GenericService) {
                    ((GenericService) service).addListener(new ServiceChangeListener() {
                        @Override
                        public void onServiceEvent(ServiceEvent event) {
                            notifyServiceStateChanged(service);
                        }
                    });
                }
            } else {
                logger.debug("Accessory '{}' (Type: {}) already contains Service '{}' (Type: {})", 
                    this.getLabel(), this.getClass().getSimpleName(),
                    service.getName(), service.getInstanceType());
            }
        }
    }

    @Override
    public void addListener(AccessoryChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(AccessoryChangeListener listener) {
        listeners.remove(listener);
    }

    protected void notifyServiceAdded(Service service) {
        logger.debug("Notifying listeners of Service '{}' (Type: {}) addition to Accessory '{}'", 
            service.getName(), service.getInstanceType(), this.getLabel());
        AccessoryEvent event = new AccessoryEvent(this, service, AccessoryEvent.AccessoryEventType.SERVICE_ADDED);
        notifyListeners(event);
    }

    protected void notifyServiceRemoved(Service service) {
        logger.debug("Notifying listeners of Service '{}' (Type: {}) removal from Accessory '{}'", 
            service.getName(), service.getInstanceType(), this.getLabel());
        AccessoryEvent event = new AccessoryEvent(this, service, AccessoryEvent.AccessoryEventType.SERVICE_REMOVED);
        notifyListeners(event);
    }

    protected void notifyServiceStateChanged(Service service) {
        logger.debug("Notifying listeners of Service '{}' (Type: {}) state change in Accessory '{}'", 
            service.getName(), service.getInstanceType(), this.getLabel());
        AccessoryEvent event = new AccessoryEvent(this, service, AccessoryEvent.AccessoryEventType.SERVICE_STATE_CHANGED);
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
        JsonArrayBuilder services = Json.createArrayBuilder();

        for (Service service : getServices()) {
            services.add(service.toJson());
        }

        JsonObjectBuilder builder = Json.createObjectBuilder().add("aid", instanceId).add("services", services);

        return builder.build();
    }

    @Override
    @NonNull
    public JsonObject toReducedJson() {
        JsonArrayBuilder services = Json.createArrayBuilder();

        for (Service service : getServices()) {
            services.add(service.toReducedJson());
        }

        JsonObjectBuilder builder = Json.createObjectBuilder().add("aid", instanceId).add("services", services);

        return builder.build();
    }

    @Override
    public void identify() {
        // TODO No Op?
    }
}
