package org.openhab.io.homekit.library.service;

import java.util.Collection;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.internal.events.HomekitEventManager;
import org.openhab.io.homekit.internal.service.GenericService;
import org.openhab.io.homekit.library.characteristic.FirmwareRevisionCharacteristic;
import org.openhab.io.homekit.library.characteristic.IdentifyCharacteristic;
import org.openhab.io.homekit.library.characteristic.ManufacturerCharacteristic;
import org.openhab.io.homekit.library.characteristic.ModelCharacteristic;
import org.openhab.io.homekit.library.characteristic.NameCharacteristic;
import org.openhab.io.homekit.library.characteristic.SerialNumberCharacteristic;

public class AccessoryInformationService extends GenericService {
    private static final String TYPE = "0000003E-0000-1000-8000-0026BB765291";

    public AccessoryInformationService(Accessory accessory, long instanceId, boolean extend, @NonNull String serviceName, HomekitEventManager eventManager, Collection<HomekitFactory> factories) {
        super(accessory, instanceId, extend, serviceName, TYPE, eventManager, factories);
    }

    public AccessoryInformationService(Accessory accessory, JsonValue value, String name, HomekitEventManager eventManager, Collection<HomekitFactory> factories) {
        super(accessory, value, name, eventManager, factories);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(
                new IdentifyCharacteristic(this, ((Accessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
        addCharacteristic(
                new ManufacturerCharacteristic(this, ((Accessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
        addCharacteristic(
                new ModelCharacteristic(this, ((Accessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
        addCharacteristic(
                new NameCharacteristic(this, ((Accessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
        addCharacteristic(
                new SerialNumberCharacteristic(this, ((Accessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
        addCharacteristic(
                new FirmwareRevisionCharacteristic(this, ((Accessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }

    public static String getType() {
        return TYPE;
    }

    public static String getTag() {
        return AccessoryInformationService.class.getSimpleName().replace("Service", "");
    }
}
