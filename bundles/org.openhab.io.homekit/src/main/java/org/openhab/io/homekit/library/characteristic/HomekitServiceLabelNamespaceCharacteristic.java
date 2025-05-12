package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Characteristic for Service Label Namespace.
 * This characteristic represents the namespace for service labels.
 *
 * @author Andy Lintner
 */
@NonNullByDefault
@HomekitCharacteristicType(type = "000000CD-0000-1000-8000-0026BB765291", name = "Service Label Namespace", tag = "serviceLabelNamespace")
public class HomekitServiceLabelNamespaceCharacteristic extends HomekitIntegerCharacteristic {
    public HomekitServiceLabelNamespaceCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, 1, "");
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Service Label Namespace");
    }

    public HomekitServiceLabelNamespaceCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value <= 1;
    }
} 