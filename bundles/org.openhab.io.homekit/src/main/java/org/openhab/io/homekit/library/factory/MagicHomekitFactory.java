package org.openhab.io.homekit.library.factory;

import org.openhab.core.magic.binding.MagicBindingConstants;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.io.homekit.BaseHomekitFactory;
import org.openhab.io.homekit.api.HomekitFactory;
import org.openhab.io.homekit.library.characteristic.BrightnessCharacteristic;
import org.openhab.io.homekit.library.characteristic.CurrentTemperatureCharacteristic;
import org.openhab.io.homekit.library.characteristic.HueCharacteristic;
import org.openhab.io.homekit.library.characteristic.OnCharacteristic;
import org.openhab.io.homekit.library.characteristic.SaturationCharacteristic;
import org.openhab.io.homekit.library.characteristic.TargetTemperatureCharacteristic;
import org.openhab.io.homekit.library.service.ColorLightBulbService;
import org.openhab.io.homekit.library.service.ContactSensorService;
import org.openhab.io.homekit.library.service.DimmableLightBulbService;
import org.openhab.io.homekit.library.service.LightBulbService;
import org.openhab.io.homekit.library.service.ThermostatService;
import org.openhab.io.homekit.library.service.WindowCoveringService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = HomekitFactory.class)
public class MagicHomekitFactory extends BaseHomekitFactory {

    @Activate
    public MagicHomekitFactory() {
        super();

        this.addService(MagicBindingConstants.THING_TYPE_ON_OFF_LIGHT, LightBulbService.class);
        this.addService(MagicBindingConstants.THING_TYPE_DIMMABLE_LIGHT, DimmableLightBulbService.class);
        this.addService(MagicBindingConstants.THING_TYPE_COLOR_LIGHT, ColorLightBulbService.class);
        this.addService(MagicBindingConstants.THING_TYPE_CONTACT_SENSOR, ContactSensorService.class);
        this.addService(MagicBindingConstants.THING_TYPE_THERMOSTAT, ThermostatService.class);
        this.addService(MagicBindingConstants.THING_TYPE_ROLLERSHUTTER, WindowCoveringService.class);

        this.addCharacteristic(
                new ChannelTypeUID(MagicBindingConstants.BINDING_ID, MagicBindingConstants.CHANNEL_SWITCH),
                OnCharacteristic.class);

        this.addCharacteristic(
                new ChannelTypeUID(MagicBindingConstants.BINDING_ID, MagicBindingConstants.CHANNEL_BRIGHTNESS),
                BrightnessCharacteristic.class);

        this.addCharacteristic(
                new ChannelTypeUID(MagicBindingConstants.BINDING_ID, MagicBindingConstants.CHANNEL_COLOR),
                HueCharacteristic.class);
        this.addCharacteristic(
                new ChannelTypeUID(MagicBindingConstants.BINDING_ID, MagicBindingConstants.CHANNEL_COLOR),
                SaturationCharacteristic.class);
        this.addCharacteristic(
                new ChannelTypeUID(MagicBindingConstants.BINDING_ID, MagicBindingConstants.CHANNEL_COLOR),
                BrightnessCharacteristic.class);

        this.addCharacteristic(
                new ChannelTypeUID(MagicBindingConstants.BINDING_ID, MagicBindingConstants.CHANNEL_CONTACT),
                OnCharacteristic.class);

        this.addCharacteristic(
                new ChannelTypeUID(MagicBindingConstants.BINDING_ID, MagicBindingConstants.CHANNEL_TEMPERATURE),
                CurrentTemperatureCharacteristic.class);
        this.addCharacteristic(
                new ChannelTypeUID(MagicBindingConstants.BINDING_ID, MagicBindingConstants.CHANNEL_SET_TEMPERATURE),
                TargetTemperatureCharacteristic.class);

    }
}
