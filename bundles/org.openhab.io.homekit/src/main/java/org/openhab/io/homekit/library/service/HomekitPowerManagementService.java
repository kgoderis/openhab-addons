package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitWakeConfigurationCharacteristic;

/**
 * HomeKit Power Management Service.
 * 
 * <p>
 * This service provides power management functionality for accessories, including:
 * <ul>
 *   <li>Wake configuration management</li>
 *   <li>Power state monitoring</li>
 *   <li>Power-related accessory states</li>
 * </ul>
 * </p>
 *
 * <p>
 * The service is used to:
 * <ul>
 *   <li>Configure wake behavior of accessories</li>
 *   <li>Monitor power states</li>
 *   <li>Manage power-related accessory states</li>
 *   <li>Provide power management information to iOS devices</li>
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
@HomekitServiceType(type = "00000221-0000-1000-8000-0026BB765291", name = "PowerManagement", tag = "powerManagement")
@NonNullByDefault
public class HomekitPowerManagementService extends AbstractHomekitService {

    /**
     * Creates a new Power Management service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @since 1.0
     */
    public HomekitPowerManagementService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Power Management").withExtensible(true).withPrimary(false).withHidden(false);
    }

    /**
     * Creates a new Power Management service from a JSON configuration.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     * @since 1.0
     */
    public HomekitPowerManagementService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    /**
     * Adds the required characteristics for this service.
     * 
     * <p>
     * Required characteristics:
     * <ul>
     *   <li>WakeConfiguration</li>
     * </ul>
     * </p>
     *
     * @since 1.0
     */
    @Override
    public void addCharacteristics() {
        // Required characteristics
        addCharacteristic(new HomekitWakeConfigurationCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
    }
}
