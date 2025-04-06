package org.openhab.io.homekit.internal.characteristic;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.hap.Characteristic;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.api.listener.CharacteristicChangeListener;
import org.openhab.io.homekit.internal.events.CharacteristicEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GenericCharacteristic<T> implements Characteristic<T> {

    private static final Logger logger = LoggerFactory.getLogger(GenericCharacteristic.class);

    // Instance fields - final
    private final Service service;
    private final long instanceId;
    private final String format;
    private final String description;
    private final Collection<CharacteristicChangeListener> listeners = new CopyOnWriteArraySet<>();

    // Instance fields - mutable
    private boolean isWritable = false;
    private boolean isReadable = false;
    private boolean isHidden = false;
    private boolean hasEvents = false;
    private String type;
    protected T value;

    // Constructors
    public GenericCharacteristic(Service service, JsonValue value) {
        this.service = service;
        this.value = toValue(value);
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
                    case "pw":
                        this.isWritable = true;
                        break;
                    case "pr":
                        this.isReadable = true;
                        break;
                    case "ev":
                        this.hasEvents = true;
                        break;
                }
            }
        }

        if (((JsonObject) value).containsKey("ev")) {
            this.hasEvents = ((JsonObject) value).getBoolean("ev");
        }

        if (((JsonObject) value).containsKey("value")) {
            this.value = toValue(((JsonObject) value).get("value"));
        }
    }

    //TODO add constructor that takes servce, instanceid and value

    public GenericCharacteristic(Service service, long instanceId, String format, boolean isWritable,
            boolean isReadable, boolean hasEvents, String description) {
        this.service = service;
        this.instanceId = instanceId;
        this.format = format;
        this.isWritable = isWritable;
        this.isReadable = isReadable;
        this.hasEvents = hasEvents;
        this.description = description;
        this.value = getDefault();
    }

    // Interface implementation methods
    @Override
    public Service getService() {
        return service;
    }

    @Override
    public long getId() {
        return instanceId;
    }

    @Override
    @NonNull public CharacteristicUID getUID() {
        Service service = getService();
        return new CharacteristicUID("homekit", service.getAccessory().getAccessoryId(), service.getInstanceId(), getId());
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
        if (getValue() != null) {
            return enrich(baseJson, "value", getValue());
        }
        return baseJson;
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
        if (getValue() != null) {
            return enrich(baseJson, "value", getValue());
        }
        return baseJson;
    }

    @Override
    public JsonObject toEventJson() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("iid", instanceId);
        builder.add("aid", service.getAccessory().getAccessoryId());
        JsonObject baseJson = builder.build();
        if (getValue() != null) {
            return enrich(baseJson, "value", getValue());
        }
        return baseJson;
    }

    @Override
    public JsonObject toEventJson(T value) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("iid", instanceId);
        builder.add("aid", service.getAccessory().getAccessoryId());
        JsonObject baseJson = builder.build();
        if (value != null) {
            return enrich(baseJson, "value", value);
        }
        return baseJson;
    }

    @Override
    public JsonValue toValueJson(T value) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        JsonObject baseJson = builder.build();
        if (value != null) {
            return enrich(baseJson, "value", getValue());
        }
        return baseJson;
    }

    // Public methods
    @Override
    public T getValue() {
        if (value != null) {
            return value;
        } else {
            return getDefault();
        }
    }

    @Override
    public void setValue(T value) throws Exception {
        if (isWritable) {
            T oldValue = this.value;
            this.value = value;
            if (!Objects.equals(oldValue, value)) {
                CharacteristicEvent event = new CharacteristicEvent(this, toValueJson(oldValue), toValueJson(value));
                notifyValueChanged(oldValue, value);
                notifyListeners(event);
            }
        }
    }
    
    @Override
    public final void setValue(JsonValue jsonValue) throws Exception {
        if (isWritable) {
            try {
                setValue(toValue(jsonValue));
            } catch (Exception e) {
                logger.error("Error while setting JSON value", e);
            }
        } else {
            throw new Exception("Can not modify a readonly characteristic");
        }
    }

    @Override
    public void addListener(CharacteristicChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(CharacteristicChangeListener listener) {
        listeners.remove(listener);
    }

    // Protected methods
    protected void notifyValueChanged(T oldValue, T newValue) {
        for (CharacteristicChangeListener listener : listeners) {
            listener.onCharacteristicEvent(new CharacteristicEvent(this, toValueJson(oldValue), toValueJson(value)));
        }
    }

    protected void notifyListeners(CharacteristicEvent event) {
        for (CharacteristicChangeListener listener : listeners) {
            listener.onCharacteristicEvent(event);
        }
    }

    protected JsonObject enrich(JsonObject source, String key, Object value) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        addValue(builder, key, value);
        source.entrySet().forEach(e -> builder.add(e.getKey(), e.getValue()));
        return builder.build();
    }

    protected void addValue(JsonObjectBuilder builder, String name, Object value) {
        if (value instanceof Boolean) {
            builder.add(name, (Boolean) value);
        } else if (value instanceof Double) {
            builder.add(name, (Double) value);
        } else if (value instanceof Integer) {
            builder.add(name, (Integer) value);
        } else if (value instanceof Long) {
            builder.add(name, (Long) value);
        } else if (value instanceof BigInteger) {
            builder.add(name, (BigInteger) value);
        } else if (value instanceof BigDecimal) {
            builder.add(name, (BigDecimal) value);
        } else if (value instanceof JsonValue) {
            builder.add(name, (JsonValue) value);
        } else if (value instanceof JsonObjectBuilder) {
            builder.add(name, (JsonObjectBuilder) value);
        } else if (value instanceof JsonArrayBuilder) {
            builder.add(name, (JsonArrayBuilder) value);
        } else if (value == null) {
            // builder.addNull(name);
        } else {
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GenericCharacteristic<?> that = (GenericCharacteristic<?>) o;
        return instanceId == that.instanceId && Objects.equals(service, that.service);
    }

    @Override
    public int hashCode() {
        return Objects.hash(service, instanceId);
    }

    // Abstract methods
    @Override
    public abstract T toValue(JsonValue jsonValue);
    @Override
    public abstract T toValue(State state);
    @Override
    public abstract State toState(T value);

    public State toState(JsonValue jsonValue) {
        return toState(toValue(jsonValue));
    }

    @Override
    public abstract T getDefault();

    @Override
    public String getDescription() {
        return description;
    }
}
