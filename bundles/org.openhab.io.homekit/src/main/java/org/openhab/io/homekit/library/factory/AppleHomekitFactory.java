package org.openhab.io.homekit.library.factory;

import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.openhab.io.homekit.exception.HomekitRegistrationException;
import org.openhab.io.homekit.internal.factory.AbstractHomekitFactory;
import org.openhab.io.homekit.library.characteristic.BrightnessCharacteristic;
import org.openhab.io.homekit.library.characteristic.ColorTemperatureCharacteristic;
import org.openhab.io.homekit.library.characteristic.CurrentHeatingCoolingStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.CurrentTemperatureCharacteristic;
import org.openhab.io.homekit.library.characteristic.FirmwareRevisionCharacteristic;
import org.openhab.io.homekit.library.characteristic.HueCharacteristic;
import org.openhab.io.homekit.library.characteristic.IdentifyCharacteristic;
import org.openhab.io.homekit.library.characteristic.ManufacturerCharacteristic;
import org.openhab.io.homekit.library.characteristic.ModelCharacteristic;
import org.openhab.io.homekit.library.characteristic.NameCharacteristic;
import org.openhab.io.homekit.library.characteristic.OnCharacteristic;
import org.openhab.io.homekit.library.characteristic.OutletInUseCharacteristic;
import org.openhab.io.homekit.library.characteristic.SaturationCharacteristic;
import org.openhab.io.homekit.library.characteristic.SerialNumberCharacteristic;
import org.openhab.io.homekit.library.characteristic.StatusLowBatteryCharacteristic;
import org.openhab.io.homekit.library.characteristic.TargetHeatingCoolingStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.TemperatureDisplayUnitsCharacteristic;
import org.openhab.io.homekit.library.characteristic.VersionCharacteristic;
import org.openhab.io.homekit.library.service.AccessoryInformationService;
import org.openhab.io.homekit.library.service.HAPProtocolInformationService;
import org.openhab.io.homekit.library.service.LightBulbService;
import org.openhab.io.homekit.library.service.OutletService;
import org.openhab.io.homekit.library.service.SwitchService;
import org.openhab.io.homekit.library.service.ThermostatService;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = HomekitFactory.class)
public class AppleHomekitFactory extends AbstractHomekitFactory {
    @Override
    protected void initializeMappers() {
        try {
            this.addService(AccessoryInformationService.class);
            this.addService(HAPProtocolInformationService.class);
            this.addService(LightBulbService.class);
            this.addService(OutletService.class);
            this.addService(SwitchService.class);
            this.addService(ThermostatService.class);

            this.addCharacteristic(BrightnessCharacteristic.class);
            this.addCharacteristic(ColorTemperatureCharacteristic.class);
            this.addCharacteristic(CurrentHeatingCoolingStateCharacteristic.class);
            this.addCharacteristic(CurrentTemperatureCharacteristic.class);
            this.addCharacteristic(FirmwareRevisionCharacteristic.class);
            this.addCharacteristic(HueCharacteristic.class);
            this.addCharacteristic(IdentifyCharacteristic.class);
            this.addCharacteristic(ManufacturerCharacteristic.class);
            this.addCharacteristic(ModelCharacteristic.class);
            this.addCharacteristic(NameCharacteristic.class);
            this.addCharacteristic(OnCharacteristic.class);
            this.addCharacteristic(OutletInUseCharacteristic.class);
            this.addCharacteristic(SaturationCharacteristic.class);
            this.addCharacteristic(SerialNumberCharacteristic.class);
            this.addCharacteristic(StatusLowBatteryCharacteristic.class);
            this.addCharacteristic(TargetHeatingCoolingStateCharacteristic.class);
            this.addCharacteristic(TemperatureDisplayUnitsCharacteristic.class);
            this.addCharacteristic(VersionCharacteristic.class);
        } catch (HomekitRegistrationException e) {
            logger.error("{}Failed to initialize mappers in AppleHomekitFactory: {}", AbstractHomekitFactory.LOG_ERROR,
                    e.getMessage(), e);
        }
    }
}
