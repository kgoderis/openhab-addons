package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitAirParticulateDensityCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitAirParticulateSizeCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitAirQualityCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitNitrogenDioxideDensityCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitOzoneDensityCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitPM10DensityCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSulphurDioxideDensityCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitVOCDensityCharacteristic;

/**
 * Service that represents an air quality sensor in HomeKit.
 * This service provides information about air quality and various air pollutants.
 * 
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitServiceType(type = "0000008D-0000-1000-8000-0026BB765291", name = "Air Quality Sensor", tag = "airQualitySensor")
@NonNullByDefault
public class HomekitAirQualitySensorService extends AbstractHomekitService {

    /**
     * Creates a new HomekitAirQualitySensorService.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     */
    public HomekitAirQualitySensorService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Air Quality Sensor").withPrimary(false).withHidden(false);
    }

    /**
     * Creates a new HomekitAirQualitySensorService from a JSON value.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     */
    public HomekitAirQualitySensorService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    @Override
    public void addCharacteristics() {
        addCharacteristic(
                new HomekitAirQualityCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        addCharacteristic(new HomekitAirParticulateDensityCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        addCharacteristic(new HomekitAirParticulateSizeCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        addCharacteristic(
                new HomekitOzoneDensityCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        addCharacteristic(new HomekitNitrogenDioxideDensityCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        addCharacteristic(new HomekitSulphurDioxideDensityCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        addCharacteristic(
                new HomekitPM10DensityCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        addCharacteristic(
                new HomekitVOCDensityCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }
}
