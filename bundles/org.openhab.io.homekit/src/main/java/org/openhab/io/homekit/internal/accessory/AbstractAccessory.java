package org.openhab.io.homekit.internal.accessory;

import java.util.Collection;
import java.util.HashSet;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.library.service.AccessoryInformationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractAccessory implements Accessory {

    private final Logger logger = LoggerFactory.getLogger(AbstractAccessory.class);

    protected final HomekitCommunicationManager manager;

    private final AccessoryServer server;
    private final long instanceId;
    private long instanceIdPool = 1;

    private Collection<Service> services = new HashSet<Service>();

    public AbstractAccessory(HomekitCommunicationManager manager, AccessoryServer server, long instanceId,
            boolean extend) throws Exception {
        super();
        this.manager = manager;
        this.server = server;
        this.instanceId = instanceId;

        if (extend) {
            addServices();
        }
    }

    @Override
    public void addServices() throws Exception {
        addService(new AccessoryInformationService(manager, this, getInstanceId(), true, getLabel()));
    }

    @Override
    public AccessoryUID getUID() {
        return new AccessoryUID(server.getId(), Long.toString(instanceId));
    }

    @Override
    public long getId() {
        return instanceId;
    }

    @Override
    public AccessoryServer getServer() {
        return server;
    }

    @Override
    public long getInstanceId() {
        return instanceIdPool++;
    }

    @Override
    public long getCurrentInstanceId() {
        return instanceIdPool;
    }

    @Override
    public String getSerialNumber() {
        return getUID().getId();
    }

    @Override
    public String getModel() {
        return this.getClass().getSimpleName();
    }

    @Override
    public void identify() {
    }

    @Override
    public boolean isExtensible() {
        return false;
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
                logger.debug("Added Service {} of Type {} to Accessory {} of Type {}", service.getUID(),
                        service.getClass().getSimpleName(), this.getUID(), this.getClass().getSimpleName());
            } else {
                logger.debug("Accessory {} of Type {} already holds a Service {} of Type {} to ", this.getUID(),
                        this.getClass().getSimpleName(), service.getUID(), service.getClass().getSimpleName());
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
