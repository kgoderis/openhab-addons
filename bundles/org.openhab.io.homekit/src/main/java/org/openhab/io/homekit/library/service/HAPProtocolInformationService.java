package org.openhab.io.homekit.library.service;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.internal.service.AbstractService;
import org.openhab.io.homekit.library.characteristic.VersionCharacteristic;

public class HAPProtocolInformationService extends AbstractService {

    public HAPProtocolInformationService(HomekitCommunicationManager manager, Accessory accessory, long instanceId,
            boolean extend, @NonNull String serviceName) throws Exception {
        super(manager, accessory, instanceId, extend, serviceName);
    }

    @Override
    public void addCharacteristics() throws Exception {
        VersionCharacteristic characteristic = new VersionCharacteristic(manager, this,
                this.getAccessory().getInstanceId());
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
}
