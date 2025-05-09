package org.openhab.io.homekit.provider;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.storage.StorageService;
import org.openhab.core.thing.binding.AbstractStorageBasedTypeProvider;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeBuilder;
import org.openhab.core.thing.type.ChannelTypeProvider;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.StateChannelTypeBuilder;
import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.openhab.io.homekit.exception.HomekitException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides ChannelTypes based on registered HomekitFactory instances.
 * 
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
@Component(service = { ChannelTypeProvider.class })
public class HomekitChannelTypeProvider extends AbstractStorageBasedTypeProvider {
    private final Logger logger = LoggerFactory.getLogger(HomekitChannelTypeProvider.class);
    private final Map<String, HomekitFactory> homekitFactories = new ConcurrentHashMap<>();

    @Activate
    public HomekitChannelTypeProvider(@Reference StorageService storageService) {
        super(storageService);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addHomekitFactory(HomekitFactory homekitFactory) {
        // Add all supported characteristic types from this factory
        Set<String> characteristicTypes = homekitFactory.getSupportedCharacteristicTypes();
        for (String characteristicType : characteristicTypes) {
            homekitFactories.put(characteristicType, homekitFactory);

            // Create and store channel type for this characteristic
            String acceptedItemType = homekitFactory.getCharacteristicAcceptedItemType(characteristicType);
            if (acceptedItemType != null) {
                ChannelTypeUID channelTypeUID = homekitFactory.getChannelTypeUID(characteristicType);
                if (channelTypeUID != null) {
                    StateChannelTypeBuilder builder = ChannelTypeBuilder.state(channelTypeUID, characteristicType,
                            acceptedItemType);
                    ChannelType channelType = builder.build();
                    putChannelType(channelType);
                }
            }
        }
    }

    protected void removeHomekitFactory(HomekitFactory homekitFactory) {
        // Remove all channel types from this factory
        Set<String> characteristicTypes = homekitFactory.getSupportedCharacteristicTypes();
        for (String characteristicType : characteristicTypes) {
            homekitFactories.remove(characteristicType);

            // Remove the channel type
            ChannelTypeUID channelTypeUID = homekitFactory.getChannelTypeUID(characteristicType);
            if (channelTypeUID != null) {
                removeChannelType(channelTypeUID);
            }
        }
    }

    @Override
    public Collection<ChannelType> getChannelTypes(@Nullable Locale locale) {
        // Return all stored channel types
        return super.getChannelTypes(locale);
    }

    @Override
    public @Nullable ChannelType getChannelType(ChannelTypeUID channelTypeUID, @Nullable Locale locale) {
        // Return the specific channel type if it exists
        return super.getChannelType(channelTypeUID, locale);
    }

    public String getCharacteristicTypeFromTag(String tag) throws HomekitException {
        // traverse factories and get the instance type from the tag
        for (HomekitFactory factory : homekitFactories.values()) {
            String instanceType = factory.getCharacteristicTypeFromTag(tag);
            if (instanceType != null) {
                return instanceType;
            }
        }
        throw new HomekitException("No factory found for tag: " + tag);
    }

    public String getCharacteristicTag(String characteristicType) throws HomekitException {
        // get the tag from the characteristic type
        @Nullable
        HomekitFactory factory = homekitFactories.get(characteristicType);
        if (factory != null) {
            String tag = factory.getTagFromCharacteristicType(characteristicType);
            if (tag != null) {
                return tag;
            }
        }
        throw new HomekitException("No factory found for characteristic type: " + characteristicType);
    }
}
