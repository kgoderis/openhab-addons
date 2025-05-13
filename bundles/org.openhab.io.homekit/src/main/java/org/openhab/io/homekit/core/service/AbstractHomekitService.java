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
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.characteristic.AbstractHomekitCharacteristic;
import org.openhab.io.homekit.event.core.HomekitEventSubscription;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.event.model.characteristic.HomekitCharacteristicEvent;
import org.openhab.io.homekit.event.model.service.HomekitServiceEvent;
import org.openhab.io.homekit.library.characteristic.HomekitNameCharacteristic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public abstract class AbstractHomekitService implements HomekitService {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractHomekitService.class);

    private final HomekitAccessory accessory;
    private long instanceId;
    private String name;
    private boolean isHidden;
    private boolean isPrimary;
    private final List<HomekitCharacteristic<?>> characteristics;
    private boolean isExtensible;
    protected final HomekitEventManager eventManager;
    protected final HomekitCharacteristicFactory characteristicFactory;
    private final Set<HomekitEventSubscription> eventSubscriptions;

    /**
     * Creates a new HomekitBaseService with required parameters.
     * 
     * @param accessory the accessory this service belongs to
     * @param eventManager the event manager for handling events
     * @param characteristicFactory the factory for creating characteristics
     * @throws IllegalArgumentException if any required parameter is null
     */
    public AbstractHomekitService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        if (accessory == null) {
            throw new IllegalArgumentException("Accessory cannot be null");
        }
        if (eventManager == null) {
            throw new IllegalArgumentException("EventManager cannot be null");
        }
        if (characteristicFactory == null) {
            throw new IllegalArgumentException("CharacteristicFactory cannot be null");
        }

        this.accessory = accessory;
        this.eventManager = eventManager;
        this.characteristicFactory = characteristicFactory;
        this.characteristics = new LinkedList<>();
        this.eventSubscriptions = new HashSet<>();
    }

    /**
     * Creates a new HomekitBaseService from a JSON value.
     * 
     * @param accessory the accessory this service belongs to
     * @param eventManager the event manager for handling events
     * @param characteristicFactory the factory for creating characteristics
     * @param value the JSON value containing service configuration
     * @throws IllegalArgumentException if the JSON value is invalid or required parameters are null
     */
    public AbstractHomekitService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {

        if (accessory == null) {
            throw new IllegalArgumentException("Accessory cannot be null");
        }
        if (eventManager == null) {
            throw new IllegalArgumentException("EventManager cannot be null");
        }
        if (characteristicFactory == null) {
            throw new IllegalArgumentException("CharacteristicFactory cannot be null");
        }
        if (value == null || !value.getValueType().equals(JsonValue.ValueType.OBJECT)) {
            throw new IllegalArgumentException("Invalid JSON value for service creation");
        }

        String jsonType = ((JsonObject) value).getString("type");
        if (!getType().equals(jsonType)) {
            throw new IllegalArgumentException("Invalid JSON value for characteristic creation");
        }

        JsonObject jsonObject = (JsonObject) value;
        this.accessory = accessory;
        this.eventManager = eventManager;
        this.characteristicFactory = characteristicFactory;
        this.characteristics = new LinkedList<>();
        this.eventSubscriptions = new HashSet<>();

        this.name = jsonObject.getString("name");
        this.instanceId = jsonObject.getInt("iid");

        JsonArray characteristicsArray = jsonObject.getJsonArray("characteristics");
        for (JsonValue characteristicValue : characteristicsArray) {
            createCharacteristic(characteristicValue).ifPresent(this::addCharacteristic);
        }
    }

    /**
     * Initializes the service after construction.
     * This method should be called after construction to perform any initialization that requires overridable methods.
     * It adds characteristics if the service is extensible and sets the name characteristic if present.
     */
    final public void initialise() {
        if (isExtensible()) {
            addCharacteristics();
        }

        @Nullable
        HomekitCharacteristic<?> nameCharacteristic = getCharacteristic(HomekitNameCharacteristic.class)
                .orElse(null);
        if (nameCharacteristic != null) {
            try {
                ((HomekitNameCharacteristic) nameCharacteristic).setValue(name);
            } catch (Exception e) {
                logger.error("Error setting name characteristic value", e);
            }
        }
    }

    /**
     * Adds default characteristics to this service.
     * This method is called during initialization if the service is extensible.
     */
    @Override
    public void addCharacteristics() {
        addCharacteristic(
                new HomekitNameCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId()));
    }

    /**
     * Gets the unique identifier for this service.
     * 
     * @return the HomekitServiceUID for this service
     */
    @Override
    @NonNull
    public HomekitServiceUID getUID() {
        return new HomekitServiceUID(getAccessory().getUID().getPairingId(), getAccessory().getAccessoryId(),
                getInstanceId());
    }

    /**
     * Gets the instance ID of this service.
     * 
     * @return the instance ID
     */
    @Override
    public long getInstanceId() {
        return instanceId;
    }

    /**
     * Gets the name of this service.
     * 
     * @return the service name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Gets the accessory this service belongs to.
     * 
     * @return the HomekitAccessory instance
     */
    @Override
    public HomekitAccessory getAccessory() {
        return accessory;
    }

    /**
     * Gets the type of this service.
     * 
     * @return the service type
     * @throws IllegalStateException if the class is not properly annotated
     */
    @Override
    final public String getType() {
        HomekitServiceType annotation = getClass().getAnnotation(HomekitServiceType.class);
        if (annotation == null) {
            throw new IllegalStateException(
                    "Service class " + getClass().getName() + " must be annotated with @HomekitServiceType");
        }

        String type = annotation.type();

        if (type.length() == 2) {
            return String.format("%0" + (8 - type.length()) + "d%s", 0, type) + "-0000-1000-8000-0026BB765291";
        } else {
            return type;
        }
    }

    /**
     * Gets the tag for this service.
     * 
     * @return the service tag
     */
    @Override
    final public String getTag() {
        HomekitServiceType annotation = getClass().getAnnotation(HomekitServiceType.class);
        return annotation != null ? annotation.tag() : getType();
    }

    /**
     * Checks if this service is of the specified type.
     * 
     * @param aType the type to check against
     * @return true if this service is of the specified type
     */
    @Override
    public boolean isType(String aType) {
        return getType().equals(aType);
    }

    /**
     * Checks if this service is hidden.
     * A service is considered hidden if all its characteristics are hidden or if it is explicitly marked as hidden.
     * 
     * @return true if this service is hidden
     */
    @Override
    public boolean isHidden() {
        if (characteristics.stream().allMatch(c -> c.isHidden())) {
            return true;
        } else {
            return isHidden;
        }
    }

    /**
     * Checks if this service is extensible.
     * 
     * @return true if this service is extensible
     */
    @Override
    public boolean isExtensible() {
        return isExtensible;
    }

    /**
     * Checks if this service is primary.
     * 
     * @return true if this service is primary
     */
    @Override
    public boolean isPrimary() {
        return isPrimary;
    }

    /**
     * Gets the services linked to this service.
     * 
     * @return a collection of linked services
     */
    @Override
    public Collection<HomekitService> getLinkedServices() {
        return new HashSet<>();
    }

    /**
     * Adds a characteristic to this service.
     * The characteristic is only added if the service is extensible and doesn't already contain a characteristic of the
     * same type.
     * 
     * @param characteristic the characteristic to add
     */
    @Override
    public void addCharacteristic(@NonNull HomekitCharacteristic<?> characteristic) {
        if (getCharacteristic(characteristic.getType()).isEmpty() && isExtensible()) {
            characteristics.add(characteristic);
            logger.debug("Added HomekitCharacteristic '{}' (Type: {}) to HomekitService '{}' (Type: {})",
                    characteristic.getDescription(), characteristic.getType(), this.getName(), this.getType());
            notifyCharacteristicAdded(characteristic);

            if (characteristic instanceof AbstractHomekitCharacteristic) {
                eventSubscriptions.add(eventManager.subscribe(HomekitEventType.CHARACTERISTIC_STATE_CHANGED,
                        characteristic.getUID(), getUID(), event -> {
                            onEvent(event);
                        }));
            } else {
                logger.warn("HomekitCharacteristic '{}' (Type: {}) is not a HomekitEventPublisher",
                        characteristic.getDescription(), characteristic.getType());
            }

        } else {
            logger.debug("HomekitService '{}' (Type: {}) already contains HomekitCharacteristic '{}' (Type: {})",
                    this.getName(), this.getType(), characteristic.getDescription(), characteristic.getType());
        }
    }

    /**
     * Creates a characteristic from a JSON value.
     * 
     * @param value the JSON value containing characteristic configuration
     * @return an Optional containing the created characteristic, or empty if creation failed
     */
    private Optional<HomekitCharacteristic<?>> createCharacteristic(JsonValue value) {
        String characteristicType = ((JsonObject) value).getString("type");
        if (characteristicFactory.supportsCharacteristicType(characteristicType)) {
            try {
                HomekitCharacteristic<?> characteristic = characteristicFactory.createCharacteristic(characteristicType,
                        this, eventManager);
                if (characteristic != null) {
                    return Optional.of(characteristic);
                }
            } catch (IllegalArgumentException e) {
                logger.error("Error creating characteristic: {}", e.getMessage());
                return Optional.empty();
            }
        }
        logger.warn("No HomekitCharacteristicFactory found to create characteristic from JSON value");
        return Optional.empty();
    }

    /**
     * Removes a characteristic from this service.
     * 
     * @param characteristic the characteristic to remove
     */
    @Override
    public void removeCharacteristic(HomekitCharacteristic<?> characteristic) {
        if (characteristics.remove(characteristic)) {
            logger.debug("Removed HomekitCharacteristic '{}' (Type: {}) from HomekitService '{}' (Type: {})",
                    characteristic.getDescription(), characteristic.getType(), this.getName(), this.getType());
            notifyCharacteristicRemoved(characteristic);
        }
    }

    /**
     * Removes a characteristic of the specified class from this service.
     * 
     * @param characteristicClass the class of the characteristic to remove
     */
    @Override
    public void removeCharacteristic(Class<? extends HomekitCharacteristic<?>> characteristicClass) {
        characteristics.stream().filter(c -> c.getClass() == characteristicClass).findFirst()
                .ifPresent(this::removeCharacteristic);
    }

    /**
     * Gets all characteristics of this service.
     * 
     * @return an unmodifiable set of characteristics, sorted by instance ID
     */
    @Override
    public Set<HomekitCharacteristic<?>> getCharacteristics() {
        Set<HomekitCharacteristic<?>> set = characteristics.stream().filter(Objects::nonNull)
                .sorted((o1, o2) -> Long.compare(o1.getInstanceId(), o2.getInstanceId()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(set);
    }

    /**
     * Gets a characteristic by its instance ID.
     * 
     * @param iid the instance ID to look up
     * @return an Optional containing the characteristic if found
     */
    @Override
    public Optional<HomekitCharacteristic<?>> getCharacteristic(long iid) {
        return characteristics.stream().filter(c -> c.getInstanceId() == iid).findFirst();
    }

    /**
     * Gets a characteristic by its type.
     * 
     * @param characteristicType the type to look up
     * @return an Optional containing the characteristic if found
     */
    @Override
    public Optional<HomekitCharacteristic<?>> getCharacteristic(String characteristicType) {
        return characteristics.stream().filter(s -> s.isType(characteristicType)).findAny();
    }

    /**
     * Gets a characteristic by its class.
     * 
     * @param characteristicClass the class to look up
     * @return an Optional containing the characteristic if found
     */
    @Override
    public Optional<HomekitCharacteristic<?>> getCharacteristic(
            Class<? extends HomekitCharacteristic<?>> characteristicClass) {
        return characteristics.stream().filter(c -> c.getClass() == characteristicClass).findFirst();
    }

    /**
     * Converts this service to a JSON object.
     * 
     * @return the JSON representation of this service
     */
    @Override
    public JsonObject toJson() {
        JsonArrayBuilder jsonCharacteristics = Json.createArrayBuilder();
        for (HomekitCharacteristic<?> characteristic : getCharacteristics()) {
            jsonCharacteristics.add(characteristic.toJson());
        }

        JsonObjectBuilder builder = Json.createObjectBuilder().add("iid", getInstanceId()).add("type", getType())
                .add("characteristics", jsonCharacteristics);

        return builder.build();
    }

    /**
     * Converts this service to a reduced JSON object.
     * The reduced format uses a shorter type representation.
     * 
     * @return the reduced JSON representation of this service
     */
    @Override
    public JsonObject toReducedJson() {
        JsonArrayBuilder jsonCharacteristics = Json.createArrayBuilder();
        for (HomekitCharacteristic<?> characteristic : getCharacteristics()) {
            jsonCharacteristics.add(characteristic.toReducedJson());
        }

        JsonObjectBuilder builder = Json.createObjectBuilder().add("iid", getInstanceId())
                .add("type", getType().replaceAll("^0*([0-9a-fA-F]+)-0000-1000-8000-0026BB765291$", "$1"))
                .add("characteristics", jsonCharacteristics);

        return builder.build();
    }

    /**
     * Notifies listeners that a characteristic has been added to this service.
     * 
     * @param characteristic the characteristic that was added
     */
    protected void notifyCharacteristicAdded(HomekitCharacteristic<?> characteristic) {
        logger.debug("Notifying listeners of HomekitCharacteristic '{}' (Type: {}) addition to HomekitService '{}'",
                characteristic.getDescription(), characteristic.getType(), this.getName());
        HomekitServiceEvent event = new HomekitServiceEvent(HomekitEventType.CHARACTERISTIC_ADDED, this,
                characteristic);
        eventManager.publishEvent(event);
    }

    /**
     * Notifies listeners that a characteristic has been removed from this service.
     * 
     * @param characteristic the characteristic that was removed
     */
    protected void notifyCharacteristicRemoved(HomekitCharacteristic<?> characteristic) {
        logger.debug("Notifying listeners of HomekitCharacteristic '{}' (Type: {}) removal from HomekitService '{}'",
                characteristic.getDescription(), characteristic.getType(), this.getName());
        HomekitServiceEvent event = new HomekitServiceEvent(HomekitEventType.CHARACTERISTIC_REMOVED, this,
                characteristic);
        eventManager.publishEvent(event);
    }

    /**
     * Notifies listeners that a characteristic's state has changed.
     * 
     * @param characteristic the characteristic whose state changed
     */
    protected void notifyCharacteristicStateChanged(HomekitCharacteristic<?> characteristic) {
        logger.debug("Notifying listeners of HomekitCharacteristic '{}' (Type: {}) state change in HomekitService '{}'",
                characteristic.getDescription(), characteristic.getType(), this.getName());
        HomekitServiceEvent event = new HomekitServiceEvent(HomekitEventType.CHARACTERISTIC_STATE_CHANGED, this,
                characteristic);
        eventManager.publishEvent(event);
    }

    /**
     * Handles events related to this service.
     * 
     * @param event the event to handle
     */
    public void onEvent(HomekitEvent event) {
        if (event instanceof HomekitCharacteristicEvent characteristicEvent) {
            notifyCharacteristicStateChanged(characteristicEvent.getCharacteristic().get());
        }
    }

    /**
     * Sets the instance ID for this service.
     * 
     * @param instanceId the instance ID to set
     * @return this instance
     */
    @Override
    public AbstractHomekitService withInstanceId(long instanceId) {
        this.instanceId = instanceId;
        return this;
    }

    /**
     * Sets the name for this service.
     * 
     * @param name the name to set
     * @return this instance
     */
    @Override
    public AbstractHomekitService withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets whether this service is extensible.
     * 
     * @param isExtensible whether the service is extensible
     * @return this instance
     */
    @Override
    public AbstractHomekitService withExtensible(boolean isExtensible) {
        this.isExtensible = isExtensible;
        return this;
    }

    /**
     * Sets whether this service is primary.
     * 
     * @param isPrimary whether the service is primary
     * @return this instance
     */
    @Override
    public AbstractHomekitService withPrimary(boolean isPrimary) {
        this.isPrimary = isPrimary;
        return this;
    }

    /**
     * Sets whether this service is hidden.
     * 
     * @param isHidden whether the service is hidden
     * @return this instance
     */
    @Override
    public AbstractHomekitService withHidden(boolean isHidden) {
        this.isHidden = isHidden;
        return this;
    }

    /**
     * Compares this service with another object for equality.
     * 
     * @param obj the object to compare with
     * @return true if the objects are equal
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        AbstractHomekitService that = (AbstractHomekitService) obj;

        // Compare basic fields
        if (instanceId != that.instanceId)
            return false;
        if (isHidden != that.isHidden)
            return false;
        if (isPrimary != that.isPrimary)
            return false;
        if (!getType().equals(that.getType()))
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

    /**
     * Generates a hash code for this service.
     * 
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(instanceId, getType(), isHidden, isPrimary, characteristics);
    }

    /**
     * Compares this service with another service.
     * 
     * @param other the service to compare with
     * @return a negative integer, zero, or a positive integer as this service is less than, equal to, or greater than
     *         the specified service
     */
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
        int typeCompare = this.getType().compareTo(other.getType());
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
        AbstractHomekitService that = (AbstractHomekitService) other;
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
