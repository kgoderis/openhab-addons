package org.openhab.io.homekit.library.factory;

import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.openhab.io.homekit.core.factory.AbstractHomekitFactory;
import org.openhab.io.homekit.exception.HomekitRegistrationException;
import org.openhab.io.homekit.library.characteristic.HomekitBrightnessCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitColorTemperatureCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitCurrentHeatingCoolingStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitCurrentTemperatureCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitFirmwareRevisionCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitHueCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitIdentifyCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitManufacturerCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitModelCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitNameCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitOnCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitOutletInUseCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSaturationCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSerialNumberCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusLowBatteryCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitTargetHeatingCoolingStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitTemperatureDisplayUnitsCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitVersionCharacteristic;
import org.openhab.io.homekit.library.service.HomekitAccessoryInformationService;
import org.openhab.io.homekit.library.service.HomekitHAPProtocolInformationService;
import org.openhab.io.homekit.library.service.HomekitLightBulbService;
import org.openhab.io.homekit.library.service.HomekitOutletService;
import org.openhab.io.homekit.library.service.HomekitSwitchService;
import org.openhab.io.homekit.library.service.HomekitThermostatService;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = HomekitFactory.class)
public class AppleHomekitFactory extends AbstractHomekitFactory {
    @Override
    protected void initializeMappers() {
        try {
            this.addService(HomekitAccessoryInformationService.class);
            this.addService(HomekitHAPProtocolInformationService.class);
            this.addService(HomekitLightBulbService.class);
            this.addService(HomekitOutletService.class);
            this.addService(HomekitSwitchService.class);
            this.addService(HomekitThermostatService.class);

            this.addCharacteristic(HomekitBrightnessCharacteristic.class);
            this.addCharacteristic(HomekitColorTemperatureCharacteristic.class);
            this.addCharacteristic(HomekitCurrentHeatingCoolingStateCharacteristic.class);
            this.addCharacteristic(HomekitCurrentTemperatureCharacteristic.class);
            this.addCharacteristic(HomekitFirmwareRevisionCharacteristic.class);
            this.addCharacteristic(HomekitHueCharacteristic.class);
            this.addCharacteristic(HomekitIdentifyCharacteristic.class);
            this.addCharacteristic(HomekitManufacturerCharacteristic.class);
            this.addCharacteristic(HomekitModelCharacteristic.class);
            this.addCharacteristic(HomekitNameCharacteristic.class);
            this.addCharacteristic(HomekitOnCharacteristic.class);
            this.addCharacteristic(HomekitOutletInUseCharacteristic.class);
            this.addCharacteristic(HomekitSaturationCharacteristic.class);
            this.addCharacteristic(HomekitSerialNumberCharacteristic.class);
            this.addCharacteristic(HomekitStatusLowBatteryCharacteristic.class);
            this.addCharacteristic(HomekitTargetHeatingCoolingStateCharacteristic.class);
            this.addCharacteristic(HomekitTemperatureDisplayUnitsCharacteristic.class);
            this.addCharacteristic(HomekitVersionCharacteristic.class);
        } catch (HomekitRegistrationException e) {
            logger.error("{}Failed to initialize mappers in AppleHomekitFactory: {}", AbstractHomekitFactory.LOG_ERROR,
                    e.getMessage(), e);
        }
    }
}
