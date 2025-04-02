package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.WriteOnlyBooleanCharacteristic;

public class IdentifyCharacteristic extends WriteOnlyBooleanCharacteristic {

    public IdentifyCharacteristic(Service service, long instanceId) {
        super(service, instanceId, "Identify");
    }

    public static String getType() {
        return "00000014-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    @Override
    public void setValue(Boolean value) {
        if (value) {
            getService().getAccessory().identify();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Boolean getDefault() {
        return null;
    }
}
