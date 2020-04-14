package org.openhab.io.homekit.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Identifiable;
import org.openhab.io.homekit.internal.pairing.PairingUID;

@NonNullByDefault
public interface Pairing extends Identifiable<PairingUID> {

    public byte[] getSourcePairingId();

    public byte[] getDestinationPairingId();

    public byte[] getDestinationPublicKey();
}
