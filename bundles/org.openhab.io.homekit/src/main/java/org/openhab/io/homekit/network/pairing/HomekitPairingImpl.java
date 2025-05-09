package org.openhab.io.homekit.network.pairing;

import java.util.Base64;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.protocol.pairing.HomekitPairing;

public class HomekitPairingImpl implements HomekitPairing {

    private final String sourcePairingId;
    private final String destinationPairingId;
    private final String destinationLongtermPublicKey;

    public HomekitPairingImpl() {
        this.sourcePairingId = "";
        this.destinationPairingId = "";
        this.destinationLongtermPublicKey = "";
    }

    public HomekitPairingImpl(byte[] sourcePairingId, byte[] destinationPairingId, byte[] destinationLongtermPublicKey) {
        this.sourcePairingId = Base64.getEncoder().encodeToString(sourcePairingId);
        this.destinationPairingId = Base64.getEncoder().encodeToString(destinationPairingId);
        this.destinationLongtermPublicKey = Base64.getEncoder().encodeToString(destinationLongtermPublicKey);
    }

    @Override
    public @NonNull HomekitPairingUID getUID() {
        return new HomekitPairingUID(getSourceId(), getDestinationId());
    }

    @Override
    public byte[] getSourceId() {
        return Base64.getDecoder().decode(sourcePairingId);
    }

    @Override
    public byte[] getDestinationId() {
        return Base64.getDecoder().decode(destinationPairingId);
    }

    @Override
    public byte[] getPublicKey() {
        return Base64.getDecoder().decode(destinationLongtermPublicKey);
    }
}
