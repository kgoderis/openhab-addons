package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitVersionCharacteristic;
// import org.openhab.io.homekit.library.characteristic.HomekitSoftwareUpdateCharacteristic; // Uncomment if available

/**
 * HomeKit Protocol Information Service.
 * This service provides protocol version information for HomeKit accessories.
 * For more information, see https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitServiceType(type = "000000A2-0000-1000-8000-0026BB765291", name = "Protocol Information", tag = "protocolInformation")
@NonNullByDefault
public class HomekitProtocolInformationService extends AbstractHomekitService {

    /**
     * Creates a new HomekitProtocolInformationService.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     */
    public HomekitProtocolInformationService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Protocol Information")
            .withExtensible(false)
            .withPrimary(false)
            .withHidden(false);
    }

    /**
     * Creates a new HomekitProtocolInformationService from a JSON value.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     */
    public HomekitProtocolInformationService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(new HomekitVersionCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        // Uncomment below if SoftwareUpdate characteristic is available
        // addCharacteristic(new HomekitSoftwareUpdateCharacteristic(this, eventManager,
        //        getAccessory().getNextAvailableInstanceId()).withMandatory(false));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }
} 