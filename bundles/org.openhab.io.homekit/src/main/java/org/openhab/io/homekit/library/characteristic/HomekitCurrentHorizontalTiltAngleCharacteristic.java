package org.openhab.io.homekit.library.characteristic;

import java.util.Set;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Current Horizontal Tilt Angle Characteristic.
 * <p>
 * This characteristic represents the current horizontal tilt angle for a device (e.g., window covering), measured in
 * arcdegrees. The value indicates the current position of the horizontal slats. See the HAP specification for valid
 * value range and usage.
 * <p>
 * See the HomeKit Accessory Protocol (HAP) specification for details: https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitCharacteristicType(type = "0000006C-0000-1000-8000-0026BB765291", name = "Current Horizontal Tilt Angle", tag = "currentHorizontalTiltAngle", acceptedItemTypes = {"Number", "Rollershutter"})
@NonNullByDefault
public class HomekitCurrentHorizontalTiltAngleCharacteristic extends HomekitIntegerCharacteristic {

    /**
     * Constructs a new Current Horizontal Tilt Angle characteristic.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param instanceId the instance ID for this characteristic
     */
    public HomekitCurrentHorizontalTiltAngleCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, -90, 90, "arcdegrees");
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("Current Horizontal Tilt Angle");
    }

    /**
     * Constructs a new Current Horizontal Tilt Angle characteristic from a JSON value.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param value the JSON value to initialize the characteristic with
     */
    public HomekitCurrentHorizontalTiltAngleCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is an allowed horizontal tilt angle.
     *
     * @param value the value to check
     * @return true if the value is allowed, false otherwise
     */
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= -90 && value <= 90;
    }

    /**
     * Returns the set of allowed horizontal tilt angle values (empty set for continuous range).
     *
     * @return the set of allowed values
     */
    @Override
    public Set<Integer> getAllowedValues() {
        return Set.of();
    }
}
