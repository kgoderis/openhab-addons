package org.openhab.io.homekit.library.service;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.ManagedAccessory;
import org.openhab.io.homekit.internal.service.AbstractManagedService;
import org.openhab.io.homekit.library.characteristic.IdentifyCharacteristic;
import org.openhab.io.homekit.library.characteristic.ManufacturerCharacteristic;
import org.openhab.io.homekit.library.characteristic.ModelCharacteristic;
import org.openhab.io.homekit.library.characteristic.SerialNumberCharacteristic;

public class AccessoryInformationService extends AbstractManagedService {

    public AccessoryInformationService(HomekitCommunicationManager manager, ManagedAccessory accessory, long instanceId,
            boolean extend, @NonNull String serviceName) {
        super(manager, accessory, instanceId, extend, serviceName);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        // addCharacteristic(new FirmwareRevisionCharacteristic(manager, this, getAccessory().getInstanceId()));
        addCharacteristic(
                new IdentifyCharacteristic(getManager(), this, ((ManagedAccessory) getAccessory()).getInstanceId()));
        addCharacteristic(new ManufacturerCharacteristic(getManager(), this,
                ((ManagedAccessory) getAccessory()).getInstanceId()));
        addCharacteristic(
                new ModelCharacteristic(getManager(), this, ((ManagedAccessory) getAccessory()).getInstanceId()));
        addCharacteristic(new SerialNumberCharacteristic(getManager(), this,
                ((ManagedAccessory) getAccessory()).getInstanceId()));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }

    public static String getType() {
        return "0000003E-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }
}
