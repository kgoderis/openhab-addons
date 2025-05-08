// package org.openhab.io.homekit.internal.client;

// import java.util.Collection;
// import java.util.Collections;
// import java.util.Date;
// import java.util.Set;
// import java.util.stream.Collectors;
// import java.util.stream.Stream;

// import org.eclipse.jdt.annotation.NonNull;
// import org.eclipse.jdt.annotation.Nullable;
// import org.openhab.core.config.discovery.AbstractDiscoveryService;
// import org.openhab.core.config.discovery.DiscoveryResult;
// import org.openhab.core.config.discovery.DiscoveryResultBuilder;
// import org.openhab.core.thing.Bridge;
// import org.openhab.core.thing.ThingTypeUID;
// import org.openhab.core.thing.ThingUID;
// import org.openhab.io.homekit.api.hap.HomekitAccessory;
// import org.openhab.io.homekit.api.hap.HomekitCharacteristic;
// import org.openhab.io.homekit.api.hap.HomekitService;
// import org.openhab.io.homekit.api.listener.HomekitStatusListener;
// import org.openhab.io.homekit.internal.handler.HomekitAccessoryBridgeHandler;
// import org.openhab.io.homekit.internal.handler.HomekitAccessoryHandler;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// public class HomekitAccessoryBridgeDiscoveryService extends AbstractDiscoveryService implements HomekitStatusListener
// {

// private final Logger logger = LoggerFactory.getLogger(HomekitAccessoryBridgeDiscoveryService.class);

// public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.unmodifiableSet(Stream
// .of(HomekitAccessoryHandler.SUPPORTED_THING_TYPES.stream(),
// HomekitAccessoryBridgeHandler.SUPPORTED_THING_TYPES.stream())
// .flatMap(i -> i).collect(Collectors.toSet()));

// private static final int SEARCH_TIME = 10;

// private final HomekitAccessoryBridgeHandler homekitBridgeHandler;

// public HomekitAccessoryBridgeDiscoveryService(HomekitAccessoryBridgeHandler homekitBridgeHandler) {
// super(SEARCH_TIME);
// this.homekitBridgeHandler = homekitBridgeHandler;
// }

// @Override
// protected void startScan() {
// // TO DO : clear the list in the BridgeHandler when the inbox is cleared. Can we capture that event?
// // homekitBridgeHandler.startSearch();
// Collection<HomekitAccessory> accessories = homekitBridgeHandler.getAccessories();

// for (HomekitAccessory accessory : accessories) {
// onAccessoryAdded(homekitBridgeHandler.getThing(), accessory);
// }
// }

// @Override
// protected synchronized void stopScan() {
// super.stopScan();
// removeOlderResults(getTimestampOfLastScan());
// }

// public void activate() {
// homekitBridgeHandler.registerHomekitStatusListener(this);
// }

// @Override
// public void deactivate() {
// removeOlderResults(new Date().getTime(), homekitBridgeHandler.getThing().getUID());
// homekitBridgeHandler.unregisterHomekitStatusListener(this);
// }

// @Override
// public Set<ThingTypeUID> getSupportedThingTypes() {
// return SUPPORTED_THING_TYPES;
// }

// @Override
// public void onAccessoryRemoved(Bridge bridge, HomekitAccessory accessory) {
// if (accessory.getAccessoryId() == 1) {
// // String id = bridge.getUID().getId().replace(":", "");
// // ThingUID uid = new ThingUID(HomekitBindingConstants.THING_TYPE_BRIDGE, id);
// // thingRemoved(uid);
// } else {
// ThingUID uid = new ThingUID(HomekitBindingConstants.THING_TYPE_ACCESSORY, bridge.getUID(),
// String.valueOf(accessory.getAccessoryId()));
// logger.info("HomekitAccessory {} was removed. The affiliated Thing {} will equally be removed",
// accessory.getAccessoryId(), uid);
// thingRemoved(uid);
// }
// }

// @Override
// public void onAccessoryAdded(Bridge bridge, HomekitAccessory accessory) {
// if (accessory.getAccessoryId() == 1) {
// // String id = bridge.getUID().getId().replace(":", "");
// // ThingUID uid = new ThingUID(HomekitBindingConstants.THING_TYPE_BRIDGE, id);
// //
// // DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(uid)
// // .withThingType(HomekitBindingConstants.THING_TYPE_BRIDGE)
// // .withRepresentationProperty(HomekitBindingConstants.DEVICE_ID).withLabel("Homekit HomekitAccessory Bridge")
// // .build();
// //
// // thingDiscovered(discoveryResult);
// } else {
// ThingUID uid = new ThingUID(HomekitBindingConstants.THING_TYPE_ACCESSORY, bridge.getUID(),
// String.valueOf(accessory.getAccessoryId()));

// DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(uid)
// .withThingType(HomekitBindingConstants.THING_TYPE_ACCESSORY).withBridge(bridge.getUID())
// .withLabel("Homekit HomekitAccessory").build();

// logger.info("HomekitAccessory {} was added. A Discovery result for Thing {} will be reported",
// accessory.getAccessoryId(), uid);

// thingDiscovered(discoveryResult);
// }
// }

// @Override
// public void onCharacteristicRemoved(@Nullable Bridge bridge, @NonNull HomekitCharacteristic characteristic) {
// // TODO Auto-generated method stub
// }

// @Override
// public void onCharacteristicAdded(@Nullable Bridge bridge, @NonNull HomekitCharacteristic characteristic) {
// // TODO Auto-generated method stub
// }

// @Override
// public void onCharacteristicStateChanged(@Nullable Bridge bridge, @NonNull HomekitCharacteristic characteristic) {
// // TODO Auto-generated method stub
// }

// @Override
// public void onServiceAdded(@Nullable Bridge bridge, @NonNull HomekitService service) {
// // TODO Auto-generated method stub
// }

// @Override
// public void onServiceRemoved(@Nullable Bridge bridge, @NonNull HomekitService service) {
// // TODO Auto-generated method stub
// }
// }
