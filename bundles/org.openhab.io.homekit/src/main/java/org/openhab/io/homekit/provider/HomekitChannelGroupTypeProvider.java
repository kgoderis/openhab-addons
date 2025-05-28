package org.openhab.io.homekit.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.storage.StorageService;
import org.openhab.core.thing.binding.AbstractStorageBasedTypeProvider;
import org.openhab.core.thing.type.ChannelDefinition;
import org.openhab.core.thing.type.ChannelDefinitionBuilder;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelGroupTypeBuilder;
import org.openhab.core.thing.type.ChannelGroupTypeProvider;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.io.homekit.HomekitBindingConstants;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.factory.HomekitServiceFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the creation and registration of HomeKit channel group types in the OpenHAB ecosystem.
 *
 * This class serves as the central provider for HomeKit channel group types, converting HomeKit service types
 * into OpenHAB channel group types. It handles the mapping between HomeKit services and their characteristics,
 * ensuring proper integration between HomeKit and OpenHAB's channel group system.
 *
 * The provider implements a sophisticated type conversion system that:
 * - Creates channel group types for HomeKit services
 * - Maps service types to channel group types
 * - Manages mandatory and optional characteristics
 * - Handles service type to tag conversions
 * - Maintains a cache for improved performance
 *
 * The class integrates with:
 * - {@link StorageService} for persistent storage of channel group types
 * - {@link HomekitServiceFactory} for service type management
 * - {@link HomekitCharacteristicFactory} for characteristic type management
 * - {@link org.openhab.core.thing.type.ChannelGroupType OpenHAB's channel group type system} for type registration
 *
 * Key features:
 * - Automatic channel group type generation from service types
 * - Support for mandatory and optional characteristics
 * - Persistent storage with caching
 * - Dynamic type updates and refresh
 * - Factory-dependent type management
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0
 */
@NonNullByDefault
@Component(service = { ChannelGroupTypeProvider.class })
public class HomekitChannelGroupTypeProvider extends AbstractStorageBasedTypeProvider {
    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit ChannelGroupType Provider: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    protected static final String LOG_TYPE = LOG_PREFIX + "Type - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitChannelGroupTypeProvider.class);
    private final HomekitServiceFactory serviceFactory;
    private final HomekitCharacteristicFactory characteristicFactory;
    private final Map<ChannelGroupTypeUID, ChannelGroupType> channelGroupTypeCache = new ConcurrentHashMap<>();

    /**
     * Creates a new HomeKit channel group type provider.
     *
     * This constructor initializes the provider with all required services and establishes the foundation
     * for managing HomeKit channel group types. It sets up the necessary connections to various system services
     * and prepares the provider for operation.
     *
     * The initialization process includes:
     * - Setting up storage for channel group types
     * - Configuring service factory integration
     * - Setting up characteristic factory integration
     * - Initializing channel group types
     *
     * @param storageService Service for persistent storage of channel group types
     * @param serviceFactory Factory for managing HomeKit services
     * @param characteristicFactory Factory for managing HomeKit characteristics
     */
    @Activate
    public HomekitChannelGroupTypeProvider(@Reference StorageService storageService,
            @Reference HomekitServiceFactory serviceFactory,
            @Reference HomekitCharacteristicFactory characteristicFactory) {
        super(storageService);
        this.serviceFactory = serviceFactory;
        this.characteristicFactory = characteristicFactory;
        logger.info("{}Initializing HomeKit channel group type provider", LOG_INIT);
        initializeChannelGroupTypes();
        logger.info("{}HomeKit channel group type provider initialized", LOG_INIT);
    }

    /**
     * Deactivates the provider and cleans up resources.
     *
     * This method is called by the OSGi framework when the component is being deactivated.
     * It performs cleanup operations including:
     * - Clearing the channel group type cache
     * - Logging the deactivation
     */
    @Deactivate
    protected void deactivate() {
        logger.info("{}Deactivating HomeKit channel group type provider", LOG_STATE);
        channelGroupTypeCache.clear();
        logger.info("{}HomeKit channel group type provider deactivated", LOG_STATE);
    }

    /**
     * Handles configuration changes by refreshing channel group types.
     *
     * This method is called by the OSGi framework when the component's configuration is modified.
     * It triggers a refresh of all channel group types to ensure they reflect the current configuration.
     */
    @Modified
    protected void modified() {
        logger.info("{}Configuration modified, refreshing channel group types", LOG_CONFIG);
        refreshChannelGroupTypes();
        logger.info("{}Channel group types refreshed after configuration change", LOG_CONFIG);
    }

    /**
     * Initializes all channel group types based on available services.
     *
     * This method performs the complete initialization of channel group types:
     * - Retrieves all supported service types
     * - Creates channel group types for each service
     * - Handles any initialization errors
     * - Updates the cache with created types
     *
     * @throws IllegalStateException if initialization fails
     */
    private void initializeChannelGroupTypes() {
        logger.debug("{}Initializing channel group types", LOG_INIT);
        try {
            Set<String> serviceTypes = serviceFactory.getSupportedServiceTypes();
            logger.debug("{}Found {} supported service types", LOG_TYPE, serviceTypes.size());

            for (String serviceType : serviceTypes) {
                logger.debug("{}Processing service type: {}", LOG_TYPE, serviceType);
                createChannelGroupTypeForService(serviceType);
            }
            logger.info("{}Successfully initialized {} channel group types", LOG_INIT, channelGroupTypeCache.size());
        } catch (Exception e) {
            logger.error("{}Failed to initialize channel group types: {}", LOG_ERROR, e.getMessage(), e);
            throw new IllegalStateException("Failed to initialize channel group types", e);
        }
    }

    /**
     * Refreshes all channel group types.
     *
     * This method is called when the configuration changes or when services are updated.
     * It performs a complete refresh of all channel group types:
     * - Clears the existing cache
     * - Reinitializes all channel group types
     * - Updates the cache with new types
     */
    public void refreshChannelGroupTypes() {
        logger.info("{}Refreshing channel group types", LOG_STATE);
        channelGroupTypeCache.clear();
        initializeChannelGroupTypes();
        logger.info("{}Channel group types refreshed successfully", LOG_STATE);
    }

    /**
     * Creates a channel group type for a specific service.
     *
     * This method handles the complete process of creating a channel group type for a HomeKit service:
     * - Validates service type and tag
     * - Creates channel definitions for mandatory characteristics
     * - Creates channel definitions for optional characteristics
     * - Builds and stores the channel group type
     *
     * @param serviceType The service type to create a channel group type for
     * @throws IllegalArgumentException if the service type is invalid or not supported
     * @throws IllegalStateException if channel group type creation fails
     */
    private void createChannelGroupTypeForService(String serviceType) {
        logger.debug("{}Creating channel group type for service: {}", LOG_TYPE, serviceType);
        try {
            String serviceTag = serviceFactory.getTagFromServiceType(serviceType);
            if (serviceTag == null || serviceTag.isEmpty()) {
                logger.warn("{}Invalid service tag for service type: {}", LOG_WARN, serviceType);
                return;
            }

            ChannelGroupTypeUID channelGroupTypeUID = new ChannelGroupTypeUID(HomekitBindingConstants.BINDING_ID,
                    "service-" + serviceTag);
            logger.debug("{}Created channel group type UID: {}", LOG_TYPE, channelGroupTypeUID);

            Map<String, Set<String>> characteristicTypes = serviceFactory.getCharacteristicTypes(serviceType);
            List<ChannelDefinition> channelDefinitions = new ArrayList<>();

            // Process mandatory characteristics
            for (String characteristicType : characteristicTypes.get("mandatory")) {
                logger.debug("{}Processing mandatory characteristic: {}", LOG_TYPE, characteristicType);
                String characteristicTag = characteristicFactory.getTagFromCharacteristicType(characteristicType);
                if (characteristicTag == null || characteristicTag.isEmpty()) {
                    logger.warn("{}Invalid characteristic tag for type: {}", LOG_WARN, characteristicType);
                    continue;
                }

                ChannelTypeUID channelTypeUID = new ChannelTypeUID(HomekitBindingConstants.BINDING_ID,
                        characteristicType);

                channelDefinitions.add(new ChannelDefinitionBuilder(characteristicTag, channelTypeUID)
                        .withLabel(characteristicTag)
                        .withDescription("HomeKit " + characteristicTag + " Characteristic (Mandatory)").build());
                logger.debug("{}Added mandatory channel definition for: {}", LOG_TYPE, characteristicTag);
            }

            // Process optional characteristics
            for (String characteristicType : characteristicTypes.get("optional")) {
                logger.debug("{}Processing optional characteristic: {}", LOG_TYPE, characteristicType);
                String characteristicTag = characteristicFactory.getTagFromCharacteristicType(characteristicType);
                if (characteristicTag == null || characteristicTag.isEmpty()) {
                    logger.warn("{}Invalid characteristic tag for type: {}", LOG_WARN, characteristicType);
                    continue;
                }

                ChannelTypeUID channelTypeUID = new ChannelTypeUID(HomekitBindingConstants.BINDING_ID,
                        characteristicType);

                channelDefinitions.add(new ChannelDefinitionBuilder(characteristicTag, channelTypeUID)
                        .withLabel(characteristicTag)
                        .withDescription("HomeKit " + characteristicTag + " Characteristic (Optional)").build());
                logger.debug("{}Added optional channel definition for: {}", LOG_TYPE, characteristicTag);
            }

            if (channelDefinitions.isEmpty()) {
                logger.warn("{}No channel definitions found for service type: {}", LOG_WARN, serviceType);
                return;
            }

            ChannelGroupType channelGroupType = ChannelGroupTypeBuilder.instance(channelGroupTypeUID, serviceTag)
                    .withDescription("HomeKit " + serviceTag + " Service").withChannelDefinitions(channelDefinitions)
                    .build();

            channelGroupTypeCache.put(channelGroupTypeUID, channelGroupType);
            putChannelGroupType(channelGroupType);

            logger.info("{}Created channel group type for service {} with {} channels", LOG_TYPE, serviceType,
                    channelDefinitions.size());
        } catch (Exception e) {
            logger.error("{}Failed to create channel group type for service {}: {}", LOG_ERROR, serviceType,
                    e.getMessage(), e);
            throw new IllegalStateException("Failed to create channel group type for service: " + serviceType, e);
        }
    }

    /**
     * Gets a channel group type by its UID.
     *
     * This method implements a two-level lookup strategy:
     * - First checks the cache for quick access
     * - Falls back to storage if not found in cache
     * - Updates cache if found in storage
     *
     * @param channelGroupTypeUID The UID of the channel group type to retrieve
     * @param locale The locale for localization, may be null
     * @return The channel group type, or null if not found
     */
    @Override
    public @Nullable ChannelGroupType getChannelGroupType(ChannelGroupTypeUID channelGroupTypeUID,
            @Nullable Locale locale) {
        logger.debug("{}Getting channel group type for UID: {}", LOG_TYPE, channelGroupTypeUID);

        ChannelGroupType cachedType = channelGroupTypeCache.get(channelGroupTypeUID);
        if (cachedType != null) {
            logger.debug("{}Found channel group type in cache", LOG_TYPE);
            return cachedType;
        }

        ChannelGroupType storedType = super.getChannelGroupType(channelGroupTypeUID, locale);
        if (storedType != null) {
            channelGroupTypeCache.put(channelGroupTypeUID, storedType);
            logger.debug("{}Found channel group type in storage and added to cache", LOG_TYPE);
        } else {
            logger.debug("{}Channel group type not found in cache or storage", LOG_TYPE);
        }
        return storedType;
    }

    /**
     * Gets all channel group types.
     *
     * This method combines cached and stored types to provide a complete view:
     * - Retrieves all stored types
     * - Adds any cached types not in storage
     * - Returns an unmodifiable collection
     *
     * @param locale The locale for localization, may be null
     * @return An unmodifiable collection of all channel group types
     */
    @Override
    public Collection<ChannelGroupType> getChannelGroupTypes(@Nullable Locale locale) {
        logger.debug("{}Getting all channel group types", LOG_TYPE);

        Collection<ChannelGroupType> storedTypes = super.getChannelGroupTypes(locale);

        for (ChannelGroupType cachedType : channelGroupTypeCache.values()) {
            if (!storedTypes.contains(cachedType)) {
                storedTypes.add(cachedType);
                logger.debug("{}Added cached type to collection: {}", LOG_TYPE, cachedType.getUID());
            }
        }

        logger.debug("{}Returning {} channel group types", LOG_TYPE, storedTypes.size());
        return Collections.unmodifiableCollection(storedTypes);
    }
}
