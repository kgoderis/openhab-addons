package org.openhab.io.homekit.internal.pairing;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.api.Pairing;

public class PairingImpl implements Pairing {

    private final String accessoryPairingId;
    private final String clientPairingId;
    private final byte[] clientLongtermPublicKey;

    public PairingImpl() {
        this.accessoryPairingId = "";
        this.clientPairingId = "";
        this.clientLongtermPublicKey = new byte[0];
    }

    public PairingImpl(String accessoryPairingId, String clientPairingId, byte[] clientLongtermPublicKey) {
        this.accessoryPairingId = accessoryPairingId;
        this.clientPairingId = clientPairingId;
        this.clientLongtermPublicKey = clientLongtermPublicKey;
    }

    @Override
    public @NonNull PairingUID getUID() {
        return new PairingUID(accessoryPairingId, clientPairingId);
    }

    @Override
    public @NonNull String getAccessoryPairingId() {
        return accessoryPairingId;
    }

    @Override
    public @NonNull String getClientPairingId() {
        return clientPairingId;
    }

    @Override
    public byte[] getClientLongtermPublicKey() {
        return clientLongtermPublicKey;
    }

}
