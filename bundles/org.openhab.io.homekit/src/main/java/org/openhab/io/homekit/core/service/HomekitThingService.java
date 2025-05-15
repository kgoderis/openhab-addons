package org.openhab.io.homekit.core.service;

import java.util.Collection;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.util.HomekitUUID5;

/**
 * Service that represents a thing in HomeKit.
 * This service provides control over generic things.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@NonNullByDefault
public class HomekitThingService extends AbstractHomekitService {
    private static final String TYPE = HomekitUUID5
            .fromNamespaceAndString(HomekitUUID5.NAMESPACE_SERVICE, HomekitThingService.class.getName()).toString();

    /**
     * Creates a new HomekitThingService.
     *
     * @param accessory The accessory this service belongs to
     * @param instanceId The instance ID for this service
     * @param extend Whether this service is extensible
     * @param serviceName The name of this service
     * @param eventManager The event manager for handling HomeKit events
     * @param factories Collection of HomeKit factories
     * @throws Exception If an error occurs during initialization
     */
    public HomekitThingService(HomekitAccessory accessory, long instanceId, boolean extend, String serviceName,
            HomekitEventManager eventManager, HomekitCharacteristicFactory characteristicFactory) throws Exception {
        super(accessory, eventManager, characteristicFactory);
        withName(serviceName).withInstanceId(instanceId).withExtensible(extend).withPrimary(false).withHidden(false);
    }

    /**
     * Creates a new HomekitThingService from a JSON value.
     *
     * @param accessory The accessory this service belongs to
     * @param value JSON value containing service configuration
     * @param name The name of this service
     * @param eventManager The event manager for handling HomeKit events
     * @param factories Collection of HomeKit factories
     */
    public HomekitThingService(HomekitAccessory accessory, JsonValue value, String name,
            HomekitEventManager eventManager, Collection<HomekitFactory> factories) {
        super(accessory, value, name, eventManager, factories);
    }

    public static String getType() {
        return TYPE;
    }

    public static String getTag() {
        return HomekitThingService.class.getSimpleName().replace("HomekitService", "");
    }
}
