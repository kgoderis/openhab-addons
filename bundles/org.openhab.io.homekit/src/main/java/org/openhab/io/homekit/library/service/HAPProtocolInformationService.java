package org.openhab.io.homekit.library.service;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.internal.service.GenericService;
import org.openhab.io.homekit.library.characteristic.VersionCharacteristic;
import javax.json.JsonValue;

public class HAPProtocolInformationService extends GenericService {

    public HAPProtocolInformationService(Accessory accessory, long instanceId, boolean extend,
            @NonNull String serviceName) throws Exception {
        super(accessory, instanceId, extend, serviceName);
    }

    public HAPProtocolInformationService(Accessory accessory, JsonValue value, String name) {
        super(accessory, value, name);
    }

    @Override
    public void addCharacteristics() {
        VersionCharacteristic characteristic = new VersionCharacteristic(this,
                getAccessory().getNextAvailableInstanceId());
        characteristic.setVersion("01.01.00");
        addCharacteristic(characteristic);
    }

    @Override
    public boolean isExtensible() {
        return false;
    }

    public static String getType() {
        return "000000A2-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    public static String getTag() {
        return HAPProtocolInformationService.class.getSimpleName().replace("Service", "");
    }
}
