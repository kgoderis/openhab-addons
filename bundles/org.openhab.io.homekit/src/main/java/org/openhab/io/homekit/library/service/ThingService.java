package org.openhab.io.homekit.library.service;

import java.util.Collection;

import javax.json.JsonValue;

import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.internal.events.HomekitEventManager;
import org.openhab.io.homekit.internal.service.GenericService;
import org.openhab.io.homekit.util.UUID5;

public class ThingService extends GenericService {
    private static final String TYPE = UUID5.fromNamespaceAndString(UUID5.NAMESPACE_SERVICE, ThingService.class.getName()).toString();

    public ThingService(Accessory accessory, long instanceId, boolean extend, String serviceName,HomekitEventManager eventManager, Collection<HomekitFactory> factories) throws Exception {
        super(accessory, instanceId, extend, serviceName, TYPE, eventManager, factories);
    }

    public ThingService(Accessory accessory, JsonValue value, String name, HomekitEventManager eventManager, Collection<HomekitFactory> factories) {
        super(accessory, value, name, eventManager, factories);
    }

    public static String getType() {
        return TYPE;
    }

    public static String getTag() {
        return ThingService.class.getSimpleName().replace("Service", "");
    }
}
