package org.openhab.io.homekit.internal.accessory;

public class PersistedAccessory {

    private String json;
    private String instanceIdPool;
    private String serverId;
    private String thingUID;
    private String accessoryClass;

    public PersistedAccessory() {
        json = "";
        instanceIdPool = "";
        serverId = "";
        thingUID = "";
        accessoryClass = "";
    }

    public PersistedAccessory(String accessoryClass, String json, String serverId, long instanceIdPool,
            String thingUID) {
        this.json = json;
        this.instanceIdPool = Long.toString(instanceIdPool);
        this.serverId = serverId;
        this.thingUID = thingUID;
        this.accessoryClass = accessoryClass;
    }

    public String getJson() {
        return json;
    }

    public long getInstanceIdPool() {
        return Long.parseLong(instanceIdPool);
    }

    public String getServerId() {
        return serverId;
    }

    public String getThingUID() {
        return thingUID;
    }

    public String getAccessoryClass() {
        return accessoryClass;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public void setInstanceIdPool(long instanceIdPool) {
        this.instanceIdPool = Long.toString(instanceIdPool);
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public void setThingUID(String thingUID) {
        this.thingUID = thingUID;
    }

    public void setAccessoryClass(String accessoryClass) {
        this.accessoryClass = accessoryClass;
    }
}
