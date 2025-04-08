package org.openhab.io.homekit.library.accessory;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.internal.accessory.GenericAccessory;

public class BridgeAccessory extends GenericAccessory {

    public BridgeAccessory(long instanceId, boolean extend)
            throws Exception {
        super(instanceId);
    }

    @Override
    public void addServices() {
        super.addServices();
        // addService(new HAPProtocolInformationService(manager, this, this.getInstanceId(), true, getLabel()));
    }

    @Override
    public @NonNull String getLabel() {
        return "Bridge";
    }
}
