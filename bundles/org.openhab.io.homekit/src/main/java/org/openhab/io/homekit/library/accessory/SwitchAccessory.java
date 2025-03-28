package org.openhab.io.homekit.library.accessory;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.library.service.SwitchService;

public class SwitchAccessory extends ThingAccessory {

    public SwitchAccessory(HomekitCommunicationManager manager, AccessoryServer server, long instanceId,
            boolean extend) {
        super(manager, server, instanceId, extend);
    }

    @Override
    public void addServices() {
        super.addServices();
        addService(new SwitchService(getManager(), this, this.getInstanceId(), true, getLabel()));
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
