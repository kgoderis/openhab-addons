package org.openhab.io.homekit.internal.client;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.ThingTypeProvider;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeBuilder;
import org.osgi.service.component.annotations.Component;

@NonNullByDefault
@Component(service = ThingTypeProvider.class)
public class HomekitThingTypeProvider implements ThingTypeProvider {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections
            .singleton(HomekitBindingConstants.THING_TYPE_ACCESSORY);

    @Override
    public @NonNull Collection<@NonNull ThingType> getThingTypes(@Nullable Locale locale) {
        ThingTypeBuilder builder = ThingTypeBuilder.instance(HomekitBindingConstants.THING_TYPE_ACCESSORY,
                "Homekit Accessory");
        return Collections.singleton(builder.build());
    }

    @Override
    public @Nullable ThingType getThingType(@NonNull ThingTypeUID thingTypeUID, @Nullable Locale locale) {
        if (SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID)) {
            ThingTypeBuilder builder = ThingTypeBuilder.instance(HomekitBindingConstants.THING_TYPE_ACCESSORY,
                    "Homekit Accessory");
            return builder.build();
        }

        return null;
    }

}
