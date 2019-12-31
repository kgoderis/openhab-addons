package org.openhab.io.homekit.internal.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.api.Characteristic;
import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.hap.accessories.BatteryStatusAccessory;
import org.openhab.io.homekit.library.characteristic.NameCharacteristic;
import org.openhab.io.homekit.library.characteristic.StatusLowBatteryCharacteristic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractService implements Service {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractService.class);

    protected final HomekitCommunicationManager manager;
    private final String name;
    private final Accessory accessory;
    private final long instanceId;
    // private final String type;
    private boolean isHidden;
    private boolean isPrimary;
    private final List<Characteristic<?>> characteristics = new LinkedList<>();

    /**
     * Creates a new instance of this class with the specified UUID and {@link Accessory}.
     * Download and install <i>HomeKit Accessory Simulator</i> to discover the corresponding UUID for
     * the specific service.
     *
     * <p>
     * The new service will automatically add {@link Name} characteristic. If the accessory is
     * battery operated then it must implement {@link BatteryStatusAccessory} and {@link
     * StatusLowBatteryCharacteristic} will be added too.
     *
     * @param type unique UUID of the service. This information can be obtained from HomeKit Accessory
     *            Simulator.
     * @param accessory HomeKit accessory exposed as a service.
     * @param name name of the service. This information is usually the name of the accessory.
     * @throws Exception
     */
    public AbstractService(@NonNull HomekitCommunicationManager manager, @NonNull Accessory accessory, long instanceId,
            boolean extend, String name) throws Exception {
        this.manager = manager;
        this.accessory = accessory;
        this.name = name;
        this.instanceId = instanceId;

        if (extend) {
            addCharacteristics();
        }

        // If battery operated accessory then add LowBatteryStatusAccessory
        // if (accessory instanceof BatteryStatusAccessory) {
        // addCharacteristic(new StatusLowBatteryCharacteristic(manager, (BatteryService) this));
        // }
    }

    @Override
    public void addCharacteristics() throws Exception {
        addCharacteristic(new NameCharacteristic(manager, this, this.getAccessory().getInstanceId()));
    }

    // public AbstractService(@NonNull HomekitCommunicationManager manager, @NonNull Accessory accessory, String name) {
    // this(manager, accessory, name, accessory.getInstanceId());
    // }

    @Override
    public ServiceUID getUID() {
        return new ServiceUID(accessory.getServer().getId(), Long.toString(accessory.getId()),
                Long.toString(instanceId));
    }

    @Override
    public long getId() {
        return instanceId;
    }

    @Override
    public boolean isType(String aType) {
        return getInstanceType().equals(aType);
    }

    @Override
    public abstract String getInstanceType();

    @Override
    public Accessory getAccessory() {
        return accessory;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isHidden() {
        if (characteristics.stream().allMatch(c -> c.isHidden())) {
            return true;
        } else {
            return isHidden;
        }
    }

    @Override
    public boolean isExtensible() {
        return false;
    }

    @Override
    public @NonNull List<Characteristic<?>> getCharacteristics() {
        return Collections.unmodifiableList(characteristics.stream()
                .sorted((o1, o2) -> new Long(o1.getId()).compareTo(new Long(o2.getId()))).collect(Collectors.toList()));
    }

    @Override
    public Characteristic<?> getCharacteristic(long iid) {
        return characteristics.stream().filter(c -> c.getId() == iid).findFirst().orElse(null);
    }

    @Override
    public Characteristic<?> getCharacteristic(String characteristicType) {
        return characteristics.stream().filter(s -> s.isType(characteristicType) == true).findAny().orElse(null);
    }

    @Override
    public Characteristic<?> getCharacteristic(
            @NonNull Class<@NonNull ? extends Characteristic<?>> characteristicClass) {
        return characteristics.stream().filter(c -> c.getClass() == characteristicClass).findFirst().get();
    }

    @Override
    public void removeCharacteristic(@NonNull Class<@NonNull ? extends Characteristic<?>> characteristicClass) {
        for (Characteristic<?> characteristic : characteristics.stream()
                .filter(c -> c.getClass() == characteristicClass).collect(Collectors.toList())) {
            characteristics.remove(characteristic);
            logger.debug("Removed Characteristic {} of Type {} from Service {} of Type {}", characteristic.getUID(),
                    characteristic.getClass().getSimpleName(), this.getUID(), this.getClass().getSimpleName());
        }
    }

    /**
     * The maximum number of characteristics must not exceed 100, and each characteristic in the array must have a
     * unique type.
     *
     * @param characteristic
     */
    @Override
    public void addCharacteristic(Characteristic<?> characteristic) {
        if (getCharacteristic(characteristic.getInstanceType()) == null) {
            characteristics.add(characteristic);
            logger.debug("Added Characteristic {} of Type {} to Service {} of Type {}", characteristic.getUID(),
                    characteristic.getClass().getSimpleName(), this.getUID(), this.getClass().getSimpleName());
        } else {
            logger.warn("Service {} of Type {} already holds a Characteristic {} of Type {}", this.getUID(),
                    this.getClass().getSimpleName(), characteristic.getUID(),
                    characteristic.getClass().getSimpleName());

        }
    }

    @Override
    public JsonObject toJson() {
        JsonArrayBuilder characteristics = Json.createArrayBuilder();
        for (Characteristic<?> characteristic : getCharacteristics()) {
            characteristics.add(characteristic.toJson());
        }

        JsonObjectBuilder builder = Json.createObjectBuilder().add("iid", getId()).add("type", getInstanceType())
                .add("characteristics", characteristics);

        return builder.build();
    }

    @Override
    public JsonObject toReducedJson() {
        JsonArrayBuilder characteristics = Json.createArrayBuilder();
        for (Characteristic<?> characteristic : getCharacteristics()) {
            characteristics.add(characteristic.toReducedJson());
        }

        JsonObjectBuilder builder = Json.createObjectBuilder().add("iid", getId())
                .add("type", getInstanceType().replaceAll("^0*([0-9a-fA-F]+)-0000-1000-8000-0026BB765291$", "$1"))
                .add("characteristics", characteristics);

        return builder.build();
    }

    @Override
    public boolean isPrimary() {
        return isPrimary;
    }

    @Override
    public void setPrimary(boolean isPrimary) {
        this.isPrimary = isPrimary;
    }

    @Override
    public Collection<Service> getLinkedServices() {
        return new HashSet<Service>();
    }

}
