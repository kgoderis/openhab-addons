/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.openhab.io.homekit.provider;

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.storage.StorageService;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.AbstractStorageBasedTypeProvider;
import org.openhab.core.thing.binding.ThingTypeProvider;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeBuilder;
import org.openhab.io.homekit.HomekitBindingConstants;
import org.openhab.io.homekit.api.factory.HomekitServiceFactory;
import org.openhab.io.homekit.exception.HomekitFactoryException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// ARCHITECTURE NOTES:
// ===================
// This provider creates TWO types of thing types:
//
// 1. SERVICE THING TYPES (homekit:lightbulb, homekit:fan, etc.)
//    - Represent individual HomeKit services
//    - Have direct channels (no channel groups)
//    - Channels are added dynamically by handlers at runtime
//
// 2. ACCESSORY THING TYPE (homekit:accessory)
//    - Represents entire HomeKit accessories (can contain multiple services)
//    - Channel groups are created DYNAMICALLY at runtime by HomekitAccessoryThingHandler
//    - Static channel group definitions are NOT possible because:
//      * We don't know what services an accessory will have beforehand
//      * We don't know how many instances of each service there will be
//      * Different accessories have different service combinations
//
// Channel groups are handled by HomekitChannelGroupTypeProvider (defines types)
// and HomekitAccessoryThingHandler (creates instances dynamically)  

/**
 * Manages the creation and registration of HomeKit thing types in the OpenHAB ecosystem.
 *
 * CORRECTED ARCHITECTURE: This provider creates static thing type definitions only.
 * Channel groups are handled dynamically at runtime by thing handlers.
 *
 * This class creates two distinct types of thing types:
 *
 * ## 1. Service Thing Types (homekit:lightbulb, homekit:fan, etc.)
 * - Represent individual HomeKit services
 * - Have NO static channel group definitions
 * - Channels are added directly by service handlers at runtime
 * - One thing = one service
 *
 * ## 2. Accessory Thing Type (homekit:accessory)
 * - Represents entire HomeKit accessories (multiple services)
 * - Has NO static channel group definitions (cannot be known beforehand)
 * - Channel groups are created DYNAMICALLY by HomekitAccessoryThingHandler
 * - One thing = one accessory with multiple service groups
 *
 * ## Why No Static Channel Groups for Accessories?
 * - Unknown service combinations until discovery
 * - Unknown number of service instances
 * - Different accessories have different structures
 * - Runtime discovery determines actual channel group needs
 *
 * The class integrates with:
 * - {@link StorageService} for persistent storage of thing types
 * - {@link HomekitServiceFactory} for service type management
 * - {@link HomekitChannelGroupTypeProvider} for channel group type definitions (used by handlers)
 * - {@link org.openhab.core.thing.type.ThingType OpenHAB's thing type system} for type registration
 *
 * Key responsibilities:
 * - Create static thing type definitions
 * - Service type to tag conversion
 * - Thing type persistence
 * - Integration with OpenHAB's type system
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@Component(service = { HomekitThingTypeProvider.class, ThingTypeProvider.class })
@NonNullByDefault
public class HomekitThingTypeProvider extends AbstractStorageBasedTypeProvider {
    private static final Logger logger = LoggerFactory.getLogger(HomekitThingTypeProvider.class);

    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit ThingTypeProvider: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    protected static final String LOG_TYPE = LOG_PREFIX + "Type - ";

    private final HomekitServiceFactory homekitServiceFactory;
    // NOTE: HomekitChannelGroupTypeProvider is used by handlers at runtime, not by this provider

    /**
     * Creates a new HomeKit thing type provider.
     *
     * This constructor initializes the provider with all required services and
     * establishes the foundation
     * for managing HomeKit thing types. It sets up the necessary connections to
     * various system services
     * and prepares the provider for operation.
     *
     * The initialization process includes:
     * - Setting up storage for thing types
     * - Configuring service factory integration
     * - Setting up channel group type provider
     * - Creating factory-independent thing types
     * - Creating factory-dependent thing types
     *
     * @param storageService Service for persistent storage of thing types
     * @param homekitServiceFactory Factory for managing HomeKit services
     * @param channelGroupTypeProvider Provider for channel group types
     */
    @Activate
    public HomekitThingTypeProvider(@Reference StorageService storageService,
            @Reference HomekitServiceFactory homekitServiceFactory) {
        super(storageService);
        this.homekitServiceFactory = homekitServiceFactory;
        logger.info("{}Initializing HomeKit thing type provider", LOG_INIT);
        addFactoryIndependentThingTypes();
        addFactoryDependentThingTypes();
        logger.info("{}HomeKit thing type provider initialized", LOG_INIT);
    }

    /**
     * Adds thing types that are not dependent on specific factories.
     *
     * This method creates and registers the base HomeKit accessory thing type,
     * which serves
     * as the foundation for all HomeKit accessories in the system.
     */
    private void addFactoryIndependentThingTypes() {
        logger.debug("{}Adding factory-independent thing types", LOG_TYPE);
        ThingTypeUID thingTypeUID = new ThingTypeUID(HomekitBindingConstants.BINDING_ID, "accessory");

        ThingType thingType = ThingTypeBuilder.instance(thingTypeUID, "Homekit Accessory")
                .withDescription("Homekit Accessory").withCategory("homekit").build();

        putThingType(thingType);
        logger.info("{}Created base HomeKit accessory thing type: {}", LOG_TYPE, thingTypeUID);
    }

    /**
     * Adds thing types that are dependent on specific factories.
     *
     * This method iterates through all supported service types from the service
     * factory
     * and creates corresponding thing types for each service.
     */
    private void addFactoryDependentThingTypes() {
        logger.debug("{}Adding factory-dependent thing types", LOG_TYPE);
        for (String serviceType : homekitServiceFactory.getSupportedServiceTypes()) {
            createThingTypeForService(serviceType);
        }
    }

    // @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy =
    // ReferencePolicy.DYNAMIC)
    // protected void addHomekitFactory(HomekitFactory homekitFactory) {
    // // Add all supported service types from this factory
    // Set<String> serviceTypes = homekitFactory.getSupportedServiceTypes();
    // for (String serviceType : serviceTypes) {
    // homekitFactories.put(serviceType, homekitFactory);

    // // Create and store thing type for this service
    // createThingTypeForService(serviceType, homekitFactory);
    // }
    // }

    // protected void removeHomekitFactory(HomekitFactory homekitFactory) {
    // // Remove all thing types from this factory
    // Set<String> serviceTypes = homekitFactory.getSupportedServiceTypes();
    // for (String serviceType : serviceTypes) {
    // homekitFactories.remove(serviceType);

    // // Remove the thing type
    // ThingTypeUID thingTypeUID = getThingTypeUID(serviceType);
    // if (thingTypeUID != null) {
    // removeThingType(thingTypeUID);
    // }
    // }
    // }

    /**
     * Creates a thing type for a specific service type.
     *
     * ARCHITECTURAL FIX: Service thing types should NOT have channel groups!
     * =====================================================================
     * 
     * Service thing types (e.g., "homekit:lightbulb") represent individual HomeKit services.
     * They should have direct channels, not channel groups, because:
     * 
     * 1. Each service thing represents ONE service instance
     * 2. Channels belong directly to the service (no grouping needed)
     * 3. Channel groups are only needed for accessory things that contain multiple services
     * 
     * This method now creates service thing types WITHOUT channel group definitions.
     * Channel groups are handled dynamically by the HomekitAccessoryThingHandler for accessory types.
     *
     * @param serviceType The HomeKit service type to create a thing type for
     */
    private void createThingTypeForService(String serviceType) {
        logger.debug("{}Creating service thing type for: {}", LOG_TYPE, serviceType);

        Optional<ThingTypeUID> thingTypeUIDOpt = getThingTypeUID(serviceType);
        if (thingTypeUIDOpt.isEmpty()) {
            logger.warn("{}Could not create ThingTypeUID for service type: {}", LOG_WARN, serviceType);
            return;
        }

        @SuppressWarnings("null") // Optional.get() is safe after isEmpty() check above
        ThingTypeUID thingTypeUID = thingTypeUIDOpt.get();

        try {
            String serviceName = homekitServiceFactory.getTagFromServiceType(serviceType);
            if (serviceName.isEmpty()) {
                logger.warn("{}Invalid service name for service type: {}", LOG_WARN, serviceType);
                return;
            }

            // FIXED: Service thing types should NOT have channel groups
            // They represent individual services and have direct channels
            ThingType thingType = ThingTypeBuilder.instance(thingTypeUID, serviceName)
                    .withDescription("HomeKit " + serviceName + " Service").withCategory("homekit").build(); // No
                                                                                                             // channel
                                                                                                             // group
                                                                                                             // definitions!

            putThingType(thingType);
            logger.info(
                    "{}Created service thing type {} for service {} (no channel groups - channels added at runtime)",
                    LOG_TYPE, thingTypeUID, serviceType);
        } catch (HomekitFactoryException e) {
            logger.error("{}Failed to get service name for service type {}: {}", LOG_ERROR, serviceType,
                    e.getMessage());
            return;
        }
    }

    // REMOVED: createChannelGroupDefinitions method
    //
    // This method was removed because service thing types should NOT have static channel group definitions.
    // Channel groups are only needed for accessory thing types, and they must be created dynamically
    // at runtime based on the actual services discovered in the accessory.
    //
    // For service thing types (homekit:lightbulb), channels are added directly without groups.
    // For accessory thing types (homekit:accessory), channel groups are created dynamically
    // by the HomekitAccessoryThingHandler when services are discovered.

    /**
     * Gets the thing type UID for a service type.
     *
     * This method creates a unique identifier for a thing type based on the service
     * type,
     * ensuring proper identification and registration in the system.
     *
     * @param serviceType The service type to get the thing type UID for
     * @return Optional containing the thing type UID, or empty if the service type
     *         is invalid
     */
    public Optional<ThingTypeUID> getThingTypeUID(String serviceType) {
        try {
            String serviceTag = homekitServiceFactory.getTagFromServiceType(serviceType);
            if (serviceTag.isEmpty()) {
                logger.warn("{}Invalid service tag for service type: {}", LOG_WARN, serviceType);
                return Optional.empty();
            }
            return Optional.of(new ThingTypeUID(HomekitBindingConstants.BINDING_ID, serviceTag));
        } catch (HomekitFactoryException e) {
            logger.error("{}Failed to get service tag for service type {}: {}", LOG_ERROR, serviceType, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Gets all registered thing types.
     *
     * This method returns all thing types registered with the provider, optionally
     * filtered
     * by locale for internationalization support.
     *
     * @param locale The locale to get thing types for, or null for default
     * @return Collection of registered thing types
     */
    @Override
    public Collection<ThingType> getThingTypes(@Nullable Locale locale) {
        return super.getThingTypes(locale);
    }

    /**
     * Gets a specific thing type by its UID.
     *
     * This method retrieves a specific thing type from the provider based on its
     * unique identifier, optionally filtered by locale for internationalization support.
     *
     * @param thingTypeUID The UID of the thing type to get
     * @param locale The locale to get the thing type for, or null for default
     * @return Optional containing the thing type, or empty if not found
     */
    @Override
    public @Nullable ThingType getThingType(ThingTypeUID thingTypeUID, @Nullable Locale locale) {
        return super.getThingType(thingTypeUID, locale);
    }

    // public String getServiceTag(String serviceType) throws HomekitException {
    // // find the factory that supports the service
    // @Nullable
    // HomekitFactory factory = homekitFactories.get(serviceType);
    // if (factory != null) {
    // String tag = factory.getTagFromServiceType(serviceType);
    // if (tag != null) {
    // return tag;
    // }
    // }
    // throw new HomekitException("No factory found for service type: " +
    // serviceType);
    // }

    // public String getServiceTypeFromTag(String tag) throws HomekitException {
    // // find the factory that supports the tag
    // for (HomekitFactory factory : homekitFactories.values()) {
    // String serviceType = factory.getServiceTypeFromTag(tag);
    // if (serviceType != null) {
    // return serviceType;
    // }
    // }
    // throw new HomekitException("No service type found for tag: " + tag);
    // }
}
