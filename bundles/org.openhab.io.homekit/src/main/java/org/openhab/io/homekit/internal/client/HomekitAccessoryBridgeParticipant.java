package org.openhab.io.homekit.internal.client;

import java.util.Collections;
import java.util.Set;

import javax.jmdns.ServiceInfo;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.mdns.MDNSDiscoveryParticipant;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomekitAccessoryBridgeParticipant implements MDNSDiscoveryParticipant {

    private static final String HAP_SERVICE_TYPE = "_hap._tcp.local.";

    private final Logger logger = LoggerFactory.getLogger(HomekitAccessoryBridgeParticipant.class);

    private final HomekitAccessoryBridgeHandler homekitBridgeHandler;
    int lastConfigurationNumber;

    public HomekitAccessoryBridgeParticipant(@NonNull HomekitAccessoryBridgeHandler homekitBridgeHandler) {
        this.homekitBridgeHandler = homekitBridgeHandler;
    }

    @Override
    public @NonNull String getServiceType() {
        return HAP_SERVICE_TYPE;
    }

    @Override
    public @NonNull Set<@NonNull ThingTypeUID> getSupportedThingTypeUIDs() {
        return Collections.emptySet();
    }

    @Override
    public @Nullable DiscoveryResult createResult(@NonNull ServiceInfo service) {
        if (homekitBridgeHandler.getAccessoryPairingId().equals(service.getPropertyString("id"))) {
            int configurationNumber = Integer.parseInt(service.getPropertyString("c#"));
            int lastConfigurationNumber = homekitBridgeHandler.getConfigurationNumber();

            if (configurationNumber > lastConfigurationNumber
                    || (configurationNumber >= 1 && lastConfigurationNumber == 65535)) {
                logger.info("The Homekit Configuration Number of {} got bumped from {} to {}",
                        homekitBridgeHandler.getThing().getUID(), lastConfigurationNumber, configurationNumber);
                homekitBridgeHandler.updateConfigurationNumber(configurationNumber);
                homekitBridgeHandler.startSearch();
            }
        }

        return null;
    }

    @Override
    public @Nullable ThingUID getThingUID(@NonNull ServiceInfo service) {
        return null;
    }

}
