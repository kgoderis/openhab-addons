package org.openhab.io.homekit.internal.client;

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

@Component(service = ConfigDescriptionProvider.class, immediate = true)
@NonNullByDefault
public class HomekitConfigDescriptionProvider implements ConfigDescriptionProvider {

    @Override
    public @NonNull Collection<@NonNull ConfigDescription> getConfigDescriptions(@Nullable Locale locale) {
        try {

            ConfigDescriptionParameterBuilder clientIdParameterBuilder = ConfigDescriptionParameterBuilder
                    .create(HomekitAccessoryConfiguration.CLIENT_PAIRING_ID, Type.TEXT).withContext("thing");

            ConfigDescriptionParameterBuilder clientLTSKParameterBuilder = ConfigDescriptionParameterBuilder
                    .create(HomekitAccessoryConfiguration.CLIENT_LTSK, Type.TEXT).withContext("thing");

            ConfigDescriptionParameterBuilder configurationNumberBuilder = ConfigDescriptionParameterBuilder
                    .create(HomekitAccessoryConfiguration.CONFIGURATION_NUMBER, Type.TEXT).withContext("thing");

            ConfigDescriptionParameterBuilder idParameterBuilder = ConfigDescriptionParameterBuilder
                    .create(HomekitAccessoryConfiguration.ACCESSORY_PAIRING_ID, Type.TEXT).withContext("thing");

            ConfigDescriptionParameterBuilder hostParameterBuilder = ConfigDescriptionParameterBuilder
                    .create(HomekitAccessoryConfiguration.HOST, Type.TEXT).withContext("thing");

            ConfigDescriptionParameterBuilder portParameterBuilder = ConfigDescriptionParameterBuilder
                    .create(HomekitAccessoryConfiguration.PORT, Type.TEXT).withContext("thing");

            ConfigDescriptionBuilder builder = ConfigDescriptionBuilder
                    .create(new URI(HomekitBindingConstants.CONFIGURATION_URI))
                    .withParameter(clientIdParameterBuilder.build()).withParameter(clientLTSKParameterBuilder.build())
                    .withParameter(configurationNumberBuilder.build()).withParameter(idParameterBuilder.build())
                    .withParameter(hostParameterBuilder.build()).withParameter(portParameterBuilder.build());

            return Collections.singletonList(builder.build());
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return Collections.EMPTY_LIST;
    }

    @Override
    public @Nullable ConfigDescription getConfigDescription(@NonNull URI uri, @Nullable Locale locale) {
        return getConfigDescriptions(locale).stream().findFirst().get();
    }

}
