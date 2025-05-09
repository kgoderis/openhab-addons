package org.openhab.io.homekit.protocol.pairing;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Identifiable;
import org.openhab.io.homekit.network.pairing.HomekitPairingUID;

@NonNullByDefault
public interface HomekitPairing extends Identifiable<HomekitPairingUID> {

    public byte[] getSourceId();

    public byte[] getDestinationId();

    public byte[] getPublicKey();
}
