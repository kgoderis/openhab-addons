package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Service Label Namespace Characteristic.
 * This characteristic represents the namespace for service labels.
 * The value can be either 0 (DOTS) or 1 (ARABIC_NUMERALS).
 * This is used to determine the format of service labels in the HomeKit ecosystem.
 *
 * @author Andy Lintner
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "000000CD-0000-1000-8000-0026BB765291", name = "Service Label Namespace", tag = "serviceLabelNamespace", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitServiceLabelNamespaceCharacteristic extends HomekitIntegerCharacteristic {
    public enum ServiceLabelNamespace {
        DOTS(0),
        ARABIC_NUMERALS(1);

        private final int value;

        ServiceLabelNamespace(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static ServiceLabelNamespace fromValue(int value) {
            for (ServiceLabelNamespace namespace : values()) {
                if (namespace.value == value) {
                    return namespace;
                }
            }
            throw new IllegalArgumentException("Invalid Service Label Namespace value: " + value);
        }
    }

    public HomekitServiceLabelNamespaceCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 0, 1, "");
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Service Label Namespace");
    }

    public HomekitServiceLabelNamespaceCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        if (value == null)
            return false;
        try {
            ServiceLabelNamespace.fromValue(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        java.util.Set<Integer> allowed = new java.util.HashSet<>();
        for (ServiceLabelNamespace namespace : ServiceLabelNamespace.values()) {
            allowed.add(namespace.getValue());
        }
        return allowed;
    }
}
