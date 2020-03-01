package org.openhab.io.homekit.internal.characteristic;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.openhab.core.thing.ChannelUID;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.ManagedAccessory;
import org.openhab.io.homekit.api.ManagedCharacteristic;
import org.openhab.io.homekit.api.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractManagedCharacteristic<T> extends GenericCharacteristic
        implements ManagedCharacteristic<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractManagedCharacteristic.class);

    protected final HomekitCommunicationManager manager;
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
    public AbstractManagedCharacteristic(HomekitCommunicationManager manager, ManagedService service, long instanceId,
            String format, boolean isWritable, boolean isReadable, boolean hasEvents, String description) {
        super(service, instanceId, format, isWritable, isReadable, hasEvents, description);

        if (manager == null || format == null || description == null) {
            throw new IllegalArgumentException();
        }

        this.manager = manager;
    }

    @Override
    public CharacteristicUID getUID() {
        return new CharacteristicUID(((ManagedAccessory) getService().getAccessory()).getServer().getId(),
                getService().getAccessory().getId(), getService().getId(), getId());
    }

    @Override
    public abstract String getInstanceType();

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
        ChannelUID uid = getChannelUID();

        if (uid != null) {
            State state = manager.getState(uid);

            if (state != UnDefType.UNDEF) {
                return convert(state);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public void setValue(T value) throws Exception {
        if (isWritable()) {
            manager.stateUpdated(getChannelUID(), convert(value));
        } else {
            throw new Exception("Can not modify a readonly characteristic");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    @Override
    public final void setValue(JsonValue jsonValue) throws Exception {
        if (isWritable()) {
            try {
                this.setValue(convert(jsonValue));
            } catch (Exception e) {
                logger.error("Error while setting JSON value", e);
            }
        } else {
            throw new Exception("Can not modify a readonly characteristic");
        }
    }

    @Override
    protected JsonObject toJson(boolean aid, boolean iid, boolean type, boolean shortType, boolean perms,
            boolean format, boolean ev, boolean description, boolean addValue) {

        JsonObjectBuilder builder = Json.createObjectBuilder();

        if (aid) {
            builder.add("aid", getService().getAccessory().getId());
        }

        if (iid) {
            builder.add("iid", this.getId());
        }

        if (type) {
            if (shortType) {
                builder.add("type",
                        getInstanceType().replaceAll("^0*([0-9a-fA-F]+)-0000-1000-8000-0026BB765291$", "$1"));
            } else {
                builder.add("type", getInstanceType());
            }
        }

        if (perms) {
            JsonArrayBuilder permissions = Json.createArrayBuilder();
            if (isWritable()) {
                permissions.add("pw");
            }
            if (isReadable()) {
                permissions.add("pr");
            }
            if (isHasEvents()) {
                permissions.add("ev");
            }

            builder.add("perms", permissions.build());
        }

        if (format) {
            builder.add("format", this.getFormat());
        }

        if (ev) {
            builder.add("ev", this.isEventsEnabled());
        }

        if (description) {
            builder.add("description", this.getDescription());
        }

        JsonObject jsonObject = builder.build();

        if (addValue) {
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
        } else {
            return jsonObject;
        }
    }

    @Override
    public JsonObject toJson() {
        return toJson(true, true, true, false, true, true, true, true, true);
    }

    @Override
    public JsonObject toReducedJson() {
        return toJson(false, true, true, true, true, true, true, true, true);
    }

    @Override
    public JsonObject toEventJson() {
        return toJson(true, true, false, false, false, false, false, false, true);
    }

    protected JsonObject toEventJson(T value) {
        JsonObject base = toJson(true, true, false, false, false, false, false, false, false);
        return enrich(base, "value", value);
    }

    @Override
    public JsonObject toEventJson(State state) {
        return toEventJson(convert(state));
    }

    @Override
    public JsonObject toJson(boolean includeMeta, boolean includePermissions, boolean includeType,
            boolean includeEvent) {
        return toJson(true, true, includeType, includeType, includePermissions, includeMeta, includeEvent, false, true);
    }

    /**
     * Converts from the JSON value to a Java object of the type T
     *
     * @param jsonValue the JSON value to convert from.
     * @return the converted Java object.
     */
    protected abstract T convert(JsonValue jsonValue);

    protected abstract T convert(State state);

    protected abstract State convert(T value);

    /**
     * Provide a default value for the characteristic to be send when the real value cannot be retrieved.
     *
     * @return a sensible default value.
     */
    protected abstract T getDefault();
}
