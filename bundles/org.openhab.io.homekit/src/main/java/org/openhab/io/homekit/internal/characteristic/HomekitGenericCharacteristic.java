package org.openhab.io.homekit.internal.characteristic;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.hap.HomekitCharacteristic;
import org.openhab.io.homekit.api.hap.HomekitService;
import org.openhab.io.homekit.internal.events.AbstractHomekitEvent;
import org.openhab.io.homekit.internal.events.HomekitCharacteristicChangeValueEvent;
import org.openhab.io.homekit.internal.events.HomekitCharacteristicValueChangedEvent;
import org.openhab.io.homekit.internal.events.HomekitEventMetadata;
import org.openhab.io.homekit.internal.events.HomekitEventManager;
import org.openhab.io.homekit.internal.events.HomekitEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public abstract class HomekitGenericCharacteristic<@NonNull T> implements HomekitCharacteristic<@NonNull T> {

    private static final Logger logger = LoggerFactory.getLogger(HomekitGenericCharacteristic.class);

    protected static final String LOG_PREFIX = "HomeKit HomekitCharacteristic: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_CHARACTERISTIC = LOG_PREFIX + "HomekitCharacteristic - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    // Instance fields - final
    private final HomekitService service;
    private final long instanceId;
    private String format;
    private String description;

    // Instance fields - mutable
    private boolean isWritable = false;
    private boolean isReadable = false;
    private boolean isHidden = false;
    private boolean hasEvents = false;
    private String type;
    protected @Nullable T value = null;
    protected @Nullable JsonValue initialValue = null;
    private HomekitEventManager eventManager;

    // Constructors
    public HomekitGenericCharacteristic(HomekitService service, JsonValue value, HomekitEventManager eventManager) {
        this.service = service;
        this.initialValue = value;
        this.eventManager = eventManager;
        this.instanceId = ((JsonObject) value).getInt("iid");
        this.type = ((JsonObject) value).getString("type");
        this.format = ((JsonObject) value).getString("format");

        if (((JsonObject) value).containsKey("description")) {
            this.description = ((JsonObject) value).getString("description");
        } else {
            this.description = "";
        }

        if (((JsonObject) value).containsKey("perms")) {
            JsonArray perms = ((JsonObject) value).getJsonArray("perms");
            for (JsonValue perm : perms) {
                String permString = ((JsonString) perm).getString();
                switch (permString) {
                    case "pw" -> this.isWritable = true;
                    case "pr" -> this.isReadable = true;
                    case "ev" -> this.hasEvents = true;
                }
            }
        }

        if (((JsonObject) value).containsKey("ev")) {
            this.hasEvents = ((JsonObject) value).getBoolean("ev");
        }

        setupSubscription();
    }

    public HomekitGenericCharacteristic(HomekitService service, long instanceId, String format, boolean isWritable,
            boolean isReadable, boolean hasEvents, String description, String type, HomekitEventManager eventManager) {
        this.service = service;
        this.instanceId = instanceId;
        this.format = format;
        this.isWritable = isWritable;
        this.isReadable = isReadable;
        this.hasEvents = hasEvents;
        this.description = description;
        this.type = format;
        this.eventManager = eventManager;

        setupSubscription();
    }

    /**
     * Initializes the value after construction. This must be called by subclasses
     * after calling super() in their constructors.
     */
    public void initializeValue() {
        if (initialValue != null) {
            this.value = toValue(initialValue);
            initialValue = null;
        } else {
            this.value = getDefault();
        }
    }

    @SuppressWarnings("null")
    protected final void setupSubscription() {
        eventManager.subscribe(HomekitEventType.CHARACTERISTIC_CHANGE_VALUE, AbstractHomekitEvent.WILDCARD_UID,
                getUID(), event -> {
                    if (event instanceof HomekitCharacteristicChangeValueEvent) {
                        HomekitCharacteristicChangeValueEvent changeEvent = (HomekitCharacteristicChangeValueEvent) event;
                        // Optionally check if the event is for this characteristic
                        if (changeEvent.getCharacteristic().get().equals(HomekitGenericCharacteristic.this)) {
                            // Update the value in response to the event
                            try {
                                HomekitGenericCharacteristic.this.setValue(changeEvent.getNewValue().get(),
                                        event.getMetadata());
                            } catch (Exception e) {
                                // Handle error
                            }
                        }
                    }
                });
    }

    // Interface implementation methods
    @Override
    public HomekitService getService() {
        return service;
    }

    @Override
    public long getInstanceId() {
        return instanceId;
    }

    @Override
    @NonNull
    public HomekitCharacteristicUID getUID() {
        return new HomekitCharacteristicUID(getService().getAccessory().getUID().getPairingId(),
                getService().getAccessory().getAccessoryId(), getService().getInstanceId(), getInstanceId());
    }

    @Override
    public boolean isType(String aType) {
        return getInstanceType().equals(aType);
    }

    @Override
    public String getInstanceType() {
        if (type.length() == 2) {
            return String.format("%0" + (8 - type.length()) + "d%s", 0, type) + "-0000-1000-8000-0026BB765291";
        }
        return type;
    }

    @Override
    public boolean isHidden() {
        return isHidden;
    }

    @Override
    public void setHasEvents(boolean value) {
        this.hasEvents = value;
    }

    @Override
    public JsonObject toJson(boolean includeMeta, boolean includePermissions, boolean includeType,
            boolean includeEvent) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        if (includeMeta) {
            builder.add("iid", instanceId);
            builder.add("aid", service.getAccessory().getAccessoryId());
            builder.add("type", getInstanceType());
        }
        if (includePermissions) {
            builder.add("perms", getPermissions());
        }
        if (includeType) {
            builder.add("format", format);
        }
        if (includeEvent) {
            builder.add("ev", hasEvents);
        }
        JsonObject baseJson = builder.build();
        if (value != null) {
            return enrich(baseJson, "value", value);
        }
        return baseJson;
    }

    @Override
    public JsonObject toJson() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("iid", instanceId);
        builder.add("aid", service.getAccessory().getAccessoryId());
        builder.add("type", getInstanceType());
        builder.add("perms", getPermissions());
        builder.add("format", format);
        builder.add("description", description);
        builder.add("ev", hasEvents);
        JsonObject baseJson = builder.build();
        return getValue() != null ? enrich(baseJson, "value", getValue()) : baseJson;
    }

    @Override
    public JsonObject toReducedJson() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("iid", instanceId);
        builder.add("type", getInstanceType());
        builder.add("perms", getPermissions());
        builder.add("format", format);
        builder.add("description", description);
        builder.add("ev", hasEvents);
        JsonObject baseJson = builder.build();
        return getValue() != null ? enrich(baseJson, "value", getValue()) : baseJson;
    }

    @Override
    public JsonObject toEventJson() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("iid", instanceId);
        builder.add("aid", service.getAccessory().getAccessoryId());
        JsonObject baseJson = builder.build();
        return getValue() != null ? enrich(baseJson, "value", getValue()) : baseJson;
    }

    @Override
    public JsonObject toEventJson(T value) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("iid", instanceId);
        builder.add("aid", service.getAccessory().getAccessoryId());
        JsonObject baseJson = builder.build();
        return value != null ? enrich(baseJson, "value", value) : baseJson;
    }

    @Override
    public JsonValue toValueJson(@Nullable T value) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        JsonObject baseJson = builder.build();
        return value != null ? enrich(baseJson, "value", value) : baseJson;
    }

    @Override
    public JsonValue toValueJson(State state) {
        return toValueJson(toValue(state));
    }

    // Public methods
    @Override
    public T getValue() {
        if (value == null && initialValue != null) {
            value = toValue(initialValue);
            initialValue = null;
        }
        return value != null ? value : getDefault();
    }

    @Override
    public void setValue(@Nullable T value) throws Exception {
        if (!isWritable) {
            throw new Exception("Cannot modify a readonly characteristic");
        }
        @Nullable
        T oldValue = this.value;
        setValueInternal(value);
        notifyValueChanged(oldValue, this.value);
    }

    @Override
    public final void setValue(JsonValue jsonValue) throws Exception {
        if (!isWritable) {
            throw new Exception("Cannot modify a readonly characteristic");
        }
        try {
            setValue(toValue(jsonValue));
        } catch (Exception e) {
            logger.error("{}Error while setting JSON value: {}", LOG_ERROR, e.getMessage(), e);
            throw e;
        }
    }

    protected void setValue(JsonValue value, HomekitEventMetadata metadata) throws Exception {
        if (!isWritable) {
            throw new Exception("Cannot modify a readonly characteristic");
        }
        try {
            @Nullable
            T oldValue = this.value;
            setValueInternal(toValue(value));
            notifyValueChanged(oldValue, this.value, metadata);
        } catch (Exception e) {
            logger.error("{}Error while setting value with metadata: {}", LOG_ERROR, e.getMessage(), e);
            throw e;
        }
    }

    protected void setValueInternal(@Nullable T value) throws Exception {
        this.value = value;
    }

    // Protected methods
    protected void notifyValueChanged(@Nullable T oldValue, @Nullable T newValue) {
        eventManager.publishEvent(new HomekitCharacteristicValueChangedEvent((HomekitCharacteristic<?>) this, toValueJson(oldValue),
                toValueJson(newValue)));
    }

    protected void notifyValueChanged(@Nullable T oldValue, @Nullable T newValue, HomekitEventMetadata metadata) {
        eventManager.publishEvent(new HomekitCharacteristicValueChangedEvent((HomekitCharacteristic<?>) this, toValueJson(oldValue),
                toValueJson(newValue), metadata));
    }

    protected JsonObject enrich(JsonObject source, String key, Object value) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        addValue(builder, key, value);
        source.entrySet().forEach(e -> builder.add(e.getKey(), e.getValue()));
        return builder.build();
    }

    protected void addValue(JsonObjectBuilder builder, String name, Object value) {
        if (value instanceof Boolean aBoolean) {
            builder.add(name, aBoolean);
        } else if (value instanceof Double aDouble) {
            builder.add(name, aDouble);
        } else if (value instanceof Integer anInteger) {
            builder.add(name, anInteger);
        } else if (value instanceof Long aLong) {
            builder.add(name, aLong);
        } else if (value instanceof BigInteger aBigInteger) {
            builder.add(name, aBigInteger);
        } else if (value instanceof BigDecimal aBigDecimal) {
            builder.add(name, aBigDecimal);
        } else if (value instanceof JsonValue aJsonValue) {
            builder.add(name, aJsonValue);
        } else if (value instanceof JsonObjectBuilder aJsonObjectBuilder) {
            builder.add(name, aJsonObjectBuilder);
        } else if (value instanceof JsonArrayBuilder aJsonArrayBuilder) {
            builder.add(name, aJsonArrayBuilder);
        } else if (value instanceof JsonObject aJsonObject) {
            builder.add(name, aJsonObject);
        } else if (value != null) {
            builder.add(name, value.toString());
        }
    }

    // Private methods
    private JsonArray getPermissions() {
        JsonArrayBuilder builder = Json.createArrayBuilder();
        if (isWritable) {
            builder.add("pw");
        }
        if (isReadable) {
            builder.add("pr");
        }
        if (hasEvents) {
            builder.add("ev");
        }
        return builder.build();
    }

    // Object methods
    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        HomekitGenericCharacteristic<?> that = (HomekitGenericCharacteristic<?>) o;

        // Compare fields in the same order as compareTo
        return instanceId == that.instanceId && getInstanceType().equals(that.getInstanceType())
                && format.equals(that.format) && isWritable == that.isWritable && isReadable == that.isReadable
                && hasEvents == that.hasEvents && description.equals(that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instanceId, type, service);
    }

    @Override
    public int compareTo(@Nullable HomekitCharacteristic<?> other) {
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

        // Compare by format
        int formatCompare = this.format.compareTo(((HomekitGenericCharacteristic<?>) other).format);
        if (formatCompare != 0)
            return formatCompare;

        // Compare by isWritable
        int writableCompare = Boolean.compare(this.isWritable, ((HomekitGenericCharacteristic<?>) other).isWritable);
        if (writableCompare != 0)
            return writableCompare;

        // Compare by isReadable
        int readableCompare = Boolean.compare(this.isReadable, ((HomekitGenericCharacteristic<?>) other).isReadable);
        if (readableCompare != 0)
            return readableCompare;

        // Compare by hasEvents
        int eventsCompare = Boolean.compare(this.hasEvents, ((HomekitGenericCharacteristic<?>) other).hasEvents);
        if (eventsCompare != 0)
            return eventsCompare;

        // Finally compare by description
        return this.description.compareTo(((HomekitGenericCharacteristic<?>) other).description);
    }

    // Abstract methods
    @Override
    public abstract T toValue(JsonValue jsonValue);

    @Override
    public abstract T toValue(State state);

    @Override
    public abstract State toState(T value);

    @Override
    public State toState(JsonValue jsonValue) {
        return toState(toValue(jsonValue));
    }

    @Override
    public abstract T getDefault();

    @Override
    public String getDescription() {
        return description;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void updateWith(@Nullable HomekitCharacteristic<?> other) {
        if (other == null)
            return;
        if (other instanceof HomekitGenericCharacteristic<?> otherGeneric) {
            if (this.getInstanceType().equals(otherGeneric.getInstanceType())
                    && this.instanceId == otherGeneric.getInstanceId()) {
                this.isWritable = otherGeneric.isWritable;
                this.isReadable = otherGeneric.isReadable;
                this.hasEvents = otherGeneric.hasEvents;
                this.description = otherGeneric.description;
                this.format = otherGeneric.format;
                this.isHidden = otherGeneric.isHidden;
                try {
                    setValue((T) otherGeneric.getValue());
                } catch (Exception e) {
                    logger.error("{}Error updating characteristic value: {}", LOG_ERROR, e.getMessage(), e);
                }
            }
        }
    }

    // @Override
    // public boolean isWritable() {
    // return isWritable;
    // }

    // @Override
    // public boolean isReadable() {
    // return isReadable;
    // }

    // @Override
    // public boolean hasEvents() {
    // return hasEvents;
    // }
}
