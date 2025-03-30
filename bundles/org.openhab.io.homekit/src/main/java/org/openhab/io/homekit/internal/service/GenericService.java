package org.openhab.io.homekit.internal.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
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
import org.openhab.io.homekit.internal.events.ServiceEvent;
import org.openhab.io.homekit.internal.events.CharacteristicEvent;
import org.openhab.io.homekit.internal.listeners.ServiceChangeListener;
import org.openhab.io.homekit.internal.listeners.CharacteristicChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericService implements Service {

    protected static final Logger logger = LoggerFactory.getLogger(GenericService.class);

    private final Accessory accessory;
    private final long instanceId;
    private final String name;
    private String type;
    private boolean isHidden;
    private boolean isPrimary;
    private final List<Characteristic> characteristics = new LinkedList<>();
    private final Collection<ServiceChangeListener> listeners = new CopyOnWriteArraySet<>();

    public GenericService(Accessory accessory, JsonValue value, String name) {
        this.accessory = accessory;
        this.instanceId = ((JsonObject) value).getInt("iid");
        this.type = ((JsonObject) value).getString("type");
        this.name = name;

        JsonArray characteristicsArray = ((JsonObject) value).getJsonArray("characteristics");
        for (JsonValue characteristicValue : characteristicsArray) {
            characteristics.add(new GenericCharacteristic(this, characteristicValue));
        }
    }

    public GenericService(@NonNull Accessory accessory, long instanceId, String name) {
        this.accessory = accessory;
        this.instanceId = instanceId;
        this.name = name;
    }

    @Override
    public ServiceUID getUID() {
        return new ServiceUID(getAccessory().getServer().getId(), Long.toString(getAccessory().getId()),
                Long.toString(getId()));
    }

    @Override
    public long getId() {
        return instanceId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isType(String aType) {
        return getInstanceType().equals(aType);
    }

    @Override
    public String getInstanceType() {
        if (type.length() == 2) {
            return String.format("%0" + (8 - type.length()) + "d%s", 0, type) + "-0000-1000-8000-0026BB765291";
        } else {
            return type;
        }
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
            notifyCharacteristicRemoved(characteristic);
        }
    }

    public boolean removeCharacteristic(Characteristic characteristic) {
        boolean removed = characteristics.remove(characteristic);
        if (removed) {
            notifyCharacteristicRemoved(characteristic);
        }
        return removed;
    }

    @Override
    public boolean isExtensible() {
        return true;
    }

    /**
     * The maximum number of characteristics must not exceed 100, and each characteristic in the array must have a
     * unique type.
     *
     * @param characteristic
     */
    @Override
    public void addCharacteristic(Characteristic characteristic) {
        if (getCharacteristic(characteristic.getInstanceType()) == null && isExtensible()) {
            characteristics.add(characteristic);
            notifyCharacteristicAdded(characteristic);
            
            // Listen for characteristic value changes
            if (characteristic instanceof GenericCharacteristic) {
                ((GenericCharacteristic) characteristic).addListener(new CharacteristicChangeListener() {
                    @Override
                    public void onCharacteristicEvent(CharacteristicEvent event) {
                        notifyCharacteristicStateChanged(characteristic);
                    }
                });
            }
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

    public void addListener(ServiceChangeListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ServiceChangeListener listener) {
        listeners.remove(listener);
    }

    protected void notifyCharacteristicAdded(Characteristic characteristic) {
        ServiceEvent event = new ServiceEvent(this, characteristic, ServiceEvent.ServiceEventType.CHARACTERISTIC_ADDED);
        notifyListeners(event);
    }

    protected void notifyCharacteristicRemoved(Characteristic characteristic) {
        ServiceEvent event = new ServiceEvent(this, characteristic, ServiceEvent.ServiceEventType.CHARACTERISTIC_REMOVED);
        notifyListeners(event);
    }

    protected void notifyCharacteristicStateChanged(Characteristic characteristic) {
        ServiceEvent event = new ServiceEvent(this, characteristic, ServiceEvent.ServiceEventType.CHARACTERISTIC_STATE_CHANGED);
        notifyListeners(event);
    }

    private void notifyListeners(ServiceEvent event) {
        for (ServiceChangeListener listener : listeners) {
            try {
                listener.onServiceEvent(event);
            } catch (Exception e) {
                logger.error("Error notifying listener of service event", e);
            }
        }
    }
}
