package org.openhab.io.homekit.internal.accessory;

import java.util.Collection;
import java.util.HashSet;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.internal.service.GenericService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericAccessory implements Accessory {

    private final Logger logger = LoggerFactory.getLogger(GenericAccessory.class);

    private final long instanceId;
    private Collection<Service> services = new HashSet<Service>();

    public GenericAccessory(long instanceId) {
        this.instanceId = instanceId;
    }

    public GenericAccessory(JsonValue value) {
        this.instanceId = ((JsonObject) value).getInt("aid");

        JsonArray servicesArray = ((JsonObject) value).getJsonArray("services");
        for (JsonValue serviceValue : servicesArray) {
            services.add(new GenericService(this, serviceValue));
        }
    }

    @Override
    public long getId() {
        return instanceId;
    }

    @Override
    public Collection<Service> getServices() {
        return services;
    }

    @Override
    public @NonNull Service getPrimaryService() {
        return services.stream().filter(s -> s.isPrimary()).findAny().get();
    }

    @Override
    public Service getService(String serviceType) {
        return services.stream().filter(s -> s.isType(serviceType) == true).findAny().orElse(null);
    }

    @Override
    public void addService(@Nullable Service service) {
        if (service != null) {
            if (getService(service.getInstanceType()) == null) {
                services.add(service);
                // logger.debug("Added Service {} of Type {} to Accessory {} of Type {}", service.getUID(),
                // service.getClass().getSimpleName(), this.getUID(), this.getClass().getSimpleName());
            } else {
                // logger.debug("Accessory {} of Type {} already holds a Service {} of Type {} to ", this.getUID(),
                // this.getClass().getSimpleName(), service.getUID(), service.getClass().getSimpleName());
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

}
