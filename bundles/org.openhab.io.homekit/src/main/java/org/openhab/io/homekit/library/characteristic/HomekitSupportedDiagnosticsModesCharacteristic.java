package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Supported Diagnostics Modes Characteristic.
 * This characteristic represents the supported diagnostics modes for a device.
 *
 * @author Karel Goderis - Initial Contribution
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "0000024C-0000-1000-8000-0026BB765291", name = "Supported Diagnostics Modes", tag = "supportedDiagnosticsModes", acceptedItemTypes = {
        "Number" })
@NonNullByDefault
public class HomekitSupportedDiagnosticsModesCharacteristic extends HomekitIntegerCharacteristic {

    /**
     * Constructs a new Supported Diagnostics Modes characteristic.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param instanceId the instance ID for this characteristic
     */
    public HomekitSupportedDiagnosticsModesCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 0, 255, "");
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Supported Diagnostics Modes");
    }

    /**
     * Constructs a new Supported Diagnostics Modes characteristic from a JSON value.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param value the JSON value to initialize the characteristic with
     */
    public HomekitSupportedDiagnosticsModesCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is a valid supported diagnostics mode (0-255).
     *
     * @param value the value to check
     * @return true if the value is between 0 and 255, false otherwise
     */
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value <= 255;
    }
}
