package org.openhab.io.homekit.internal.pairing;

import java.util.Base64;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.api.hap.Pairing;

public class HomekitPairing implements Pairing {

    private final String sourcePairingId;
    private final String destinationPairingId;
    private final String destinationLongtermPublicKey;

    public HomekitPairing() {
        this.sourcePairingId = "";
        this.destinationPairingId = "";
        this.destinationLongtermPublicKey = "";
    }

    public HomekitPairing(byte[] sourcePairingId, byte[] destinationPairingId, byte[] destinationLongtermPublicKey) {
        this.sourcePairingId = Base64.getEncoder().encodeToString(sourcePairingId);
        this.destinationPairingId = Base64.getEncoder().encodeToString(destinationPairingId);
        this.destinationLongtermPublicKey = Base64.getEncoder().encodeToString(destinationLongtermPublicKey);
    }

    @Override
    public @NonNull PairingUID getUID() {
        return new PairingUID(getSourceId(), getDestinationId());
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
