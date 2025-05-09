package org.openhab.io.homekit.api.registry;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.Registry;
import org.openhab.io.homekit.api.provider.HomekitAccessoryServerProvider;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;
import org.openhab.io.homekit.server.HomekitAccessoryServerUID;

/**
 * {@link HomekitAccessoryServerRegistry} tracks all {@link HomekitAccessoryServer}s from different {@link HomekitAccessoryServerProvider}s
 * and provides access to them. The {@link HomekitAccessoryServerRegistry} supports adding of listeners (see
 * {@link HomekitAccessoryServerChangeListener})
 *
 * @author Karel Goderis - Initial contribution
 */

@NonNullByDefault
public interface HomekitAccessoryServerRegistry extends Registry<HomekitAccessoryServer, HomekitAccessoryServerUID> {

    public @Nullable HomekitAccessoryServer getAvailableBridgeAccessoryServer();
}
