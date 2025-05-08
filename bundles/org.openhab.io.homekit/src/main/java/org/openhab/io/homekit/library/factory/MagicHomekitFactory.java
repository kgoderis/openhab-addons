package org.openhab.io.homekit.library.factory;

import org.openhab.core.magic.binding.MagicBindingConstants;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.openhab.io.homekit.exception.HomekitRegistrationException;
import org.openhab.io.homekit.internal.factory.AbstractHomekitFactory;
import org.openhab.io.homekit.library.characteristic.HomekitBrightnessCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitCurrentTemperatureCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitHueCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitOnCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSaturationCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitTargetTemperatureCharacteristic;
import org.openhab.io.homekit.library.service.HomekitColorLightBulbService;
import org.openhab.io.homekit.library.service.HomekitContactSensorService;
import org.openhab.io.homekit.library.service.HomekitDimmableLightBulbService;
import org.openhab.io.homekit.library.service.HomekitLightBulbService;
import org.openhab.io.homekit.library.service.HomekitThermostatService;
import org.openhab.io.homekit.library.service.HomekitWindowCoveringService;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = HomekitFactory.class)
public class MagicHomekitFactory extends AbstractHomekitFactory {

    @Override
    protected void initializeMappers() {
        try {
            this.addService(MagicBindingConstants.THING_TYPE_ON_OFF_LIGHT, HomekitLightBulbService.class);
            this.addService(MagicBindingConstants.THING_TYPE_DIMMABLE_LIGHT, HomekitDimmableLightBulbService.class);
            this.addService(MagicBindingConstants.THING_TYPE_COLOR_LIGHT, HomekitColorLightBulbService.class);
            this.addService(MagicBindingConstants.THING_TYPE_CONTACT_SENSOR, HomekitContactSensorService.class);
            this.addService(MagicBindingConstants.THING_TYPE_THERMOSTAT, HomekitThermostatService.class);
            this.addService(MagicBindingConstants.THING_TYPE_ROLLERSHUTTER, HomekitWindowCoveringService.class);

            this.addCharacteristic(
                    new ChannelTypeUID(MagicBindingConstants.BINDING_ID, MagicBindingConstants.CHANNEL_SWITCH),
                    HomekitOnCharacteristic.class);

            this.addCharacteristic(
                    new ChannelTypeUID(MagicBindingConstants.BINDING_ID, MagicBindingConstants.CHANNEL_BRIGHTNESS),
                    HomekitBrightnessCharacteristic.class);

            this.addCharacteristic(
                    new ChannelTypeUID(MagicBindingConstants.BINDING_ID, MagicBindingConstants.CHANNEL_COLOR),
                    HomekitHueCharacteristic.class);
            this.addCharacteristic(
                    new ChannelTypeUID(MagicBindingConstants.BINDING_ID, MagicBindingConstants.CHANNEL_COLOR),
                    HomekitSaturationCharacteristic.class);
            this.addCharacteristic(
                    new ChannelTypeUID(MagicBindingConstants.BINDING_ID, MagicBindingConstants.CHANNEL_COLOR),
                    HomekitBrightnessCharacteristic.class);

            this.addCharacteristic(
                    new ChannelTypeUID(MagicBindingConstants.BINDING_ID, MagicBindingConstants.CHANNEL_CONTACT),
                    HomekitOnCharacteristic.class);

            this.addCharacteristic(
                    new ChannelTypeUID(MagicBindingConstants.BINDING_ID, MagicBindingConstants.CHANNEL_TEMPERATURE),
                    HomekitCurrentTemperatureCharacteristic.class);
            this.addCharacteristic(
                    new ChannelTypeUID(MagicBindingConstants.BINDING_ID, MagicBindingConstants.CHANNEL_SET_TEMPERATURE),
                    HomekitTargetTemperatureCharacteristic.class);
        } catch (HomekitRegistrationException e) {
            logger.error("{}Failed to initialize mappers in MagicHomekitFactory: {}", AbstractHomekitFactory.LOG_ERROR,
                    e.getMessage(), e);
        }
    }
}
