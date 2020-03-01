package org.openhab.io.homekit.internal.characteristic;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.openhab.io.homekit.api.Characteristic;
import org.openhab.io.homekit.api.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericCharacteristic implements Characteristic {

    private static final Logger logger = LoggerFactory.getLogger(GenericCharacteristic.class);

    private final Service service;
    private final long instanceId;
    private final String format;
    private boolean isWritable = false;
    private boolean isReadable = false;
    private boolean isHidden = false;
    private boolean hasEvents = false;
    private boolean hasEventsEnabled;
    private final String description;
    private String type;
    private Object value;

    public GenericCharacteristic(Service service, JsonValue value) {
        this.service = service;
        this.instanceId = ((JsonObject) value).getInt("iid");
        this.type = ((JsonObject) value).getString("type");
        this.format = ((JsonObject) value).getString("format");
        this.description = ((JsonObject) value).getString("description");
        this.hasEventsEnabled = ((JsonObject) value).getBoolean("ev");

        JsonArray permissionsArray = ((JsonObject) value).getJsonArray("perms");

        for (JsonValue permsValue : permissionsArray) {
            switch (((JsonString) permsValue).getString()) {
                case "pr":
                    isReadable = true;
                case "pw":
                    isWritable = true;
                case "ev":
                    hasEvents = true;
                case "hd":
                    isHidden = true;
            }
        }

        this.value = ((JsonObject) value).get("value");

    }

    public GenericCharacteristic(Service service, long instanceId, String format, boolean isWritable,
            boolean isReadable, boolean hasEvents, String description) {
        this.service = service;
        this.instanceId = instanceId;
        this.format = format;
        this.isWritable = isWritable;
        this.isReadable = isReadable;
        this.hasEvents = hasEvents;
        this.description = description;
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
    public Service getService() {
        return service;
    }

    @Override
    public boolean isHidden() {
        return isHidden;
    }

    public boolean isEventsEnabled() {
        return hasEventsEnabled;
    }

    @Override
    public void setEventsEnabled(boolean value) {
        this.hasEventsEnabled = value;
    }

    public boolean isWritable() {
        return isWritable;
    }

    public void setWritable(boolean isWritable) {
        this.isWritable = isWritable;
    }

    public boolean isReadable() {
        return isReadable;
    }

    public void setReadable(boolean isReadable) {
        this.isReadable = isReadable;
    }

    public void setHidden(boolean isHidden) {
        this.isHidden = isHidden;
    }

    public boolean isHasEvents() {
        return hasEvents;
    }

    public void setHasEvents(boolean hasEvents) {
        this.hasEvents = hasEvents;
    }

    public String getDescription() {
        return description;
    }

    public String getFormat() {
        return format;
    }

    protected JsonObject toJson(boolean aid, boolean iid, boolean type, boolean shortType, boolean perms,
            boolean format, boolean ev, boolean description, boolean addValue) {

        JsonObjectBuilder builder = Json.createObjectBuilder();

        if (aid) {
            builder.add("aid", service.getAccessory().getId());
        }

        if (iid) {
            builder.add("iid", this.instanceId);
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

    @Override
    public JsonObject toJson(boolean includeMeta, boolean includePermissions, boolean includeType,
            boolean includeEvent) {
        return toJson(true, true, includeType, includeType, includePermissions, includeMeta, includeEvent, false, true);
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

}
