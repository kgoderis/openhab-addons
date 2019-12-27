package org.openhab.io.homekit;

import java.util.Set;

import javax.json.JsonObject;

import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.ItemStateConverter;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.openhab.io.homekit.api.Notification;
import org.openhab.io.homekit.api.NotificationRegistry;
import org.openhab.io.homekit.internal.characteristic.CharacteristicUID;
import org.openhab.io.homekit.internal.notification.NotificationUID;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = { HomekitCommunicationManager.class })
public class HomekitCommunicationManager {

    @Reference
    private ThingRegistry thingRegistry;
    @Reference
    private ItemChannelLinkRegistry linkRegistry;
    @Reference
    private ItemStateConverter itemStateConverter;
    @Reference
    private EventPublisher eventPublisher;
    @Reference
    private ItemRegistry itemRegistry;
    @Reference
    private NotificationRegistry notificationRegistry;

    public State getValue(ChannelUID channelUID) {
        State theState = UnDefType.UNDEF;
        Channel channel = thingRegistry.getChannel(channelUID);

        if (channel != null) {
            Set<Item> items = linkRegistry.getLinkedItems(channel.getUID());

            for (Item item : items) {
                State state = item.getState();
                if (state != UnDefType.NULL && state != UnDefType.UNDEF) {
                    theState = state;
                    break;
                }
            }
        }

        return theState;
    }

    public void setValue(ChannelUID channelUID, State state) {
        linkRegistry.getLinks(channelUID).forEach(link -> {
            final Item item = itemRegistry.get(link.getItemName());
            if (item != null) {
                State acceptedState = itemStateConverter.convertToAcceptedState(state, item);
                eventPublisher.post(ItemEventFactory.createStateEvent(link.getItemName(), acceptedState,
                        link.getLinkedUID().toString()));
            }
        });
    }

    public void setValue(CharacteristicUID characteristicUID, JsonObject jsonObject) {
        Notification notification = notificationRegistry.get(new NotificationUID(characteristicUID.toString()));

        if (notification != null) {
            notification.publish(jsonObject);
        }
    }

    public Thing getThing(ThingUID thingUID) {
        return thingRegistry.get(thingUID);
    }

}
