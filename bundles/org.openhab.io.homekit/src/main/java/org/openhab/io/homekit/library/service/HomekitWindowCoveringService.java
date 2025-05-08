package org.openhab.io.homekit.library.service;

import java.util.Collection;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.openhab.io.homekit.api.hap.HomekitAccessory;
import org.openhab.io.homekit.internal.events.HomekitEventManager;
import org.openhab.io.homekit.internal.service.HomekitBaseService;
import org.openhab.io.homekit.library.characteristic.HomekitCurrentHorizontalTiltAngleCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitCurrentPositionCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitCurrentVerticalTiltAngleCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitHoldPositionCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitObstructionDetectedCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitPositionStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitTargetHorizontalTiltAngleCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitTargetPositionCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitTargetVerticalTiltAngleCharacteristic;

public class HomekitWindowCoveringService extends HomekitBaseService {
    private static final String TYPE = "0000008C-0000-1000-8000-0026BB765291";

    public HomekitWindowCoveringService(HomekitAccessory accessory, long instanceId, boolean extend, @NonNull String serviceName, HomekitEventManager eventManager, Collection<HomekitFactory> factories)
           {
        super(accessory, instanceId, extend, serviceName, TYPE, eventManager, factories);
    }

    public HomekitWindowCoveringService(HomekitAccessory accessory, JsonValue value, String name, HomekitEventManager eventManager, Collection<HomekitFactory> factories) {
        super(accessory, value, name, eventManager, factories);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(
                new HomekitTargetPositionCharacteristic(this, getAccessory().getNextAvailableInstanceId(), eventManager));
        addCharacteristic(
                new HomekitCurrentPositionCharacteristic(this, getAccessory().getNextAvailableInstanceId(), eventManager));
        addCharacteristic(
                new HomekitPositionStateCharacteristic(this, getAccessory().getNextAvailableInstanceId(), eventManager));
        addCharacteristic(
                new HomekitHoldPositionCharacteristic(this, getAccessory().getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitCurrentHorizontalTiltAngleCharacteristic(this,
                getAccessory().getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitTargetHorizontalTiltAngleCharacteristic(this, getAccessory().getNextAvailableInstanceId(),
                eventManager));
        addCharacteristic(new HomekitCurrentVerticalTiltAngleCharacteristic(this, getAccessory().getNextAvailableInstanceId(),
                eventManager));
        addCharacteristic(new HomekitTargetVerticalTiltAngleCharacteristic(this, getAccessory().getNextAvailableInstanceId(),
                eventManager));
        addCharacteristic(
                new HomekitObstructionDetectedCharacteristic(this, getAccessory().getNextAvailableInstanceId(), eventManager));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }

    public static String getType() {
        return TYPE;
    }

    public static String getTag() {
        return HomekitWindowCoveringService.class.getSimpleName().replace("HomekitService", "");
    }
}
