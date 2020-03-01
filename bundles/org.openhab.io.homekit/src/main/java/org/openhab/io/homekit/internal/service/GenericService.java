package org.openhab.io.homekit.internal.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.api.Characteristic;
import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.internal.characteristic.GenericCharacteristic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericService implements Service {

    protected static final Logger logger = LoggerFactory.getLogger(GenericService.class);

    private final Accessory accessory;
    private final long instanceId;
    private String type;
    private boolean isHidden;
    private boolean isPrimary;
    private final List<Characteristic> characteristics = new LinkedList<>();

    public GenericService(Accessory accessory, JsonValue value) {
        this.accessory = accessory;
        this.instanceId = ((JsonObject) value).getInt("iid");

        JsonArray characteristicsArray = ((JsonObject) value).getJsonArray("characteristics");
        for (JsonValue characteristicValue : characteristicsArray) {
            characteristics.add(new GenericCharacteristic(this, characteristicValue));
        }
    }

    public GenericService(@NonNull Accessory accessory, long instanceId) {
        this.accessory = accessory;
        this.instanceId = instanceId;
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
    public String getInstanceType() {
        return type;
    }

    @Override
    public Accessory getAccessory() {
        return accessory;
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
    public @NonNull List<Characteristic> getCharacteristics() {
        return Collections.unmodifiableList(characteristics.stream()
                .sorted((o1, o2) -> Long.valueOf(o1.getId()).compareTo(Long.valueOf(o2.getId())))
                .collect(Collectors.toList()));
    }

    @Override
    public Characteristic getCharacteristic(long iid) {
        return characteristics.stream().filter(c -> c.getId() == iid).findFirst().orElse(null);
    }

    @Override
    public Characteristic getCharacteristic(String characteristicType) {
        return characteristics.stream().filter(s -> s.isType(characteristicType) == true).findAny().orElse(null);
    }

    @Override
    public Characteristic getCharacteristic(@NonNull Class<@NonNull ? extends Characteristic> characteristicClass) {
        return characteristics.stream().filter(c -> c.getClass() == characteristicClass).findFirst().get();
    }

    @Override
    public void removeCharacteristic(@NonNull Class<@NonNull ? extends Characteristic> characteristicClass) {
        for (Characteristic characteristic : characteristics.stream().filter(c -> c.getClass() == characteristicClass)
                .collect(Collectors.toList())) {
            characteristics.remove(characteristic);
            // logger.debug("Removed Characteristic {} of Type {} from Service {} of Type {}", characteristic.getUID(),
            // characteristic.getClass().getSimpleName(), this.getUID(), this.getClass().getSimpleName());
        }
    }

    public boolean removeCharacteristic(Characteristic characteristic) {
        return characteristics.remove(characteristic);
    }

    /**
     * The maximum number of characteristics must not exceed 100, and each characteristic in the array must have a
     * unique type.
     *
     * @param characteristic
     */
    @Override
    public void addCharacteristic(Characteristic characteristic) {
        if (getCharacteristic(characteristic.getInstanceType()) == null) {
            characteristics.add(characteristic);
            // logger.debug("Added Characteristic {} of Type {} to Service {} of Type {}", characteristic.getUID(),
            // characteristic.getClass().getSimpleName(), this.getUID(), this.getClass().getSimpleName());
        } else {
            // logger.warn("Service {} of Type {} already holds a Characteristic {} of Type {}", this.getUID(),
            // this.getClass().getSimpleName(), characteristic.getUID(),
            // characteristic.getClass().getSimpleName());

        }
    }

    @Override
    public JsonObject toJson() {
        JsonArrayBuilder characteristics = Json.createArrayBuilder();
        for (Characteristic characteristic : getCharacteristics()) {
            characteristics.add(characteristic.toJson());
        }

        JsonObjectBuilder builder = Json.createObjectBuilder().add("iid", getId()).add("type", getInstanceType())
                .add("characteristics", characteristics);

        return builder.build();
    }

    @Override
    public JsonObject toReducedJson() {
        JsonArrayBuilder characteristics = Json.createArrayBuilder();
        for (Characteristic characteristic : getCharacteristics()) {
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
