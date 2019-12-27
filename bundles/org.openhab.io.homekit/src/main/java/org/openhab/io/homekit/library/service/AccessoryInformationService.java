package org.openhab.io.homekit.library.service;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.internal.service.AbstractService;
import org.openhab.io.homekit.library.characteristic.FirmwareRevisionCharacteristic;
import org.openhab.io.homekit.library.characteristic.IdentifyCharacteristic;
import org.openhab.io.homekit.library.characteristic.ManufacturerCharacteristic;
import org.openhab.io.homekit.library.characteristic.ModelCharacteristic;
import org.openhab.io.homekit.library.characteristic.NameCharacteristic;
import org.openhab.io.homekit.library.characteristic.SerialNumberCharacteristic;

public class AccessoryInformationService extends AbstractService {

    public AccessoryInformationService(HomekitCommunicationManager manager, Accessory accessory, long instanceId,
            boolean extend, @NonNull String serviceName) throws Exception {
        super(manager, accessory, instanceId, extend, serviceName);
    }

    @Override
    public void addCharacteristics() throws Exception {
        super.addCharacteristics();
        addCharacteristic(new FirmwareRevisionCharacteristic(manager, this, this.getAccessory().getInstanceId()));
        addCharacteristic(new IdentifyCharacteristic(manager, this, this.getAccessory().getInstanceId()));
        addCharacteristic(new ManufacturerCharacteristic(manager, this, this.getAccessory().getInstanceId()));
        addCharacteristic(new ModelCharacteristic(manager, this, this.getAccessory().getInstanceId()));
        addCharacteristic(new NameCharacteristic(manager, this, this.getAccessory().getInstanceId()));
        addCharacteristic(new SerialNumberCharacteristic(manager, this, this.getAccessory().getInstanceId()));
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
