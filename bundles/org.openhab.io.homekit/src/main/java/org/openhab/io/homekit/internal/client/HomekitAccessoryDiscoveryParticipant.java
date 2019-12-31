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
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = MDNSDiscoveryParticipant.class, immediate = true)
public class HomekitAccessoryDiscoveryParticipant implements MDNSDiscoveryParticipant {

    private final Logger logger = LoggerFactory.getLogger(HomekitAccessoryDiscoveryParticipant.class);

    private static final String HAP_SERVICE_TYPE = "_hap._tcp.local.";

    public HomekitAccessoryDiscoveryParticipant() {
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
            ThingUID uid = getThingUID(service);

            Enumeration<String> propertiesList = service.getPropertyNames();
            while (propertiesList.hasMoreElements()) {
                String element = propertiesList.nextElement();
                String value = service.getPropertyString(element);
                // process element
                logger.debug("Property {} {}", element, value);
            }

            if (uid != null) {
                String hostAddress = service.getName() + "." + service.getDomain() + ".";
                Map<String, Object> properties = new HashMap<>(2);
                // properties.put(DigitalSTROMBindingConstants.HOST, hostAddress);
                return DiscoveryResultBuilder.create(uid).withProperties(properties)
                        .withRepresentationProperty(uid.getId()).withLabel("digitalSTROM-Server").build();
            }
        }
        return null;
    }

    @Override
    public @Nullable ThingUID getThingUID(@NonNull ServiceInfo service) {
        // TODO Auto-generated method stub
        return null;
    }

}
