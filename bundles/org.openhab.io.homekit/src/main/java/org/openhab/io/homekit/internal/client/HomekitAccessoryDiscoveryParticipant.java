package org.openhab.io.homekit.internal.client;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.jmdns.ServiceInfo;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.mdns.MDNSDiscoveryParticipant;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.api.AccessoryServerRegistry;
import org.openhab.io.homekit.internal.server.AccessoryServerUID;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = MDNSDiscoveryParticipant.class, immediate = true)
public class HomekitAccessoryDiscoveryParticipant implements MDNSDiscoveryParticipant {

    private final Logger logger = LoggerFactory.getLogger(HomekitAccessoryDiscoveryParticipant.class);

    private static final String HAP_SERVICE_TYPE = "_hap._tcp.local.";

    private final AccessoryServerRegistry accessoryServerRegistry;

    @Activate
    public HomekitAccessoryDiscoveryParticipant(@Reference AccessoryServerRegistry accessoryServerRegistry) {
        this.accessoryServerRegistry = accessoryServerRegistry;
    }

    @Override
    public @NonNull Set<@NonNull ThingTypeUID> getSupportedThingTypeUIDs() {
        return Collections.singleton(HomekitBindingConstants.THING_TYPE_ACCESSORY);
    }

    @Override
    public @NonNull String getServiceType() {
        return HAP_SERVICE_TYPE;
    }

    @Override
    public @Nullable DiscoveryResult createResult(@NonNull ServiceInfo service) {
        if (service.getApplication().contains("hap")) {

            String id = service.getPropertyString("id");
            if (id != null) {
                AccessoryServer accessoryServer = accessoryServerRegistry
                        .get(new AccessoryServerUID("BridgeAccessoryServer", id.replace(":", "")));

                if (accessoryServer != null) {
                    logger.debug("Skipping service info associated with an openHAB Accessory Server {}",
                            accessoryServer.getUID());
                } else {
                    ThingUID uid = getThingUID(service);

                    if (uid != null) {
                        Enumeration<String> serviceProperties = service.getPropertyNames();
                        Map<String, Object> properties = new HashMap<>();

                        properties.put("ip.addresses", service.getHostAddresses());
                        properties.put("port", service.getPort());

                        while (serviceProperties.hasMoreElements()) {
                            String element = serviceProperties.nextElement();
                            String value = service.getPropertyString(element);
                            properties.put(element, value);
                        }

                        return DiscoveryResultBuilder.create(uid).withProperties(properties)
                                .withRepresentationProperty(uid.getId()).withLabel("Homekit Accessory").build();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public @Nullable ThingUID getThingUID(@NonNull ServiceInfo service) {
        if (service.getApplication().contains("hap") && service.getPropertyString("id") != null) {

            String category = service.getPropertyString("ci");
            String id = service.getPropertyString("id").replace(":", "");

            if (category.equals("2")) {
                return new ThingUID(HomekitBindingConstants.THING_TYPE_BRIDGE, id);

            } else {
                return new ThingUID(HomekitBindingConstants.THING_TYPE_ACCESSORY, id);
            }
        }

        return null;
    }
}
