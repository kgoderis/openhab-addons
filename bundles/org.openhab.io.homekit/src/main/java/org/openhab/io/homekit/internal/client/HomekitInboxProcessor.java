// package org.openhab.io.homekit.internal.client;
//
// import java.util.Map;
// import java.util.Objects;
// import java.util.Optional;
//
// import org.eclipse.jdt.annotation.NonNull;
// import org.eclipse.jdt.annotation.NonNullByDefault;
// import org.eclipse.jdt.annotation.Nullable;
// import org.openhab.core.config.core.Configuration;
// import org.openhab.core.config.discovery.DiscoveryResult;
// import org.openhab.core.config.discovery.inbox.Inbox;
// import org.openhab.core.config.discovery.inbox.InboxListener;
// import org.openhab.core.thing.Thing;
// import org.openhab.core.thing.ThingRegistry;
// import org.openhab.core.thing.binding.ThingHandler;
// import org.openhab.core.thing.type.ThingType;
// import org.openhab.core.thing.type.ThingTypeRegistry;
// import org.osgi.service.component.annotations.Activate;
// import org.osgi.service.component.annotations.Component;
// import org.osgi.service.component.annotations.Deactivate;
// import org.osgi.service.component.annotations.Reference;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
//
// @Component(immediate = true, service = InboxListener.class)
// @NonNullByDefault
// public class HomekitInboxProcessor implements InboxListener {
//
// private final Logger logger = LoggerFactory.getLogger(HomekitInboxProcessor.class);
//
// private final ThingRegistry thingRegistry;
// private final ThingTypeRegistry thingTypeRegistry;
// private final Inbox inbox;
//
// @Activate
// public HomekitInboxProcessor(final @Reference ThingTypeRegistry thingTypeRegistry,
// final @Reference ThingRegistry thingRegistry, final @Reference Inbox inbox) {
// this.thingTypeRegistry = thingTypeRegistry;
// this.thingRegistry = thingRegistry;
// this.inbox = inbox;
// }
//
// @Activate
// protected void activate(@Nullable Map<String, @Nullable Object> properties) {
// this.inbox.addInboxListener(this);
// }
//
// @Deactivate
// protected void deactivate() {
// this.inbox.removeInboxListener(this);
// }
//
// @Override
// public void thingAdded(@NonNull Inbox source, @NonNull DiscoveryResult result) {
// logger.info("thingAdded");
// }
//
// @Override
// public void thingUpdated(@NonNull Inbox source, @NonNull DiscoveryResult result) {
// String value = getRepresentationValue(result);
// if (value != null) {
// Optional<Thing> thing = thingRegistry.stream()
// .filter(t -> Objects.equals(value, getRepresentationPropertyValueForThing(t)))
// .filter(t -> Objects.equals(t.getThingTypeUID(), result.getThingTypeUID())).findFirst();
// if (thing.isPresent()) {
// Thing theThing = thing.get();
//
// String currentHost = theThing.getProperties().get(HomekitAccessoryConfiguration.HOST_ADDRESS);
// int currentPort = Integer.parseInt(theThing.getProperties().get(HomekitAccessoryConfiguration.PORT));
//
// if (!currentHost.equals(result.getProperties().get(HomekitAccessoryConfiguration.HOST_ADDRESS))
// || currentPort != Integer
// .parseInt((String) result.getProperties().get(HomekitAccessoryConfiguration.PORT))) {
// logger.info("'{}' : The Homekit Accessory's destination changed from {}:{} to {}:{}",
// theThing.getUID(), currentHost, currentPort,
// result.getProperties().get(HomekitAccessoryConfiguration.HOST_ADDRESS),
// result.getProperties().get(HomekitAccessoryConfiguration.PORT));
//
// ThingHandler thingHandler = theThing.getHandler();
//
// if (thingHandler instanceof HomekitAccessoryBridgeHandler) {
// ((HomekitAccessoryBridgeHandler) thingHandler).updateDestination(
// (String) result.getProperties().get(HomekitAccessoryConfiguration.HOST_ADDRESS),
// Integer.parseInt(
// (String) result.getProperties().get(HomekitAccessoryConfiguration.PORT)));
// }
//
// // Map<String, String> properties = theThing.getProperties();
// // properties.put(HomekitAccessoryConfiguration.HOST_ADDRESS,
// // (String) result.getProperties().get(HomekitAccessoryConfiguration.HOST_ADDRESS));
// // properties.put(HomekitAccessoryConfiguration.PORT,
// // (String) result.getProperties().get(HomekitAccessoryConfiguration.PORT));
// // theThing.setProperties(properties);
// }
// }
// }
// }
//
// @Override
// public void thingRemoved(@NonNull Inbox source, @NonNull DiscoveryResult result) {
// logger.info("thingRemoved");
// }
//
// // public @Nullable ThingUID getThingUID(DiscoveryResult result) {
// // if (result.getProperties().containsKey("hap") && result.getProperties().containsKey("id")) {
// // String id = ((String) result.getProperties().get("id")).replace(":", "");
// // return new ThingUID(HomekitBindingConstants.THING_TYPE_BRIDGE, id);
// // }
// //
// // return null;
// // }
//
// private @Nullable String getRepresentationValue(DiscoveryResult result) {
// return result.getRepresentationProperty() != null
// ? Objects.toString(result.getProperties().get(result.getRepresentationProperty()), null)
// : null;
// }
//
// private @Nullable String getRepresentationPropertyValueForThing(Thing thing) {
// ThingType thingType = thingTypeRegistry.getThingType(thing.getThingTypeUID());
// if (thingType != null) {
// String representationProperty = thingType.getRepresentationProperty();
// if (representationProperty == null) {
// return null;
// }
// Map<String, String> properties = thing.getProperties();
// if (properties.containsKey(representationProperty)) {
// return properties.get(representationProperty);
// }
// Configuration configuration = thing.getConfiguration();
// if (configuration.containsKey(representationProperty)) {
// return String.valueOf(configuration.get(representationProperty));
// }
// }
// return null;
// }
//
// }
