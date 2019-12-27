package org.openhab.io.homekit.internal.characteristic;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.items.Item;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.Characteristic;
import org.openhab.io.homekit.api.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCharacteristic<T> implements Characteristic<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractCharacteristic.class);

    protected final HomekitCommunicationManager manager;

    private final Service service;
    private final long instanceId;
    // private final String shortType;
    private final String format;
    private final boolean isWritable;
    private final boolean isReadable;
    private final boolean hasEvents;
    private final String description;

    private ChannelUID channelUID;

    /**
     * Default constructor
     *
     * @param type a string containing a UUID that indicates the type of characteristic. Apple defines
     *            a set of these, however implementors can create their own as well.
     * @param format a string indicating the value type, which must be a recognized type by the
     *            consuming device.
     * @param isWritable indicates whether the value can be changed.
     * @param isReadable indicates whether the value can be retrieved.
     * @param description a description of the characteristic to be passed to the consuming device.
     */
    public AbstractCharacteristic(HomekitCommunicationManager manager, Service service, long instanceId, String format,
            boolean isWritable, boolean isReadable, boolean hasEvents, String description) {
        super();

        if (manager == null || format == null || description == null) {
            throw new NullPointerException();
        }

        this.manager = manager;
        this.service = service;
        this.instanceId = instanceId;
        // this.shortType = getType().replaceAll("^0*([0-9a-fA-F]+)-0000-1000-8000-0026BB765291$", "$1");
        this.format = format;
        this.isWritable = isWritable;
        this.isReadable = isReadable;
        this.hasEvents = hasEvents;
        this.description = description;
    }

    // public AbstractCharacteristic(HomekitCommunicationManager manager, Service service, String format,
    // boolean isWritable, boolean isReadable, boolean hasEvents, String description) {
    // this(manager, service, service.getAccessory().getInstanceId(), format, isWritable, isReadable, hasEvents,
    // description);
    // }

    @Override
    public CharacteristicUID getUID() {
        return new CharacteristicUID(service.getAccessory().getServer().getId(), service.getAccessory().getId(),
                service.getId(), instanceId);
    }

    @Override
    public long getId() {
        return instanceId;
    }

    // public static String getType() {
    // return UUID5.fromNamespaceAndString(UUID5.NAMESPACE_CHARACTERISTIC, new Object() {
    // }.getClass().getEnclosingClass().getName()).toString();
    // }

    @Override
    public boolean isType(String aType) {
        return getInstanceType().equals(aType);
    }

    @Override
    public abstract String getInstanceType();

    @Override
    public Service getService() {
        return service;
    }

    @Override
    public ChannelUID getChannelUID() {
        return channelUID;
    }

    @Override
    public void setChannelUID(ChannelUID channelUID) {
        this.channelUID = channelUID;
    }

    @Override
    public T getValue() throws Exception {
        State state = manager.getValue(getChannelUID());

        if (state != UnDefType.UNDEF) {
            return convert(state);
        } else {
            return null;
        }
    }

    @Override
    public void setValue(T value) throws Exception {
        manager.setValue(getChannelUID(), convert(value));
    }

    /** {@inheritDoc} */
    @Override
    public final void setValue(JsonObject jsonObject) {
        try {
            this.setValue(convert(jsonObject));
        } catch (Exception e) {
            logger.error("Error while setting JSON value", e);
        }
    }

    @Override
    public JsonObject toJson() {

        JsonArrayBuilder perms = Json.createArrayBuilder();
        if (isWritable) {
            perms.add("pw");
        }
        if (isReadable) {
            perms.add("pr");
        }
        if (hasEvents) {
            perms.add("ev");
        }

        JsonObjectBuilder builder = Json.createObjectBuilder().add("aid", service.getId()).add("iid", instanceId)
                .add("type", getInstanceType()).add("perms", perms.build()).add("format", format).add("ev", true)
                .add("description", description);

        JsonObject jsonObject = builder.build();

        T value;
        try {
            value = getValue();
            if (value == null) {
                value = getDefault();
            }
        } catch (Exception e) {
            value = getDefault();
        }

        return enrich(jsonObject, "value", value);
    }

    @Override
    public JsonObject toReducedJson() {

        JsonArrayBuilder perms = Json.createArrayBuilder();
        if (isWritable) {
            perms.add("pw");
        }
        if (isReadable) {
            perms.add("pr");
        }
        if (hasEvents) {
            perms.add("ev");
        }

        JsonObjectBuilder builder = Json.createObjectBuilder().add("iid", instanceId)
                .add("type", getInstanceType().replaceAll("^0*([0-9a-fA-F]+)-0000-1000-8000-0026BB765291$", "$1"))
                .add("perms", perms.build()).add("format", format);

        JsonObject jsonObject = builder.build();

        T value;
        try {
            value = getValue();
            if (value == null) {
                value = getDefault();
            }
        } catch (Exception e) {
            value = getDefault();
        }

        return enrich(jsonObject, "value", value);
    }

    @Override
    public JsonObject toEventJson() {
        T value;
        try {
            value = getValue();
            if (value == null) {
                value = getDefault();
            }
        } catch (Exception e) {
            value = getDefault();
        }

        return toEventJson(value);
    }

    protected JsonObject toEventJson(T value) {
        JsonObjectBuilder builder = Json.createObjectBuilder().add("aid", service.getAccessory().getId()).add("iid",
                instanceId);
        JsonObject jsonObject = builder.build();

        return enrich(jsonObject, "value", value);
    }

    /**
     * Converts from the JSON value to a Java object of the type T
     *
     * @param jsonValue the JSON value to convert from.
     * @return the converted Java object.
     */
    protected abstract T convert(JsonObject jsonObject);

    protected abstract T convert(State state);

    protected abstract State convert(T value);

    /**
     * Provide a default value for the characteristic to be send when the real value cannot be retrieved.
     *
     * @return a sensible default value.
     */
    @NonNull
    protected abstract T getDefault();

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
            builder.addNull(name);
        } else {
            builder.add(name, value.toString());
        }
    }

    @Override
    public void stateChanged(@NonNull Item item, @NonNull State oldState, @NonNull State newState) {
        manager.setValue(getUID(), toEventJson(convert(newState)));
    }

    @Override
    public void stateUpdated(@NonNull Item item, @NonNull State state) {
        // No Op - We do not want to flood the network per Apple HAP Spec guidelines
    }
}
