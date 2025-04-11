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
import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.Characteristic;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.api.listener.CharacteristicChangeListener;
import org.openhab.io.homekit.api.listener.ServiceChangeListener;
import org.openhab.io.homekit.internal.characteristic.GenericCharacteristic;
import org.openhab.io.homekit.internal.events.CharacteristicEvent;
import org.openhab.io.homekit.internal.events.ServiceEvent;
import org.openhab.io.homekit.library.characteristic.ServiceNameCharacteristic;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericService implements Service {

    protected static final Logger logger = LoggerFactory.getLogger(GenericService.class);
    private static ServiceTracker<org.openhab.io.homekit.api.factory.HomekitFactory, org.openhab.io.homekit.api.factory.HomekitFactory> homekitFactoryTracker;

    private final Accessory accessory;
    private final long instanceId;
    private final String name;
    private String type = "";
    private boolean isHidden;
    private boolean isPrimary;
    private final List<Characteristic<?>> characteristics = new LinkedList<>();
    private final Collection<ServiceChangeListener> listeners = new CopyOnWriteArraySet<>();
    private final boolean isExtensible;

    public GenericService(@NonNull Accessory accessory, long instanceId, boolean extend, String name) {
        this.accessory = accessory;
        this.instanceId = instanceId;
        this.name = name;
        this.isExtensible = extend;

        if (isExtensible()) {
            addCharacteristics();
        }

        Characteristic<?> nameCharacteristic = getCharacteristic(ServiceNameCharacteristic.class);
        if (nameCharacteristic != null) {
            try {
                ((ServiceNameCharacteristic) nameCharacteristic).setValue(name);
            } catch (Exception e) {
                logger.error("Error setting name characteristic value", e);
            }
        }
    }

    public GenericService(Accessory accessory, JsonValue value, String name) {
        this.accessory = accessory;
        this.instanceId = ((JsonObject) value).getInt("iid");
        this.type = ((JsonObject) value).getString("type");
        this.name = name;
        this.isExtensible = false; // Not extensible when created from JSON

        JsonArray characteristicsArray = ((JsonObject) value).getJsonArray("characteristics");
        for (JsonValue characteristicValue : characteristicsArray) {
            Characteristic<?> characteristic = createCharacteristic(characteristicValue);
            if (characteristic != null) {
                characteristics.add(characteristic);
            }
        }
    }

    @Override
    public void addCharacteristics() {
        addCharacteristic(new ServiceNameCharacteristic(this, getAccessory().getAccessoryId()));
    }

    private Characteristic<?> createCharacteristic(JsonValue value) {
        if (homekitFactoryTracker == null) {
            BundleContext context = FrameworkUtil.getBundle(GenericService.class).getBundleContext();
            homekitFactoryTracker = new ServiceTracker<>(context, HomekitFactory.class, null);
            homekitFactoryTracker.open();
        }

        Object[] factories = homekitFactoryTracker.getServices();
        if (factories != null) {
            for (Object factory : factories) {
                if (factory instanceof HomekitFactory homekitFactory) {
                    String characteristicType = ((JsonObject) value).getString("type");
                    if (homekitFactory.isCharacteristicSupported(characteristicType)) {
                        Characteristic<?> characteristic = homekitFactory.createCharacteristic(this, value);
                        if (characteristic != null) {
                            return characteristic;
                        }
                    }
                }
            }
        }
        logger.warn("No HomekitFactory found to create characteristic from JSON value");
        return null;
    }

    @Override
    @NonNull
    public ServiceUID getUID() {
        return new ServiceUID(getAccessory().getUID().getHexId(), getAccessory().getAccessoryId(), getInstanceId());
    }

    @Override
    public long getInstanceId() {
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
    public @NonNull List<Characteristic<?>> getCharacteristics() {
        return Collections.unmodifiableList(characteristics.stream()
                .sorted((o1, o2) -> Long.valueOf(o1.getId()).compareTo(Long.valueOf(o2.getId())))
                .collect(Collectors.toList()));
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
    public Characteristic<?> getCharacteristic(@NonNull Class<@NonNull ? extends Characteristic> characteristicClass) {
        return characteristics.stream().filter(c -> c.getClass() == characteristicClass).findFirst().get();
    }

    @Override
    public void removeCharacteristic(@NonNull Class<@NonNull ? extends Characteristic> characteristicClass) {
        for (Characteristic<?> characteristic : characteristics.stream()
                .filter(c -> c.getClass() == characteristicClass).collect(Collectors.toList())) {
            characteristics.remove(characteristic);
            String description = characteristic instanceof GenericCharacteristic ? characteristic.getDescription()
                    : characteristic.getInstanceType();
            logger.debug("Removed Characteristic '{}' (Type: {}) from Service '{}' (Type: {})", description,
                    characteristic.getInstanceType(), this.getName(), this.getInstanceType());
            notifyCharacteristicRemoved(characteristic);
        }
    }

    public boolean removeCharacteristic(Characteristic<?> characteristic) {
        boolean removed = characteristics.remove(characteristic);
        if (removed) {
            String description = characteristic instanceof GenericCharacteristic ? characteristic.getDescription()
                    : characteristic.getInstanceType();
            logger.debug("Removed Characteristic '{}' (Type: {}) from Service '{}' (Type: {})", description,
                    characteristic.getInstanceType(), this.getName(), this.getInstanceType());
            notifyCharacteristicRemoved(characteristic);
        }
        return removed;
    }

    @Override
    public boolean isExtensible() {
        return isExtensible;
    }

    /**
     * The maximum number of characteristics must not exceed 100, and each characteristic in the array must have a
     * unique type.
     *
     * @param characteristic
     */
    @Override
    public void addCharacteristic(Characteristic<?> characteristic) {
        if (getCharacteristic(characteristic.getInstanceType()) == null && isExtensible()) {
            characteristics.add(characteristic);
            logger.debug("Added Characteristic '{}' (Type: {}) to Service '{}' (Type: {})",
                    characteristic.getDescription(), characteristic.getInstanceType(), this.getName(),
                    this.getInstanceType());
            notifyCharacteristicAdded(characteristic);

            // Listen for characteristic value changes

            characteristic.addChangeListener(new CharacteristicChangeListener() {
                @Override
                public void onCharacteristicEvent(CharacteristicEvent event) {
                    notifyCharacteristicStateChanged(characteristic);
                }
            });

        } else {
            logger.debug("Service '{}' (Type: {}) already contains Characteristic '{}' (Type: {})", this.getName(),
                    this.getInstanceType(), characteristic.getDescription(), characteristic.getInstanceType());
        }
    }

    @Override
    public JsonObject toJson() {
        JsonArrayBuilder characteristics = Json.createArrayBuilder();
        for (Characteristic<?> characteristic : getCharacteristics()) {
            characteristics.add(characteristic.toJson());
        }

        JsonObjectBuilder builder = Json.createObjectBuilder().add("iid", getInstanceId())
                .add("type", getInstanceType()).add("characteristics", characteristics);

        return builder.build();
    }

    @Override
    public JsonObject toReducedJson() {
        JsonArrayBuilder characteristics = Json.createArrayBuilder();
        for (Characteristic<?> characteristic : getCharacteristics()) {
            characteristics.add(characteristic.toReducedJson());
        }

        JsonObjectBuilder builder = Json.createObjectBuilder().add("iid", getInstanceId())
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

    @Override
    public void addChangeListener(ServiceChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeChangeListener(ServiceChangeListener listener) {
        listeners.remove(listener);
    }

    protected void notifyCharacteristicAdded(Characteristic characteristic) {
        logger.debug("Notifying listeners of Characteristic '{}' (Type: {}) addition to Service '{}'",
                characteristic.getDescription(), characteristic.getInstanceType(), this.getName());
        ServiceEvent event = new ServiceEvent(this, characteristic, ServiceEvent.ServiceEventType.CHARACTERISTIC_ADDED);
        notifyListeners(event);
    }

    protected void notifyCharacteristicRemoved(Characteristic characteristic) {
        logger.debug("Notifying listeners of Characteristic '{}' (Type: {}) removal from Service '{}'",
                characteristic.getDescription(), characteristic.getInstanceType(), this.getName());
        ServiceEvent event = new ServiceEvent(this, characteristic,
                ServiceEvent.ServiceEventType.CHARACTERISTIC_REMOVED);
        notifyListeners(event);
    }

    protected void notifyCharacteristicStateChanged(Characteristic characteristic) {
        logger.debug("Notifying listeners of Characteristic '{}' (Type: {}) state change in Service '{}'",
                characteristic.getDescription(), characteristic.getInstanceType(), this.getName());
        ServiceEvent event = new ServiceEvent(this, characteristic,
                ServiceEvent.ServiceEventType.CHARACTERISTIC_STATE_CHANGED);
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
