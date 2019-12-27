package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.internal.characteristic.WriteOnlyBooleanCharacteristic;

public class IdentifyCharacteristic extends WriteOnlyBooleanCharacteristic {

    public IdentifyCharacteristic(HomekitCommunicationManager manager, Service service, long instanceId)
            throws Exception {
        super(manager, service, instanceId, "Identifies the accessory via a physical action on the accessory");
    }

    public static String getType() {
        return "00000014-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    @Override
    public void setValue(Boolean value) throws Exception {
        if (value) {
            getService().getAccessory().identify();
        }
    }

}
