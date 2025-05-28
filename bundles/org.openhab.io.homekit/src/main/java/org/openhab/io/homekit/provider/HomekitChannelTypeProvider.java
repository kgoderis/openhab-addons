package org.openhab.io.homekit.provider;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.storage.StorageService;
import org.openhab.core.thing.binding.AbstractStorageBasedTypeProvider;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeBuilder;
import org.openhab.core.thing.type.ChannelTypeProvider;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.StateChannelTypeBuilder;
import org.openhab.io.homekit.HomekitBindingConstants;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the creation and registration of HomeKit channel types in the OpenHAB ecosystem.
 *
 * This class serves as the central provider for HomeKit channel types, converting HomeKit characteristic types
 * into OpenHAB channel types. It handles the mapping between HomeKit characteristics and OpenHAB item types,
 * ensuring proper integration between HomeKit and OpenHAB's channel system.
 *
 * The provider implements a sophisticated type conversion system that:
 * - Creates channel types for HomeKit characteristics
 * - Maps characteristic types to accepted item types
 * - Manages channel type persistence
 * - Handles characteristic type to tag conversions
 *
 * The class integrates with:
 * - {@link StorageService} for persistent storage of channel types
 * - {@link HomekitCharacteristicFactory} for characteristic type management
 * - {@link org.openhab.core.thing.type.ChannelType OpenHAB's channel type system} for type registration
 *
 * Key features:
 * - Automatic channel type generation from characteristic types
 * - Support for multiple item types per characteristic
 * - Persistent storage of channel types
 * - Factory-dependent type management
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0
 */
@NonNullByDefault
@Component(service = { ChannelTypeProvider.class })
public class HomekitChannelTypeProvider extends AbstractStorageBasedTypeProvider {
    private final Logger logger = LoggerFactory.getLogger(HomekitChannelTypeProvider.class);

    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit ChannelType Provider: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    protected static final String LOG_TYPE = LOG_PREFIX + "Type - ";

    private final HomekitCharacteristicFactory characteristicFactory;

    /**
     * Creates a new HomeKit channel type provider.
     *
     * This constructor initializes the provider with all required services and establishes the foundation
     * for managing HomeKit channel types. It sets up the necessary connections to various system services
     * and prepares the provider for operation.
     *
     * The initialization process includes:
     * - Setting up storage for channel types
     * - Configuring characteristic factory integration
     * - Creating factory-dependent channel types
     *
     * @param storageService Service for persistent storage of channel types
     * @param characteristicFactory Factory for managing HomeKit characteristics
     */
    @Activate
    public HomekitChannelTypeProvider(@Reference StorageService storageService,
            @Reference HomekitCharacteristicFactory characteristicFactory) {
        super(storageService);
        this.characteristicFactory = characteristicFactory;
        logger.info("{}Initializing HomeKit channel type provider", LOG_INIT);
        addFactoryDependentChannelTypes();
        logger.info("{}HomeKit channel type provider initialized", LOG_INIT);
    }

    /**
     * Adds channel types that are dependent on specific factories.
     *
     * This method iterates through all supported characteristic types from the characteristic factory
     * and creates corresponding channel types for each characteristic and its accepted item types.
     */
    private void addFactoryDependentChannelTypes() {
        logger.debug("{}Adding factory-dependent channel types", LOG_TYPE);
        Set<String> characteristicTypes = characteristicFactory.getSupportedCharacteristicTypes();

        for (String characteristicType : characteristicTypes) {
            logger.debug("{}Processing characteristic type: {}", LOG_TYPE, characteristicType);
            Set<String> acceptedItemTypes = characteristicFactory.getAcceptedItemTypes(characteristicType);

            for (String acceptedItemType : acceptedItemTypes) {
                ChannelTypeUID channelTypeUID = new ChannelTypeUID(HomekitBindingConstants.BINDING_ID,
                        characteristicType);
                if (channelTypeUID != null) {
                    try {
                        StateChannelTypeBuilder builder = ChannelTypeBuilder.state(channelTypeUID, characteristicType,
                                acceptedItemType);
                        ChannelType channelType = builder.build();
                        putChannelType(channelType);
                        logger.debug("{}Created channel type {} for characteristic {} with item type {}", LOG_TYPE,
                                channelTypeUID, characteristicType, acceptedItemType);
                    } catch (Exception e) {
                        logger.error("{}Failed to create channel type for characteristic {} with item type {}: {}",
                                LOG_ERROR, characteristicType, acceptedItemType, e.getMessage(), e);
                    }
                } else {
                    logger.warn("{}Could not create ChannelTypeUID for characteristic type: {}", LOG_WARN,
                            characteristicType);
                }
            }
        }
    }

    /**
     * Gets all registered channel types.
     *
     * This method returns all channel types registered with the provider, optionally filtered
     * by locale for internationalization support.
     *
     * @param locale The locale to get channel types for, or null for default
     * @return Collection of registered channel types
     */
    @Override
    public Collection<ChannelType> getChannelTypes(@Nullable Locale locale) {
        return super.getChannelTypes(locale);
    }

    /**
     * Gets a specific channel type by its UID.
     *
     * This method retrieves a specific channel type from the provider based on its unique
     * identifier, optionally filtered by locale for internationalization support.
     *
     * @param channelTypeUID The UID of the channel type to get
     * @param locale The locale to get the channel type for, or null for default
     * @return The channel type, or null if not found
     */
    @Override
    public @Nullable ChannelType getChannelType(ChannelTypeUID channelTypeUID, @Nullable Locale locale) {
        return super.getChannelType(channelTypeUID, locale);
    }

    // public String getCharacteristicTypeFromTag(String tag) throws HomekitException {
    // // traverse factories and get the instance type from the tag
    // for (HomekitFactory factory : homekitFactories.values()) {
    // String instanceType = factory.getCharacteristicTypeFromTag(tag);
    // if (instanceType != null) {
    // return instanceType;
    // }
    // }
    // throw new HomekitException("No factory found for tag: " + tag);
    // }

    // public String getCharacteristicTag(String characteristicType) throws HomekitException {
    // // get the tag from the characteristic type
    // @Nullable
    // HomekitFactory factory = homekitFactories.get(characteristicType);
    // if (factory != null) {
    // String tag = factory.getTagFromCharacteristicType(characteristicType);
    // if (tag != null) {
    // return tag;
    // }
    // }
    // throw new HomekitException("No factory found for characteristic type: " + characteristicType);
    // }
}
