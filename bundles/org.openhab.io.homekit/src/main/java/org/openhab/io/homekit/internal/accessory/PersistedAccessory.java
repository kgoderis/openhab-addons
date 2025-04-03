package org.openhab.io.homekit.internal.accessory;

import org.openhab.io.homekit.internal.server.AccessoryServerUID;

public class PersistedAccessory {

    private String json;
    private String instanceIdPool;
    private String serverId;
    private String accessoryClass;

    public PersistedAccessory() {
        json = "";
        instanceIdPool = "";
        serverId = "";
        accessoryClass = "";
    }

    public PersistedAccessory(String accessoryClass, String json, String serverId, long instanceIdPool) {
        this.json = json;
        this.instanceIdPool = Long.toString(instanceIdPool);
        this.serverId = serverId;
        this.accessoryClass = accessoryClass;
    }

    public String getJson() {
        return json;
    }

    public long getInstanceIdPool() {
        return Long.parseLong(instanceIdPool);
    }

    public AccessoryServerUID getServerId() {
        return new AccessoryServerUID(serverId);
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

    public void setServerId(AccessoryServerUID serverId) {
        this.serverId = serverId.toString();
    }

    public void setAccessoryClass(String accessoryClass) {
        this.accessoryClass = accessoryClass;
    }
}
