package org.openhab.io.homekit.library.service;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.internal.service.GenericService;
import org.openhab.io.homekit.library.characteristic.CurrentHorizontalTiltAngleCharacteristic;
import org.openhab.io.homekit.library.characteristic.CurrentPositionCharacteristic;
import org.openhab.io.homekit.library.characteristic.CurrentVerticalTiltAngleCharacteristic;
import org.openhab.io.homekit.library.characteristic.HoldPositionCharacteristic;
import org.openhab.io.homekit.library.characteristic.ObstructionDetectedCharacteristic;
import org.openhab.io.homekit.library.characteristic.PositionStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.TargetHorizontalTiltAngleCharacteristic;
import org.openhab.io.homekit.library.characteristic.TargetPositionCharacteristic;
import org.openhab.io.homekit.library.characteristic.TargetVerticalTiltAngleCharacteristic;

public class WindowCoveringService extends GenericService {

    public WindowCoveringService(Accessory accessory, long instanceId, boolean extend, @NonNull String serviceName)
            throws Exception {
        super(accessory, instanceId, extend, serviceName);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(new TargetPositionCharacteristic(this, getAccessory().getNextAvailableInstanceId()));
        addCharacteristic(new CurrentPositionCharacteristic(this, getAccessory().getNextAvailableInstanceId()));
        addCharacteristic(new PositionStateCharacteristic(this, getAccessory().getNextAvailableInstanceId()));
        addCharacteristic(new HoldPositionCharacteristic(this, getAccessory().getNextAvailableInstanceId()));
        addCharacteristic(
                new CurrentHorizontalTiltAngleCharacteristic(this, getAccessory().getNextAvailableInstanceId()));
        addCharacteristic(
                new TargetHorizontalTiltAngleCharacteristic(this, getAccessory().getNextAvailableInstanceId()));
        addCharacteristic(
                new CurrentVerticalTiltAngleCharacteristic(this, getAccessory().getNextAvailableInstanceId()));
        addCharacteristic(new TargetVerticalTiltAngleCharacteristic(this, getAccessory().getNextAvailableInstanceId()));
        addCharacteristic(new ObstructionDetectedCharacteristic(this, getAccessory().getNextAvailableInstanceId()));
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

    public static String getTag() {
        return WindowCoveringService.class.getSimpleName().replace("Service", "");
    }
}
