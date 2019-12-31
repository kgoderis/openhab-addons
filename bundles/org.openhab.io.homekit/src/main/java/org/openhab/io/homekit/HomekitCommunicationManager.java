package org.openhab.io.homekit;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.measure.Quantity;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.AbstractUID;
import org.openhab.core.common.SafeCaller;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemFactory;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.ItemStateConverter;
import org.openhab.core.items.ItemUtil;
import org.openhab.core.items.events.ItemCommandEvent;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.items.events.ItemStateEvent;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.events.ChannelTriggeredEvent;
import org.openhab.core.thing.events.ThingEventFactory;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.profiles.Profile;
import org.openhab.core.thing.profiles.ProfileAdvisor;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileFactory;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.StateProfile;
import org.openhab.core.thing.profiles.TriggerProfile;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.Type;
import org.openhab.core.types.UnDefType;
import org.openhab.core.types.util.UnitUtils;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.api.AccessoryServerRegistry;
import org.openhab.io.homekit.api.Characteristic;
import org.openhab.io.homekit.api.ManagedCharacteristic;
import org.openhab.io.homekit.api.Notification;
import org.openhab.io.homekit.api.NotificationRegistry;
import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.internal.notification.NotificationUID;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = { EventSubscriber.class, HomekitCommunicationManager.class })
public class HomekitCommunicationManager implements EventSubscriber, RegistryChangeListener<ItemChannelLink> {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitCommunicationManager.class);

    public static final String PARAM_PROFILE = "profile";
    public static final long THINGHANDLER_EVENT_TIMEOUT = TimeUnit.SECONDS.toMillis(30);

    private static final Set<String> SUBSCRIBED_EVENT_TYPES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(ItemStateEvent.TYPE, ItemCommandEvent.TYPE, ChannelTriggeredEvent.TYPE)));

    private final ThingRegistry thingRegistry;
    private final ItemRegistry itemRegistry;
    private final ItemChannelLinkRegistry itemChannelLinkRegistry;
    private final ItemStateConverter itemStateConverter;
    private final EventPublisher eventPublisher;
    private final AccessoryServerRegistry accessoryServerRegistry;
    private final NotificationRegistry notificationRegistry;
    private final SafeCaller safeCaller;
    // TODO : Discuss making SystemProfileFacgtory public
    // private final SystemProfileFactory defaultProfileFactory;
    private final ChannelTypeRegistry channelTypeRegistry;

    // link UID -> profile
    private final Map<String, Profile> profiles = new ConcurrentHashMap<>();
    // factory instance -> link UIDs which the factory has created profiles for
    private final Map<ProfileFactory, Set<String>> profileFactories = new ConcurrentHashMap<>();
    private final Set<ProfileAdvisor> profileAdvisors = new CopyOnWriteArraySet<>();

    private final Map<String, @Nullable List<Class<? extends Command>>> acceptedCommandTypeMap = new ConcurrentHashMap<>();
    private final Map<String, @Nullable List<Class<? extends State>>> acceptedStateTypeMap = new ConcurrentHashMap<>();

    private final Set<ItemFactory> itemFactories = new CopyOnWriteArraySet<>();

    @Activate
    public HomekitCommunicationManager(@Reference ThingRegistry thingRegistry, @Reference ItemRegistry itemRegistry,
            @Reference ItemChannelLinkRegistry itemChannelLinkRegistry,
            @Reference ItemStateConverter itemStateConverter, @Reference EventPublisher eventPublisher,
            @Reference AccessoryServerRegistry accessoryServerRegistry,
            @Reference NotificationRegistry notificationRegistry, @Reference SafeCaller safeCaller,
            @Reference ChannelTypeRegistry channelTypeRegistry) {
        this.thingRegistry = thingRegistry;
        this.itemRegistry = itemRegistry;
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
        this.itemStateConverter = itemStateConverter;
        this.eventPublisher = eventPublisher;
        this.accessoryServerRegistry = accessoryServerRegistry;
        this.notificationRegistry = notificationRegistry;
        this.safeCaller = safeCaller;
        // this.defaultProfileFactory = defaultProfileFactory;
        this.channelTypeRegistry = channelTypeRegistry;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addProfileFactory(ProfileFactory profileFactory) {
        this.profileFactories.put(profileFactory, ConcurrentHashMap.newKeySet());
    }

    protected void removeProfileFactory(ProfileFactory profileFactory) {
        Set<String> links = this.profileFactories.remove(profileFactory);
        synchronized (profiles) {
            links.forEach(link -> {
                profiles.remove(link);
            });
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addProfileAdvisor(ProfileAdvisor profileAdvisor) {
        profileAdvisors.add(profileAdvisor);
    }

    protected void removeProfileAdvisor(ProfileAdvisor profileAdvisor) {
        profileAdvisors.remove(profileAdvisor);
    }

    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
    protected void addItemFactory(ItemFactory itemFactory) {
        itemFactories.add(itemFactory);
        calculateAcceptedTypes();
    }

    protected void removeItemFactory(ItemFactory itemFactory) {
        itemFactories.remove(itemFactory);
        calculateAcceptedTypes();
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return SUBSCRIBED_EVENT_TYPES;
    }

    @Override
    public @Nullable EventFilter getEventFilter() {
        return null;
    }

    @Override
    public void receive(Event event) {
        if (event instanceof ItemStateEvent) {
            receiveUpdate((ItemStateEvent) event);
        } else if (event instanceof ItemCommandEvent) {
            receiveCommand((ItemCommandEvent) event);
        } else if (event instanceof ChannelTriggeredEvent) {
            receiveTrigger((ChannelTriggeredEvent) event);
        }
    }

    public @Nullable Thing getThing(ThingUID thingUID) {
        return thingRegistry.get(thingUID);
    }

    private Profile getProfile(ItemChannelLink link, Item item, @Nullable Thing thing, ProfileCallback callback) {
        synchronized (profiles) {
            Profile profile = profiles.get(link.getUID());
            if (profile != null) {
                return profile;
            }
            ProfileTypeUID profileTypeUID = determineProfileTypeUID(link, item, thing);
            if (profileTypeUID != null) {
                profile = getProfileFromFactories(profileTypeUID, link, callback);
                if (profile != null) {
                    profiles.put(link.getUID(), profile);
                    return profile;
                }
            }
            return new NoOpProfile();
        }
    }

    private @Nullable ProfileTypeUID determineProfileTypeUID(ItemChannelLink link, Item item, @Nullable Thing thing) {
        ProfileTypeUID profileTypeUID = getConfiguredProfileTypeUID(link);
        Channel channel = null;
        if (profileTypeUID == null) {
            if (thing == null) {
                return null;
            }

            channel = thing.getChannel(link.getLinkedUID().getId());
            if (channel == null) {
                return null;
            }

            // ask advisors
            profileTypeUID = getAdvice(link, item, channel);

            // if (profileTypeUID == null) {
            // // ask default advisor
            // logger.trace("No profile advisor found for link {}, falling back to the defaults", link);
            // profileTypeUID = defaultProfileFactory.getSuggestedProfileTypeUID(channel, item.getType());
            // }
        }
        return profileTypeUID;
    }

    private @Nullable ProfileTypeUID getAdvice(ItemChannelLink link, Item item, Channel channel) {
        ProfileTypeUID ret;
        for (ProfileAdvisor advisor : profileAdvisors) {
            ret = advisor.getSuggestedProfileTypeUID(channel, item.getType());
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    private @Nullable ProfileTypeUID getConfiguredProfileTypeUID(ItemChannelLink link) {
        String profileName = (String) link.getConfiguration().get(PARAM_PROFILE);
        if (profileName != null && !profileName.trim().isEmpty()) {
            profileName = normalizeProfileName(profileName);
            return new ProfileTypeUID(profileName);
        }
        return null;
    }

    private String normalizeProfileName(String profileName) {
        if (!profileName.contains(AbstractUID.SEPARATOR)) {
            return ProfileTypeUID.SYSTEM_SCOPE + AbstractUID.SEPARATOR + profileName;
        }
        return profileName;
    }

    private @Nullable Profile getProfileFromFactories(ProfileTypeUID profileTypeUID, ItemChannelLink link,
            ProfileCallback callback) {
        HomekitProfileContextImpl context = new HomekitProfileContextImpl(link.getConfiguration());
        // if (supportsProfileTypeUID(defaultProfileFactory, profileTypeUID)) {
        // logger.trace("using the default ProfileFactory to create profile '{}'", profileTypeUID);
        // return defaultProfileFactory.createProfile(profileTypeUID, callback, context);
        // }
        for (Entry<ProfileFactory, Set<String>> entry : profileFactories.entrySet()) {
            ProfileFactory factory = entry.getKey();
            if (supportsProfileTypeUID(factory, profileTypeUID)) {
                logger.trace("using ProfileFactory '{}' to create profile '{}'", factory, profileTypeUID);
                Profile profile = factory.createProfile(profileTypeUID, callback, context);
                if (profile == null) {
                    logger.error("ProfileFactory '{}' returned 'null' although it claimed it supports item type '{}'",
                            factory, profileTypeUID);
                } else {
                    entry.getValue().add(link.getUID());
                    return profile;
                }
            }
        }
        logger.debug("no ProfileFactory found which supports '{}'", profileTypeUID);
        return null;
    }

    private boolean supportsProfileTypeUID(ProfileFactory profileFactory, ProfileTypeUID profileTypeUID) {
        return profileFactory.getSupportedProfileTypeUIDs().contains(profileTypeUID);
    }

    private void receiveCommand(ItemCommandEvent commandEvent) {
        final String itemName = commandEvent.getItemName();
        final Command command = commandEvent.getItemCommand();
        // final Item item = getItem(itemName);

        // if (item != null) {
        // autoUpdateManager.receiveCommand(commandEvent, item);
        // }

        handleEvent(itemName, command, commandEvent.getSource(), s -> acceptedCommandTypeMap.get(s),
                (profile, thing, convertedCommand) -> {
                    if (profile instanceof StateProfile) {
                        safeCaller.create(((StateProfile) profile), StateProfile.class) //
                                .withAsync() //
                                .withIdentifier(thing) //
                                .withTimeout(THINGHANDLER_EVENT_TIMEOUT) //
                                .build().onCommandFromItem(convertedCommand);
                    }
                });
    }

    private void receiveUpdate(ItemStateEvent updateEvent) {
        final String itemName = updateEvent.getItemName();
        final State newState = updateEvent.getItemState();
        handleEvent(itemName, newState, updateEvent.getSource(), s -> acceptedStateTypeMap.get(s),
                (profile, thing, convertedState) -> {
                    safeCaller.create(profile, Profile.class) //
                            .withAsync() //
                            .withIdentifier(thing) //
                            .withTimeout(THINGHANDLER_EVENT_TIMEOUT) //
                            .build().onStateUpdateFromItem(convertedState);
                });
    }

    @FunctionalInterface
    private static interface ProfileAction<T extends Type> {
        void handle(Profile profile, Thing thing, T type);
    }

    private <T extends Type> void handleEvent(String itemName, T type, @Nullable String source,
            Function<@Nullable String, @Nullable List<Class<? extends T>>> acceptedTypesFunction,
            ProfileAction<T> action) {
        final Item item = getItem(itemName);
        if (item == null) {
            logger.debug("Received an event for item {} which does not exist", itemName);
            return;
        }

        if (source == HomekitCommunicationManager.class.getCanonicalName()) {
            logger.debug("Received an event that originated from homekit", itemName);
            return;
        }

        itemChannelLinkRegistry.getLinks(itemName).stream().forEach(link -> {
            ChannelUID channelUID = link.getLinkedUID();
            Thing thing = getThing(channelUID.getThingUID());
            if (thing != null) {
                Channel channel = thing.getChannel(channelUID.getId());
                if (channel != null) {
                    @Nullable
                    T convertedType = toAcceptedType(type, channel, acceptedTypesFunction, item);
                    if (convertedType != null) {
                        Profile profile = getProfile(link, item, thing, new HomekitProfileCallbackImpl(link,
                                thingUID -> getThing(thingUID), linkedItem -> getItem(linkedItem)));
                        action.handle(profile, thing, convertedType);
                    } else {
                        logger.debug(
                                "Received event '{}' ({}) could not be converted to any type accepted by item '{}' ({})",
                                type, type.getClass().getSimpleName(), itemName, item.getType());
                    }
                } else {
                    logger.debug("Received  event '{}' for non-existing channel '{}', not forwarding it to homekit",
                            type, channelUID);
                }
            } else {
                logger.debug("Received  event '{}' for non-existing thing '{}', not forwarding it to homekit", type,
                        channelUID.getThingUID());
            }
        });

        // itemChannelLinkRegistry.getLinks(itemName).stream().filter(link -> {
        // // make sure the command event is not sent back to its source
        // return !link.getLinkedUID().toString().equals(source);
        // }).forEach(link -> {
        // ChannelUID channelUID = link.getLinkedUID();
        // Thing thing = getThing(channelUID.getThingUID());
        // if (thing != null) {
        // Channel channel = thing.getChannel(channelUID.getId());
        // if (channel != null) {
        // @Nullable
        // T convertedType = toAcceptedType(type, channel, acceptedTypesFunction, item);
        // if (convertedType != null) {
        // if (thing.getHandler() != null) {
        // Profile profile = getProfile(link, item, thing);
        // action.handle(profile, thing, convertedType);
        // }
        // } else {
        // logger.debug(
        // "Received event '{}' ({}) could not be converted to any type accepted by item '{}' ({})",
        // type, type.getClass().getSimpleName(), itemName, item.getType());
        // }
        // } else {
        // logger.debug("Received event '{}' for non-existing channel '{}', not forwarding it to the handler",
        // type, channelUID);
        // }
        // } else {
        // logger.debug("Received event '{}' for non-existing thing '{}', not forwarding it to the handler", type,
        // channelUID.getThingUID());
        // }
        // });
    }

    @SuppressWarnings("unchecked")
    private <T extends Type> @Nullable T toAcceptedType(T originalType, Channel channel,
            Function<@Nullable String, @Nullable List<Class<? extends T>>> acceptedTypesFunction, Item item) {
        String acceptedItemType = channel.getAcceptedItemType();

        // DecimalType command sent to a NumberItem with dimension defined:
        if (originalType instanceof DecimalType && hasDimension(item, acceptedItemType)) {
            @Nullable
            QuantityType<?> quantityType = convertToQuantityType((DecimalType) originalType, item, acceptedItemType);
            if (quantityType != null) {
                return (T) quantityType;
            }
        }

        // The command is sent to an item w/o dimension defined and the channel is legacy (created from a ThingType
        // definition before UoM was introduced to the binding). The dimension information might now be defined on the
        // current ThingType. The binding might expect us to provide a QuantityType so try to convert to the dimension
        // the ChannelType provides.
        // This can be removed once a suitable solution for https://github.com/eclipse/smarthome/issues/2555 (Thing
        // migration) is found.
        if (originalType instanceof DecimalType && !hasDimension(item, acceptedItemType)
                && channelTypeDefinesDimension(channel.getChannelTypeUID())) {
            ChannelType channelType = channelTypeRegistry.getChannelType(channel.getChannelTypeUID());

            String acceptedItemTypeFromChannelType = channelType != null ? channelType.getItemType() : null;
            @Nullable
            QuantityType<?> quantityType = convertToQuantityType((DecimalType) originalType, item,
                    acceptedItemTypeFromChannelType);
            if (quantityType != null) {
                return (T) quantityType;
            }
        }

        if (acceptedItemType == null) {
            return originalType;
        }

        List<Class<? extends T>> acceptedTypes = acceptedTypesFunction.apply(acceptedItemType);
        if (acceptedTypes == null) {
            return originalType;
        }

        if (acceptedTypes.contains(originalType.getClass())) {
            return originalType;
        } else {
            // Look for class hierarchy and convert appropriately
            for (Class<? extends T> typeClass : acceptedTypes) {
                if (!typeClass.isEnum() && typeClass.isAssignableFrom(originalType.getClass()) //
                        && State.class.isAssignableFrom(typeClass) && originalType instanceof State) {
                    T ret = (T) ((State) originalType).as((Class<? extends State>) typeClass);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Converted '{}' ({}) to accepted type '{}' ({}) for channel '{}' ", originalType,
                                originalType.getClass().getSimpleName(), ret, ret.getClass().getName(),
                                channel.getUID());
                    }
                    return ret;
                }
            }
        }
        logger.debug("Received not accepted type '{}' for channel '{}'", originalType.getClass().getSimpleName(),
                channel.getUID());
        return null;
    }

    private boolean channelTypeDefinesDimension(@Nullable ChannelTypeUID channelTypeUID) {
        if (channelTypeUID == null) {
            return false;
        }

        ChannelType channelType = channelTypeRegistry.getChannelType(channelTypeUID);
        return channelType != null && getDimension(channelType.getItemType()) != null;
    }

    private boolean hasDimension(Item item, @Nullable String acceptedItemType) {
        return (item instanceof NumberItem && ((NumberItem) item).getDimension() != null)
                || getDimension(acceptedItemType) != null;
    }

    private @Nullable QuantityType<?> convertToQuantityType(DecimalType originalType, Item item,
            @Nullable String acceptedItemType) {
        NumberItem numberItem = (NumberItem) item;

        // DecimalType command sent via a NumberItem with dimension:
        Class<? extends Quantity<?>> dimension = numberItem.getDimension();

        if (dimension == null) {
            // DecimalType command sent via a plain NumberItem w/o dimension.
            // We try to guess the correct unit from the channel-type's expected item dimension
            // or from the item's state description.
            dimension = getDimension(acceptedItemType);
        }

        if (dimension != null) {
            return numberItem.toQuantityType(originalType, dimension);
        }

        return null;
    }

    private @Nullable Class<? extends Quantity<?>> getDimension(@Nullable String acceptedItemType) {
        if (acceptedItemType == null || acceptedItemType.isEmpty()) {
            return null;
        }
        String itemTypeExtension = ItemUtil.getItemTypeExtension(acceptedItemType);
        if (itemTypeExtension == null) {
            return null;
        }

        return UnitUtils.parseDimension(itemTypeExtension);
    }

    private @Nullable Item getItem(final String itemName) {
        return itemRegistry.get(itemName);
    }

    private void receiveTrigger(ChannelTriggeredEvent channelTriggeredEvent) {
        final ChannelUID channelUID = channelTriggeredEvent.getChannel();
        final String event = channelTriggeredEvent.getEvent();
        final Thing thing = getThing(channelUID.getThingUID());

        handleCall(channelUID, thing, profile -> {
            if (profile instanceof TriggerProfile) {
                ((TriggerProfile) profile).onTriggerFromHandler(event);
            }
        });
    }

    public void stateUpdated(ChannelUID channelUID, State state) {
        final Thing thing = getThing(channelUID.getThingUID());

        handleCall(channelUID, thing, profile -> {
            if (profile instanceof StateProfile) {
                ((StateProfile) profile).onStateUpdateFromHandler(state);
            }
        });
    }

    public void postCommand(ChannelUID channelUID, Command command) {
        final Thing thing = getThing(channelUID.getThingUID());

        handleCall(channelUID, thing, profile -> {
            if (profile instanceof StateProfile) {
                ((StateProfile) profile).onCommandFromHandler(command);
            }
        });
    }

    void handleCall(ChannelUID channelUID, @Nullable Thing thing, Consumer<Profile> action) {
        itemChannelLinkRegistry.getLinks(channelUID).forEach(link -> {
            final Item item = getItem(link.getItemName());
            if (item != null) {
                final Profile profile = getProfile(link, item, thing, new HomekitProfileCallbackImpl(link,
                        thingUID -> getThing(thingUID), linkedItem -> getItem(linkedItem)));
                action.accept(profile);
            }
        });
    }

    public State getState(ChannelUID channelUID) {
        final Thing thing = getThing(channelUID.getThingUID());
        State theState = UnDefType.UNDEF;

        for (ItemChannelLink link : itemChannelLinkRegistry.getLinks(channelUID)) {
            final Item item = getItem(link.getItemName());
            if (item != null) {

                ProfileCallback callback = new GetStateProfileCallbackImpl(link, thingUID -> getThing(thingUID),
                        linkedItem -> getItem(linkedItem));

                final Profile profile = getProfile(link, item, thing, callback);

                if (profile instanceof StateProfile) {
                    ((StateProfile) profile).onStateUpdateFromItem(item.getState());
                    theState = ((GetStateProfileCallbackImpl) callback).getState();
                }
            }
        }

        // if (channelUID != null) {
        // Channel channel = thingRegistry.getChannel(channelUID);
        //
        // if (channel != null) {
        // Set<Item> items = itemChannelLinkRegistry.getLinkedItems(channel.getUID());
        //
        // for (Item item : items) {
        // State state = item.getState();
        // if (state != UnDefType.NULL && state != UnDefType.UNDEF) {
        // theState = state;
        // break;
        // }
        // }
        // }
        // }

        return theState;
    }

    public void channelTriggered(Thing thing, ChannelUID channelUID, String event) {
        eventPublisher.post(ThingEventFactory.createTriggerEvent(event, channelUID));
    }

    private void cleanup(ItemChannelLink link) {
        synchronized (profiles) {
            profiles.remove(link.getUID());
        }
        profileFactories.values().forEach(list -> list.remove(link.getUID()));
    }

    @Override
    public void added(ItemChannelLink element) {
        // nothing to do
    }

    @Override
    public void removed(ItemChannelLink element) {
        cleanup(element);
    }

    @Override
    public void updated(ItemChannelLink oldElement, ItemChannelLink element) {
        cleanup(oldElement);
    }

    private synchronized void calculateAcceptedTypes() {
        acceptedCommandTypeMap.clear();
        acceptedStateTypeMap.clear();
        for (ItemFactory itemFactory : itemFactories) {
            for (String itemTypeName : itemFactory.getSupportedItemTypes()) {
                Item item = itemFactory.createItem(itemTypeName, "tmp");
                if (item != null) {
                    acceptedCommandTypeMap.put(itemTypeName, item.getAcceptedCommandTypes());
                    acceptedStateTypeMap.put(itemTypeName, item.getAcceptedDataTypes());
                } else {
                    logger.error("Item factory {} suggested it can create items of type {} but returned null",
                            itemFactory, itemTypeName);
                }
            }
        }
    }

    private static class NoOpProfile implements Profile {
        @Override
        public @NonNull ProfileTypeUID getProfileTypeUID() {
            return new ProfileTypeUID(ProfileTypeUID.SYSTEM_SCOPE, "noop");
        }

        @Override
        public void onStateUpdateFromItem(@NonNull State state) {
            // no-op
        }
    }

    // TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO

    // private void receiveUpdate(ItemStateEvent updateEvent) {
    // final String itemName = updateEvent.getItemName();
    // final State newState = updateEvent.getItemState();
    // final String source = updateEvent.getSource();
    //
    // logger.debug("receiveUpdate {} {} {}", itemName, newState, source);
    // handleEvent(itemName, newState, source);
    // }
    //
    // private void receiveCommand(ItemCommandEvent commandEvent) {
    // final String itemName = commandEvent.getItemName();
    // final Command command = commandEvent.getItemCommand();
    // final String source = commandEvent.getSource();
    //
    // logger.debug("receiveCommand {} {} {}", itemName, command, source);
    // }

    // private void handleEvent(String itemName, State state, String source) {
    //
    // final Item item = itemRegistry.get(itemName);
    // if (item == null) {
    // logger.debug("Received an event for item {} which does not exist", itemName);
    // return;
    // }
    //
    // if (source == HomekitCommunicationManager.class.getCanonicalName()) {
    // logger.debug("Received an event for item {} which originated from homekit", itemName);
    // return;
    // }
    //
    // itemChannelLinkRegistry.getLinks(itemName).stream().forEach(link -> {
    // ChannelUID channelUID = link.getLinkedUID();
    // Thing thing = getThing(channelUID.getThingUID());
    // if (thing != null) {
    // Channel channel = thing.getChannel(channelUID.getId());
    // if (channel != null) {
    // for (AccessoryServer server : accessoryServerRegistry.getAll()) {
    // for (Accessory accessory : server.getAccessories()) {
    // for (Service service : accessory.getServices()) {
    // for (Characteristic<?> characteristic : service.getCharacteristics()) {
    // if (channelUID.equals(characteristic.getChannelUID())) {
    // handleUpdate(characteristic.getUID(), characteristic.toEventJson(state));
    // }
    // }
    // }
    // }
    // }
    // } else {
    // logger.debug("Received event '{}' for non-existing channel '{}', not forwarding it to homekit",
    // state, channelUID);
    // }
    // } else {
    // logger.debug("Received event '{}' for non-existing thing '{}', not forwarding it to homekit", state,
    // channelUID.getThingUID());
    // }
    // });
    // }

    public class HomekitProfileCallbackImpl implements ProfileCallback {

        private final Logger logger = LoggerFactory.getLogger(HomekitProfileCallbackImpl.class);

        // private final EventPublisher eventPublisher;
        private final ItemChannelLink link;
        private final Function<ThingUID, Thing> thingProvider;
        private final Function<String, Item> itemProvider;
        // private final SafeCaller safeCaller;
        // private final ItemStateConverter itemStateConverter;

        public HomekitProfileCallbackImpl(ItemChannelLink link, Function<ThingUID, Thing> thingProvider,
                Function<String, Item> itemProvider) {
            // this.eventPublisher = eventPublisher;
            // this.safeCaller = safeCaller;
            // this.itemStateConverter = itemStateConverter;
            this.link = link;
            this.thingProvider = thingProvider;
            this.itemProvider = itemProvider;
        }

        @Override
        public void handleCommand(Command command) {
            // Thing thing = thingProvider.apply(link.getLinkedUID().getThingUID());
            // if (thing != null) {
            // final ThingHandler handler = thing.getHandler();
            // if (handler != null) {
            // if (ThingHandlerHelper.isHandlerInitialized(thing)) {
            // logger.debug("Delegating command '{}' for item '{}' to handler for channel '{}'", command,
            // link.getItemName(), link.getLinkedUID());
            // safeCaller.create(handler, ThingHandler.class)
            // .withTimeout(CommunicationManager.THINGHANDLER_EVENT_TIMEOUT).onTimeout(() -> {
            // logger.warn("Handler for thing '{}' takes more than {}ms for handling a command",
            // handler.getThing().getUID(),
            // CommunicationManager.THINGHANDLER_EVENT_TIMEOUT);
            // }).build().handleCommand(link.getLinkedUID(), command);
            // } else {
            // logger.debug("Not delegating command '{}' for item '{}' to handler for channel '{}', "
            // + "because handler is not initialized (thing must be in status UNKNOWN, ONLINE or OFFLINE but was {}).",
            // command, link.getItemName(), link.getLinkedUID(), thing.getStatus());
            // }
            // } else {
            // logger.warn("Cannot delegate command '{}' for item '{}' to handler for channel '{}', "
            // + "because no handler is assigned. Maybe the binding is not installed or not "
            // + "propertly initialized.", command, link.getItemName(), link.getLinkedUID());
            // }
            // } else {
            // logger.warn(
            // "Cannot delegate command '{}' for item '{}' to handler for channel '{}', "
            // + "because no thing with the UID '{}' could be found.",
            // command, link.getItemName(), link.getLinkedUID(), link.getLinkedUID().getThingUID());
            // }
        }

        @Override
        public void handleUpdate(State state) {
            for (AccessoryServer server : accessoryServerRegistry.getAll()) {
                for (Accessory accessory : server.getAccessories()) {
                    for (Service service : accessory.getServices()) {
                        for (Characteristic characteristic : service.getCharacteristics()) {
                            if (link.getLinkedUID()
                                    .equals(((ManagedCharacteristic<?>) characteristic).getChannelUID())) {
                                Notification notification = notificationRegistry.get(new NotificationUID(
                                        ((ManagedCharacteristic<?>) characteristic).getUID().toString()));

                                if (notification != null) {
                                    notification
                                            .publish(((ManagedCharacteristic<?>) characteristic).toEventJson(state));
                                }
                            }
                        }
                    }
                }
            }

            // Thing thing = thingProvider.apply(link.getLinkedUID().getThingUID());
            // if (thing != null) {
            // final ThingHandler handler = thing.getHandler();
            // if (handler != null) {
            // if (ThingHandlerHelper.isHandlerInitialized(thing)) {
            // logger.debug("Delegating update '{}' for item '{}' to handler for channel '{}'", state,
            // link.getItemName(), link.getLinkedUID());
            // safeCaller.create(handler, ThingHandler.class)
            // .withTimeout(CommunicationManager.THINGHANDLER_EVENT_TIMEOUT).onTimeout(() -> {
            // logger.warn("Handler for thing '{}' takes more than {}ms for handling an update",
            // handler.getThing().getUID(),
            // CommunicationManager.THINGHANDLER_EVENT_TIMEOUT);
            // }).build().handleUpdate(link.getLinkedUID(), state);
            // } else {
            // logger.debug("Not delegating update '{}' for item '{}' to handler for channel '{}', "
            // + "because handler is not initialized (thing must be in status UNKNOWN, ONLINE or OFFLINE but was {}).",
            // state, link.getItemName(), link.getLinkedUID(), thing.getStatus());
            // }
            // } else {
            // logger.warn("Cannot delegate update '{}' for item '{}' to handler for channel '{}', "
            // + "because no handler is assigned. Maybe the binding is not installed or not "
            // + "propertly initialized.", state, link.getItemName(), link.getLinkedUID());
            // }
            // } else {
            // logger.warn(
            // "Cannot delegate update '{}' for item '{}' to handler for channel '{}', "
            // + "because no thing with the UID '{}' could be found.",
            // state, link.getItemName(), link.getLinkedUID(), link.getLinkedUID().getThingUID());
            // }
        }

        @Override
        public void sendCommand(Command command) {
            eventPublisher.post(ItemEventFactory.createCommandEvent(link.getItemName(), command,
                    HomekitCommunicationManager.class.getCanonicalName()));
            // HomekitCommunicationManager.this.sendCommand(link.getLinkedUID(), command);
        }

        @Override
        public void sendUpdate(State state) {
            Item item = itemProvider.apply(link.getItemName());
            State acceptedState = itemStateConverter.convertToAcceptedState(state, item);
            eventPublisher.post(ItemEventFactory.createStateEvent(link.getItemName(), acceptedState,
                    HomekitCommunicationManager.class.getCanonicalName()));
            // HomekitCommunicationManager.this.sendUpdate(link.getLinkedUID(), state);
        }
    }

    public class GetStateProfileCallbackImpl implements ProfileCallback {

        private State state;

        public GetStateProfileCallbackImpl(ItemChannelLink link, Function<ThingUID, Thing> thingProvider,
                Function<String, Item> itemProvider) {
        }

        @Override
        public void handleCommand(Command command) {
        }

        @Override
        public void handleUpdate(State state) {
            this.state = state;
        }

        @Override
        public void sendCommand(Command command) {
        }

        @Override
        public void sendUpdate(State state) {
        }

        public State getState() {
            return state;
        }
    }

    public class HomekitProfileContextImpl implements ProfileContext {

        private static final String THREAD_POOL_NAME = "profiles";
        private final Configuration configuration;

        public HomekitProfileContextImpl(Configuration configuration) {
            this.configuration = configuration;
        }

        @Override
        public Configuration getConfiguration() {
            return configuration;
        }

        @Override
        public ScheduledExecutorService getExecutorService() {
            return ThreadPoolManager.getScheduledPool(THREAD_POOL_NAME);
        }

    }
    // public void stateUpdated(ChannelUID channelUID, State state) {
    // itemChannelLinkRegistry.getLinks(channelUID).forEach(link -> {
    // final Item item = itemRegistry.get(link.getItemName());
    // if (item != null) {
    // State acceptedState = itemStateConverter.convertToAcceptedState(state, item);
    // eventPublisher.post(ItemEventFactory.createStateEvent(link.getItemName(), acceptedState,
    // HomekitCommunicationManager.class.getCanonicalName()));
    // }
    // });
    // }

    // public void sendCommand(ChannelUID channelUID, Command command) {
    // itemChannelLinkRegistry.getLinks(channelUID).forEach(link -> {
    // final Item item = itemRegistry.get(link.getItemName());
    // if (item != null) {
    // eventPublisher.post(ItemEventFactory.createCommandEvent(link.getItemName(), command,
    // HomekitCommunicationManager.class.getCanonicalName()));
    // }
    // });
    // }

    // public void handleUpdate(CharacteristicUID characteristicUID, JsonObject jsonObject) {
    // Notification notification = notificationRegistry.get(new NotificationUID(characteristicUID.toString()));
    //
    // if (notification != null) {
    // notification.publish(jsonObject);
    // }
    // }
}
