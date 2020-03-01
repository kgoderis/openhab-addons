package org.openhab.io.homekit.internal.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.types.Command;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.api.Characteristic;
import org.openhab.io.homekit.api.PairingRegistry;
import org.openhab.io.homekit.api.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomekitBridgeHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(HomekitBridgeHandler.class);

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections
            .singleton(HomekitBindingConstants.THING_TYPE_BRIDGE);

    private static final String STATE_ADDED = "added";
    private static final String STATE_REMOVED = "removed";

    private @Nullable HomekitAccessoryConfiguration config;
    private HomekitClient homekitClient;
    private final PairingRegistry pairingRegistry;
    private boolean isMultiAccessoryBridge = false;

    private final List<HomekitStatusListener> homekitStatusListeners = new CopyOnWriteArrayList<>();
    Collection<Accessory> lastSearch;

    public HomekitBridgeHandler(Bridge bridge, PairingRegistry pairingRegistry) {
        super(bridge);
        this.pairingRegistry = pairingRegistry;
    }

    @Override
    public void handleCommand(@NonNull ChannelUID channelUID, @NonNull Command command) {
    }

    @Override
    public void initialize() {
        // logger.debug("Start initializing!");
        config = getConfigAs(HomekitAccessoryConfiguration.class);

        Map<String, String> props = getThing().getProperties();

        String category = props.get("ci");
        if (category.equals("2")) {
            isMultiAccessoryBridge = true;
        }

        lastSearch = new HashSet<Accessory>();

        // TODO: Initialize the handler.
        // The framework requires you to return from this method quickly. Also, before leaving this method a thing
        // status from one of ONLINE, OFFLINE or UNKNOWN must be set. This might already be the real thing status in
        // case you can decide it directly.
        // In case you can not decide the thing status directly (e.g. for long running connection handshake using WAN
        // access or similar) you should set status UNKNOWN here and then decide the real status asynchronously in the
        // background.

        // set the thing status to UNKNOWN temporarily and let the background task decide for the real status.
        // the framework is then able to reuse the resources from the thing handler initialization.
        // we set this upfront to reliably check status updates in unit tests.
        updateStatus(ThingStatus.UNKNOWN);

        // Example for background initialization:
        scheduler.execute(() -> {

            try {

                byte[] clientPairingId = null;
                try {
                    clientPairingId = Base64.getDecoder()
                            .decode(props.get(HomekitAccessoryConfiguration.CLIENT_PAIRING_ID));
                } catch (Exception e) {
                }

                byte[] clientLongtermSecretKey = null;
                try {
                    clientLongtermSecretKey = Base64.getDecoder()
                            .decode(props.get(HomekitAccessoryConfiguration.CLIENT_LTSK));
                } catch (Exception e) {
                }

                if (clientPairingId != null && clientLongtermSecretKey != null) {
                    try {
                        homekitClient = new HomekitClient(
                                InetAddress.getByName(props.get(HomekitAccessoryConfiguration.HOST_ADDRESS)),
                                Integer.parseInt(props.get(HomekitAccessoryConfiguration.PORT)), clientPairingId,
                                clientLongtermSecretKey, pairingRegistry);
                    } catch (UnknownHostException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                } else {

                    try {
                        homekitClient = new HomekitClient(
                                InetAddress.getByName(props.get(HomekitAccessoryConfiguration.HOST_ADDRESS)),
                                Integer.parseInt(props.get(HomekitAccessoryConfiguration.PORT)), pairingRegistry);

                        Map<String, String> properties = editProperties();
                        properties.put(HomekitAccessoryConfiguration.CLIENT_PAIRING_ID,
                                Base64.getEncoder().encodeToString(homekitClient.getPairingId()));
                        properties.put(HomekitAccessoryConfiguration.CLIENT_LTSK,
                                Base64.getEncoder().encodeToString(homekitClient.getLongTermSecretKey()));
                        this.updateProperties(properties);

                    } catch (UnknownHostException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }

                if (homekitClient != null) {
                    String setupCode = props.get(HomekitAccessoryConfiguration.SETUP_CODE);

                    if (setupCode != null && !homekitClient.isPaired()) {
                        pair(setupCode);
                    }

                    if (homekitClient.isPaired() && !homekitClient.isPairVerified()) {
                        if (!homekitClient.pairVerify()) {
                            logger.warn("Verification of the pairing for {} failed", getThing().getUID());
                            homekitClient.pairRemove();
                        }
                    }
                }

                if (homekitClient != null && homekitClient.isPairVerified()) {
                    updateStatus(ThingStatus.ONLINE);
                } else {
                    updateStatus(ThingStatus.OFFLINE);
                }

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });

        // logger.debug("Finished initializing!");

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");

    }

    public void pair(String setupCode) {
        if (homekitClient != null) {

            Map<String, String> properties = editProperties();
            // TODO use PairingUID instead
            properties.put(HomekitAccessoryConfiguration.SETUP_CODE, setupCode);
            this.updateProperties(properties);

            homekitClient.setSetupCode(setupCode);
            try {
                if (homekitClient.isPaired()) {
                    homekitClient.pairRemove();
                }

                homekitClient.pairSetup();

                if (homekitClient.isPaired()) {
                    properties = editProperties();
                    // TODO use PairingUID instead
                    properties.put(HomekitAccessoryConfiguration.CLIENT_PAIRING_ID,
                            Base64.getEncoder().encodeToString(homekitClient.getPairingId()));
                    properties.put(HomekitAccessoryConfiguration.CLIENT_LTSK,
                            Base64.getEncoder().encodeToString(homekitClient.getLongTermSecretKey()));
                    this.updateProperties(properties);

                    if (!homekitClient.pairVerify()) {
                        logger.warn("Verification of the pairing of the Homekit Accessory for {} failed",
                                getThing().getUID());
                        homekitClient.pairRemove();
                    }
                } else {
                    logger.warn("Unable to pair the Homekit Accessory for {}", getThing().getUID());
                }

                if (homekitClient.isPairVerified()) {
                    updateStatus(ThingStatus.ONLINE);
                } else {
                    updateStatus(ThingStatus.OFFLINE);
                }

            } catch (IOException | HomekitException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void populateThing() {
        Collection<Accessory> accessories = homekitClient.getAccessories();

        for (Accessory accessory : accessories) {
            for (Service service : accessory.getServices()) {
                for (Characteristic characteristic : service.getCharacteristics()) {
                    logger.info("Verifying A/S/C {}/{}/{} of type {} ", accessory.getId(), service.getId(),
                            characteristic.getId(), characteristic.getClass().getSimpleName());
                    String tempUID = accessory.getId() + ":" + service.getId() + ":" + characteristic.getId();

                    Channel associatedChannel = null;
                    for (Channel aChannel : getThing().getChannels()) {
                        String assoaciatedCharacteristicUID = aChannel.getProperties().get("homekit.characteristic");

                        if (assoaciatedCharacteristicUID != null && assoaciatedCharacteristicUID.equals(tempUID)) {
                            associatedChannel = aChannel;
                            break;
                        }
                    }

                    if (associatedChannel == null) {

                        // Define channelUID

                        // Define accepted Item type

                        ThingBuilder thingBuilder = editThing();
                        Channel channel = ChannelBuilder.create(new ChannelUID("bindingId:type:thingId:1"), "String")
                                .build();
                        thingBuilder.withChannel(channel);
                        updateThing(thingBuilder.build());
                    } else {
                        logger.info("A/S/C {}/{}/{} is already associated with Channel {}", accessory.getId(),
                                service.getId(), characteristic.getId(), associatedChannel.getUID());
                    }
                }
            }
        }
    }

    public void startSearch() {
        Collection<Accessory> accessories = homekitClient.getAccessories();

        for (Accessory accessory : accessories) {
            boolean doesExist = false;
            for (Accessory existingAccessory : lastSearch) {
                if (accessory.getId() == existingAccessory.getId()) {
                    doesExist = true;
                    break;
                }
            }
            if (!doesExist) {
                notifyHomekitStatusListeners(accessory, STATE_ADDED);
            }
        }

        for (Accessory existingAccessory : lastSearch) {
            boolean isRemoved = true;
            for (Accessory accessory : accessories) {
                if (accessory.getId() == existingAccessory.getId()) {
                    isRemoved = false;
                    break;
                }
            }
            if (!isRemoved) {
                notifyHomekitStatusListeners(existingAccessory, STATE_REMOVED);
            }
        }

        lastSearch = accessories;
    }

    public void registerHomekitStatusListener(HomekitStatusListener homekitListener) {
        homekitStatusListeners.add(homekitListener);
    }

    public void unregisterHomekitStatusListener(HomekitStatusListener homekitListener) {
        homekitStatusListeners.remove(homekitListener);
    }

    private void notifyHomekitStatusListeners(final Accessory accessory, final String type) {
        if (homekitStatusListeners.isEmpty()) {
            logger.debug("No Homekit status listeners to notify of change for Accessory '{}'", accessory.getId());
            return;
        }

        for (HomekitStatusListener homekitStatusListener : homekitStatusListeners) {
            try {
                switch (type) {
                    case STATE_ADDED:
                        logger.debug("Sending accessoryAdded for Accessory '{}'", accessory.getId());
                        homekitStatusListener.onAccessoryAdded(getThing(), accessory);
                        break;
                    case STATE_REMOVED:
                        logger.debug("Sending accessoryRemoved for Accessory '{}'", accessory.getId());
                        homekitStatusListener.onAccessoryRemoved(getThing(), accessory);
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "Could not notify Homekit status listeners for unknown event type " + type);
                }
            } catch (Exception e) {
                logger.error("An exception occurred while calling the Homekit status listeners", e);
            }
        }
    }
}
