package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitActiveCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitCurrentHumidifierDehumidifierStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitCurrentRelativeHumidityCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitRelativeHumidityDehumidifierThresholdCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitRelativeHumidityHumidifierThresholdCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitRotationSpeedCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSwingModeCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitTargetHumidifierDehumidifierStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitWaterLevelCharacteristic;

@HomekitServiceType(type = "000000BD-0000-1000-8000-0026BB765291", name = "HumidifierDehumidifier", tag = "HumidifierDehumidifier")
public class HomekitHumidifierDehumidifierService extends AbstractHomekitService {
    /**
     * Creates a new HomekitHumidifierDehumidifierService.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     */
    public HomekitHumidifierDehumidifierService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
    }

    /**
     * Creates a new HomekitHumidifierDehumidifierService from a JSON value.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     */
    public HomekitHumidifierDehumidifierService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(
                new HomekitActiveCharacteristic(this, getAccessory().getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitCurrentHumidifierDehumidifierStateCharacteristic(this,
                getAccessory().getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitTargetHumidifierDehumidifierStateCharacteristic(this,
                getAccessory().getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitCurrentRelativeHumidityCharacteristic(this,
                getAccessory().getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitRelativeHumidityDehumidifierThresholdCharacteristic(this,
                getAccessory().getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitRelativeHumidityHumidifierThresholdCharacteristic(this,
                getAccessory().getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitRotationSpeedCharacteristic(this, getAccessory().getNextAvailableInstanceId(),
                eventManager));
        addCharacteristic(
                new HomekitSwingModeCharacteristic(this, getAccessory().getNextAvailableInstanceId(), eventManager));
        addCharacteristic(
                new HomekitWaterLevelCharacteristic(this, getAccessory().getNextAvailableInstanceId(), eventManager));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }
}
