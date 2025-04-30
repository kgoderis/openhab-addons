// package org.openhab.io.homekit.internal.handler;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.Map;
// import java.util.concurrent.ConcurrentHashMap;

// import org.eclipse.jdt.annotation.NonNullByDefault;
// import org.eclipse.jdt.annotation.Nullable;
// import org.openhab.core.config.core.Configuration;
// import org.openhab.core.thing.Channel;
// import org.openhab.core.thing.ChannelUID;
// import org.openhab.core.thing.Thing;
// import org.openhab.core.thing.ThingStatus;
// import org.openhab.core.thing.ThingStatusDetail;
// import org.openhab.core.thing.binding.BaseThingHandler;
// import org.openhab.core.thing.binding.builder.ChannelBuilder;
// import org.openhab.core.thing.binding.builder.ThingBuilder;
// import org.openhab.core.thing.type.ChannelGroupTypeUID;
// import org.openhab.core.thing.type.ChannelType;
// import org.openhab.core.thing.type.ChannelTypeUID;
// import org.openhab.core.types.Command;
// import org.openhab.core.types.State;
// import org.openhab.io.homekit.api.hap.Accessory;
// import org.openhab.io.homekit.api.hap.AccessoryServer;
// import org.openhab.io.homekit.api.hap.Characteristic;
// import org.openhab.io.homekit.api.hap.Service;
// import org.openhab.io.homekit.api.listener.AccessoryChangeListener;
// import org.openhab.io.homekit.api.listener.AccessoryServerChangeListener;
// import org.openhab.io.homekit.api.listener.CharacteristicChangeListener;
// import org.openhab.io.homekit.api.listener.ServiceChangeListener;
// import org.openhab.io.homekit.api.registry.AccessoryRegistry;
// import org.openhab.io.homekit.api.registry.AccessoryServerRegistry;
// import org.openhab.io.homekit.internal.accessory.AccessoryUID;
// import org.openhab.io.homekit.internal.client.HomekitBindingConstants;
// import org.openhab.io.homekit.internal.client.HomekitException;
// import org.openhab.io.homekit.internal.events.AccessoryEvent;
// import org.openhab.io.homekit.internal.events.AccessoryServerEvent;
// import org.openhab.io.homekit.internal.events.CharacteristicEvent;
// import org.openhab.io.homekit.internal.events.ServiceEvent;
// import org.openhab.io.homekit.internal.provider.HomekitChannelGroupTypeProvider;
// import org.openhab.io.homekit.internal.provider.HomekitChannelTypeProvider;
// import org.openhab.io.homekit.internal.provider.HomekitThingTypeProvider;
// import org.openhab.io.homekit.internal.server.AccessoryServerUID;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// @NonNullByDefault
// public class OldAccessoryThingHandler extends BaseThingHandler implements AccessoryServerChangeListener,
// ServiceChangeListener, AccessoryChangeListener, CharacteristicChangeListener {
// private final Logger logger = LoggerFactory.getLogger(OldAccessoryThingHandler.class);
// private final AccessoryServerRegistry serverRegistry;
// private final AccessoryRegistry accessoryRegistry;
// private final String deviceId;
// private final String accessoryId;
// private AccessoryServer server;
// private Accessory accessory;
// private final Map<Channel, @Nullable Characteristic<?>> characteristicMap = new ConcurrentHashMap<>();
// private final HomekitChannelTypeProvider homekitChannelTypeProvider;
// private final HomekitChannelGroupTypeProvider homekitChannelGroupTypeProvider;
// private final HomekitThingTypeProvider homekitThingTypeProvider;

// public OldAccessoryThingHandler(Thing thing, AccessoryServerRegistry serverRegistry,
// AccessoryRegistry accessoryRegistry, HomekitChannelTypeProvider homekitChannelTypeProvider,
// HomekitChannelGroupTypeProvider homekitChannelGroupTypeProvider,
// HomekitThingTypeProvider homekitThingTypeProvider) {
// super(thing);
// this.serverRegistry = serverRegistry;
// this.accessoryRegistry = accessoryRegistry;
// this.homekitChannelTypeProvider = homekitChannelTypeProvider;
// this.homekitChannelGroupTypeProvider = homekitChannelGroupTypeProvider;
// this.homekitThingTypeProvider = homekitThingTypeProvider;
// // Parse configuration
// Configuration config = thing.getConfiguration();
// deviceId = (String) config.get("deviceId");
// Object accessoryIdConfig = config.get("accessoryId");
// accessoryId = accessoryIdConfig != null ? (String) accessoryIdConfig : "1";

// if (deviceId == null) {
// throw new IllegalArgumentException("Thing configuration must contain deviceId and serviceId parameters");
// }

// // Get the server from registry
// AccessoryServer foundServer = this.serverRegistry.get(new AccessoryServerUID(deviceId));
// if (foundServer == null) {
// throw new IllegalArgumentException("No AccessoryServer found for deviceId: " + deviceId);
// }
// this.server = foundServer;

// // Get the accessory from registry
// Accessory foundAccessory = this.accessoryRegistry.get(new AccessoryUID(accessoryId));
// if (foundAccessory == null) {
// throw new IllegalArgumentException("No Accessory found for accessoryId: " + accessoryId);
// }
// this.accessory = foundAccessory;

// // If the Thing does not have any channels, traverse the services of the accessory, and for each service add a
// // ChannelGroup. then, for each characteristic of the service, add a channel in the ChannelGroup. Add logging to
// // the process.
// if (thing.getChannels().isEmpty()) {
// logger.info("Thing has no channels, traversing services of accessory");
// for (Service service : accessory.getServices()) {
// addChannelGroupForService(service);
// }
// } else {
// // traverse channels and add them to the characteristicMap
// for (Channel channel : thing.getChannels()) {

// String serviceTag = channel.getUID().getGroupId().split("\\.")[0];
// String serviceId = channel.getUID().getGroupId().split("\\.")[1];

// String serviceType;
// try {
// serviceType = homekitThingTypeProvider.getServiceTypeFromTag(serviceTag);
// } catch (HomekitException e) {
// logger.warn("Service type could not be determined for service tag: {}", serviceTag);
// continue;
// }

// Service service = accessory.getService(serviceType);
// if (service == null) {
// logger.warn("Service {} not found in accessory", serviceTag);
// continue;
// }

// // verify that the serviceId matches the serviceId of the service
// if (service.getInstanceId() != Long.parseLong(serviceId)) {
// logger.warn("Service ID {} does not match service ID {} for service {}", serviceId,
// service.getInstanceId(), serviceTag);
// continue;
// }

// String characteristicType;
// try {
// characteristicType = homekitChannelTypeProvider
// .getCharacteristicTypeFromTag(channel.getUID().getIdWithoutGroup());
// } catch (HomekitException e) {
// logger.warn("Characteristic type could not be determined for characteristic tag: {}",
// channel.getUID().getIdWithoutGroup());
// continue;
// }

// Characteristic<?> characteristic = service.getCharacteristic(characteristicType);
// if (characteristic == null) {
// logger.warn("Characteristic {} not found in service {}", characteristicType, serviceTag);
// continue;
// }

// characteristicMap.put(channel, characteristic);
// }
// }

// // Register as listener
// this.server.addChangeListener(this);
// this.accessory.addChangeListener(this);
// }

// @Override
// public void channelLinked(ChannelUID channelUID) {
// Channel channel = thing.getChannel(channelUID);
// if (channel != null) {
// Characteristic<?> characteristic = characteristicMap.get(channel);
// if (characteristic != null) {
// characteristic.addChangeListener(this);
// }
// }
// super.channelLinked(channelUID);
// }

// @Override
// public void channelUnlinked(ChannelUID channelUID) {
// Channel channel = thing.getChannel(channelUID);
// if (channel != null) {
// Characteristic<?> characteristic = characteristicMap.get(channel);
// if (characteristic != null) {
// characteristic.removeChangeListener(this);
// }
// }
// super.channelUnlinked(channelUID);
// }

// private String getCharacteristicTag(Characteristic<?> characteristic) throws HomekitException {
// try {
// Class<?> characteristicClass = characteristic.getClass();
// java.lang.reflect.Method getTagMethod = characteristicClass.getMethod("getTag");
// return (String) getTagMethod.invoke(null);
// } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
// throw new HomekitException(
// "Characteristic " + characteristic.getClass().getName() + " does not implement getTag() method");
// }
// }

// @Override
// public void initialize() {
// try {
// // Start the server if it's not already running
// if (!server.isPaired()) {
// server.advertise();
// server.start();
// }

// updateStatus(ThingStatus.ONLINE);
// } catch (Exception e) {
// updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
// }
// }

// @Override
// public void dispose() {
// // Unregister listeners
// this.server.removeChangeListener(this);
// this.accessory.removeChangeListener(this);
// characteristicMap.values().forEach(characteristic -> characteristic.removeChangeListener(this));
// super.dispose();
// }

// @Override
// public void onAccessoryServerEvent(AccessoryServerEvent event) {
// if (event.getServer().equals(server)) {
// switch (event.getType()) {
// case SERVER_STATE_CONNECTED:
// updateStatus(ThingStatus.ONLINE);
// break;
// case SERVER_STATE_DISCONNECTED:
// updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Server disconnected");
// break;
// case SERVER_STATE_PAIRED:
// case SERVER_STATE_PAIR_VERIFIED:
// updateStatus(ThingStatus.ONLINE);
// break;
// case SERVER_STATE_UNPAIRED:
// case SERVER_STATE_PAIR_UNVERIFIED:
// updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Server not paired");
// break;
// case SERVER_STATE_MISSING_SETUP_CODE:
// updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Missing setup code");
// break;
// case SERVER_STATE_PAIRING_MISSING:
// updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
// "Pairing information missing");
// break;
// case ACCESSORY_ADDED:
// if (event.getAccessory().equals(accessory)) {
// updateStatus(ThingStatus.ONLINE);
// }
// break;
// case ACCESSORY_REMOVED:
// if (event.getAccessory().equals(accessory)) {
// updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.GONE, "Accessory was removed");
// }
// break;
// default:
// break;
// }
// }
// }

// @Override
// public void onAccessoryEvent(AccessoryEvent accessoryEvent) {
// if (accessoryEvent.getAccessory().equals(accessory)) {
// switch (accessoryEvent.getType()) {
// case SERVICE_ADDED:
// addChannelGroupForService(accessoryEvent.getService());
// break;
// case SERVICE_STATE_CHANGED:
// // No action needed for state changes
// break;
// case SERVICE_REMOVED:
// // No action needed for service removal
// break;
// }
// }
// }

// @Override
// public void onServiceEvent(ServiceEvent serviceEvent) {
// // if (serviceEvent.getService().equals(service)) {
// // switch (serviceEvent.getType()) {
// // case CHARACTERISTIC_ADDED:
// // addChannelForCharacteristic(serviceEvent.getCharacteristic());
// // break;
// // case CHARACTERISTIC_REMOVED:
// // removeChannelForCharacteristic(serviceEvent.getCharacteristic());
// // break;
// // case CHARACTERISTIC_STATE_CHANGED:
// // // TODO: Handle characteristic state changed
// // break;
// // }
// // }
// }

// @Override
// public void onCharacteristicEvent(CharacteristicEvent event) {
// Characteristic<?> characteristic = event.getCharacteristic();
// ChannelUID channelUID = new ChannelUID(thing.getUID(), characteristic.getUID().getHomekitId());
// Channel channel = thing.getChannel(channelUID);

// if (channel != null && characteristicMap.get(channel) == characteristic) {
// Object newValue = event.getNewValue();
// if (newValue instanceof State) {
// updateState(channelUID, (State) newValue);
// }
// }
// }

// private Channel addChannelForCharacteristic(Characteristic<?> characteristic) {
// ChannelUID channelUID = new ChannelUID(thing.getUID(), characteristic.getUID().getHomekitId());

// ChannelTypeUID channelTypeUID = new ChannelTypeUID(HomekitBindingConstants.BINDING_ID,
// characteristic.getInstanceType());
// // TODO : Check consistency of channelTypeUID throughout the solution

// ChannelType channelType = homekitChannelTypeProvider.getChannelType(channelTypeUID, null);
// if (channelType == null) {
// throw new IllegalArgumentException("No ChannelType found for characteristic " + characteristic.getUID());
// }

// Channel channel = ChannelBuilder.create(channelUID).withType(channelTypeUID)
// .withLabel(characteristic.getDescription()).withDescription(characteristic.getDescription()).build();

// characteristicMap.put(channel, characteristic);

// updateThing(editThing().withChannel(channel).build());
// return channel;
// }

// private void removeChannelForCharacteristic(Characteristic<?> characteristic) {
// ChannelUID channelUID = new ChannelUID(thing.getUID(), characteristic.getUID().getHomekitId());
// Channel channel = thing.getChannel(channelUID);
// if (channel != null) {
// characteristicMap.remove(channel);
// updateThing(editThing().withoutChannel(channelUID).build());
// }
// }

// @Override
// public void handleCommand(ChannelUID channelUID, Command command) {
// Channel channel = thing.getChannel(channelUID);
// if (channel != null) {
// Characteristic<?> characteristic = characteristicMap.get(channel);
// if (characteristic != null) {
// try {
// handleCharacteristicCommand(characteristic, command);
// } catch (Exception e) {
// logger.warn("Failed to set value for characteristic {}: {}", channelUID.getId(), e.getMessage());
// }
// }
// }
// }

// private <T> void handleCharacteristicCommand(Characteristic<T> characteristic, Command command) throws Exception {
// if (command instanceof State) {
// @SuppressWarnings("unchecked")
// T value = (T) command;
// characteristic.setValue(value);
// }
// }

// private void addChannelGroupForService(Service service) {
// logger.info("Adding channel group for service: {}", service.getUID());
// try {
// String groupId = homekitThingTypeProvider.getServiceTag(service.getInstanceType()) + "."
// + service.getInstanceId();
// ChannelGroupTypeUID channelGroupTypeUID = homekitChannelGroupTypeProvider
// .getChannelGroupTypeUID(service.getInstanceType());

// // Create a list to hold all channels for this group
// List<Channel> channels = new ArrayList<>();

// // Add channels for each characteristic
// for (Characteristic<?> characteristic : service.getCharacteristics()) {
// ChannelUID channelUID = new ChannelUID(thing.getUID(), groupId,
// homekitChannelTypeProvider.getCharacteristicTag(characteristic.getInstanceType()));
// ChannelTypeUID channelTypeUID = new ChannelTypeUID(HomekitBindingConstants.BINDING_ID,
// characteristic.getInstanceType());

// ChannelType channelType = homekitChannelTypeProvider.getChannelType(channelTypeUID, null);
// if (channelType == null) {
// logger.warn("No ChannelType found for characteristic {}", characteristic.getUID());
// continue;
// }

// Channel channel = ChannelBuilder.create(channelUID).withType(channelTypeUID)
// .withLabel(characteristic.getDescription()).build();

// channels.add(channel);
// characteristicMap.put(channel, characteristic);
// }

// // Create a ThingBuilder to modify the thing
// ThingBuilder thingBuilder = editThing();

// // Add all channels at once
// channels.forEach(thingBuilder::withChannel);

// // Update the thing with all new channels
// updateThing(thingBuilder.build());

// } catch (IllegalArgumentException | HomekitException e) {
// logger.warn("Error adding channel group for service {}: {}", service.getUID(), e.getMessage());
// }
// }
// }
