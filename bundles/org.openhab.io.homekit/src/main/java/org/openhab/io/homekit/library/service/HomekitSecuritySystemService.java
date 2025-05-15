package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitNameCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSecuritySystemCurrentStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSecuritySystemTargetStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusFaultCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusTamperedCharacteristic;

/**
 * Service that represents a security system in HomeKit.
 * This service provides control over security system state and operation.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitServiceType(type = "0000007E-0000-1000-8000-0026BB765291", name = "Security System", tag = "securitySystem")
@NonNullByDefault
public class HomekitSecuritySystemService extends AbstractHomekitService {
    /**
     * Creates a new HomekitSecuritySystemService.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     */
    public HomekitSecuritySystemService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Security System").withPrimary(false).withHidden(false);
    }

    /**
     * Creates a new HomekitSecuritySystemService from a JSON value.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     */
    public HomekitSecuritySystemService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    @Override
    public void addCharacteristics() {
        addCharacteristic(new HomekitSecuritySystemCurrentStateCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitSecuritySystemTargetStateCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitNameCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                .withMandatory(false));
        addCharacteristic(
                new HomekitStatusFaultCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        addCharacteristic(
                new HomekitStatusTamperedCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }
}
