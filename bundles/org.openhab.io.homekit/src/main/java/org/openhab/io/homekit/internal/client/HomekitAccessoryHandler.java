/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.io.homekit.internal.client;

import static org.openhab.io.homekit.internal.client.HomekitBindingConstants.CHANNEL_1;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.io.homekit.api.PairingRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HomekitAccessoryHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public class HomekitAccessoryHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(HomekitAccessoryHandler.class);

    private @Nullable HomekitAccessoryConfiguration config;
    private @Nullable HomekitClient homekitClient;
    private final PairingRegistry pairingRegistry;

    public HomekitAccessoryHandler(Thing thing, PairingRegistry pairingRegistry) {
        super(thing);
        this.pairingRegistry = pairingRegistry;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (CHANNEL_1.equals(channelUID.getId())) {
            if (command instanceof RefreshType) {
                // TODO: handle data refresh
            }

            // TODO: handle command

            // Note: if communication with thing fails for some reason,
            // indicate that by setting the status with detail information:
            // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
            // "Could not control device at IP address x.x.x.x");
        }
    }

    @Override
    public void initialize() {
        // logger.debug("Start initializing!");
        config = getConfigAs(HomekitAccessoryConfiguration.class);

        Map<String, String> props = getThing().getProperties();

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
                    clientPairingId = props.get(HomekitAccessoryConfiguration.CLIENT_PAIRING_ID)
                            .getBytes(StandardCharsets.UTF_8);
                } catch (Exception e) {
                }

                byte[] clientLongtermSecretKey = null;
                try {
                    clientLongtermSecretKey = props.get(HomekitAccessoryConfiguration.CLIENT_LTSK)
                            .getBytes(StandardCharsets.UTF_8);
                } catch (Exception e) {
                }

                byte[] accessoryPairingId = null;
                try {
                    accessoryPairingId = props.get(HomekitAccessoryConfiguration.ACCESSORY_PAIRING_ID)
                            .getBytes(StandardCharsets.UTF_8);
                } catch (Exception e) {
                }

                if (clientPairingId != null && accessoryPairingId != null && accessoryPairingId != null) {

                    try {
                        homekitClient = new HomekitClient(
                                InetAddress.getByName(props.get(HomekitAccessoryConfiguration.HOST_ADDRESS)),
                                Integer.parseInt(props.get(HomekitAccessoryConfiguration.PORT)), clientPairingId,
                                clientLongtermSecretKey, "572-95-036", pairingRegistry);
                    } catch (UnknownHostException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }

                    homekitClient.PairVerify();

                } else {

                    try {
                        homekitClient = new HomekitClient(
                                InetAddress.getByName(props.get(HomekitAccessoryConfiguration.HOST_ADDRESS)),
                                Integer.parseInt(props.get(HomekitAccessoryConfiguration.PORT)), "572-95-036",
                                pairingRegistry);
                    } catch (UnknownHostException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }

                    homekitClient.PairSetup();

                    logger.debug("PairSetup done");
                    logger.debug("Check is Paired {}", homekitClient.isPaired());

                    if (homekitClient.isPaired()) {
                        Map<String, String> properties = editProperties();
                        properties.put(HomekitAccessoryConfiguration.CLIENT_PAIRING_ID,
                                new String(homekitClient.getPairingId(), StandardCharsets.UTF_8));
                        properties.put(HomekitAccessoryConfiguration.CLIENT_LTSK,
                                new String(homekitClient.getLongTermSecretKey(), StandardCharsets.UTF_8));
                        properties.put(HomekitAccessoryConfiguration.ACCESSORY_PAIRING_ID,
                                new String(homekitClient.getAccessoryPairingId(), StandardCharsets.UTF_8));
                        this.updateProperties(properties);

                        homekitClient.PairVerify();
                    }
                }

                if (homekitClient.isPairVerified()) {
                    updateStatus(ThingStatus.ONLINE);
                } else {
                    updateStatus(ThingStatus.OFFLINE);
                }

            } catch (IOException e) {
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

        // try {
        // homekitClient.PairSetup();
        // } catch (IOException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }

    }

}
