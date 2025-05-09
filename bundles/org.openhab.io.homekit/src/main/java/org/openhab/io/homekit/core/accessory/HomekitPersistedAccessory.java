package org.openhab.io.homekit.core.accessory;

public class HomekitPersistedAccessory {

    private String json;
    // private String instanceIdPool;
    private String accessoryClass;

    public HomekitPersistedAccessory() {
        json = "";
        // instanceIdPool = "";
        accessoryClass = "";
    }

    public HomekitPersistedAccessory(String accessoryClass, String json) {
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

    // public void setServerUID(HomekitAccessoryServerUID serverUID) {
    // this.serverUID = serverUID.toString();
    // }

    public void setAccessoryClass(String accessoryClass) {
        this.accessoryClass = accessoryClass;
    }
}
