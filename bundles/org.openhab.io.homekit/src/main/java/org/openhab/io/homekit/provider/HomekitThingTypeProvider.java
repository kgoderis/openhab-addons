package org.openhab.io.homekit.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.storage.StorageService;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.AbstractStorageBasedTypeProvider;
import org.openhab.core.thing.binding.ThingTypeProvider;
import org.openhab.core.thing.type.ChannelGroupDefinition;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
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

//TODO : Rename to HomekitServiceThingTypeProvider
//TODO : Create a HomekitAccessoryThingTypeProvider
//TODO : make use of the HomekitChannelGroupTypeProvider and HomekitChannelTypeProvider
//TODO : make use of ChannelTypeRegistry and ChannelGroupTypeRegistry
//TODO : add createChannelGroupDefinitionWithIndex that takes an index and creates a channel group definition with that index
//TODO : add createChannelDefinitionWithIndex that takes an index and creates a channel definition with that index  

/**
 * Manages the creation and registration of HomeKit thing types in the OpenHAB
 * ecosystem.
 *
 * This class serves as the central provider for HomeKit thing types, converting
 * HomeKit service types
 * into OpenHAB thing types. It handles both factory-independent accessory types
 * and factory-specific
 * service types, ensuring proper integration between HomeKit and OpenHAB's
 * thing system.
 *
 * The provider implements a sophisticated type conversion system that:
 * - Creates thing types for HomeKit accessories
 * - Converts HomeKit service types to thing types
 * - Manages channel group definitions
 * - Handles service type to tag conversions
 * - Maintains type persistence
 *
 * The class integrates with:
 * - {@link StorageService} for persistent storage of thing types
 * - {@link HomekitServiceFactory} for service type management
 * - {@link HomekitChannelGroupTypeProvider} for channel group definitions
 * - {@link org.openhab.core.thing.type.ThingType OpenHAB's thing type system}
 * for type registration
 *
 * Key features:
 * - Automatic thing type generation from service types
 * - Channel group definition management
 * - Service type to tag conversion
 * - Persistent storage of thing types
 * - Support for factory-specific and independent types
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0
 */
@NonNullByDefault
@Component(service = { ThingTypeProvider.class })
public class HomekitThingTypeProvider extends AbstractStorageBasedTypeProvider {
    private final Logger logger = LoggerFactory.getLogger(HomekitThingTypeProvider.class);

    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit ThingType Provider: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    protected static final String LOG_TYPE = LOG_PREFIX + "Type - ";

    private final HomekitServiceFactory homekitServiceFactory;
    private final HomekitChannelGroupTypeProvider channelGroupTypeProvider;

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
            @Reference HomekitServiceFactory homekitServiceFactory,
            @Reference HomekitChannelGroupTypeProvider channelGroupTypeProvider) {
        super(storageService);
        this.homekitServiceFactory = homekitServiceFactory;
        this.channelGroupTypeProvider = channelGroupTypeProvider;
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
     * This method handles the complete process of creating a thing type for a
     * HomeKit service,
     * including validation, channel group creation, and type registration.
     *
     * The creation process includes:
     * - Validating service type and name
     * - Creating channel group definitions
     * - Building the thing type
     * - Registering the type in the system
     *
     * @param serviceType The HomeKit service type to create a thing type for
     */
    private void createThingTypeForService(String serviceType) {
        logger.debug("{}Creating thing type for service: {}", LOG_TYPE, serviceType);

        Optional<ThingTypeUID> thingTypeUIDOpt = getThingTypeUID(serviceType);
        if (thingTypeUIDOpt.isEmpty()) {
            logger.warn("{}Could not create ThingTypeUID for service type: {}", LOG_WARN, serviceType);
            return;
        }

        @SuppressWarnings("null")
        ThingTypeUID thingTypeUID = thingTypeUIDOpt.get();

        try {
            String serviceName = homekitServiceFactory.getTagFromServiceType(serviceType);
            if (serviceName == null || serviceName.isEmpty()) {
                logger.warn("{}Invalid service name for service type: {}", LOG_WARN, serviceType);
                return;
            }

            List<ChannelGroupDefinition> channelGroupDefinitions = createChannelGroupDefinitions(serviceType);
            if (channelGroupDefinitions.isEmpty()) {
                logger.warn("{}No channel group definitions found for service type: {}", LOG_WARN, serviceType);
                return;
            }

            ThingType thingType = ThingTypeBuilder.instance(thingTypeUID, serviceName)
                    .withDescription("Homekit " + serviceName + " Service").withCategory("homekit")
                    .withChannelGroupDefinitions(channelGroupDefinitions).build();

            putThingType(thingType);
            logger.info("{}Created thing type {} for service {} with {} channel groups", LOG_TYPE, thingTypeUID,
                    serviceType, channelGroupDefinitions.size());
        } catch (HomekitFactoryException e) {
            logger.error("{}Failed to get service name for service type {}: {}", LOG_ERROR, serviceType,
                    e.getMessage());
            return;
        }
    }

    /**
     * Creates channel group definitions for a service type.
     *
     * This method handles the creation of channel group definitions for a HomeKit
     * service,
     * including validation and error handling.
     *
     * The creation process includes:
     * - Validating service tag
     * - Creating channel group type UID
     * - Verifying channel group type existence
     * - Creating channel group definition
     *
     * @param serviceType The service type to create channel group definitions for
     * @return List of created channel group definitions
     */
    private List<ChannelGroupDefinition> createChannelGroupDefinitions(String serviceType) {
        logger.debug("{}Creating channel group definitions for service: {}", LOG_TYPE, serviceType);
        List<ChannelGroupDefinition> definitions = new ArrayList<>();

        try {
            String serviceTag = homekitServiceFactory.getTagFromServiceType(serviceType);
            if (serviceTag == null || serviceTag.isEmpty()) {
                logger.warn("{}Invalid service tag for service type: {}", LOG_WARN, serviceType);
                return definitions;
            }

            ChannelGroupTypeUID channelGroupTypeUID = new ChannelGroupTypeUID(HomekitBindingConstants.BINDING_ID,
                    "service-" + serviceTag);

            if (channelGroupTypeProvider.getChannelGroupType(channelGroupTypeUID, null) == null) {
                logger.warn("{}No channel group type found for service type: {}", LOG_WARN, serviceType);
                return definitions;
            }

            definitions.add(new ChannelGroupDefinition(serviceTag + ".1", channelGroupTypeUID, serviceTag,
                    "Homekit " + serviceTag + " Service"));

            logger.debug("{}Created channel group definition for service {} with UID {}", LOG_TYPE, serviceType,
                    channelGroupTypeUID);
        } catch (Exception e) {
            logger.error("{}Failed to create channel group definitions for service {}: {}", LOG_ERROR, serviceType,
                    e.getMessage(), e);
        }
        return definitions;
    }

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
     * unique
     * identifier, optionally filtered by locale for internationalization support.
     *
     * @param thingTypeUID The UID of the thing type to get
     * @param locale The locale to get the thing type for, or null for default
     * @return The thing type, or null if not found
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
