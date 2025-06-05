package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.exception.HomekitServiceException;
import org.openhab.io.homekit.library.characteristic.HomekitActiveCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitCurrentHumidifierDehumidifierStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitCurrentRelativeHumidityCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitNameCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitRelativeHumidityDehumidifierThresholdCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitRelativeHumidityHumidifierThresholdCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitRotationSpeedCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitTargetHumidifierDehumidifierStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitWaterLevelCharacteristic;

/**
 * Service that represents a humidifier/dehumidifier in HomeKit.
 * This service provides control over humidity levels and device operation.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitServiceType(type = "000000BD-0000-1000-8000-0026BB765291", name = "Humidifier Dehumidifier", tag = "humidifierDehumidifier")
@NonNullByDefault
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
        withName("Humidifier Dehumidifier").withPrimary(false).withHidden(false);
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
    public void addCharacteristics() throws HomekitServiceException {
        addCharacteristic(
                new HomekitActiveCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        addCharacteristic(new HomekitCurrentHumidifierDehumidifierStateCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitTargetHumidifierDehumidifierStateCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitCurrentRelativeHumidityCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitRelativeHumidityHumidifierThresholdCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        addCharacteristic(new HomekitRelativeHumidityDehumidifierThresholdCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        addCharacteristic(
                new HomekitRotationSpeedCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        addCharacteristic(
                new HomekitWaterLevelCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        addCharacteristic(new HomekitNameCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                .withMandatory(false));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }
}
