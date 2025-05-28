package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitRouterStatusCharacteristic;

/**
 * HomeKit WiFi Router Service.
 * 
 * <p>
 * This service provides WiFi router functionality in HomeKit, including:
 * <ul>
 * <li>Router status monitoring</li>
 * <li>Network connectivity management</li>
 * <li>WiFi network state tracking</li>
 * </ul>
 * </p>
 *
 * <p>
 * The service is used to:
 * <ul>
 * <li>Monitor the operational status of WiFi routers</li>
 * <li>Track network connectivity state</li>
 * <li>Provide router status information to iOS devices</li>
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
@HomekitServiceType(type = "0000020A-0000-1000-8000-0026BB765291", name = "WiFiRouter", tag = "wifiRouter")
@NonNullByDefault
public class HomekitWiFiRouterService extends AbstractHomekitService {

    /**
     * Creates a new WiFi Router service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @since 1.0
     */
    public HomekitWiFiRouterService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("WiFi Router").withExtensible(true).withPrimary(false).withHidden(false);
    }

    /**
     * Creates a new WiFi Router service from a JSON configuration.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     * @since 1.0
     */
    public HomekitWiFiRouterService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    /**
     * Adds the required characteristics for this service.
     * 
     * <p>
     * Required characteristics:
     * <ul>
     * <li>RouterStatus (UUID: 0000020E-0000-1000-8000-0026BB765291)</li>
     * </ul>
     * </p>
     *
     * <p>
     * The RouterStatus characteristic provides:
     * <ul>
     * <li>Current operational state of the router</li>
     * <li>Network connectivity status</li>
     * <li>WiFi network state information</li>
     * </ul>
     * </p>
     *
     * @since 1.0
     */
    @Override
    public void addCharacteristics() {
        // Required characteristics
        addCharacteristic(
                new HomekitRouterStatusCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
    }
}
