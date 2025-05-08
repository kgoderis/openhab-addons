package org.openhab.io.homekit.library.service;

import java.util.Collection;

import javax.json.JsonValue;

import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.openhab.io.homekit.api.hap.HomekitAccessory;
import org.openhab.io.homekit.internal.events.HomekitEventManager;
import org.openhab.io.homekit.internal.service.HomekitBaseService;
import org.openhab.io.homekit.util.HomekitUUID5;

public class HomekitThingService extends HomekitBaseService {
    private static final String TYPE = HomekitUUID5
            .fromNamespaceAndString(HomekitUUID5.NAMESPACE_SERVICE, HomekitThingService.class.getName()).toString();

    public HomekitThingService(HomekitAccessory accessory, long instanceId, boolean extend, String serviceName,HomekitEventManager eventManager, Collection<HomekitFactory> factories) throws Exception {
        super(accessory, instanceId, extend, serviceName, TYPE, eventManager, factories);
    }

    public HomekitThingService(HomekitAccessory accessory, JsonValue value, String name, HomekitEventManager eventManager, Collection<HomekitFactory> factories) {
        super(accessory, value, name, eventManager, factories);
    }

    public static String getType() {
        return TYPE;
    }

    public static String getTag() {
        return HomekitThingService.class.getSimpleName().replace("HomekitService", "");
    }
}
