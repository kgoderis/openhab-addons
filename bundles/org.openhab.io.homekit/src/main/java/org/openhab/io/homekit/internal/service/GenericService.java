package org.openhab.io.homekit.internal.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.Characteristic;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.exception.HomekitFactoryException;
import org.openhab.io.homekit.internal.characteristic.GenericCharacteristic;
import org.openhab.io.homekit.internal.events.CharacteristicEvent;
import org.openhab.io.homekit.internal.events.HomekitEvent;
import org.openhab.io.homekit.internal.events.HomekitEventManager;
import org.openhab.io.homekit.internal.events.HomekitEventPublisher;
import org.openhab.io.homekit.internal.events.HomekitEventSubscriber;
import org.openhab.io.homekit.internal.events.HomekitEventType;
import org.openhab.io.homekit.internal.events.ServiceEvent;
import org.openhab.io.homekit.library.characteristic.ServiceNameCharacteristic;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class GenericService implements Service, HomekitEventPublisher, HomekitEventSubscriber {

    protected static final Logger logger = LoggerFactory.getLogger(GenericService.class);
    @Nullable
    private static ServiceTracker<HomekitFactory, HomekitFactory> homekitFactoryTracker;
    @Nullable
    private static ServiceTracker<HomekitEventManager, HomekitEventManager> eventManagerTracker;

    private final Accessory accessory;
    private final long instanceId;
    private final String name;
    private String type = "";
    private boolean isHidden;
    private boolean isPrimary;
    private final List<Characteristic<?>> characteristics = new LinkedList<>();
    private final boolean isExtensible;

    public GenericService(Accessory accessory, long instanceId, boolean extend, String name) {
        this.accessory = accessory;
        this.instanceId = instanceId;
        this.name = name;
        this.isExtensible = extend;
        initialise();
    }

    public GenericService(Accessory accessory, JsonValue value, String name) {
        this.accessory = accessory;
        this.instanceId = ((JsonObject) value).getInt("iid");
        this.type = ((JsonObject) value).getString("type");
        this.name = name;
        this.isExtensible = false; // Not extensible when created from JSON

        JsonArray characteristicsArray = ((JsonObject) value).getJsonArray("characteristics");
        for (JsonValue characteristicValue : characteristicsArray) {
            createCharacteristic(characteristicValue).ifPresent(this::addCharacteristic);
        }
    }

    /**
     * Call this after construction to perform any initialisation that requires overridable methods.
     */
    @SuppressWarnings("null")
    final public void initialise() {
        if (isExtensible()) {
            addCharacteristics();
        }
        @Nullable
        Characteristic<?> nameCharacteristic = getCharacteristic(ServiceNameCharacteristic.class).orElse(null);
        if (nameCharacteristic != null) {
            try {
                ((ServiceNameCharacteristic) nameCharacteristic).setValue(name);
            } catch (Exception e) {
                logger.error("Error setting name characteristic value", e);
            }
        }
    }

    @Override
    @SuppressWarnings("unused")
    public HomekitEventManager getEventManager() {
        @Nullable
        ServiceTracker<HomekitEventManager, HomekitEventManager> tracker = getEventManagerTracker();
        if (tracker != null) {
            @Nullable
            HomekitEventManager manager = tracker.getService();
            if (manager != null) {
                return manager;
            }
        }
        throw new IllegalStateException("HomekitEventManager service is not available");
    }

    @SuppressWarnings("null")
    private static ServiceTracker<HomekitEventManager, HomekitEventManager> getEventManagerTracker() {
        if (eventManagerTracker == null) {
            BundleContext context = FrameworkUtil.getBundle(GenericService.class).getBundleContext();
            eventManagerTracker = new ServiceTracker<>(context, HomekitEventManager.class, null);
            eventManagerTracker.open();
        }

        return eventManagerTracker;
    }

    @SuppressWarnings("null")
    protected static Set<HomekitFactory> getHomekitFactories() {
        ServiceTracker<HomekitFactory, HomekitFactory> tracker = getHomekitFactoryTracker();
        if (tracker != null && tracker.getServices() != null) {
            return Arrays.stream(tracker.getServices()).filter(HomekitFactory.class::isInstance)
                    .map(HomekitFactory.class::cast).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    @Nullable
    private static ServiceTracker<HomekitFactory, HomekitFactory> getHomekitFactoryTracker() {
        if (homekitFactoryTracker == null) {
            BundleContext context = FrameworkUtil.getBundle(GenericService.class).getBundleContext();
            homekitFactoryTracker = new ServiceTracker<>(context, HomekitFactory.class, null);
            homekitFactoryTracker.open();
        }
        return homekitFactoryTracker;
    }

    @Override
    public String getSourceUID() {
        return getUID().toString();
    }

    @Override
    public void addCharacteristics() {
        addCharacteristic(new ServiceNameCharacteristic(this, getAccessory().getAccessoryId()));
    }

    private Optional<Characteristic<?>> createCharacteristic(JsonValue value) {
        for (HomekitFactory factory : getHomekitFactories()) {
            String characteristicType = ((JsonObject) value).getString("type");
            if (factory.supportsCharacteristicsType(characteristicType)) {
                try {
                    Characteristic<?> characteristic = factory.createCharacteristic(this, value);
                    if (characteristic != null) {
                        return Optional.of(characteristic);
                    }
                } catch (HomekitFactoryException e) {
                    logger.error("Error creating characteristic: {}", e.getMessage());
                    return Optional.empty();
                }
            }
        }
        logger.warn("No HomekitFactory found to create characteristic from JSON value");
        return Optional.empty();
    }

    @Override
    @NonNull
    public ServiceUID getUID() {
        return new ServiceUID(getAccessory().getUID().getPairingId(), getAccessory().getAccessoryId(), getInstanceId());
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
    public Set<Characteristic<?>> getCharacteristics() {
        Set<Characteristic<?>> set = characteristics.stream().filter(Objects::nonNull)
                .sorted((o1, o2) -> Long.compare(o1.getInstanceId(), o2.getInstanceId()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(set);
    }

    @Override
    public Optional<Characteristic<?>> getCharacteristic(long iid) {
        return characteristics.stream().filter(c -> c.getInstanceId() == iid).findFirst();
    }

    @Override
    public Optional<Characteristic<?>> getCharacteristic(String characteristicType) {
        return characteristics.stream().filter(s -> s.isType(characteristicType)).findAny();
    }

    @Override
    public Optional<Characteristic<?>> getCharacteristic(Class<? extends Characteristic<?>> characteristicClass) {
        return characteristics.stream().filter(c -> c.getClass() == characteristicClass).findFirst();
    }

    @Override
    @SuppressWarnings("unused")
    public void removeCharacteristic(Class<? extends Characteristic<?>> characteristicClass) {
        // Create a copy to avoid ConcurrentModificationException
        List<Characteristic<?>> toRemove = new ArrayList<>();
        for (Characteristic<?> characteristic : characteristics) {
            if (characteristic != null && characteristic.getClass() == characteristicClass) {
                toRemove.add(characteristic);
            }
        }
        for (Characteristic<?> characteristic : toRemove) {
            if (characteristic == null) {
                continue;
            }
            characteristics.remove(characteristic);
            String description = characteristic instanceof GenericCharacteristic ? characteristic.getDescription()
                    : characteristic.getInstanceType();
            logger.debug("Removed Characteristic '{}' (Type: {}) from Service '{}' (Type: {})", description,
                    characteristic.getInstanceType(), this.getName(), this.getInstanceType());
            notifyCharacteristicRemoved(characteristic);
        }
    }

    @Override
    public void removeCharacteristic(Characteristic<?> characteristic) {
        boolean removed = characteristics.remove(characteristic);
        if (removed) {
            if (characteristic != null) {
                String description = characteristic instanceof GenericCharacteristic ? characteristic.getDescription()
                        : characteristic.getInstanceType();
                logger.debug("Removed Characteristic '{}' (Type: {}) from Service '{}' (Type: {})", description,
                        characteristic.getInstanceType(), this.getName(), this.getInstanceType());
                notifyCharacteristicRemoved(characteristic);
            }
        }
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
        if (getCharacteristic(characteristic.getInstanceType()).isEmpty() && isExtensible()) {
            characteristics.add(characteristic);
            logger.debug("Added Characteristic '{}' (Type: {}) to Service '{}' (Type: {})",
                    characteristic.getDescription(), characteristic.getInstanceType(), this.getName(),
                    this.getInstanceType());
            notifyCharacteristicAdded(characteristic);

            if (characteristic instanceof GenericCharacteristic) {
                String sourceUID = ((GenericCharacteristic<?>) characteristic).getSourceUID();
                getEventManager().subscribe(HomekitEventType.CHARACTERISTIC_STATE_CHANGED, sourceUID, this);
            } else {
                logger.warn("Characteristic '{}' (Type: {}) is not a HomekitEventPublisher",
                        characteristic.getDescription(), characteristic.getInstanceType());
            }

        } else {
            logger.debug("Service '{}' (Type: {}) already contains Characteristic '{}' (Type: {})", this.getName(),
                    this.getInstanceType(), characteristic.getDescription(), characteristic.getInstanceType());
        }
    }

    @Override
    public void onEvent(HomekitEvent event) {
        if (event instanceof CharacteristicEvent characteristicEvent) {
            notifyCharacteristicStateChanged(characteristicEvent.getCharacteristic());
        }
    }

    @Override
    public void onEventError(HomekitEvent event, Exception e) {
        logger.error("Error processing event: {}", e.getMessage());
    }

    @Override
    public JsonObject toJson() {
        JsonArrayBuilder jsonCharacteristics = Json.createArrayBuilder();
        for (Characteristic<?> characteristic : getCharacteristics()) {
            jsonCharacteristics.add(characteristic.toJson());
        }

        JsonObjectBuilder builder = Json.createObjectBuilder().add("iid", getInstanceId())
                .add("type", getInstanceType()).add("characteristics", jsonCharacteristics);

        return builder.build();
    }

    @Override
    public JsonObject toReducedJson() {
        JsonArrayBuilder jsonCharacteristics = Json.createArrayBuilder();
        for (Characteristic<?> characteristic : getCharacteristics()) {
            jsonCharacteristics.add(characteristic.toReducedJson());
        }

        JsonObjectBuilder builder = Json.createObjectBuilder().add("iid", getInstanceId())
                .add("type", getInstanceType().replaceAll("^0*([0-9a-fA-F]+)-0000-1000-8000-0026BB765291$", "$1"))
                .add("characteristics", jsonCharacteristics);

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
        return new HashSet<>();
    }

    protected void notifyCharacteristicAdded(Characteristic<?> characteristic) {
        logger.debug("Notifying listeners of Characteristic '{}' (Type: {}) addition to Service '{}'",
                characteristic.getDescription(), characteristic.getInstanceType(), this.getName());
        ServiceEvent event = new ServiceEvent(HomekitEventType.CHARACTERISTIC_ADDED, this, characteristic);
        getEventManager().publishEvent(event);
    }

    protected void notifyCharacteristicRemoved(Characteristic<?> characteristic) {
        logger.debug("Notifying listeners of Characteristic '{}' (Type: {}) removal from Service '{}'",
                characteristic.getDescription(), characteristic.getInstanceType(), this.getName());
        ServiceEvent event = new ServiceEvent(HomekitEventType.CHARACTERISTIC_REMOVED, this, characteristic);
        getEventManager().publishEvent(event);
    }

    protected void notifyCharacteristicStateChanged(Characteristic<?> characteristic) {
        logger.debug("Notifying listeners of Characteristic '{}' (Type: {}) state change in Service '{}'",
                characteristic.getDescription(), characteristic.getInstanceType(), this.getName());
        ServiceEvent event = new ServiceEvent(HomekitEventType.CHARACTERISTIC_STATE_CHANGED, this, characteristic);
        getEventManager().publishEvent(event);
    }

    @SuppressWarnings("null")
    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        GenericService that = (GenericService) obj;

        // Compare basic fields
        if (instanceId != that.instanceId)
            return false;
        if (isHidden != that.isHidden)
            return false;
        if (isPrimary != that.isPrimary)
            return false;
        if (!getInstanceType().equals(that.getInstanceType()))
            return false;

        // Compare characteristics
        List<Characteristic<?>> thisChars = new ArrayList<>(this.characteristics);
        List<Characteristic<?>> thatChars = new ArrayList<>(that.characteristics);

        // Sort both lists by instance ID for consistent comparison
        thisChars.sort((c1, c2) -> Long.compare(c1.getInstanceId(), c2.getInstanceId()));
        thatChars.sort((c1, c2) -> Long.compare(c1.getInstanceId(), c2.getInstanceId()));

        // Compare sizes
        if (thisChars.size() != thatChars.size())
            return false;

        // Compare each characteristic
        for (int i = 0; i < thisChars.size(); i++) {
            if (!thisChars.get(i).equals(thatChars.get(i)))
                return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(instanceId, getInstanceType(), isHidden, isPrimary, characteristics);
    }

    @SuppressWarnings("null")
    @Override
    public int compareTo(@Nullable Service other) {
        if (other == null)
            return 1;
        if (this == other)
            return 0;

        // Compare by instance ID
        int idCompare = Long.compare(this.instanceId, other.getInstanceId());
        if (idCompare != 0)
            return idCompare;

        // Compare by instance type
        int typeCompare = this.getInstanceType().compareTo(other.getInstanceType());
        if (typeCompare != 0)
            return typeCompare;

        // Compare by isHidden
        int hiddenCompare = Boolean.compare(this.isHidden, other.isHidden());
        if (hiddenCompare != 0)
            return hiddenCompare;

        // Compare by isPrimary
        int primaryCompare = Boolean.compare(this.isPrimary, other.isPrimary());
        if (primaryCompare != 0)
            return primaryCompare;

        // Compare characteristics
        GenericService that = (GenericService) other;
        List<Characteristic<?>> thisChars = new ArrayList<>(this.characteristics);
        List<Characteristic<?>> thatChars = new ArrayList<>(that.characteristics);

        // Sort both lists by instance ID for consistent comparison
        thisChars.sort((c1, c2) -> Long.compare(c1.getInstanceId(), c2.getInstanceId()));
        thatChars.sort((c1, c2) -> Long.compare(c1.getInstanceId(), c2.getInstanceId()));

        // Compare sizes first
        int sizeCompare = Integer.compare(thisChars.size(), thatChars.size());
        if (sizeCompare != 0)
            return sizeCompare;

        // Compare each characteristic
        for (int i = 0; i < thisChars.size(); i++) {
            int charCompare = thisChars.get(i).compareTo(thatChars.get(i));
            if (charCompare != 0)
                return charCompare;
        }

        return 0;
    }
}
