package org.openhab.io.homekit.library.accessory;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.library.service.SwitchService;

public class SwitchAccessory extends ThingAccessory {

    public SwitchAccessory(HomekitCommunicationManager manager, AccessoryServer server, long instanceId, boolean extend)
            throws Exception {
        super(manager, server, instanceId, extend);
    }

    @Override
    public void addServices() throws Exception {
        super.addServices();
        addService(new SwitchService(manager, this, this.getInstanceId(), true, getLabel()));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }

    @Override
    public @NonNull String getLabel() {
        return this.getServer().getId();
    }
}
