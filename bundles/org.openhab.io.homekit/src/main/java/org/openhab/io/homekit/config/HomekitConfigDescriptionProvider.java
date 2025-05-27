package org.openhab.io.homekit.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionBuilder;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.config.core.ConfigDescriptionProvider;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides configuration descriptions for HomeKit configurations.
 * This allows the configuration to be managed through the OpenHAB UI.
 *
 * The class integrates with:
 * - {@link org.openhab.core.config.core.ConfigDescriptionProvider} for configuration description provision
 * - {@link org.openhab.core.config.core.ConfigDescription} for configuration structure
 * - {@link org.openhab.core.config.core.ConfigDescriptionParameter} for parameter definitions
 *
 * Configuration Parameters:
 * - Item Configuration:
 *   - item.enabled: Enable/disable HomeKit integration for items
 *   - item.type: HomeKit type for items
 * - Thing Configuration:
 *   - thing.enabled: Enable/disable HomeKit integration for things
 *   - thing.type: HomeKit type for things
 * - Channel Configuration:
 *   - channel.enabled: Enable/disable HomeKit integration for channels
 *   - channel.type: HomeKit type for channels
 * - Accessory Configuration:
 *   - accessory.enabled: Enable/disable HomeKit integration for accessories
 *   - accessory.type: HomeKit type for accessories
 *
 * Key Features:
 * - UI-friendly configuration descriptions
 * - Type-safe parameter definitions
 * - Default value support
 * - Localization support
 * - Parameter validation
 * - Configuration grouping
 *
 * Usage Patterns:
 * - Retrieving descriptions: Use {@link #getConfigDescriptions(Locale)}
 * - Getting specific description: Use {@link #getConfigDescription(URI, Locale)}
 * - Creating descriptions: Use {@link #createConfigDescription()}
 *
 * UI Integration:
 * - Provides structured configuration UI
 * - Supports parameter validation
 * - Enables default values
 * - Facilitates configuration management
 * - Supports localization
 *
 * @author Karel Goderis - Initial contribution
 */
@Component(service = ConfigDescriptionProvider.class)
@NonNullByDefault
public class HomekitConfigDescriptionProvider implements ConfigDescriptionProvider {
    private static final Logger logger = LoggerFactory.getLogger(HomekitConfigDescriptionProvider.class);
    private static final String CONFIG_URI_STRING = "homekit:config";
    private static final URI CONFIG_URI;

    static {
        try {
            CONFIG_URI = new URI(CONFIG_URI_STRING);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Invalid URI: " + CONFIG_URI_STRING, e);
        }
    }

    @Override
    public @NonNull Collection<@NonNull ConfigDescription> getConfigDescriptions(@Nullable Locale locale) {
        return Collections.singleton(createConfigDescription());
    }

    @Override
    public @Nullable ConfigDescription getConfigDescription(@NonNull URI uri, @Nullable Locale locale) {
        if (CONFIG_URI.equals(uri)) {
            return createConfigDescription();
        }
        return null;
    }

    private ConfigDescription createConfigDescription() {
        return ConfigDescriptionBuilder.create(CONFIG_URI)
                .withParameter(ConfigDescriptionParameterBuilder.create("item.enabled", Type.BOOLEAN)
                        .withLabel("Item Enabled").withDescription("Enable/disable HomeKit integration for an item")
                        .withDefault("true").build())
                .withParameter(ConfigDescriptionParameterBuilder.create("item.type", Type.TEXT).withLabel("Item Type")
                        .withDescription("The HomeKit type for this item").build())
                .withParameter(ConfigDescriptionParameterBuilder.create("thing.enabled", Type.BOOLEAN)
                        .withLabel("Thing Enabled").withDescription("Enable/disable HomeKit integration for a thing")
                        .withDefault("true").build())
                .withParameter(ConfigDescriptionParameterBuilder.create("thing.type", Type.TEXT).withLabel("Thing Type")
                        .withDescription("The HomeKit type for this thing").build())
                .withParameter(ConfigDescriptionParameterBuilder.create("channel.enabled", Type.BOOLEAN)
                        .withLabel("Channel Enabled")
                        .withDescription("Enable/disable HomeKit integration for a channel").withDefault("true")
                        .build())
                .withParameter(ConfigDescriptionParameterBuilder.create("channel.type", Type.TEXT)
                        .withLabel("Channel Type").withDescription("The HomeKit type for this channel").build())
                .withParameter(ConfigDescriptionParameterBuilder.create("accessory.enabled", Type.BOOLEAN)
                        .withLabel("Accessory Enabled")
                        .withDescription("Enable/disable HomeKit integration for an accessory").withDefault("true")
                        .build())
                .withParameter(ConfigDescriptionParameterBuilder.create("accessory.type", Type.TEXT)
                        .withLabel("Accessory Type").withDescription("The HomeKit type for this accessory").build())
                .build();
    }
}
