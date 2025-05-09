package org.openhab.io.homekit.core.service;

import java.util.ArrayList;
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
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.event.HomekitEvent;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitBaseCharacteristic;
import org.openhab.io.homekit.event.model.characteristic.HomekitCharacteristicEvent;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.event.core.HomekitEventSubscription;
import org.openhab.io.homekit.event.model.service.HomekitServiceEvent;
import org.openhab.io.homekit.exception.HomekitFactoryException;
import org.openhab.io.homekit.library.characteristic.HomekitServiceNameCharacteristic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class HomekitBaseService implements HomekitService {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitBaseService.class);

    private final HomekitAccessory accessory;
    private final long instanceId;
    private final String name;
    private String type = "";
    private boolean isHidden;
    private boolean isPrimary;
    private final List<HomekitCharacteristic<?>> characteristics = new LinkedList<>();
    private final boolean isExtensible;
    protected final HomekitEventManager eventManager;
    protected final Collection<HomekitFactory> factories;

    private final Set<HomekitEventSubscription> eventSubscriptions = new HashSet<>();

    public HomekitBaseService(HomekitAccessory accessory, long instanceId, boolean extend, String serviceName, String type,
            HomekitEventManager eventManager, Collection<HomekitFactory> factories) {
        this.accessory = accessory;
        this.instanceId = instanceId;
        this.name = serviceName;
        this.type = type;
        this.isExtensible = extend;
        this.eventManager = eventManager;
        this.factories = factories;

        initialise();
    }

    public HomekitBaseService(HomekitAccessory accessory, JsonValue value, String name, HomekitEventManager eventManager,
            Collection<HomekitFactory> factories) {
        this.accessory = accessory;
        this.instanceId = ((JsonObject) value).getInt("iid");
        this.type = ((JsonObject) value).getString("type");
        this.name = name;
        this.isExtensible = false; // Not extensible when created from JSON
        this.eventManager = eventManager;
        this.factories = factories;

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
        HomekitCharacteristic<?> nameCharacteristic = getCharacteristic(HomekitServiceNameCharacteristic.class).orElse(null);
        if (nameCharacteristic != null) {
            try {
                ((HomekitServiceNameCharacteristic) nameCharacteristic).setValue(name);
            } catch (Exception e) {
                logger.error("Error setting name characteristic value", e);
            }
        }
    }

    @Override
    public void addCharacteristics() {
        addCharacteristic(new HomekitServiceNameCharacteristic(this, getAccessory().getNextAvailableInstanceId(), eventManager));
    }

    private Optional<HomekitCharacteristic<?>> createCharacteristic(JsonValue value) {
        for (HomekitFactory factory : factories) {
            String characteristicType = ((JsonObject) value).getString("type");
            if (factory.supportsCharacteristicsType(characteristicType)) {
                try {
                    HomekitCharacteristic<?> characteristic = factory.createCharacteristic(this, value);
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
    public HomekitServiceUID getUID() {
        return new HomekitServiceUID(getAccessory().getUID().getPairingId(), getAccessory().getAccessoryId(), getInstanceId());
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
    public HomekitAccessory getAccessory() {
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
    public Set<HomekitCharacteristic<?>> getCharacteristics() {
        Set<HomekitCharacteristic<?>> set = characteristics.stream().filter(Objects::nonNull)
                .sorted((o1, o2) -> Long.compare(o1.getInstanceId(), o2.getInstanceId()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(set);
    }

    @Override
    public Optional<HomekitCharacteristic<?>> getCharacteristic(long iid) {
        return characteristics.stream().filter(c -> c.getInstanceId() == iid).findFirst();
    }

    @Override
    public Optional<HomekitCharacteristic<?>> getCharacteristic(String characteristicType) {
        return characteristics.stream().filter(s -> s.isType(characteristicType)).findAny();
    }

    @Override
    public Optional<HomekitCharacteristic<?>> getCharacteristic(Class<? extends HomekitCharacteristic<?>> characteristicClass) {
        return characteristics.stream().filter(c -> c.getClass() == characteristicClass).findFirst();
    }

    @Override
    @SuppressWarnings("unused")
    public void removeCharacteristic(Class<? extends HomekitCharacteristic<?>> characteristicClass) {
        // Create a copy to avoid ConcurrentModificationException
        List<HomekitCharacteristic<?>> toRemove = new ArrayList<>();
        for (HomekitCharacteristic<?> characteristic : characteristics) {
            if (characteristic != null && characteristic.getClass() == characteristicClass) {
                toRemove.add(characteristic);
            }
        }
        for (HomekitCharacteristic<?> characteristic : toRemove) {
            if (characteristic == null) {
                continue;
            }
            removeCharacteristic(characteristic);
        }
    }

    @Override
    public void removeCharacteristic(HomekitCharacteristic<?> characteristic) {
        boolean removed = characteristics.remove(characteristic);
        if (removed) {
            if (characteristic != null) {
                String description = characteristic instanceof HomekitBaseCharacteristic ? characteristic.getDescription()
                        : characteristic.getInstanceType();
                eventSubscriptions
                        .removeIf(subscription -> subscription.getPublisherUID().equals(characteristic.getUID()));
                logger.debug("Removed HomekitCharacteristic '{}' (Type: {}) from HomekitService '{}' (Type: {})", description,
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
    public void addCharacteristic(@NonNull HomekitCharacteristic<?> characteristic) {
        if (getCharacteristic(characteristic.getInstanceType()).isEmpty() && isExtensible()) {
            characteristics.add(characteristic);
            logger.debug("Added HomekitCharacteristic '{}' (Type: {}) to HomekitService '{}' (Type: {})",
                    characteristic.getDescription(), characteristic.getInstanceType(), this.getName(),
                    this.getInstanceType());
            notifyCharacteristicAdded(characteristic);

            if (characteristic instanceof HomekitBaseCharacteristic) {
                eventSubscriptions.add(eventManager.subscribe(HomekitEventType.CHARACTERISTIC_STATE_CHANGED,
                        characteristic.getUID(), getUID(), event -> {
                            onEvent(event);
                        }));
            } else {
                logger.warn("HomekitCharacteristic '{}' (Type: {}) is not a HomekitEventPublisher",
                        characteristic.getDescription(), characteristic.getInstanceType());
            }

        } else {
            logger.debug("HomekitService '{}' (Type: {}) already contains HomekitCharacteristic '{}' (Type: {})", this.getName(),
                    this.getInstanceType(), characteristic.getDescription(), characteristic.getInstanceType());
        }
    }

    public void onEvent(HomekitEvent event) {
        if (event instanceof HomekitCharacteristicEvent characteristicEvent) {
            notifyCharacteristicStateChanged(characteristicEvent.getCharacteristic().get());
        }
    }

    @Override
    public JsonObject toJson() {
        JsonArrayBuilder jsonCharacteristics = Json.createArrayBuilder();
        for (HomekitCharacteristic<?> characteristic : getCharacteristics()) {
            jsonCharacteristics.add(characteristic.toJson());
        }

        JsonObjectBuilder builder = Json.createObjectBuilder().add("iid", getInstanceId())
                .add("type", getInstanceType()).add("characteristics", jsonCharacteristics);

        return builder.build();
    }

    @Override
    public JsonObject toReducedJson() {
        JsonArrayBuilder jsonCharacteristics = Json.createArrayBuilder();
        for (HomekitCharacteristic<?> characteristic : getCharacteristics()) {
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
    public Collection<HomekitService> getLinkedServices() {
        return new HashSet<>();
    }

    protected void notifyCharacteristicAdded(HomekitCharacteristic<?> characteristic) {
        logger.debug("Notifying listeners of HomekitCharacteristic '{}' (Type: {}) addition to HomekitService '{}'",
                characteristic.getDescription(), characteristic.getInstanceType(), this.getName());
        HomekitServiceEvent event = new HomekitServiceEvent(HomekitEventType.CHARACTERISTIC_ADDED, this, characteristic);
        eventManager.publishEvent(event);
    }

    protected void notifyCharacteristicRemoved(HomekitCharacteristic<?> characteristic) {
        logger.debug("Notifying listeners of HomekitCharacteristic '{}' (Type: {}) removal from HomekitService '{}'",
                characteristic.getDescription(), characteristic.getInstanceType(), this.getName());
        HomekitServiceEvent event = new HomekitServiceEvent(HomekitEventType.CHARACTERISTIC_REMOVED, this, characteristic);
        eventManager.publishEvent(event);
    }

    protected void notifyCharacteristicStateChanged(HomekitCharacteristic<?> characteristic) {
        logger.debug("Notifying listeners of HomekitCharacteristic '{}' (Type: {}) state change in HomekitService '{}'",
                characteristic.getDescription(), characteristic.getInstanceType(), this.getName());
        HomekitServiceEvent event = new HomekitServiceEvent(HomekitEventType.CHARACTERISTIC_STATE_CHANGED, this, characteristic);
        eventManager.publishEvent(event);
    }

    @SuppressWarnings("null")
    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        HomekitBaseService that = (HomekitBaseService) obj;

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
        List<HomekitCharacteristic<?>> thisChars = new ArrayList<>(this.characteristics);
        List<HomekitCharacteristic<?>> thatChars = new ArrayList<>(that.characteristics);

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
    public int compareTo(@Nullable HomekitService other) {
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
        HomekitBaseService that = (HomekitBaseService) other;
        List<HomekitCharacteristic<?>> thisChars = new ArrayList<>(this.characteristics);
        List<HomekitCharacteristic<?>> thatChars = new ArrayList<>(that.characteristics);

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
