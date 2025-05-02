package org.openhab.io.homekit.library.service;

import java.util.Collection;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.internal.events.HomekitEventManager;
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
    private static final String TYPE = "0000008C-0000-1000-8000-0026BB765291";

    public WindowCoveringService(Accessory accessory, long instanceId, boolean extend, @NonNull String serviceName, HomekitEventManager eventManager, Collection<HomekitFactory> factories)
           {
        super(accessory, instanceId, extend, serviceName, TYPE, eventManager, factories);
    }

    public WindowCoveringService(Accessory accessory, JsonValue value, String name, HomekitEventManager eventManager, Collection<HomekitFactory> factories) {
        super(accessory, value, name, eventManager, factories);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(new TargetPositionCharacteristic(this, getAccessory().getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new CurrentPositionCharacteristic(this, getAccessory().getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new PositionStateCharacteristic(this, getAccessory().getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HoldPositionCharacteristic(this, getAccessory().getNextAvailableInstanceId(), eventManager));
        addCharacteristic(
                new CurrentHorizontalTiltAngleCharacteristic(this, getAccessory().getNextAvailableInstanceId(), eventManager));
        addCharacteristic(
                new TargetHorizontalTiltAngleCharacteristic(this, getAccessory().getNextAvailableInstanceId(), eventManager));
        addCharacteristic(
                new CurrentVerticalTiltAngleCharacteristic(this, getAccessory().getNextAvailableInstanceId(), eventManager));
        addCharacteristic(
                new TargetVerticalTiltAngleCharacteristic(this, getAccessory().getNextAvailableInstanceId(), eventManager));
        addCharacteristic(
                new ObstructionDetectedCharacteristic(this, getAccessory().getNextAvailableInstanceId(), eventManager));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }

    public static String getType() {
        return TYPE;
    }

    public static String getTag() {
        return WindowCoveringService.class.getSimpleName().replace("Service", "");
    }
}
