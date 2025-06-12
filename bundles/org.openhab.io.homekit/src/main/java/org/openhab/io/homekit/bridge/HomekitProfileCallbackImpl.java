/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

package org.openhab.io.homekit.bridge;

import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemStateConverter;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.TimeSeries;

/**
 * A custom implementation of the ProfileCallback interface for HomeKit binding.
 * This replaces the usage of the internal ProfileCallbackImpl class.
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public class HomekitProfileCallbackImpl implements ProfileCallback {
    private final EventPublisher eventPublisher;
    private final ItemStateConverter itemStateConverter;
    private final ItemChannelLink link;
    private final Function<ThingUID, @Nullable Thing> thingProvider;
    private final Function<String, Item> itemProvider;
    private final TriFunction<Command, @Nullable Channel, @Nullable Item, Command> commandMapper;

    /**
     * Creates a new ProfileCallbackImpl
     *
     * @param eventPublisher the event publisher
     * @param safeCaller the safe caller
     * @param itemStateConverter the item state converter
     * @param link the link
     * @param thingProvider function that provides a Thing for a given ThingUID
     * @param itemProvider function that provides an Item for a given item name
     * @param commandMapper function to map commands to accepted command types
     */
    public HomekitProfileCallbackImpl(EventPublisher eventPublisher, ItemStateConverter itemStateConverter,
            ItemChannelLink link, Function<ThingUID, @Nullable Thing> thingProvider,
            Function<String, Item> itemProvider,
            TriFunction<Command, @Nullable Channel, @Nullable Item, Command> commandMapper) {
        this.eventPublisher = eventPublisher;
        this.itemStateConverter = itemStateConverter;
        this.link = link;
        this.thingProvider = thingProvider;
        this.itemProvider = itemProvider;
        this.commandMapper = commandMapper;
    }

    @Override
    public void handleCommand(Command command) {
        String itemName = link.getItemName();
        Item item = itemProvider.apply(itemName);
        ChannelUID channelUID = link.getLinkedUID();
        Thing thing = thingProvider.apply(channelUID.getThingUID());
        if (thing != null) {
            Channel channel = thing.getChannel(channelUID.getId());
            if (channel != null) {
                Command mappedCommand = commandMapper.apply(command, channel, item);
                if (mappedCommand != null) {
                    eventPublisher.post(ItemEventFactory.createCommandEvent(itemName, mappedCommand));
                }
            }
        }
    }

    @Override
    public void sendCommand(Command command) {
        String itemName = link.getItemName();
        eventPublisher.post(ItemEventFactory.createCommandEvent(itemName, command));
    }

    @Override
    public void sendUpdate(State state) {
        String itemName = link.getItemName();
        Item item = itemProvider.apply(itemName);
        State convertedState = itemStateConverter.convertToAcceptedState(state, item);
        eventPublisher.post(ItemEventFactory.createStateEvent(itemName, convertedState));
    }

    @Override
    public void sendTimeSeries(TimeSeries timeSeries) {
        // The current version of ItemEventFactory doesn't have a createTimeSeriesEvent method
        // that accepts TimeSeries objects directly. This is a no-op for now.
        // No action needed as HomeKit doesn't use time series data.
    }

    @Override
    public ItemChannelLink getItemChannelLink() {
        return link;
    }

    /**
     * A functional interface for operations that accept three input parameters and return a result.
     *
     * @param <T> the type of the first parameter
     * @param <U> the type of the second parameter
     * @param <V> the type of the third parameter
     * @param <R> the type of the result
     */
    @FunctionalInterface
    public interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }
}
