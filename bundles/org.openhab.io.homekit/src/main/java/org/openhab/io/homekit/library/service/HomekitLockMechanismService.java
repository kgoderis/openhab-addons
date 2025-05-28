package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitLockCurrentStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitLockTargetStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitNameCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusActiveCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusFaultCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusJammedCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusLowBatteryCharacteristic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomeKit Lock Mechanism Service.
 * <p>
 * This service provides control and monitoring of lock mechanisms in HomeKit accessories.
 * It enables tracking of lock states, operation status, and fault conditions.
 * </p>
 *
 * <ul>
 * <li>Lock state monitoring and control</li>
 * <li>Operation status tracking</li>
 * <li>Fault and jam detection</li>
 * <li>Battery status monitoring</li>
 * </ul>
 *
 * <p>
 * Required characteristics:
 * <ul>
 * <li>LockCurrentState - Current state of the lock (0 = Unsecured, 1 = Secured, 2 = Jammed, 3 = Unknown)</li>
 * <li>LockTargetState - Target state for the lock (0 = Unsecured, 1 = Secured)</li>
 * </ul>
 * </p>
 *
 * <p>
 * Optional characteristics:
 * <ul>
 * <li>Name - Lock name</li>
 * <li>StatusActive - Lock activation state</li>
 * <li>StatusFault - Fault state indicator</li>
 * <li>StatusJammed - Jam detection state</li>
 * <li>StatusLowBattery - Low battery warning</li>
 * </ul>
 * </p>
 *
 * <p>
 * For more information, see the
 * <a href="https://developer.apple.com/documentation/HomeKit">HomeKit Accessory Protocol Specification</a>.
 * </p>
 *
 * @author Karel Goderis
 * @version 1.0
 * @since 1.0
 */
@HomekitServiceType(type = "00000045-0000-1000-8000-0026BB765291", name = "Lock Mechanism", tag = "lockMechanism")
@NonNullByDefault
public class HomekitLockMechanismService extends AbstractHomekitService {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit LockMechanismService: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_STATE = LOG_PREFIX + "State - ";
    private static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    private static final String LOG_ACCESSORY = LOG_PREFIX + "HomekitAccessory - ";
    private static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    private static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    private static final String LOG_TRACE = LOG_PREFIX + "Trace - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitLockMechanismService.class);

    /**
     * Creates a new Lock Mechanism service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @since 1.0
     */
    public HomekitLockMechanismService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Lock Mechanism").withPrimary(false).withHidden(false);
        logger.debug("{}Created LockMechanismService for accessory {}", LOG_INIT, accessory.getLabel());
    }

    /**
     * Creates a new Lock Mechanism service from a JSON configuration.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     * @since 1.0
     */
    public HomekitLockMechanismService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
        logger.debug("{}Created LockMechanismService from JSON for accessory {}", LOG_INIT, accessory.getLabel());
    }

    /**
     * Adds the required and optional characteristics for this service.
     * <p>
     * Required characteristics:
     * <ul>
     * <li>LockCurrentState - Current state of the lock (0 = Unsecured, 1 = Secured, 2 = Jammed, 3 = Unknown)</li>
     * <li>LockTargetState - Target state for the lock (0 = Unsecured, 1 = Secured)</li>
     * </ul>
     * </p>
     * <p>
     * Optional characteristics:
     * <ul>
     * <li>Name - Lock name</li>
     * <li>StatusActive - Lock activation state</li>
     * <li>StatusFault - Fault state indicator</li>
     * <li>StatusJammed - Jam detection state</li>
     * <li>StatusLowBattery - Low battery warning</li>
     * </ul>
     * </p>
     * 
     * @since 1.0
     */
    @Override
    public void addCharacteristics() {
        logger.trace("{}Adding required characteristics to LockMechanismService for accessory {}", LOG_TRACE,
                getAccessory().getLabel());

        addCharacteristic(new HomekitLockCurrentStateCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        logger.debug("{}Added LockCurrentStateCharacteristic to LockMechanismService", LOG_STATE);

        addCharacteristic(new HomekitLockTargetStateCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        logger.debug("{}Added LockTargetStateCharacteristic to LockMechanismService", LOG_STATE);

        addCharacteristic(new HomekitNameCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                .withMandatory(false));
        logger.debug("{}Added NameCharacteristic to LockMechanismService", LOG_STATE);

        addCharacteristic(
                new HomekitStatusActiveCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        logger.debug("{}Added StatusActiveCharacteristic to LockMechanismService", LOG_STATE);

        addCharacteristic(
                new HomekitStatusFaultCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        logger.debug("{}Added StatusFaultCharacteristic to LockMechanismService", LOG_STATE);

        addCharacteristic(
                new HomekitStatusJammedCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        logger.debug("{}Added StatusJammedCharacteristic to LockMechanismService", LOG_STATE);

        addCharacteristic(new HomekitStatusLowBatteryCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        logger.debug("{}Added StatusLowBatteryCharacteristic to LockMechanismService", LOG_STATE);
    }

    /**
     * Indicates that this service is not extensible.
     * Lock mechanism service has a fixed set of characteristics.
     *
     * @return false, as this service is not extensible
     * @since 1.0
     */
    @Override
    public boolean isExtensible() {
        return false;
    }
}
