package org.openhab.io.homekit.api.hap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Identifiable;
import org.openhab.io.homekit.internal.pairing.HomekitPairingUID;

@NonNullByDefault
public interface HomekitPairing extends Identifiable<HomekitPairingUID> {

    public byte[] getSourceId();

    public byte[] getDestinationId();

    public byte[] getPublicKey();
}
