package org.openhab.io.homekit.library.service;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.ManagedAccessory;
import org.openhab.io.homekit.internal.service.AbstractManagedService;
import org.openhab.io.homekit.library.characteristic.CurrentHorizontalTiltAngleCharacteristic;
import org.openhab.io.homekit.library.characteristic.CurrentPositionCharacteristic;
import org.openhab.io.homekit.library.characteristic.CurrentVerticalTiltAngleCharacteristic;
import org.openhab.io.homekit.library.characteristic.HoldPositionCharacteristic;
import org.openhab.io.homekit.library.characteristic.ObstructionDetectedCharacteristic;
import org.openhab.io.homekit.library.characteristic.PositionStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.TargetHorizontalTiltAngleCharacteristic;
import org.openhab.io.homekit.library.characteristic.TargetPositionCharacteristic;
import org.openhab.io.homekit.library.characteristic.TargetVerticalTiltAngleCharacteristic;

public class WindowCoveringService extends AbstractManagedService {

    public WindowCoveringService(HomekitCommunicationManager manager, ManagedAccessory accessory, long instanceId,
            boolean extend, @NonNull String serviceName) throws Exception {
        super(manager, accessory, instanceId, extend, serviceName);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(new TargetPositionCharacteristic(getManager(), this,
                ((ManagedAccessory) getAccessory()).getInstanceId()));
        addCharacteristic(new CurrentPositionCharacteristic(getManager(), this,
                ((ManagedAccessory) getAccessory()).getInstanceId()));
        addCharacteristic(new PositionStateCharacteristic(getManager(), this,
                ((ManagedAccessory) getAccessory()).getInstanceId()));
        addCharacteristic(new HoldPositionCharacteristic(getManager(), this,
                ((ManagedAccessory) getAccessory()).getInstanceId()));
        addCharacteristic(new CurrentHorizontalTiltAngleCharacteristic(getManager(), this,
                ((ManagedAccessory) getAccessory()).getInstanceId()));
        addCharacteristic(new TargetHorizontalTiltAngleCharacteristic(getManager(), this,
                ((ManagedAccessory) getAccessory()).getInstanceId()));
        addCharacteristic(new CurrentVerticalTiltAngleCharacteristic(getManager(), this,
                ((ManagedAccessory) getAccessory()).getInstanceId()));
        addCharacteristic(new TargetVerticalTiltAngleCharacteristic(getManager(), this,
                ((ManagedAccessory) getAccessory()).getInstanceId()));
        addCharacteristic(new ObstructionDetectedCharacteristic(getManager(), this,
                ((ManagedAccessory) getAccessory()).getInstanceId()));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }

    public static String getType() {
        return "0000008C-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }
}
