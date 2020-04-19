package org.openhab.io.homekit.internal.client;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

import javax.jmdns.ServiceInfo;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.mdns.MDNSDiscoveryParticipant;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.io.homekit.internal.server.RemoteStandAloneAccessoryServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessoryServerConfigurationChangeParticipant implements MDNSDiscoveryParticipant {

    private static final String HAP_SERVICE_TYPE = "_hap._tcp.local.";

    private final Logger logger = LoggerFactory.getLogger(AccessoryServerConfigurationChangeParticipant.class);

    private final RemoteStandAloneAccessoryServer accessoryServer;
    int lastConfigurationNumber;

    public AccessoryServerConfigurationChangeParticipant(@NonNull RemoteStandAloneAccessoryServer participant) {
        this.accessoryServer = participant;
    }

    @Override
    public @NonNull Set<@NonNull ThingTypeUID> getSupportedThingTypeUIDs() {
        return Collections.emptySet();
    }

    @Override
    public @NonNull String getServiceType() {
        return HAP_SERVICE_TYPE;
    }

    @Override
    public @Nullable DiscoveryResult createResult(@NonNull ServiceInfo service) {
        String accessoryPairingId = new String(accessoryServer.getPairing().getDestinationPairingId(),
                StandardCharsets.UTF_8);
        if (accessoryPairingId != null && accessoryPairingId.equals(service.getPropertyString("id"))) {
            int configurationNumber = Integer.parseInt(service.getPropertyString("c#"));
            int lastConfigurationNumber = accessoryServer.getConfigurationIndex();

            if (configurationNumber > lastConfigurationNumber
                    || (configurationNumber >= 1 && lastConfigurationNumber == 65535)) {
                accessoryServer.setConfigurationIndex(configurationNumber);
                // TODO : accessoryServer -> Search for new accessories, and process them
            }
        }

        return null;
    }

    @Override
    public @Nullable ThingUID getThingUID(@NonNull ServiceInfo service) {
        return null;
    }

}
