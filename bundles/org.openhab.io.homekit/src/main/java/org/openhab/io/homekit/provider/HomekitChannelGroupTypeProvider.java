package org.openhab.io.homekit.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
 * Provides ChannelGroupTypes based on registered HomekitFactory instances.
 * Each service type registered with a HomekitFactory is converted to a ChannelGroupType.
 * The provider supports dynamic updates and maintains a cache of channel group types.
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@NonNullByDefault
@Component(service = { ChannelGroupTypeProvider.class })
public class HomekitChannelGroupTypeProvider extends AbstractStorageBasedTypeProvider {
    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit ChannelGroupTypeProvider: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "HomekitAccessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    protected static final String LOG_TRACE = LOG_PREFIX + "Trace - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitChannelGroupTypeProvider.class);
    private final HomekitServiceFactory serviceFactory;
    private final HomekitCharacteristicFactory characteristicFactory;
    private final Map<ChannelGroupTypeUID, ChannelGroupType> channelGroupTypeCache = new ConcurrentHashMap<>();

    /**
     * Creates a new HomekitChannelGroupTypeProvider instance.
     * Initializes the provider with required dependencies and starts the channel group type initialization.
     *
     * @param storageService The storage service for persisting channel group types
     * @param serviceFactory The factory for creating HomeKit services
     * @param characteristicFactory The factory for creating HomeKit characteristics
     * @throws IllegalArgumentException if any of the required dependencies are null
     */
    @Activate
    public HomekitChannelGroupTypeProvider(@Reference StorageService storageService,
            @Reference HomekitServiceFactory serviceFactory,
            @Reference HomekitCharacteristicFactory characteristicFactory) {
        super(storageService);
        this.serviceFactory = serviceFactory;
        this.characteristicFactory = characteristicFactory;
        logger.debug("{}Initializing HomekitChannelGroupTypeProvider", LOG_INIT);
        initializeChannelGroupTypes();
    }

    /**
     * Deactivates the provider and cleans up resources.
     * Clears the channel group type cache.
     * 
     * @since 1.0
     */
    @Deactivate
    protected void deactivate() {
        logger.debug("{}Deactivating HomekitChannelGroupTypeProvider", LOG_STATE);
        channelGroupTypeCache.clear();
    }

    /**
     * Handles configuration changes by refreshing channel group types.
     * This method is called by the OSGi framework when the component's configuration is modified.
     * 
     * @since 1.0
     */
    @Modified
    protected void modified() {
        logger.debug("{}Configuration modified, refreshing channel group types", LOG_CONFIG);
        refreshChannelGroupTypes();
    }

    /**
     * Initializes all channel group types based on available services.
     * Scans for supported service types and creates corresponding channel group types.
     * 
     * @throws IllegalStateException if initialization fails
     * @since 1.0
     */
    private void initializeChannelGroupTypes() {
        logger.debug("{}Initializing channel group types", LOG_INIT);
        try {
            Set<String> serviceTypes = serviceFactory.getSupportedServiceTypes();
            logger.trace("{}Found {} supported service types", LOG_TRACE, serviceTypes.size());
            
            for (String serviceType : serviceTypes) {
                logger.trace("{}Processing service type: {}", LOG_TRACE, serviceType);
                createChannelGroupTypeForService(serviceType);
            }
            logger.info("{}Successfully initialized {} channel group types", LOG_INIT, channelGroupTypeCache.size());
        } catch (Exception e) {
            logger.error("{}Failed to initialize channel group types: {}", LOG_ERROR, e.getMessage(), e);
        }
    }

    /**
     * Refreshes all channel group types.
     * This is called when the configuration changes or when services are updated.
     * Clears the existing cache and reinitializes all channel group types.
     * 
     * @since 1.0
     */
    public void refreshChannelGroupTypes() {
        logger.debug("{}Refreshing channel group types", LOG_STATE);
        channelGroupTypeCache.clear();
        initializeChannelGroupTypes();
    }

    /**
     * Creates a channel group type for a specific service.
     * 
     * @param serviceType The service type to create a channel group type for
     * @throws IllegalArgumentException if the service type is invalid or not supported
     * @throws IllegalStateException if channel group type creation fails
     * @since 1.0
     */
    private void createChannelGroupTypeForService(String serviceType) {
        logger.trace("{}Creating channel group type for service: {}", LOG_TRACE, serviceType);
        try {
            String serviceTag = serviceFactory.getTagFromServiceType(serviceType);
            if (serviceTag == null || serviceTag.isEmpty()) {
                logger.warn("{}Invalid service tag for service type: {}", LOG_WARN, serviceType);
                return;
            }

            ChannelGroupTypeUID channelGroupTypeUID = new ChannelGroupTypeUID(HomekitBindingConstants.BINDING_ID, 
                "service-" + serviceTag);
            logger.trace("{}Created channel group type UID: {}", LOG_TRACE, channelGroupTypeUID);

            // Get mandatory and optional characteristics for this service
            Map<String, Set<String>> characteristicTypes = serviceFactory.getCharacteristicTypes(serviceType);
            List<ChannelDefinition> channelDefinitions = new ArrayList<>();

            // Add mandatory characteristics
            for (String characteristicType : characteristicTypes.get("mandatory")) {
                logger.trace("{}Processing mandatory characteristic: {}", LOG_TRACE, characteristicType);
                String characteristicTag = characteristicFactory.getTagFromCharacteristicType(characteristicType);
                if (characteristicTag == null || characteristicTag.isEmpty()) {
                    logger.warn("{}Invalid characteristic tag for type: {}", LOG_WARN, characteristicType);
                    continue;
                }

                ChannelTypeUID channelTypeUID = new ChannelTypeUID(HomekitBindingConstants.BINDING_ID, 
                    characteristicType);
                
                channelDefinitions.add(new ChannelDefinitionBuilder(characteristicTag, channelTypeUID)
                    .withLabel(characteristicTag)
                    .withDescription("HomeKit " + characteristicTag + " Characteristic (Mandatory)")
                    .build());
                logger.trace("{}Added mandatory channel definition for: {}", LOG_TRACE, characteristicTag);
            }

            // Add optional characteristics
            for (String characteristicType : characteristicTypes.get("optional")) {
                logger.trace("{}Processing optional characteristic: {}", LOG_TRACE, characteristicType);
                String characteristicTag = characteristicFactory.getTagFromCharacteristicType(characteristicType);
                if (characteristicTag == null || characteristicTag.isEmpty()) {
                    logger.warn("{}Invalid characteristic tag for type: {}", LOG_WARN, characteristicType);
                    continue;
                }

                ChannelTypeUID channelTypeUID = new ChannelTypeUID(HomekitBindingConstants.BINDING_ID, 
                    characteristicType);
                
                channelDefinitions.add(new ChannelDefinitionBuilder(characteristicTag, channelTypeUID)
                    .withLabel(characteristicTag)
                    .withDescription("HomeKit " + characteristicTag + " Characteristic (Optional)")
                    .build());
                logger.trace("{}Added optional channel definition for: {}", LOG_TRACE, characteristicTag);
            }

            if (channelDefinitions.isEmpty()) {
                logger.warn("{}No channel definitions found for service type: {}", LOG_WARN, serviceType);
                return;
            }

            ChannelGroupType channelGroupType = ChannelGroupTypeBuilder.instance(channelGroupTypeUID, serviceTag)
                .withDescription("HomeKit " + serviceTag + " Service")
                .withChannelDefinitions(channelDefinitions)
                .build();

            // Store in both the cache and the storage
            channelGroupTypeCache.put(channelGroupTypeUID, channelGroupType);
            putChannelGroupType(channelGroupType);

            logger.debug("{}Created channel group type for service {} with {} channels", LOG_STATE, serviceType, 
                channelDefinitions.size());
        } catch (Exception e) {
            logger.error("{}Failed to create channel group type for service {}: {}", LOG_ERROR, serviceType, 
                e.getMessage(), e);
        }
    }

    /**
     * Gets a channel group type by its UID.
     * First checks the cache, then falls back to storage.
     * 
     * @param channelGroupTypeUID The UID of the channel group type to retrieve
     * @param locale The locale for localization, may be null
     * @return The channel group type, or null if not found
     * @throws IllegalArgumentException if the channelGroupTypeUID is null
     * @since 1.0
     */
    @Override
    public @Nullable ChannelGroupType getChannelGroupType(ChannelGroupTypeUID channelGroupTypeUID, 
            @Nullable Locale locale) {
        logger.trace("{}Getting channel group type for UID: {}", LOG_TRACE, channelGroupTypeUID);
        
        // First check the cache
        ChannelGroupType cachedType = channelGroupTypeCache.get(channelGroupTypeUID);
        if (cachedType != null) {
            logger.trace("{}Found channel group type in cache", LOG_TRACE);
            return cachedType;
        }

        // If not in cache, try to get from storage
        ChannelGroupType storedType = super.getChannelGroupType(channelGroupTypeUID, locale);
        if (storedType != null) {
            // Add to cache for future lookups
            channelGroupTypeCache.put(channelGroupTypeUID, storedType);
            logger.trace("{}Found channel group type in storage and added to cache", LOG_TRACE);
        } else {
            logger.trace("{}Channel group type not found in cache or storage", LOG_TRACE);
        }
        return storedType;
    }

    /**
     * Gets all channel group types.
     * Returns a combination of cached and stored types.
     * 
     * @param locale The locale for localization, may be null
     * @return An unmodifiable collection of all channel group types
     * @since 1.0
     */
    @Override
    public Collection<ChannelGroupType> getChannelGroupTypes(@Nullable Locale locale) {
        logger.trace("{}Getting all channel group types", LOG_TRACE);
        
        // Get all stored types
        Collection<ChannelGroupType> storedTypes = super.getChannelGroupTypes(locale);
        
        // Add any cached types that aren't in storage
        for (ChannelGroupType cachedType : channelGroupTypeCache.values()) {
            if (!storedTypes.contains(cachedType)) {
                storedTypes.add(cachedType);
                logger.trace("{}Added cached type to collection: {}", LOG_TRACE, cachedType.getUID());
            }
        }
        
        logger.debug("{}Returning {} channel group types", LOG_STATE, storedTypes.size());
        return storedTypes;
    }
}
