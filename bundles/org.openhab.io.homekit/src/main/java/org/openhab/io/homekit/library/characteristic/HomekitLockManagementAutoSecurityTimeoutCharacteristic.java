package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Lock Management Auto Security Timeout Characteristic.
 * This characteristic represents the auto security timeout for lock management.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/LockManagementAutoSecurityTimeout">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "0000001A-0000-1000-8000-0026BB765291", name = "Lock Management Auto Security Timeout", tag = "lockManagementAutoSecurityTimeout")
@NonNullByDefault
public class HomekitLockManagementAutoSecurityTimeoutCharacteristic extends HomekitIntegerCharacteristic {

    protected static final String LOG_PREFIX = "Homekit LockManagementAutoSecurityTimeoutCharacteristic: ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    public HomekitLockManagementAutoSecurityTimeoutCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, Integer.MAX_VALUE, "s");
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(false)
            .withDescription("Lock Management Auto Security Timeout");
    }
    public HomekitLockManagementAutoSecurityTimeoutCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0;
    }
    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Collections.emptySet();
    }
} 