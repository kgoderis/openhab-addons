package org.openhab.io.homekit.internal.handler;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.api.PairingRegistry;
import org.openhab.io.homekit.internal.client.HomekitAccessoryConfiguration;
import org.openhab.io.homekit.internal.client.HomekitAccessoryProtocolParticipant;
import org.openhab.io.homekit.internal.client.HomekitBindingConstants;
import org.openhab.io.homekit.internal.client.RemoteAccessoryServer;
import org.openhab.io.homekit.internal.client.HomekitException;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StandAloneHomekitAccessoryHandler extends AbstractHomekitAccessoryHandler
        implements HomekitAccessoryProtocolParticipant {

    private final Logger logger = LoggerFactory.getLogger(StandAloneHomekitAccessoryHandler.class);

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections
            .singleton(HomekitBindingConstants.THING_TYPE_STANDALONE_ACCESSORY);

    private @Nullable HomekitAccessoryConfiguration config;
    private RemoteAccessoryServer homekitClient;
    private final PairingRegistry pairingRegistry;
    // private Collection<HomekitFactory> homekitFactories = new CopyOnWriteArrayList<>();

    public StandAloneHomekitAccessoryHandler(Thing thing, PairingRegistry pairingRegistry, BundleContext context) {
        super(thing, context);
        this.pairingRegistry = pairingRegistry;
    }

    @Override
    public void updateConfigurationNumber(int configurationNumber) {
        Configuration config = editConfiguration();
        config.put(HomekitAccessoryConfiguration.CONFIGURATION_NUMBER, configurationNumber);
        updateConfiguration(config);
    }

    @Override
    public int getConfigurationNumber() {
        return Integer.parseInt(
                (String) getThing().getConfiguration().get(HomekitAccessoryConfiguration.CONFIGURATION_NUMBER));
    }

    @Override
    public String getAccessoryPairingId() {
        return new String(Base64.getDecoder().decode(
                (String) getThing().getConfiguration().get(HomekitAccessoryConfiguration.ACCESSORY_PAIRING_ID)));
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

                if (homekitClient != null) {
                    homekitClient.dispose();
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

                    homekitClient = new RemoteAccessoryServer(InetAddress.getByName(config.host), config.port, clientPairingId,
                            clientLongtermSecretKey, accessoryPairingId, pairingRegistry);

                } else {
                    homekitClient = new RemoteAccessoryServer(InetAddress.getByName(config.host), config.port, pairingRegistry);

                    logger.info("'{}' : Creating a Homekit client using a newly generated Id '{}'", getThing().getUID(),
                            new String(homekitClient.getPairingId()));

                    Configuration config = editConfiguration();
                    config.put(HomekitAccessoryConfiguration.CLIENT_PAIRING_ID,
                            Base64.getEncoder().encodeToString(homekitClient.getPairingId()));
                    config.put(HomekitAccessoryConfiguration.CLIENT_LTSK,
                            Base64.getEncoder().encodeToString(homekitClient.getSecretKey()));
                    updateConfiguration(config);
                }

                if (homekitClient != null) {
                    if (config.setupCode != null) {
                        if (!homekitClient.isPaired()) {
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

                    if (homekitClient.isPaired()) {
                        pairVerify();
                    }

                    if (homekitClient.isPairVerified()) {
                        configureThing();
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
        if (homekitClient != null) {
            homekitClient.dispose();
        }

        homekitFactoryRegistryServiceTracker.close();

        super.dispose();
    }

    @Override
    public void pair(String setupCode) {
        if (homekitClient != null) {
            logger.info("'{}' : Pairing the Homekit Accessory using Setup Code {}", getThing().getUID(), setupCode);

            Configuration config = editConfiguration();
            config.put(HomekitAccessoryConfiguration.SETUP_CODE, setupCode);
            updateConfiguration(config);

            homekitClient.setSetupCode(setupCode);

            if (homekitClient.isPaired()) {
                logger.info("'{}' : Removing an existing pairing with the Homekit Accessory", getThing().getUID());
                try {
                    homekitClient.pairRemove();
                } catch (HomekitException | IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                updateStatus(ThingStatus.OFFLINE);
            }

            logger.info("'{}' : Setting up a new pairing with the Homekit Accessory", getThing().getUID());
            try {
                homekitClient.pairSetup();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (homekitClient.isPaired()) {
                config = editConfiguration();
                config.put(HomekitAccessoryConfiguration.CLIENT_PAIRING_ID,
                        Base64.getEncoder().encodeToString(homekitClient.getPairingId()));
                config.put(HomekitAccessoryConfiguration.CLIENT_LTSK,
                        Base64.getEncoder().encodeToString(homekitClient.getSecretKey()));
                config.put(HomekitAccessoryConfiguration.ACCESSORY_PAIRING_ID,
                        Base64.getEncoder().encodeToString(homekitClient.getDestinationPairingId()));
                updateConfiguration(config);
            } else {
                updateStatus(ThingStatus.OFFLINE);
                logger.warn("'{}' : Pairing with the Homekit Accessory failed", getThing().getUID());
            }

        }
    }

    @Override
    public void pairVerify() {
        if (homekitClient != null) {
            if (homekitClient.isPaired() && !homekitClient.isPairVerified()) {
                logger.info("'{}' : Verifying the Homekit Accessory pairing", getThing().getUID());

                homekitClient.pairVerify();

                if (homekitClient.isPairVerified()) {
                    updateStatus(ThingStatus.ONLINE);
                    logger.info("'{}' : Verification of the Homekit Accessory pairing is successfull",
                            getThing().getUID());
                } else {
                    logger.warn("'{}' : Verification of the Homekit Accessory pairing failed. Removing the pairing",
                            getThing().getUID());
                    try {
                        homekitClient.pairRemove();
                    } catch (HomekitException | IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    updateStatus(ThingStatus.OFFLINE);
                }
            } else {
                logger.info(
                        "'{}' : Verification of the Homekit Accessory pairing failed because it is {} paired and/or the pairing was {} verified before",
                        getThing().getUID(), homekitClient.isPaired() ? "already" : "not",
                        homekitClient.isPairVerified() ? "already" : "not");
                if (!homekitClient.isPaired()) {
                    updateStatus(ThingStatus.OFFLINE);
                }
            }
        }
    }

    public void configureThing() {
        Collection<Accessory> accessories = homekitClient.getAccessories();

        if (accessories.size() > 1) {
            logger.warn("'{}' : A Standalone Homekit Accessory should not expose more than 1 Accessory");
        }

        if (accessories.size() > 0) {
            logger.info("'{}' : Configuring the Thing using Accessory {}",
                    ((Accessory) accessories.toArray()[0]).getId());
            configureThing((Accessory) accessories.toArray()[0]);
        }
    }

}
