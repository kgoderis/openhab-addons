package org.openhab.io.homekit.internal.pairing;

import java.util.Base64;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.api.Pairing;

public class PairingImpl implements Pairing {

    private final String sourcePairingId;
    private final String destinationPairingId;
    private final String destinationLongtermPublicKey;

    public PairingImpl() {
        this.sourcePairingId = "";
        this.destinationPairingId = "";
        this.destinationLongtermPublicKey = "";
    }

    public PairingImpl(byte[] sourcePairingId, byte[] destinationPairingId, byte[] destinationLongtermPublicKey) {
        this.sourcePairingId = Base64.getEncoder().encodeToString(sourcePairingId);
        this.destinationPairingId = Base64.getEncoder().encodeToString(destinationPairingId);
        this.destinationLongtermPublicKey = Base64.getEncoder().encodeToString(destinationLongtermPublicKey);
    }

    @Override
    public @NonNull PairingUID getUID() {
        return new PairingUID(getSourcePairingId(), getDestinationPairingId());
    }

    @Override
    public byte[] getSourcePairingId() {
        return Base64.getDecoder().decode(sourcePairingId);
    }

    @Override
    public byte[] getDestinationPairingId() {
        return Base64.getDecoder().decode(destinationPairingId);
    }

    @Override
    public byte[] getDestinationLongtermPublicKey() {
        return Base64.getDecoder().decode(destinationLongtermPublicKey);
    }

}
