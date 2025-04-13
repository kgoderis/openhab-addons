package org.openhab.io.homekit.library.service;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.internal.service.GenericService;
import org.openhab.io.homekit.library.characteristic.ContactSensorStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.StatusActiveCharacteristic;
import org.openhab.io.homekit.library.characteristic.StatusFaultCharacteristic;
import org.openhab.io.homekit.library.characteristic.StatusLowBatteryCharacteristic;
import org.openhab.io.homekit.library.characteristic.StatusTamperedCharacteristic;
import javax.json.JsonValue;

public class ContactSensorService extends GenericService {

    public ContactSensorService(Accessory accessory, long instanceId, boolean extend, @NonNull String serviceName)
            throws Exception {
        super(accessory, instanceId, extend, serviceName);
    }

    public ContactSensorService(Accessory accessory, JsonValue value, String name) {
        super(accessory, value, name);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(
                new ContactSensorStateCharacteristic(this, ((Accessory) getAccessory()).getNextAvailableInstanceId()));
        addCharacteristic(
                new StatusActiveCharacteristic(this, ((Accessory) getAccessory()).getNextAvailableInstanceId()));
        addCharacteristic(
                new StatusFaultCharacteristic(this, ((Accessory) getAccessory()).getNextAvailableInstanceId()));
        addCharacteristic(
                new StatusTamperedCharacteristic(this, ((Accessory) getAccessory()).getNextAvailableInstanceId()));
        addCharacteristic(
                new StatusLowBatteryCharacteristic(this, ((Accessory) getAccessory()).getNextAvailableInstanceId()));
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

    public static String getTag() {
        return ContactSensorService.class.getSimpleName().replace("Service", "");
    }
}
