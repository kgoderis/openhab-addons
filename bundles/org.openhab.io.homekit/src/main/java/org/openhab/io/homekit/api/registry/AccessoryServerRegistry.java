package org.openhab.io.homekit.api.registry;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.Registry;
import org.openhab.io.homekit.api.hap.AccessoryServer;
import org.openhab.io.homekit.api.provider.AccessoryServerProvider;
import org.openhab.io.homekit.internal.server.registry.AccessoryServerUID;

/**
 * {@link AccessoryServerRegistry} tracks all {@link AccessoryServer}s from different {@link AccessoryServerProvider}s
 * and provides access to them. The {@link AccessoryServerRegistry} supports adding of listeners (see
 * {@link HomekitAccessoryServerChangeListener})
 *
 * @author Karel Goderis - Initial contribution
 */

@NonNullByDefault
public interface AccessoryServerRegistry extends Registry<AccessoryServer, AccessoryServerUID> {

    public @Nullable AccessoryServer getAvailableBridgeAccessoryServer();
}
