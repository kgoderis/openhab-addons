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
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.exception.HomekitException;
import org.openhab.io.homekit.network.discovery.HomekitBindingConstants;
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
    private final HomekitCharacteristicFactory characteristicFactory;

    @Activate
    public HomekitChannelTypeProvider(@Reference StorageService storageService,
            @Reference HomekitCharacteristicFactory characteristicFactory) {
        super(storageService);
        this.characteristicFactory = characteristicFactory;
        addFactoryDependentChannelTypes();
    }

    private void addFactoryDependentChannelTypes() {

        Set<String> characteristicTypes = characteristicFactory.getSupportedCharacteristicTypes();
        for (String characteristicType : characteristicTypes) {

            // Create and store channel type for this characteristic
            String acceptedItemType = characteristicFactory.getCharacteristicAcceptedItemType(characteristicType);
            if (acceptedItemType != null) {
                ChannelTypeUID channelTypeUID = new ChannelTypeUID(HomekitBindingConstants.BINDING_ID,characteristicType);
                if (channelTypeUID != null) {
                    StateChannelTypeBuilder builder = ChannelTypeBuilder.state(channelTypeUID, characteristicType,
                            acceptedItemType);
                    ChannelType channelType = builder.build();
                    putChannelType(channelType);
                }
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

    // public String getCharacteristicTypeFromTag(String tag) throws HomekitException {
    //     // traverse factories and get the instance type from the tag
    //     for (HomekitFactory factory : homekitFactories.values()) {
    //         String instanceType = factory.getCharacteristicTypeFromTag(tag);
    //         if (instanceType != null) {
    //             return instanceType;
    //         }
    //     }
    //     throw new HomekitException("No factory found for tag: " + tag);
    // }

    // public String getCharacteristicTag(String characteristicType) throws HomekitException {
    //     // get the tag from the characteristic type
    //     @Nullable
    //     HomekitFactory factory = homekitFactories.get(characteristicType);
    //     if (factory != null) {
    //         String tag = factory.getTagFromCharacteristicType(characteristicType);
    //         if (tag != null) {
    //             return tag;
    //         }
    //     }
    //     throw new HomekitException("No factory found for characteristic type: " + characteristicType);
    // }
}
