package org.openhab.io.homekit.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

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
 * Provides ThingTypes based on registered HomekitFactory instances.
 * Each service type registered with a HomekitFactory is converted to a ThingType.
 * 
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
@Component(service = { ThingTypeProvider.class })
public class HomekitThingTypeProvider extends AbstractStorageBasedTypeProvider {
    private final Logger logger = LoggerFactory.getLogger(HomekitThingTypeProvider.class);
    private final HomekitServiceFactory homekitServiceFactory;
    private final HomekitChannelGroupTypeProvider channelGroupTypeProvider;

    @Activate
    public HomekitThingTypeProvider(@Reference StorageService storageService,
            @Reference HomekitServiceFactory homekitServiceFactory,
            @Reference HomekitChannelGroupTypeProvider channelGroupTypeProvider) {
        super(storageService);
        this.homekitServiceFactory = homekitServiceFactory;
        this.channelGroupTypeProvider = channelGroupTypeProvider;
        addFactoryIndependentThingTypes();
        addFactoryDependentThingTypes();
    }

    private void addFactoryIndependentThingTypes() {
        // Add thing types for services that are not factory-specific
        ThingTypeUID thingTypeUID = new ThingTypeUID(HomekitBindingConstants.BINDING_ID, "accessory");

        // Create the thing type
        ThingType thingType = ThingTypeBuilder.instance(thingTypeUID, "Homekit Accessory")
                .withDescription("Homekit Accessory")
                .withCategory("homekit")
                .build();

        // Store the thing type
        putThingType(thingType);
        logger.debug("Created ThingType {} for Homekit", thingTypeUID);
    }

    private void addFactoryDependentThingTypes() {
        // Add thing types for services that are factory-specific
        for (String serviceType : homekitServiceFactory.getSupportedServiceTypes()) {
            createThingTypeForService(serviceType);
        }
    }

    // @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
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

    private void createThingTypeForService(String serviceType) {
        // Create a unique ID for the thing type
        ThingTypeUID thingTypeUID = getThingTypeUID(serviceType);
        if (thingTypeUID == null) {
            logger.warn("Could not create ThingTypeUID for service type: {}", serviceType);
            return;
        }

        // Get the service class to determine the service name
        String serviceName = homekitServiceFactory.getTagFromServiceType(serviceType);
        if (serviceName == null || serviceName.isEmpty()) {
            logger.warn("Invalid service name for service type: {}", serviceType);
            return;
        }

        // Create channel group definitions for this service
        List<ChannelGroupDefinition> channelGroupDefinitions = createChannelGroupDefinitions(serviceType);
        if (channelGroupDefinitions.isEmpty()) {
            logger.warn("No channel group definitions found for service type: {}", serviceType);
            return;
        }

        // Create the thing type with channel groups
        ThingType thingType = ThingTypeBuilder.instance(thingTypeUID, serviceName)
                .withDescription("Homekit " + serviceName + " Service")
                .withCategory("homekit")
                .withChannelGroupDefinitions(channelGroupDefinitions)
                .build();

        // Store the thing type
        putThingType(thingType);
        logger.debug("Created ThingType {} for Homekit service type {} with {} channel groups", thingTypeUID, 
            serviceType, channelGroupDefinitions.size());
    }

    private List<ChannelGroupDefinition> createChannelGroupDefinitions(String serviceType) {
        List<ChannelGroupDefinition> definitions = new ArrayList<>();
        try {
            String serviceTag = homekitServiceFactory.getTagFromServiceType(serviceType);
            if (serviceTag == null || serviceTag.isEmpty()) {
                logger.warn("Invalid service tag for service type: {}", serviceType);
                return definitions;
            }

            // Create a channel group type UID for this service
            ChannelGroupTypeUID channelGroupTypeUID = new ChannelGroupTypeUID(HomekitBindingConstants.BINDING_ID, 
                "service-" + serviceTag);

            // Verify that the channel group type exists
            if (channelGroupTypeProvider.getChannelGroupType(channelGroupTypeUID, null) == null) {
                logger.warn("No channel group type found for service type: {}", serviceType);
                return definitions;
            }

            // Create a channel group definition with index 1 (first instance)
            definitions.add(new ChannelGroupDefinition(serviceTag + ".1", channelGroupTypeUID, serviceTag,
                    "Homekit " + serviceTag + " Service"));

            logger.debug("Created channel group definition for service type {} with UID {}", serviceType, 
                channelGroupTypeUID);
        } catch (Exception e) {
            logger.error("Failed to create channel group definitions for service type {}: {}", serviceType, 
                e.getMessage(), e);
        }
        return definitions;
    }

    public ThingTypeUID getThingTypeUID(String serviceType) {
        // Create a unique ID for the thing type based on the service type
        // Format: homekit:serviceTag
        String serviceTag = homekitServiceFactory.getTagFromServiceType(serviceType);
        if (serviceTag == null || serviceTag.isEmpty()) {
            return null;
        }
        return new ThingTypeUID(HomekitBindingConstants.BINDING_ID, serviceTag);
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
    // throw new HomekitException("No factory found for service type: " + serviceType);
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
