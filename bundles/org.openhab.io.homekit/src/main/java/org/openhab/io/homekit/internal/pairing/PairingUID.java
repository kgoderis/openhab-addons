package org.openhab.io.homekit.internal.pairing;

import java.util.Base64;
import java.util.List;

import org.openhab.core.thing.UID;

public class PairingUID extends UID {

    // accessory pairing id : client pairing id

    @Override
    protected int getMinimalNumberOfSegments() {
        return 2;
    }

    /**
     * Instantiates a new thing UID.
     *
     * @param accessoryPairingId the accessory/server pairing id
     * @param clientPairingId the controller/client pairing id
     */
    // public PairingUID(String accessoryPairingId, String clientPairingId) {
    // super(accessoryPairingId, clientPairingId);
    // }

    public PairingUID(byte[] accessoryPairingId, byte[] clientPairingId) {
        super(Base64.getEncoder().withoutPadding().encodeToString(accessoryPairingId),
                Base64.getEncoder().withoutPadding().encodeToString(clientPairingId));
    }

    /**
     * Returns the id.
     *
     * @return id the id
     */
    public byte[] getId() {
        List<String> segments = getAllSegments();
        return Base64.getDecoder().decode(segments.get(segments.size() - 1));
    }

    public byte[] getAccessoryPairingId() {
        return Base64.getDecoder().decode(getSegment(0));
    }

}
