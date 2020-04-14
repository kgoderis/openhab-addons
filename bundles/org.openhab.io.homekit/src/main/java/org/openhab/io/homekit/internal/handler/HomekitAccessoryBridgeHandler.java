package org.openhab.io.homekit.internal.handler;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.api.Characteristic;
import org.openhab.io.homekit.api.PairingRegistry;
import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.internal.client.HomekitAccessoryConfiguration;
import org.openhab.io.homekit.internal.client.HomekitAccessoryProtocolParticipant;
import org.openhab.io.homekit.internal.client.HomekitBindingConstants;
import org.openhab.io.homekit.internal.client.RemoteAccessoryServer;
import org.openhab.io.homekit.internal.client.HomekitException;
import org.openhab.io.homekit.internal.client.HomekitStatusListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomekitAccessoryBridgeHandler extends BaseBridgeHandler implements HomekitAccessoryProtocolParticipant {

    private final Logger logger = LoggerFactory.getLogger(HomekitAccessoryBridgeHandler.class);

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections
            .singleton(HomekitBindingConstants.THING_TYPE_BRIDGE);

    private static final String STATE_ADDED = "added";
    private static final String STATE_REMOVED = "removed";

    private @Nullable HomekitAccessoryConfiguration config;
    private RemoteAccessoryServer accessoryServer;
    private final PairingRegistry pairingRegistry;

    private final List<HomekitStatusListener> homekitStatusListeners = new CopyOnWriteArrayList<>();
    Collection<Accessory> lastAccessories;

    public HomekitAccessoryBridgeHandler(Bridge bridge, PairingRegistry pairingRegistry) {
        super(bridge);
        this.pairingRegistry = pairingRegistry;
    }

    @Override
    public void handleCommand(@NonNull ChannelUID channelUID, @NonNull Command command) {
    }

    @Override
    public void updateConfigurationNumber(int configurationNumber) {
        Configuration config = editConfiguration();
        config.put(HomekitAccessoryConfiguration.CONFIGURATION_NUMBER, configurationNumber);
        updateConfiguration(config);

        startSearch();
    }

    @Override
    public int getConfigurationNumber() {
        if (getThing().getConfiguration().containsKey(HomekitAccessoryConfiguration.CONFIGURATION_NUMBER)) {
            return Integer.parseInt(
                    (String) getThing().getConfiguration().get(HomekitAccessoryConfiguration.CONFIGURATION_NUMBER));
        } else {
            return 0;
        }
    }

    @Override
    @Nullable
    public String getAccessoryPairingId() {
        if (getThing().getConfiguration().containsKey(HomekitAccessoryConfiguration.ACCESSORY_PAIRING_ID)) {
            return new String(Base64.getDecoder().decode(
                    (String) getThing().getConfiguration().get(HomekitAccessoryConfiguration.ACCESSORY_PAIRING_ID)));
        } else {
            return null;
        }
    }

    @Override
    public void updateDestination(String host, int portNumber) {
        Configuration config = editConfiguration();
        config.put(HomekitAccessoryConfiguration.HOST, host);
        config.put(HomekitAccessoryConfiguration.PORT, portNumber);
        updateConfiguration(config);
        initialize();
    }

    @Override
    public void initialize() {
        // logger.debug("Start initializing!");
        config = getConfigAs(HomekitAccessoryConfiguration.class);
        lastAccessories = new HashSet<Accessory>();

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

                if (accessoryServer != null) {
                    accessoryServer.dispose();
                }

                byte[] clientPairingId = null;
                try {
                    clientPairingId = Base64.getDecoder().decode(config.clientPairingId);
                } catch (Exception e) {
                }

                byte[] clientLongtermSecretKey = null;
                try {
                    clientLongtermSecretKey = Base64.getDecoder().decode(config.clientLongTermSecretKey);
                } catch (Exception e) {
                }

                byte[] accessoryPairingId = null;
                try {
                    accessoryPairingId = Base64.getDecoder().decode(config.accessoryPairingId);
                } catch (Exception e) {
                }

                if (clientPairingId != null && clientLongtermSecretKey != null) {
                    logger.info("'{}' : Creating a Homekit client using an existing Id '{}'", getThing().getUID(),
                            new String(clientPairingId));

                    accessoryServer = new RemoteAccessoryServer(InetAddress.getByName(config.host), config.port, clientPairingId,
                            clientLongtermSecretKey, accessoryPairingId, pairingRegistry);

                } else {
                    accessoryServer = new RemoteAccessoryServer(InetAddress.getByName(config.host), config.port, pairingRegistry);

                    logger.info("'{}' : Creating a Homekit client using a newly generated Id '{}'", getThing().getUID(),
                            new String(accessoryServer.getPairingId()));

                    Configuration config = editConfiguration();
                    config.put(HomekitAccessoryConfiguration.CLIENT_PAIRING_ID,
                            Base64.getEncoder().encodeToString(accessoryServer.getPairingId()));
                    config.put(HomekitAccessoryConfiguration.CLIENT_LTSK,
                            Base64.getEncoder().encodeToString(accessoryServer.getSecretKey()));
                    updateConfiguration(config);
                }

                if (accessoryServer != null) {
                    if (config.setupCode != null) {
                        if (!accessoryServer.isPaired()) {
                            logger.info("'{}' : Pairing the Homekit Accessory using an existing Setup Code",
                                    getThing().getUID());
                            pair(config.setupCode);
                        } else {
                            logger.info("'{}' : The Homekit Accessory is already paired", getThing().getUID());
                        }
                    } else {
                        logger.info("'{}' : The Homekit Accessory can not be paired without a Setup Code",
                                getThing().getUID());
                    }

                    if (accessoryServer.isPaired()) {
                        pairVerify();
                    }

                    if (accessoryServer.isPairVerified()) {
                        startSearch();
                    }
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        });

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");

    }

    @Override
    public void dispose() {
        if (accessoryServer != null) {
            accessoryServer.dispose();
        }

        super.dispose();
    }

    @Override
    public void pair(String setupCode) {
        if (accessoryServer != null) {
            logger.info("'{}' : Pairing the Homekit Accessory using Setup Code {}", getThing().getUID(), setupCode);

            Configuration config = editConfiguration();
            config.put(HomekitAccessoryConfiguration.SETUP_CODE, setupCode);
            updateConfiguration(config);

            accessoryServer.setSetupCode(setupCode);

            if (accessoryServer.isPaired()) {
                logger.info("'{}' : Removing an existing pairing with the Homekit Accessory", getThing().getUID());
                try {
                    accessoryServer.pairRemove();
                } catch (HomekitException | IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                updateStatus(ThingStatus.OFFLINE);
            }

            logger.info("'{}' : Setting up a new pairing with the Homekit Accessory", getThing().getUID());
            try {
                accessoryServer.pairSetup();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (accessoryServer.isPaired()) {
                config = editConfiguration();
                config.put(HomekitAccessoryConfiguration.CLIENT_PAIRING_ID,
                        Base64.getEncoder().encodeToString(accessoryServer.getPairingId()));
                config.put(HomekitAccessoryConfiguration.CLIENT_LTSK,
                        Base64.getEncoder().encodeToString(accessoryServer.getSecretKey()));
                config.put(HomekitAccessoryConfiguration.ACCESSORY_PAIRING_ID,
                        Base64.getEncoder().encodeToString(accessoryServer.getDestinationPairingId()));
                updateConfiguration(config);
            } else {
                updateStatus(ThingStatus.OFFLINE);
                logger.warn("'{}' : Pairing with the Homekit Accessory failed", getThing().getUID());
            }

        }
    }

    @Override
    public void pairVerify() {
        if (accessoryServer != null) {
            if (accessoryServer.isPaired() && !accessoryServer.isPairVerified()) {
                logger.info("'{}' : Verifying the Homekit Accessory pairing", getThing().getUID());

                accessoryServer.pairVerify();

                if (accessoryServer.isPairVerified()) {
                    updateStatus(ThingStatus.ONLINE);
                    logger.info("'{}' : Verification of the Homekit Accessory pairing is successfull",
                            getThing().getUID());
                } else {
                    logger.warn("'{}' : Verification of the Homekit Accessory pairing failed. Removing the pairing",
                            getThing().getUID());
                    try {
                        accessoryServer.pairRemove();
                    } catch (HomekitException | IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    updateStatus(ThingStatus.OFFLINE);
                }
            } else {
                logger.info(
                        "'{}' : Verification of the Homekit Accessory pairing failed because it is {} paired and/or the pairing was {} verified before",
                        getThing().getUID(), accessoryServer.isPaired() ? "already" : "not",
                        accessoryServer.isPairVerified() ? "already" : "not");
                if (!accessoryServer.isPaired()) {
                    updateStatus(ThingStatus.OFFLINE);
                }
            }
        }
    }

    public void startSearch() {
        Collection<Accessory> accessories = accessoryServer.getAccessories();

        for (Accessory accessory : accessories) {
            boolean doesExist = false;
            for (Accessory existingAccessory : lastAccessories) {
                if (accessory.getId() == existingAccessory.getId()) {
                    doesExist = true;
                    break;
                }
            }
            if (!doesExist) {
                notifyHomekitStatusListeners(accessory, STATE_ADDED);
            }
        }

        for (Accessory accessory : accessories) {
            for (Service service : accessory.getServices()) {
                boolean doesExist = false;
                for (Accessory existingAccessory : lastAccessories) {
                    if (accessory.getId() == existingAccessory.getId()) {
                        for (Service existingService : existingAccessory.getServices()) {
                            if (service.getId() == existingService.getId()) {
                                doesExist = true;
                                break;
                            }
                        }
                        if (doesExist) {
                            break;
                        }
                    }
                }
                if (!doesExist) {
                    notifyHomekitStatusListeners(service, STATE_ADDED);
                }
            }
        }

        for (Accessory accessory : accessories) {
            for (Service service : accessory.getServices()) {
                for (Characteristic characteristic : service.getCharacteristics()) {
                    boolean doesExist = false;
                    for (Accessory existingAccessory : lastAccessories) {
                        if (accessory.getId() == existingAccessory.getId()) {
                            for (Service existingService : existingAccessory.getServices()) {
                                for (Characteristic existingCharacteristic : service.getCharacteristics()) {
                                    if (characteristic.getId() == existingCharacteristic.getId()) {
                                        doesExist = true;
                                        break;
                                    }
                                }
                                if (doesExist) {
                                    break;
                                }
                            }
                            if (doesExist) {
                                break;
                            }
                        }
                    }
                    if (!doesExist) {
                        notifyHomekitStatusListeners(characteristic, STATE_ADDED);
                    }
                }
            }
        }

        for (Accessory existingAccessory : lastAccessories) {
            boolean isRemoved = true;
            for (Accessory accessory : accessories) {
                if (accessory.getId() == existingAccessory.getId()) {
                    isRemoved = false;
                    break;
                }
            }
            if (isRemoved) {
                notifyHomekitStatusListeners(existingAccessory, STATE_REMOVED);
            }
        }

        for (Accessory existingAccessory : lastAccessories) {
            for (Service existingService : existingAccessory.getServices()) {
                boolean isRemoved = true;
                for (Accessory accessory : accessories) {
                    if (accessory.getId() == existingAccessory.getId()) {
                        for (Service service : accessory.getServices()) {
                            if (service.getId() == existingService.getId()) {
                                isRemoved = false;
                                break;
                            }
                        }
                        if (isRemoved) {
                            break;
                        }
                    }
                }
                if (isRemoved) {
                    notifyHomekitStatusListeners(existingService, STATE_REMOVED);
                }
            }
        }

        for (Accessory existingAccessory : lastAccessories) {
            for (Service existingService : existingAccessory.getServices()) {
                for (Characteristic existingCharacteristic : existingService.getCharacteristics()) {
                    boolean isRemoved = true;
                    for (Accessory accessory : accessories) {
                        if (accessory.getId() == existingAccessory.getId()) {
                            for (Service service : accessory.getServices()) {
                                for (Characteristic characteristic : service.getCharacteristics()) {
                                    if (characteristic.getId() == existingCharacteristic.getId()) {
                                        isRemoved = false;
                                        break;
                                    }
                                }
                                if (isRemoved) {
                                    break;
                                }
                            }
                            if (isRemoved) {
                                break;
                            }
                        }
                    }
                    if (isRemoved) {
                        notifyHomekitStatusListeners(existingCharacteristic, STATE_REMOVED);
                    }
                }
            }
        }

        lastAccessories = accessories;
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

    private void notifyHomekitStatusListeners(final Service service, final String type) {
        if (homekitStatusListeners.isEmpty()) {
            logger.debug("No Homekit status listeners to notify of change for Service '{}'", service.getId());
            return;
        }

        for (HomekitStatusListener homekitStatusListener : homekitStatusListeners) {
            try {
                switch (type) {
                    case STATE_ADDED:
                        logger.debug("Sending serviceAdded for Service '{}'", service.getId());
                        homekitStatusListener.onServiceAdded(getThing(), service);
                        break;
                    case STATE_REMOVED:
                        logger.debug("Sending serviceRemoved for Service '{}'", service.getId());
                        homekitStatusListener.onServiceRemoved(getThing(), service);
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

    private void notifyHomekitStatusListeners(final Characteristic characteristic, final String type) {
        if (homekitStatusListeners.isEmpty()) {
            logger.debug("No Homekit status listeners to notify of change for Characteristic '{}'",
                    characteristic.getId());
            return;
        }

        for (HomekitStatusListener homekitStatusListener : homekitStatusListeners) {
            try {
                switch (type) {
                    case STATE_ADDED:
                        logger.debug("Sending characteristicAdded for Service '{}'", characteristic.getId());
                        homekitStatusListener.onCharacteristicAdded(getThing(), characteristic);
                        break;
                    case STATE_REMOVED:
                        logger.debug("Sending characteristicRemoved for Service '{}'", characteristic.getId());
                        homekitStatusListener.onCharacteristicRemoved(getThing(), characteristic);
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

    @Nullable
    public Accessory getAccessory(long id) {
        startSearch();
        for (Accessory accessory : lastAccessories) {
            if (accessory.getId() == id) {
                return accessory;
            }
        }

        return null;
    }

    public Collection<Accessory> getAccessories() {
        startSearch();
        return lastAccessories;
    }
}
