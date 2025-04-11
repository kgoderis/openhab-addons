package org.openhab.io.homekit.library.service;

import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.internal.service.GenericService;
import org.openhab.io.homekit.util.UUID5;

public class ThingService extends GenericService {

    public ThingService(Accessory accessory, long instanceId, boolean extend, String serviceName) throws Exception {
        super(accessory, instanceId, extend, serviceName);
    }

    public static String getType() {
        return UUID5.fromNamespaceAndString(UUID5.NAMESPACE_SERVICE, ThingService.class.getName()).toString();
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    public static String getTag() {
        return ThingService.class.getSimpleName().replace("Service", "");
    }
}
