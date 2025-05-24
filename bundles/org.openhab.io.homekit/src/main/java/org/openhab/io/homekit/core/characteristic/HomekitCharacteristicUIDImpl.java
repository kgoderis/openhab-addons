package org.openhab.io.homekit.core.characteristic;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.uid.HomekitCharacteristicUID;
import org.openhab.io.homekit.util.HomekitUID;

/**
 * Represents a unique identifier for a HomeKit characteristic.
 * <p>
 * The UID format is: homekit:characteristic:{pairingId}:{accessoryId}:{serviceId}:{characteristicId}
 * <p>
 * See the HomeKit Accessory Protocol (HAP) specification for details: https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@NonNullByDefault
public class HomekitCharacteristicUIDImpl extends HomekitUID implements HomekitCharacteristicUID {
    private final long instanceId;

    /**
     * Instantiates a new characteristic UID.
     *
     * @param pairingId the hexadecimal pre-generated id of the accessory
     * @param accessoryId the accessory instance id
     * @param serviceId the service instance id
     * @param characteristicId the characteristic instance id
     */
    public HomekitCharacteristicUIDImpl(String pairingId, long accessoryId, long serviceId, long characteristicId) {
        super("characteristic",
                "homekit:characteristic:" + pairingId + ":" + accessoryId + ":" + serviceId + ":" + characteristicId);
        this.instanceId = accessoryId;
    }

    public HomekitCharacteristicUIDImpl(String key) {
        super("characteristic", key);
        this.instanceId = 0;
    }

    @Override
    protected int getMinimalNumberOfSegments() {
        return 6; // homekit:characteristic:pairingId:accessoryId:serviceId:characteristicId
    }

    /**
     * Returns the HomeKit ID part of this characteristic UID.
     * This is the last 4 segments joined with the separator.
     *
     * @return The HomeKit ID in the format: pairingId:accessoryId:serviceId:characteristicId
     */
    public String getHomekitId() {
        return String.join(SEPARATOR, getAllSegments().subList(getAllSegments().size() - 4, getAllSegments().size()));
    }

    @Override
    public String getAsString() {
        return toString();
    }

    @Override
    public long getInstanceId() {
        return instanceId;
    }

    @Override
    public HomekitCharacteristicUID getUID() {
        return this;
    }
}
