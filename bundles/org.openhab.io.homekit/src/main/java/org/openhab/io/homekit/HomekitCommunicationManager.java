package org.openhab.io.homekit;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.json.JsonObject;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.ItemStateConverter;
import org.openhab.core.items.events.ItemCommandEvent;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.items.events.ItemStateEvent;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.api.AccessoryServerRegistry;
import org.openhab.io.homekit.api.Characteristic;
import org.openhab.io.homekit.api.Notification;
import org.openhab.io.homekit.api.NotificationRegistry;
import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.internal.characteristic.CharacteristicUID;
import org.openhab.io.homekit.internal.notification.NotificationUID;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = { EventSubscriber.class, HomekitCommunicationManager.class })
public class HomekitCommunicationManager implements EventSubscriber {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitCommunicationManager.class);

    private static final Set<String> SUBSCRIBED_EVENT_TYPES = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList(ItemStateEvent.TYPE, ItemCommandEvent.TYPE)));

    private final ThingRegistry thingRegistry;
    private final ItemRegistry itemRegistry;
    private final ItemChannelLinkRegistry itemChannelLinkRegistry;
    private final ItemStateConverter itemStateConverter;
    private final EventPublisher eventPublisher;
    private final AccessoryServerRegistry accessoryServerRegistry;
    private final NotificationRegistry notificationRegistry;

    @Activate
    public HomekitCommunicationManager(@Reference ThingRegistry thingRegistry, @Reference ItemRegistry itemRegistry,
            @Reference ItemChannelLinkRegistry itemChannelLinkRegistry,
            @Reference ItemStateConverter itemStateConverter, @Reference EventPublisher eventPublisher,
            @Reference AccessoryServerRegistry accessoryServerRegistry,
            @Reference NotificationRegistry notificationRegistry) {
        this.thingRegistry = thingRegistry;
        this.itemRegistry = itemRegistry;
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
        this.itemStateConverter = itemStateConverter;
        this.eventPublisher = eventPublisher;
        this.accessoryServerRegistry = accessoryServerRegistry;
        this.notificationRegistry = notificationRegistry;
    }

    public State getValue(ChannelUID channelUID) {
        State theState = UnDefType.UNDEF;
        if (channelUID != null) {
            Channel channel = thingRegistry.getChannel(channelUID);

            if (channel != null) {
                Set<Item> items = itemChannelLinkRegistry.getLinkedItems(channel.getUID());

                for (Item item : items) {
                    State state = item.getState();
                    if (state != UnDefType.NULL && state != UnDefType.UNDEF) {
                        theState = state;
                        break;
                    }
                }
            }
        }

        return theState;
    }

    public void setValue(ChannelUID channelUID, State state) {
        itemChannelLinkRegistry.getLinks(channelUID).forEach(link -> {
            final Item item = itemRegistry.get(link.getItemName());
            if (item != null) {
                State acceptedState = itemStateConverter.convertToAcceptedState(state, item);
                logger.debug("Publishing {} to channel {}", state, channelUID);
                eventPublisher.post(ItemEventFactory.createStateEvent(link.getItemName(), acceptedState,
                        HomekitCommunicationManager.class.getCanonicalName()));
            }
        });
    }

    public void setValue(CharacteristicUID characteristicUID, JsonObject jsonObject) {
        Notification notification = notificationRegistry.get(new NotificationUID(characteristicUID.toString()));

        logger.debug("SetValue {} to {} ", jsonObject.toString(),
                new NotificationUID(characteristicUID.toString()).toString());

        if (notification != null) {
            notification.publish(jsonObject);
        }
    }

    public Thing getThing(ThingUID thingUID) {
        return thingRegistry.get(thingUID);
    }

    @Override
    public @NonNull Set<@NonNull String> getSubscribedEventTypes() {
        return SUBSCRIBED_EVENT_TYPES;
    }

    @Override
    public @Nullable EventFilter getEventFilter() {
        return null;
    }

    @Override
    public void receive(@NonNull Event event) {
        if (event instanceof ItemStateEvent) {
            receiveUpdate((ItemStateEvent) event);
        } else if (event instanceof ItemCommandEvent) {
            receiveCommand((ItemCommandEvent) event);
        }
    }

    private void receiveUpdate(ItemStateEvent updateEvent) {
        final String itemName = updateEvent.getItemName();
        final State newState = updateEvent.getItemState();
        final String source = updateEvent.getSource();

        logger.debug("receiveUpdate {} {} {}", itemName, newState, source);
        handleEvent(itemName, newState, source);
    }

    private void receiveCommand(ItemCommandEvent commandEvent) {
        final String itemName = commandEvent.getItemName();
        final Command command = commandEvent.getItemCommand();
        final String source = commandEvent.getSource();

        logger.debug("receiveCommand {} {} {}", itemName, command, source);
    }

    private void handleEvent(String itemName, State state, String source) {

        final Item item = itemRegistry.get(itemName);
        if (item == null) {
            logger.debug("Received an event for item {} which does not exist", itemName);
            return;
        }

        if (source == HomekitCommunicationManager.class.getCanonicalName()) {
            logger.debug("Received an event for item {} which originated from homekit", itemName);
            return;
        }

        itemChannelLinkRegistry.getLinks(itemName).stream().forEach(link -> {
            ChannelUID channelUID = link.getLinkedUID();
            Thing thing = getThing(channelUID.getThingUID());
            if (thing != null) {
                Channel channel = thing.getChannel(channelUID.getId());
                if (channel != null) {
                    for (AccessoryServer server : accessoryServerRegistry.getAll()) {
                        for (Accessory accessory : server.getAccessories()) {
                            for (Service service : accessory.getServices()) {
                                for (Characteristic<?> characteristic : service.getCharacteristics()) {
                                    if (channelUID.equals(characteristic.getChannelUID())) {
                                        setValue(characteristic.getUID(), characteristic.toEventJson(state));
                                    }
                                }
                            }
                        }
                    }
                } else {
                    logger.debug("Received  event '{}' for non-existing channel '{}', not forwarding it to homekit",
                            state, channelUID);
                }
            } else {
                logger.debug("Received  event '{}' for non-existing thing '{}', not forwarding it to homekit", state,
                        channelUID.getThingUID());
            }
        });
    }
}
