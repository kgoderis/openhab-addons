package org.openhab.io.homekit.provider;

import java.util.Collection;
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
import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides ChannelGroupTypes based on registered HomekitFactory instances.
 * Each service type registered with a HomekitFactory is converted to a ChannelGroupType.
 * 
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
@Component(service = { ChannelGroupTypeProvider.class })
public class HomekitChannelGroupTypeProvider extends AbstractStorageBasedTypeProvider {
    protected static final String LOG_PREFIX = "Homekit ChannelGroupTypeProvider: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "HomekitAccessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    private final Logger logger = LoggerFactory.getLogger(HomekitChannelGroupTypeProvider.class);
    private final Map<String, HomekitFactory> homekitFactories = new ConcurrentHashMap<>();

    @Activate
    public HomekitChannelGroupTypeProvider(@Reference StorageService storageService) {
        super(storageService);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addHomekitFactory(HomekitFactory homekitFactory) {
        // Add all supported service types from this factory
        Set<String> serviceTypes = homekitFactory.getSupportedServiceTypes();
        for (String serviceType : serviceTypes) {
            homekitFactories.put(serviceType, homekitFactory);

            // Create and store channel group type for this service
            createChannelGroupTypeForService(serviceType, homekitFactory);
        }
    }

    protected void removeHomekitFactory(HomekitFactory homekitFactory) {
        // Remove all channel group types from this factory
        Set<String> serviceTypes = homekitFactory.getSupportedServiceTypes();
        for (String serviceType : serviceTypes) {
            homekitFactories.remove(serviceType);

            // Remove the channel group type
            ChannelGroupTypeUID channelGroupTypeUID = getChannelGroupTypeUID(serviceType);
            if (channelGroupTypeUID != null) {
                removeChannelGroupType(channelGroupTypeUID);
            }
        }
    }

    private void createChannelGroupTypeForService(String serviceType, HomekitFactory homekitFactory) {
        // Create a unique ID for the channel group type
        ChannelGroupTypeUID channelGroupTypeUID = getChannelGroupTypeUID(serviceType);
        if (channelGroupTypeUID == null) {
            logger.warn("{}Could not create ChannelGroupTypeUID for service type: {}", LOG_WARN, serviceType);
            return;
        }

        // Get the service class to determine the service name
        Class<?> serviceClass = homekitFactory.getService(serviceType);
        String serviceName = serviceClass != null ? serviceClass.getSimpleName() : serviceType;

        // Create channel definitions for each characteristic type supported by this service
        List<ChannelDefinition> channelDefinitions = createChannelDefinitions(serviceType, homekitFactory);

        // Create the channel group type
        ChannelGroupType channelGroupType = ChannelGroupTypeBuilder.instance(channelGroupTypeUID, serviceName)
                .withDescription("Homekit " + serviceName + " HomekitService").withChannelDefinitions(channelDefinitions)
                .build();

        // Store the channel group type
        putChannelGroupType(channelGroupType);
        logger.debug("{}Created ChannelGroupType {} for Homekit service type {}", LOG_CONFIG, channelGroupTypeUID,
                serviceType);
    }

    private List<ChannelDefinition> createChannelDefinitions(String serviceType, HomekitFactory homekitFactory) {
        // Get all characteristic types supported by this service
        Set<String> characteristicTypes = homekitFactory.getSupportedCharacteristicTypes();

        // Filter to only include characteristics that are relevant to this service
        // This is a simplification - in a real implementation, you would need to determine
        // which characteristics belong to which service
        return characteristicTypes.stream().filter(type -> {
            // Check if this characteristic type is associated with the service
            // This is a simplification - in a real implementation, you would need to
            // determine the relationship between services and characteristics
            return homekitFactory.getService(type) != null
                    && homekitFactory.getService(type).getSimpleName().equals(serviceType);
        }).map(type -> {
            // Get the channel type UID for this characteristic
            ChannelTypeUID channelTypeUID = homekitFactory.getChannelTypeUID(type);
            if (channelTypeUID == null) {
                logger.warn("{}No ChannelTypeUID found for characteristic type: {}", LOG_WARN, type);
                return null;
            }

            // Create a channel definition
            return new ChannelDefinitionBuilder(type, channelTypeUID).withLabel(type)
                    .withDescription("Homekit " + type + " HomekitCharacteristic").build();
        }).filter(def -> def != null).collect(Collectors.toList());
    }

    public ChannelGroupTypeUID getChannelGroupTypeUID(String serviceType) {
        // Create a unique ID for the channel group type based on the service type
        // Format: homekit:service:serviceType
        String serviceTypeId = serviceType.replaceAll("^0*([0-9a-fA-F]+)-0000-1000-8000-0026BB765291$", "$1");
        return new ChannelGroupTypeUID("homekit", "service-" + serviceTypeId);
    }

    @Override
    public Collection<ChannelGroupType> getChannelGroupTypes(@Nullable Locale locale) {
        // Return all stored channel group types
        return super.getChannelGroupTypes(locale);
    }

    @Override
    public @Nullable ChannelGroupType getChannelGroupType(ChannelGroupTypeUID channelGroupTypeUID,
            @Nullable Locale locale) {
        // Return the specific channel group type if it exists
        return super.getChannelGroupType(channelGroupTypeUID, locale);
    }
}
