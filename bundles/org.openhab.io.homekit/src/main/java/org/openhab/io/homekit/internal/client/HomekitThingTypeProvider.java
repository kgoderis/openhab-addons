package org.openhab.io.homekit.internal.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.ThingTypeProvider;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeBuilder;
import org.osgi.service.component.annotations.Component;

@NonNullByDefault
@Component(immediate = true, service = ThingTypeProvider.class)
public class HomekitThingTypeProvider implements ThingTypeProvider {

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.unmodifiableSet(Stream
            .of(HomekitAccessoryHandler.SUPPORTED_THING_TYPES.stream(),
                    HomekitAccessoryBridgeHandler.SUPPORTED_THING_TYPES.stream())
            .flatMap(i -> i).collect(Collectors.toSet()));

    @Override
    public @NonNull Collection<@NonNull ThingType> getThingTypes(@Nullable Locale locale) {
        Set<ThingType> thingTypes = new HashSet<ThingType>();

        for (ThingTypeUID typeUID : SUPPORTED_THING_TYPES) {
            ThingType type = getThingType(typeUID, locale);
            if (type != null) {
                thingTypes.add(type);
            }
        }

        return thingTypes;
    }

    @Override
    public @Nullable ThingType getThingType(@NonNull ThingTypeUID thingTypeUID, @Nullable Locale locale) {
        if (SUPPORTED_THING_TYPES.contains(thingTypeUID)) {

            if (thingTypeUID.equals(HomekitBindingConstants.THING_TYPE_BRIDGE)) {
                ThingTypeBuilder builder;
                try {
                    builder = ThingTypeBuilder
                            .instance(HomekitBindingConstants.THING_TYPE_BRIDGE, "Homekit Accessory Bridge")
                            .withRepresentationProperty(HomekitBindingConstants.DEVICE_ID)
                            .withConfigDescriptionURI(new URI(HomekitBindingConstants.CONFIGURATION_URI));
                    return builder.buildBridge();

                } catch (URISyntaxException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            if (thingTypeUID.equals(HomekitBindingConstants.THING_TYPE_ACCESSORY)) {
                ThingTypeBuilder builder;
                try {
                    builder = ThingTypeBuilder
                            .instance(HomekitBindingConstants.THING_TYPE_ACCESSORY, "Homekit Accessory")
                            .withRepresentationProperty(HomekitBindingConstants.DEVICE_ID)
                            .withConfigDescriptionURI(new URI(HomekitBindingConstants.CONFIGURATION_URI));
                    return builder.build();
                } catch (URISyntaxException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        return null;
    }
}
