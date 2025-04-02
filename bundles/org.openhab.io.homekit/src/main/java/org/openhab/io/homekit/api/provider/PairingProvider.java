package org.openhab.io.homekit.api.provider;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Provider;
import org.openhab.io.homekit.api.hap.Pairing;

@NonNullByDefault
public interface PairingProvider extends Provider<Pairing> {

}
