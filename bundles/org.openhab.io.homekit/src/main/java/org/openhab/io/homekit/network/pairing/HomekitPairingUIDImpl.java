package org.openhab.io.homekit.network.pairing;

import java.util.Base64;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.uid.HomekitPairingUID;
import org.openhab.io.homekit.util.HomekitUID;

/**
 * Represents a unique identifier for a Homekit pairing.
 * The UID format is: homekit:pairing:{sourcePairingId}:{destinationPairingId}
 */
@NonNullByDefault
public class HomekitPairingUIDImpl extends HomekitUID implements HomekitPairingUID {
    private static final String PAIRING_PREFIX = "pairing";

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
    public HomekitPairingUIDImpl(byte[] sourcePairingId, byte[] destinationPairingId) {
        super(PAIRING_PREFIX, "homekit:" + PAIRING_PREFIX + ":" 
            + Base64.getEncoder().withoutPadding().encodeToString(sourcePairingId) + ":"
            + Base64.getEncoder().withoutPadding().encodeToString(destinationPairingId));
    }

    @Override
    public String getAsString() {
        return toString();
    }

    @Override
    public byte[] getId() {
        List<String> segments = getAllSegments();
        return Base64.getDecoder().decode(segments.get(segments.size() - 1));
    }

    @Override
    public byte[] getSourcePairingId() {
        return Base64.getDecoder().decode(getSegment(2));
    }

    @Override
    public HomekitPairingUID getUID() {
        return this;
    }
} 