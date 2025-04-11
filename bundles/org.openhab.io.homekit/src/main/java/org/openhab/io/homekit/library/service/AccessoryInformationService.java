package org.openhab.io.homekit.library.service;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.internal.service.GenericService;
import org.openhab.io.homekit.library.characteristic.IdentifyCharacteristic;
import org.openhab.io.homekit.library.characteristic.ManufacturerCharacteristic;
import org.openhab.io.homekit.library.characteristic.ModelCharacteristic;
import org.openhab.io.homekit.library.characteristic.SerialNumberCharacteristic;

public class AccessoryInformationService extends GenericService {

    public AccessoryInformationService(Accessory accessory, long instanceId, boolean extend,
            @NonNull String serviceName) {
        super(accessory, instanceId, extend, serviceName);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        // addCharacteristic(new FirmwareRevisionCharacteristic(manager, this, getAccessory().getInstanceId()));
        addCharacteristic(new IdentifyCharacteristic(this, ((Accessory) getAccessory()).getNextAvailableInstanceId()));
        addCharacteristic(
                new ManufacturerCharacteristic(this, ((Accessory) getAccessory()).getNextAvailableInstanceId()));
        addCharacteristic(new ModelCharacteristic(this, ((Accessory) getAccessory()).getNextAvailableInstanceId()));
        addCharacteristic(
                new SerialNumberCharacteristic(this, ((Accessory) getAccessory()).getNextAvailableInstanceId()));
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

    public static String getTag() {
        return AccessoryInformationService.class.getSimpleName().replace("Service", "");
    }
}
