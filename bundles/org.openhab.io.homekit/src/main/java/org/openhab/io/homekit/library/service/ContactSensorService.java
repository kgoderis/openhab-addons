package org.openhab.io.homekit.library.service;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.ManagedAccessory;
import org.openhab.io.homekit.internal.service.AbstractManagedService;
import org.openhab.io.homekit.library.characteristic.ContactSensorStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.StatusActiveCharacteristic;
import org.openhab.io.homekit.library.characteristic.StatusFaultCharacteristic;
import org.openhab.io.homekit.library.characteristic.StatusLowBatteryCharacteristic;
import org.openhab.io.homekit.library.characteristic.StatusTamperedCharacteristic;

public class ContactSensorService extends AbstractManagedService {

    public ContactSensorService(HomekitCommunicationManager manager, ManagedAccessory accessory, long instanceId,
            boolean extend, @NonNull String serviceName) throws Exception {
        super(manager, accessory, instanceId, extend, serviceName);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(new ContactSensorStateCharacteristic(getManager(), this,
                ((ManagedAccessory) getAccessory()).getInstanceId()));
        addCharacteristic(new StatusActiveCharacteristic(getManager(), this,
                ((ManagedAccessory) getAccessory()).getInstanceId()));
        addCharacteristic(
                new StatusFaultCharacteristic(getManager(), this, ((ManagedAccessory) getAccessory()).getInstanceId()));
        addCharacteristic(new StatusTamperedCharacteristic(getManager(), this,
                ((ManagedAccessory) getAccessory()).getInstanceId()));
        addCharacteristic(new StatusLowBatteryCharacteristic(getManager(), this,
                ((ManagedAccessory) getAccessory()).getInstanceId()));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }

    public static String getType() {
        return "00000080-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }
}
