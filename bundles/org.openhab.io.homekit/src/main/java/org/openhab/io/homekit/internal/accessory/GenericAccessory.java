package org.openhab.io.homekit.internal.accessory;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.internal.events.AccessoryEvent;
import org.openhab.io.homekit.internal.events.ServiceEvent;
import org.openhab.io.homekit.internal.listeners.AccessoryChangeListener;
import org.openhab.io.homekit.internal.listeners.ServiceChangeListener;
import org.openhab.io.homekit.internal.service.GenericService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericAccessory implements Accessory {

    private final Logger logger = LoggerFactory.getLogger(GenericAccessory.class);

    private final long instanceId;
    private Collection<Service> services = new HashSet<Service>();
    private final AccessoryServer server;
    private final Collection<AccessoryChangeListener> listeners = new CopyOnWriteArraySet<>();

    public GenericAccessory(AccessoryServer server, long instanceId) {
        this.server = server;
        this.instanceId = instanceId;
    }

    public GenericAccessory(AccessoryServer server, JsonValue value) {
        this.server = server;
        this.instanceId = ((JsonObject) value).getInt("aid");

        JsonArray servicesArray = ((JsonObject) value).getJsonArray("services");
        for (JsonValue serviceValue : servicesArray) {
            services.add(new GenericService(this, serviceValue, GenericService.class.getSimpleName()));
        }
    }

    @Override
    public AccessoryUID getUID() {
        return new AccessoryUID(getServer().getId(), Long.toString(getId()));
    }

    @Override
    public long getId() {
        return instanceId;
    }

    @Override
    public String getSerialNumber() {
        return getUID().getId();
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
    public String getModel() {
        return this.getClass().getSimpleName();
    }

    @Override
    public @NonNull AccessoryServer getServer() {
        return server;
    }

    @Override
    public Collection<Service> getServices() {
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
    public JsonObject toJson() {
        JsonArrayBuilder services = Json.createArrayBuilder();

        for (Service service : getServices()) {
            services.add(service.toJson());
        }

        JsonObjectBuilder builder = Json.createObjectBuilder().add("aid", instanceId).add("services", services);

        return builder.build();
    }

    @Override
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
