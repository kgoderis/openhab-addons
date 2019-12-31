package org.openhab.io.homekit.library.service;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.ManagedAccessory;
import org.openhab.io.homekit.internal.service.AbstractManagedService;
import org.openhab.io.homekit.library.characteristic.OnCharacteristic;
import org.openhab.io.homekit.library.characteristic.OutletInUseCharacteristic;

public class OutletService extends AbstractManagedService {

    public OutletService(HomekitCommunicationManager manager, ManagedAccessory accessory, long instanceId,
            boolean extend, @NonNull String serviceName) throws Exception {
        super(manager, accessory, instanceId, extend, serviceName);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(
                new OnCharacteristic(getManager(), this, ((ManagedAccessory) getAccessory()).getInstanceId()));
        addCharacteristic(
                new OutletInUseCharacteristic(getManager(), this, ((ManagedAccessory) getAccessory()).getInstanceId()));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }

    public static String getType() {
        return "00000047-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }
}
