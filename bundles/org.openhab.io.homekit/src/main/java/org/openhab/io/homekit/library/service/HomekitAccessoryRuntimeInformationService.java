package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitActivityIntervalCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitHeartBeatCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitPingCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSleepIntervalCharacteristic;

/**
 * HomeKit Accessory Runtime Information Service.
 * 
 * <p>
 * This service provides runtime information about the accessory, including:
 * <ul>
 * <li>Activity intervals</li>
 * <li>Heartbeat status</li>
 * <li>Ping responses</li>
 * <li>Sleep intervals</li>
 * </ul>
 * </p>
 *
 * <p>
 * The service is used to monitor and manage the accessory's runtime state, ensuring:
 * <ul>
 * <li>Proper communication between the accessory and iOS devices</li>
 * <li>Efficient power management</li>
 * <li>Reliable operation monitoring</li>
 * </ul>
 * </p>
 *
 * <p>
 * For more information, see the
 * <a href="https://developer.apple.com/documentation/HomeKit">HomeKit Accessory Protocol Specification</a>.
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@HomekitServiceType(type = "00000239-0000-1000-8000-0026BB765291", name = "AccessoryRuntimeInformation", tag = "accessoryRuntimeInformation")
@NonNullByDefault
public class HomekitAccessoryRuntimeInformationService extends AbstractHomekitService {

    /**
     * Creates a new Accessory Runtime Information service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @since 1.0
     */
    public HomekitAccessoryRuntimeInformationService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Accessory Runtime Information").withExtensible(true).withPrimary(false).withHidden(false);
    }

    /**
     * Creates a new Accessory Runtime Information service from a JSON configuration.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     * @since 1.0
     */
    public HomekitAccessoryRuntimeInformationService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    /**
     * Adds the required and optional characteristics for this service.
     * 
     * <p>
     * Required characteristics:
     * <ul>
     * <li>Ping</li>
     * </ul>
     * </p>
     *
     * <p>
     * Optional characteristics:
     * <ul>
     * <li>ActivityInterval</li>
     * <li>HeartBeat</li>
     * <li>SleepInterval</li>
     * </ul>
     * </p>
     *
     * @since 1.0
     */
    @Override
    public void addCharacteristics() {
        addCharacteristic(new HomekitPingCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                .withMandatory(true));
        addCharacteristic(new HomekitActivityIntervalCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        addCharacteristic(
                new HomekitHeartBeatCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        addCharacteristic(
                new HomekitSleepIntervalCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
    }
}
