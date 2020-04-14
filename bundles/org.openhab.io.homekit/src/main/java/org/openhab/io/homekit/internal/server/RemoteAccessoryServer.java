package org.openhab.io.homekit.internal.server;

import java.io.IOException;
import java.util.Collection;

import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.internal.client.HomekitException;

public interface RemoteAccessoryServer {

    boolean isPairVerified();

    boolean isSecure();

    void pairSetup() throws IOException;

    boolean pairVerify();

    void pairRemove() throws HomekitException, IOException;

    Collection<Accessory> getAccessories();

}