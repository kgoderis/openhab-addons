package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitBooleanCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Is Configured Characteristic.
 * This characteristic represents whether the accessory is configured or not.
 * When true, the accessory is configured and ready to use; when false, the accessory needs configuration.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "000000D6-0000-1000-8000-0026BB765291", name = "Is Configured", tag = "isConfigured", acceptedItemTypes = {
        "Switch", "Contact" })
@NonNullByDefault
public class HomekitIsConfiguredCharacteristic extends HomekitBooleanCharacteristic {

    /**
     * Creates a new Is Configured characteristic.
     * This characteristic indicates whether the accessory is configured and ready to use.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitIsConfiguredCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("Is Configured");
    }

    /**
     * Creates a new Is Configured characteristic from a JSON value.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitIsConfiguredCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }
}
