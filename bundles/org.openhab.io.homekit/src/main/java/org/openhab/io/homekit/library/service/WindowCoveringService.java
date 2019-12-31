package org.openhab.io.homekit.library.service;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.internal.service.AbstractService;
import org.openhab.io.homekit.library.characteristic.CurrentHorizontalTiltAngleCharacteristic;
import org.openhab.io.homekit.library.characteristic.CurrentPositionCharacteristic;
import org.openhab.io.homekit.library.characteristic.CurrentVerticalTiltAngleCharacteristic;
import org.openhab.io.homekit.library.characteristic.HoldPositionCharacteristic;
import org.openhab.io.homekit.library.characteristic.ObstructionDetectedCharacteristic;
import org.openhab.io.homekit.library.characteristic.PositionStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.TargetHorizontalTiltAngleCharacteristic;
import org.openhab.io.homekit.library.characteristic.TargetPositionCharacteristic;
import org.openhab.io.homekit.library.characteristic.TargetVerticalTiltAngleCharacteristic;

public class WindowCoveringService extends AbstractService {

    public WindowCoveringService(HomekitCommunicationManager manager, Accessory accessory, long instanceId,
            boolean extend, @NonNull String serviceName) throws Exception {
        super(manager, accessory, instanceId, extend, serviceName);
    }

    @Override
    public void addCharacteristics() throws Exception {
        super.addCharacteristics();
        addCharacteristic(new TargetPositionCharacteristic(manager, this, this.getAccessory().getInstanceId()));
        addCharacteristic(new CurrentPositionCharacteristic(manager, this, this.getAccessory().getInstanceId()));
        addCharacteristic(new PositionStateCharacteristic(manager, this, this.getAccessory().getInstanceId()));
        addCharacteristic(new HoldPositionCharacteristic(manager, this, this.getAccessory().getInstanceId()));
        addCharacteristic(
                new CurrentHorizontalTiltAngleCharacteristic(manager, this, this.getAccessory().getInstanceId()));
        addCharacteristic(
                new TargetHorizontalTiltAngleCharacteristic(manager, this, this.getAccessory().getInstanceId()));
        addCharacteristic(
                new CurrentVerticalTiltAngleCharacteristic(manager, this, this.getAccessory().getInstanceId()));
        addCharacteristic(
                new TargetVerticalTiltAngleCharacteristic(manager, this, this.getAccessory().getInstanceId()));
        addCharacteristic(new ObstructionDetectedCharacteristic(manager, this, this.getAccessory().getInstanceId()));
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
