package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.ManagedAccessory;
import org.openhab.io.homekit.api.ManagedService;
import org.openhab.io.homekit.internal.characteristic.WriteOnlyBooleanCharacteristic;

public class IdentifyCharacteristic extends WriteOnlyBooleanCharacteristic {

    public IdentifyCharacteristic(HomekitCommunicationManager manager, ManagedService service, long instanceId) {
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
            ((ManagedAccessory) getService().getAccessory()).identify();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected Boolean getDefault() {
        return null;
    }

}
