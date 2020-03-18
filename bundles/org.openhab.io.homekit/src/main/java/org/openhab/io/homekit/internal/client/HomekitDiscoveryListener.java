package org.openhab.io.homekit.internal.client;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.discovery.DiscoveryListener;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.config.discovery.DiscoveryServiceRegistry;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
@NonNullByDefault
public class HomekitDiscoveryListener implements DiscoveryListener {

    private final Logger logger = LoggerFactory.getLogger(HomekitDiscoveryListener.class);

    private final DiscoveryServiceRegistry discoveryServiceRegistry;
    private final ThingRegistry thingRegistry;
    private final ThingTypeRegistry thingTypeRegistry;

    @Activate
    public HomekitDiscoveryListener(final @Reference DiscoveryServiceRegistry discoveryServiceRegistry,
            final @Reference ThingRegistry thingRegistry, final @Reference ThingTypeRegistry thingTypeRegistry) {
        this.discoveryServiceRegistry = discoveryServiceRegistry;
        this.thingRegistry = thingRegistry;
        this.thingTypeRegistry = thingTypeRegistry;
    }

    @Activate
    protected void activate() {
        this.discoveryServiceRegistry.addDiscoveryListener(this);
    }

    @Deactivate
    protected void deactivate() {
        this.discoveryServiceRegistry.removeDiscoveryListener(this);
    }

    @Override
    public void thingDiscovered(@NonNull DiscoveryService source, @NonNull DiscoveryResult result) {
        String value = getRepresentationValue(result);
        if (value != null) {
            Optional<Thing> thing = thingRegistry.stream()
                    .filter(t -> Objects.equals(value, getRepresentationPropertyValueForThing(t)))
                    .filter(t -> Objects.equals(t.getThingTypeUID(), result.getThingTypeUID())).findFirst();
            if (thing.isPresent()) {
                Thing theThing = thing.get();

                InetAddress currentHost = (InetAddress) theThing.getConfiguration()
                        .get(HomekitAccessoryConfiguration.HOST);
                int currentPort = (int) theThing.getConfiguration().get(HomekitAccessoryConfiguration.PORT);

                if (!currentHost.equals(result.getProperties().get(HomekitAccessoryConfiguration.HOST))
                        || currentPort != Integer
                                .parseInt((String) result.getProperties().get(HomekitAccessoryConfiguration.PORT))) {
                    logger.info("'{}' : The Homekit Accessory's destination changed from {}:{} to {}:{}",
                            theThing.getUID(), currentHost, currentPort,
                            result.getProperties().get(HomekitAccessoryConfiguration.HOST),
                            result.getProperties().get(HomekitAccessoryConfiguration.PORT));

                    ThingHandler thingHandler = theThing.getHandler();

                    if (thingHandler instanceof HomekitAccessoryBridgeHandler) {
                        ((HomekitAccessoryBridgeHandler) thingHandler).updateDestination(
                                (InetAddress) result.getProperties().get(HomekitAccessoryConfiguration.HOST),
                                (int) result.getProperties().get(HomekitAccessoryConfiguration.PORT));
                    }

                    // Map<String, String> properties = theThing.getProperties();
                    // properties.put(HomekitAccessoryConfiguration.HOST_ADDRESS,
                    // (String) result.getProperties().get(HomekitAccessoryConfiguration.HOST_ADDRESS));
                    // properties.put(HomekitAccessoryConfiguration.PORT,
                    // (String) result.getProperties().get(HomekitAccessoryConfiguration.PORT));
                    // theThing.setProperties(properties);
                }
            }
        }
    }

    @Override
    public void thingRemoved(@NonNull DiscoveryService source, @NonNull ThingUID thingUID) {
        logger.info("thingRemoved");
    }

    @Override
    public @Nullable Collection<@NonNull ThingUID> removeOlderResults(@NonNull DiscoveryService source, long timestamp,
            @Nullable Collection<@NonNull ThingTypeUID> thingTypeUIDs, @Nullable ThingUID bridgeUID) {
        // TODO Auto-generated method stub
        return null;
    }

    private @Nullable String getRepresentationValue(DiscoveryResult result) {
        return result.getRepresentationProperty() != null
                ? Objects.toString(result.getProperties().get(result.getRepresentationProperty()), null)
                : null;
    }

    private @Nullable String getRepresentationPropertyValueForThing(Thing thing) {
        ThingType thingType = thingTypeRegistry.getThingType(thing.getThingTypeUID());
        if (thingType != null) {
            String representationProperty = thingType.getRepresentationProperty();
            if (representationProperty == null) {
                return null;
            }
            Map<String, String> properties = thing.getProperties();
            if (properties.containsKey(representationProperty)) {
                return properties.get(representationProperty);
            }
            Configuration configuration = thing.getConfiguration();
            if (configuration.containsKey(representationProperty)) {
                return String.valueOf(configuration.get(representationProperty));
            }
        }
        return null;
    }

}
