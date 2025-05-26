package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomeKit Service Label Namespace Characteristic.
 * 
 * <p>
 * This characteristic represents the namespace for service labels in a HomeKit accessory.
 * It is used to organize and categorize services within the accessory, allowing for
 * better organization and management of multiple services.
 * </p>
 *
 * <p>
 * The namespace is used to:
 * <ul>
 *   <li>Group related services together</li>
 *   <li>Provide a hierarchical organization of services</li>
 *   <li>Enable better service discovery and management</li>
 *   <li>Support multiple service instances with different purposes</li>
 * </ul>
 * </p>
 *
 * <p>
 * The class integrates with:
 * <ul>
 *   <li>{@link HomekitService} for service organization</li>
 *   <li>{@link HomekitEventManager} for event handling</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial Contribution
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "000000CD-0000-1000-8000-0026BB765291", name = "Service Label Namespace", tag = "serviceLabelNamespace", acceptedItemTypes = {
        "Number" })
@NonNullByDefault
public class HomekitServiceLabelNamespaceCharacteristic extends HomekitIntegerCharacteristic {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit ServiceLabelNamespaceCharacteristic: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitServiceLabelNamespaceCharacteristic.class);

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

    /**
     * Creates a new Service Label Namespace characteristic.
     * 
     * <p>
     * This characteristic is read-only and provides the namespace identifier for service labels.
     * </p>
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitServiceLabelNamespaceCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 0, 1, 1);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(false)
                .withDescription("Service Label Namespace");
        logger.debug("{}Created new Service Label Namespace characteristic with instance ID {}", LOG_INIT, instanceId);
    }

    /**
     * Creates a new Service Label Namespace characteristic from a JSON value.
     * 
     * <p>
     * This constructor is used when restoring a characteristic from persistent storage.
     * </p>
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitServiceLabelNamespaceCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
        logger.debug("{}Restored Service Label Namespace characteristic from JSON", LOG_INIT);
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
