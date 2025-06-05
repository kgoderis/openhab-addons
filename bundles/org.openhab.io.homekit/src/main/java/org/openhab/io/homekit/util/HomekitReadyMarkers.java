package org.openhab.io.homekit.util;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Central definition of ReadyService markers for HomeKit component startup
 * sequencing.
 *
 * This class defines the ready markers used to coordinate the startup sequence
 * of HomeKit components.
 * Components use these markers to signal their readiness and wait for
 * dependencies to be available.
 * The markers are organized in levels to ensure proper initialization order.
 *
 * <p>
 * <b>Startup Sequence Levels:</b>
 * </p>
 * <ul>
 * <li><b>Level 1:</b> Core Infrastructure (Event Manager, Configuration
 * Manager)</li>
 * <li><b>Level 2:</b> Factories (Characteristic, Service, Accessory
 * Factories)</li>
 * <li><b>Level 3:</b> Basic Registries and Providers</li>
 * <li><b>Level 4:</b> Advanced Providers and Registries</li>
 * <li><b>Level 5:</b> Type Providers</li>
 * <li><b>Level 6:</b> Bridges</li>
 * <li><b>Level 7:</b> Discovery and Handlers</li>
 * <li><b>Level 8:</b> System Ready</li>
 * </ul>
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0
 */
@NonNullByDefault
public final class HomekitReadyMarkers {

    // ========== Level 1: Core Infrastructure ==========
    /** Ready marker for the HomeKit event manager */
    public static final String HOMEKIT_EVENT_MANAGER = "homekit.eventManager";

    /** Ready marker for the HomeKit configuration manager */
    public static final String HOMEKIT_CONFIGURATION_MANAGER = "homekit.configurationManager";

    // ========== Level 2: Factories ==========
    /** Ready marker for the HomeKit characteristic factory */
    public static final String HOMEKIT_CHARACTERISTIC_FACTORY = "homekit.characteristicFactory";

    /** Ready marker for the HomeKit service factory */
    public static final String HOMEKIT_SERVICE_FACTORY = "homekit.serviceFactory";

    /** Ready marker for the HomeKit accessory factory */
    public static final String HOMEKIT_ACCESSORY_FACTORY = "homekit.accessoryFactory";

    // ========== Level 3: Basic Registries and Providers ==========
    /** Ready marker for the HomeKit accessory registry */
    public static final String HOMEKIT_ACCESSORY_REGISTRY = "homekit.accessoryRegistry";

    /** Ready marker for the managed pairing provider */
    public static final String HOMEKIT_MANAGED_PAIRING_PROVIDER = "homekit.managedPairingProvider";

    /** Ready marker for the pairing registry */
    public static final String HOMEKIT_PAIRING_REGISTRY = "homekit.pairingRegistry";

    // ========== Level 4: Advanced Providers and Registries ==========
    /** Ready marker for the managed accessory server provider */
    public static final String HOMEKIT_MANAGED_ACCESSORY_SERVER_PROVIDER = "homekit.managedAccessoryServerProvider";

    /** Ready marker for the accessory server registry */
    public static final String HOMEKIT_ACCESSORY_SERVER_REGISTRY = "homekit.accessoryServerRegistry";

    /** Ready marker for the persisted accessory provider */
    public static final String HOMEKIT_PERSISTED_ACCESSORY_PROVIDER = "homekit.persistedAccessoryProvider";

    // ========== Level 5: Type Providers ==========
    /** Ready marker for the thing type provider */
    public static final String HOMEKIT_THING_TYPE_PROVIDER = "homekit.thingTypeProvider";

    /** Ready marker for the channel type provider */
    public static final String HOMEKIT_CHANNEL_TYPE_PROVIDER = "homekit.channelTypeProvider";

    /** Ready marker for the channel group type provider */
    public static final String HOMEKIT_CHANNEL_GROUP_TYPE_PROVIDER = "homekit.channelGroupTypeProvider";

    // ========== Level 6: Bridges ==========
    /** Ready marker for the accessory bridge */
    public static final String HOMEKIT_ACCESSORY_BRIDGE = "homekit.accessoryBridge";

    /** Ready marker for the item bridge */
    public static final String HOMEKIT_ITEM_BRIDGE = "homekit.itemBridge";

    /** Ready marker for the thing bridge */
    public static final String HOMEKIT_THING_BRIDGE = "homekit.thingBridge";

    /** Ready marker for the passthrough bridge */
    public static final String HOMEKIT_PASSTHROUGH_BRIDGE = "homekit.passthroughBridge";

    // ========== Level 7: Discovery and Handlers ==========
    /** Ready marker for the discovery service */
    public static final String HOMEKIT_DISCOVERY_SERVICE = "homekit.discoveryService";

    /** Ready marker for the handler factory */
    public static final String HOMEKIT_HANDLER_FACTORY = "homekit.handlerFactory";

    // ========== Level 8: System Ready ==========
    /** Ready marker indicating the entire HomeKit system is ready */
    public static final String HOMEKIT_SYSTEM_READY = "homekit.systemReady";

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static constants.
     */
    private HomekitReadyMarkers() {
        // Utility class - no instantiation
    }
}
