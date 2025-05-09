package org.openhab.io.homekit.network.pairing;

import java.util.Base64;
import java.util.List;

import org.openhab.io.homekit.util.HomekitUID;

/**
 * Represents a unique identifier for a Homekit pairing.
 * The UID format is: homekit:pairing:{sourcePairingId}:{destinationPairingId}
 */
public class HomekitPairingUID extends HomekitUID {

    @Override
    protected int getMinimalNumberOfSegments() {
        return 4; // homekit:pairing:sourcePairingId:destinationPairingId
    }

    /**
     * Instantiates a new pairing UID.
     *
     * @param sourcePairingId the accessory/server pairing id
     * @param destinationPairingId the controller/client pairing id
     */
    public HomekitPairingUID(byte[] sourcePairingId, byte[] destinationPairingId) {
        super("pairing", "homekit:pairing:" + 
            Base64.getEncoder().withoutPadding().encodeToString(sourcePairingId) + ":" +
            Base64.getEncoder().withoutPadding().encodeToString(destinationPairingId));
    }

    /**
     * Returns the destination pairing ID.
     *
     * @return The destination pairing ID
     */
    public byte[] getId() {
        List<String> segments = getAllSegments();
        return Base64.getDecoder().decode(segments.get(segments.size() - 1));
    }

    /**
     * Returns the source pairing ID.
     *
     * @return The source pairing ID
     */
    public byte[] getSourcePairingId() {
        return Base64.getDecoder().decode(getSegment(2));
    }
}
