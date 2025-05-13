package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import java.util.Set;

/**
 * HomeKit Lock Management Auto Security Timeout Characteristic.
 * <p>
 * This characteristic represents the auto security timeout for lock management, expressed in seconds. The value determines the duration after which the lock will automatically secure itself if left unlocked.
 * <p>
 * See the HomeKit Accessory Protocol (HAP) specification for details: https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitCharacteristicType(type = "0000001A-0000-1000-8000-0026BB765291", name = "Lock Management Auto Security Timeout", tag = "lockManagementAutoSecurityTimeout")
@NonNullByDefault
public class HomekitLockManagementAutoSecurityTimeoutCharacteristic extends HomekitIntegerCharacteristic {

    /**
     * Constructs a new Lock Management Auto Security Timeout characteristic.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param instanceId the instance ID for this characteristic
     */
    public HomekitLockManagementAutoSecurityTimeoutCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, Integer.MAX_VALUE, "seconds");
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(false)
            .withDescription("Lock Management Auto Security Timeout");
    }
    /**
     * Constructs a new Lock Management Auto Security Timeout characteristic from a JSON value.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param value the JSON value to initialize the characteristic with
     */
    public HomekitLockManagementAutoSecurityTimeoutCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
    /**
     * Checks if the given value is an allowed auto security timeout (must be non-negative).
     *
     * @param value the value to check
     * @return true if the value is allowed, false otherwise
     */
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0;
    }
    /**
     * Returns the set of allowed auto security timeout values (empty set for continuous range).
     *
     * @return the set of allowed values
     */
    @Override
    public Set<Integer> getAllowedValues() {
        return Set.of();
    }
} 