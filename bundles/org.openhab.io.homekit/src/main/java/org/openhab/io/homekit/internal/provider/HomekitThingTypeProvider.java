package org.openhab.io.homekit.internal.provider;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.storage.StorageService;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.AbstractStorageBasedTypeProvider;
import org.openhab.core.thing.binding.ThingTypeProvider;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeBuilder;
import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.openhab.io.homekit.exception.HomekitException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO : Rename to HomekitServiceThingTypeProvider
//TODO : Create a HomekitAccessoryThingTypeProvider
//TODO : make use of the HomekitChannelGroupTypeProvider and HomekitChannelTypeProvider
//TODO : make use of ChannelTypeRegistry and ChannelGroupTypeRegistry
//TODO : add createChannelGroupDefinitionWithIndex that takes an index and creates a channel group definition with that index
//TODO : add createChannelDefinitionWithIndex that takes an index and creates a channel definition with that index  

/**
 * Provides ThingTypes based on registered HomekitFactory instances.
 * Each service type registered with a HomekitFactory is converted to a ThingType.
 * 
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
@Component(service = { ThingTypeProvider.class })
public class HomekitThingTypeProvider extends AbstractStorageBasedTypeProvider {
    private final Logger logger = LoggerFactory.getLogger(HomekitThingTypeProvider.class);
    private final Map<String, HomekitFactory> homekitFactories = new ConcurrentHashMap<>();

    @Activate
    public HomekitThingTypeProvider(@Reference StorageService storageService) {
        super(storageService);
        addFactoryIndependentThingTypes();
    }

    private void addFactoryIndependentThingTypes() {
        // Add thing types for services that are not factory-specific
        // This is a simplification - in a real implementation, you would need to determine
        // which services should be grouped together
        // which services should be grouped together

        // homekit:accessory:
        ThingTypeUID thingTypeUID = new ThingTypeUID("homekit", "accessory");

        // Create the thing type
        ThingType thingType = ThingTypeBuilder.instance(thingTypeUID, "Homekit HomekitAccessory")
                .withDescription("Homekit HomekitAccessory").withCategory("homekit").build();

        // Store the thing type
        putThingType(thingType);
        logger.debug("Created ThingType {} for Homekit", thingTypeUID);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addHomekitFactory(HomekitFactory homekitFactory) {
        // Add all supported service types from this factory
        Set<String> serviceTypes = homekitFactory.getSupportedServiceTypes();
        for (String serviceType : serviceTypes) {
            homekitFactories.put(serviceType, homekitFactory);

            // Create and store thing type for this service
            createThingTypeForService(serviceType, homekitFactory);
        }
    }

    protected void removeHomekitFactory(HomekitFactory homekitFactory) {
        // Remove all thing types from this factory
        Set<String> serviceTypes = homekitFactory.getSupportedServiceTypes();
        for (String serviceType : serviceTypes) {
            homekitFactories.remove(serviceType);

            // Remove the thing type
            ThingTypeUID thingTypeUID = getThingTypeUID(serviceType);
            if (thingTypeUID != null) {
                removeThingType(thingTypeUID);
            }
        }
    }

    private void createThingTypeForService(String serviceType, HomekitFactory homekitFactory) {
        // Create a unique ID for the thing type
        ThingTypeUID thingTypeUID = getThingTypeUID(serviceType);
        if (thingTypeUID == null) {
            logger.warn("Could not create ThingTypeUID for service type: {}", serviceType);
            return;
        }

        // Get the service class to determine the service name
        String serviceName = homekitFactory.getTagFromServiceType(serviceType);

        // Create the thing type
        ThingType thingType = ThingTypeBuilder.instance(thingTypeUID, serviceName)
                .withDescription("Homekit " + serviceName + " HomekitService").withCategory("homekit").build();

        // Store the thing type
        putThingType(thingType);
        logger.debug("Created ThingType {} for Homekit service type {}", thingTypeUID, serviceType);
    }

    // private List<ChannelDefinition> createChannelDefinitions(String serviceType, HomekitFactory homekitFactory) {
    // // Get all characteristic types supported by this service
    // Set<String> characteristicTypes = homekitFactory.getSupportedCharacteristicTypes();

    // // Filter to only include characteristics that are relevant to this service
    // return characteristicTypes.stream().filter(characteristicType -> {
    // // Check if this characteristic type is associated with the service
    // return homekitFactory.getService(characteristicType) != null
    // && homekitFactory.getServiceType(characteristicType).equals(serviceType);
    // }).map(characteristicType -> {
    // // Get the channel type UID for this characteristic
    // ChannelTypeUID channelTypeUID = homekitFactory.getChannelTypeUID(characteristicType);
    // if (channelTypeUID == null) {
    // logger.warn("No ChannelTypeUID found for characteristic type: {}", characteristicType);
    // return null;
    // }

    // // Create a channel definition
    // return new ChannelDefinitionBuilder(homekitFactory.getTagFromCharacteristicType(characteristicType),
    // channelTypeUID).withLabel(characteristicType)
    // .withDescription("Homekit " + characteristicType + " HomekitCharacteristic").build();
    // }).filter(def -> def != null).collect(Collectors.toList());
    // }

    // private List<ChannelGroupDefinition> createChannelGroupDefinitions(String serviceType,
    // HomekitFactory homekitFactory) {
    // // Create a channel group definition for this service
    // // This is a simplification - in a real implementation, you would need to determine
    // // which services should be grouped together
    // ChannelGroupTypeUID channelGroupTypeUID = getChannelGroupTypeUID(serviceType);
    // return List.of(new ChannelGroupDefinition(serviceType, channelGroupTypeUID, serviceType,
    // "Homekit " + serviceType + " HomekitService Group"));
    // }

    public ThingTypeUID getThingTypeUID(String serviceType) {
        // Create a unique ID for the thing type based on the service type
        // Format: homekit:serviceTag
        return new ThingTypeUID("homekit", serviceType);
    }

    private ChannelGroupTypeUID getChannelGroupTypeUID(String serviceType) {
        // Create a unique ID for the channel group type based on the service type
        // Format: homekit:service-serviceType
        String serviceTypeId = serviceType.replaceAll("^0*([0-9a-fA-F]+)-0000-1000-8000-0026BB765291$", "$1");
        return new ChannelGroupTypeUID("homekit", "service-" + serviceTypeId);
    }

    @Override
    public Collection<ThingType> getThingTypes(@Nullable Locale locale) {
        // Return all stored thing types
        return super.getThingTypes(locale);
    }

    @Override
    public @Nullable ThingType getThingType(ThingTypeUID thingTypeUID, @Nullable Locale locale) {
        // Return the specific thing type if it exists
        return super.getThingType(thingTypeUID, locale);
    }

    public String getServiceTag(String serviceType) throws HomekitException {
        // find the factory that supports the service
        @Nullable
        HomekitFactory factory = homekitFactories.get(serviceType);
        if (factory != null) {
            String tag = factory.getTagFromServiceType(serviceType);
            if (tag != null) {
                return tag;
            }
        }
        throw new HomekitException("No factory found for service type: " + serviceType);
    }

    public String getServiceTypeFromTag(String tag) throws HomekitException {
        // find the factory that supports the tag
        for (HomekitFactory factory : homekitFactories.values()) {
            String serviceType = factory.getServiceTypeFromTag(tag);
            if (serviceType != null) {
                return serviceType;
            }
        }
        throw new HomekitException("No service type found for tag: " + tag);
    }
}
