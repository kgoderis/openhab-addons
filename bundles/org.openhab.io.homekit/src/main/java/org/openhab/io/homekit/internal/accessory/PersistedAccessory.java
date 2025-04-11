package org.openhab.io.homekit.internal.accessory;

public class PersistedAccessory {

    private String json;
    // private String instanceIdPool;
    private String accessoryClass;

    public PersistedAccessory() {
        json = "";
        // instanceIdPool = "";
        accessoryClass = "";
    }

    public PersistedAccessory(String accessoryClass, String json) {
        this.json = json;
        // this.instanceIdPool = Long.toString(instanceIdPool);
        this.accessoryClass = accessoryClass;
    }

    public String getJson() {
        return json;
    }

    // public long getInstanceIdPool() {
    // return Long.parseLong(instanceIdPool);
    // }

    public String getAccessoryClass() {
        return accessoryClass;
    }

    public void setJson(String json) {
        this.json = json;
    }

    // public void setInstanceIdPool(long instanceIdPool) {
    // this.instanceIdPool = Long.toString(instanceIdPool);
    // }

    // public void setServerUID(AccessoryServerUID serverUID) {
    // this.serverUID = serverUID.toString();
    // }

    public void setAccessoryClass(String accessoryClass) {
        this.accessoryClass = accessoryClass;
    }
}
