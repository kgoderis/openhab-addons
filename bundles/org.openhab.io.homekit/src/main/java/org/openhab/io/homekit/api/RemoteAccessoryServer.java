package org.openhab.io.homekit.api;

import java.io.IOException;

import org.openhab.io.homekit.internal.client.HomekitException;

public interface RemoteAccessoryServer {

    boolean isSecure();

    void pairSetup() throws IOException;

    boolean pairVerify();

    boolean isPairVerified();

    void pairRemove() throws HomekitException, IOException;

    // Collection<Accessory> getAccessories();

}