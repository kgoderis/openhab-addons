package org.openhab.io.homekit.library.service;

import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.ManagedAccessory;
import org.openhab.io.homekit.internal.service.AbstractManagedService;
import org.openhab.io.homekit.util.UUID5;

public class ThingService extends AbstractManagedService {

    public ThingService(HomekitCommunicationManager manager, ManagedAccessory accessory, long instanceId, boolean extend,
            String serviceName) throws Exception {
        super(manager, accessory, instanceId, extend, serviceName);
    }

    public static String getType() {
        return UUID5.fromNamespaceAndString(UUID5.NAMESPACE_SERVICE, ThingService.class.getName()).toString();
    }

    @Override
    public String getInstanceType() {
        return getType();
    }
}
