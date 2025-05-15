package org.openhab.io.homekit.core.accessory;

public class HomekitPersistedAccessory {

    private String json;
    // private String instanceIdPool;
    private String accessoryType;

    public HomekitPersistedAccessory() {
        json = "";
        // instanceIdPool = "";
        accessoryType = "";
    }

    public HomekitPersistedAccessory(String accessoryType, String json) {
        this.json = json;
        // this.instanceIdPool = Long.toString(instanceIdPool);
        this.accessoryType = accessoryType;
    }

    public String getJson() {
        return json;
    }

    // public long getInstanceIdPool() {
    // return Long.parseLong(instanceIdPool);
    // }

    public String getAccessoryType() {
        return accessoryType;
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

    public void setAccessoryType(String accessoryType) {
        this.accessoryType = accessoryType;
    }
}
