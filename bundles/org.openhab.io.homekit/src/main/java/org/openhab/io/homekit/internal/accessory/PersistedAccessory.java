package org.openhab.io.homekit.internal.accessory;

import org.openhab.io.homekit.internal.server.AccessoryServerUID;

public class PersistedAccessory {

    private String json;
    // private String instanceIdPool;
    private String serverUID;
    private String accessoryClass;

    public PersistedAccessory() {
        json = "";
        // instanceIdPool = "";
        serverUID = "";
        accessoryClass = "";
    }

    public PersistedAccessory(String accessoryClass, String json, String serverUID) {
        this.json = json;
        // this.instanceIdPool = Long.toString(instanceIdPool);
        this.serverUID = serverUID;
        this.accessoryClass = accessoryClass;
    }

    public String getJson() {
        return json;
    }

        // public long getInstanceIdPool() {
        //     return Long.parseLong(instanceIdPool);
        // }

    public AccessoryServerUID getServerUID() {
        return new AccessoryServerUID(serverUID);
    }

    public String getAccessoryClass() {
        return accessoryClass;
    }

    public void setJson(String json) {
        this.json = json;
    }

    // public void setInstanceIdPool(long instanceIdPool) {
    //     this.instanceIdPool = Long.toString(instanceIdPool);
    // }

    public void setServerUID(AccessoryServerUID serverUID) {
        this.serverUID = serverUID.toString();
    }

    public void setAccessoryClass(String accessoryClass) {
        this.accessoryClass = accessoryClass;
    }
}
