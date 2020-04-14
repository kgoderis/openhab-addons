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
package org.openhab.io.homekit.internal.handler;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.api.Characteristic;
import org.openhab.io.homekit.api.HomekitFactory;
import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.internal.client.HomekitAccessoryConfiguration;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HomekitAccessoryHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractHomekitAccessoryHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(AbstractHomekitAccessoryHandler.class);

    protected @Nullable HomekitAccessoryConfiguration config;
    protected @NonNullByDefault({}) ServiceTracker<HomekitFactory, HomekitFactory> homekitFactoryRegistryServiceTracker;
    protected final @NonNullByDefault({}) BundleContext bundleContext;

    public AbstractHomekitAccessoryHandler(Thing thing, BundleContext context) {
        super(thing);
        this.bundleContext = context;

        homekitFactoryRegistryServiceTracker = new ServiceTracker<>(bundleContext, HomekitFactory.class.getName(),
                null);
        homekitFactoryRegistryServiceTracker.open();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        try {

            logger.info("{} : {} : {}", getThing().getUID(), channelUID.getId(), command);

            // find accessory that has this channel

            // get access to homekitclient

            // send command to remote accessory
            // if standalone -> we have hk client
            // if via bridge -> ask bridge to send on our behalf

            if (command instanceof RefreshType) {

            } else {

            }

        } catch (Exception e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    //
    // @Override
    // public void initialize() {
    // // logger.info("Start initializing!");
    // config = getConfigAs(HomekitAccessoryConfiguration.class);
    //
    // Map<String, String> props = getThing().getProperties();
    //
    // // TODO: Initialize the handler.
    // // The framework requires you to return from this method quickly. Also, before leaving this method a thing
    // // status from one of ONLINE, OFFLINE or UNKNOWN must be set. This might already be the real thing status in
    // // case you can decide it directly.
    // // In case you can not decide the thing status directly (e.g. for long running connection handshake using WAN
    // // access or similar) you should set status UNKNOWN here and then decide the real status asynchronously in the
    // // background.
    //
    // // set the thing status to UNKNOWN temporarily and let the background task decide for the real status.
    // // the framework is then able to reuse the resources from the thing handler initialization.
    // // we set this upfront to reliably check status updates in unit tests.
    // updateStatus(ThingStatus.UNKNOWN);
    //
    // // Example for background initialization:
    // scheduler.execute(() -> {
    //
    // });
    //
    // // logger.info("Finished initializing!");
    //
    // // Note: When initialization can NOT be done set the status with more details for further
    // // analysis. See also class ThingStatusDetail for all available status details.
    // // Add a description to give user information to understand why thing does not work as expected. E.g.
    // // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
    // // "Can not access device as username and/or password are invalid");
    //
    // }

    public void configureThing(Accessory accessory) {

        List<Channel> channels = new ArrayList<>(thing.getChannels());

        for (Service service : accessory.getServices()) {
            for (Characteristic characteristic : service.getCharacteristics()) {

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
                    HomekitFactory factory = getHomekitFactory(characteristic.getInstanceType());

                    if (factory != null) {
                        String acceptedType = factory
                                .getCharacteristicAcceptedItemType(characteristic.getInstanceType());
                        ChannelTypeUID channelTypeUID = factory.getChannelTypeUID(characteristic.getInstanceType());

                        Class<? extends Service> serviceClass = factory.getService(service.getInstanceType());
                        if (serviceClass == null) {
                            serviceClass = service.getClass();
                        }

                        Class<? extends Characteristic> characteristicClass = factory
                                .getCharacteristic(characteristic.getInstanceType());
                        if (characteristicClass == null) {
                            characteristicClass = characteristic.getClass();
                        }

                        Channel channel = ChannelBuilder
                                .create(new ChannelUID(getThing().getUID(), String.valueOf(characteristic.getId())),
                                        acceptedType)
                                .withLabel("label").withType(channelTypeUID).build();

                        channels.removeIf(c -> c.getUID().getId().equals(channel.getUID().getId()));
                        channels.add(channel);
                        logger.info(
                                "'{}' : Accessory/Service/Characteristic {}/{}/{} of type {}/{} ({}/{}) is associated with Channel {}",
                                getThing().getUID(), accessory.getId(), service.getId(), characteristic.getId(),
                                serviceClass.getSimpleName(), characteristicClass.getSimpleName(),
                                service.getInstanceType().replaceAll("^0*([0-9a-fA-F]+)-0000-1000-8000-0026BB765291$",
                                        "$1"),
                                characteristic.getInstanceType().replaceAll(
                                        "^0*([0-9a-fA-F]+)-0000-1000-8000-0026BB765291$", "$1"),
                                channel.getUID());
                    } else {
                        logger.warn(
                                "'{}' : Accessory/Service/Characteristic {}/{}/{} of type {}/{} is not supported by any Homekit Factory",
                                getThing().getUID(), accessory.getId(), service.getId(), characteristic.getId(),
                                service.getInstanceType().replaceAll("^0*([0-9a-fA-F]+)-0000-1000-8000-0026BB765291$",
                                        "$1"),
                                characteristic.getInstanceType()
                                        .replaceAll("^0*([0-9a-fA-F]+)-0000-1000-8000-0026BB765291$", "$1"));
                    }
                } else {
                    HomekitFactory factory = getHomekitFactory(characteristic.getInstanceType());
                    if (factory != null) {

                        Class<? extends Service> serviceClass = factory.getService(service.getInstanceType());
                        if (serviceClass == null) {
                            serviceClass = service.getClass();
                        }

                        Class<? extends Characteristic> characteristicClass = factory
                                .getCharacteristic(characteristic.getInstanceType());
                        if (characteristicClass == null) {
                            characteristicClass = characteristic.getClass();
                        }

                        logger.warn(
                                "'{}' : Accessory/Service/Characteristic {}/{}/{} of type {}/{} ({}/{}) is already associated with Channel {}",
                                getThing().getUID(), accessory.getId(), service.getId(), characteristic.getId(),
                                serviceClass.getSimpleName(), characteristicClass.getSimpleName(),
                                service.getInstanceType().replaceAll("^0*([0-9a-fA-F]+)-0000-1000-8000-0026BB765291$",
                                        "$1"),
                                characteristic.getInstanceType()
                                        .replaceAll("^0*([0-9a-fA-F]+)-0000-1000-8000-0026BB765291$", "$1"),
                                associatedChannel.getUID());
                    }
                }
            }
        }

        ThingBuilder thingBuilder = editThing();
        thingBuilder.withChannels(channels);

        updateThing(thingBuilder.build());
    }

    // TODO : Subscribe to notifications
    // TODO : listen to notifications

    @Nullable
    protected HomekitFactory getHomekitFactory(String instanceType) {
        for (HomekitFactory factory : homekitFactoryRegistryServiceTracker.getServices(new HomekitFactory[0])) {
            for (String type : factory.getSupportedCharacteristicTypes()) {
                if (type.equals(instanceType)) {
                    return factory;
                }
            }
        }

        return null;
    }

}
