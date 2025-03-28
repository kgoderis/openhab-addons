package org.openhab.io.homekit.obsolete;
// package org.openhab.io.homekit;
//
// import java.util.concurrent.Executors;
// import java.util.concurrent.ScheduledExecutorService;
// import java.util.concurrent.TimeUnit;
//
// import org.eclipse.jdt.annotation.NonNull;
// import org.eclipse.jdt.annotation.Nullable;
// import org.openhab.core.common.registry.RegistryChangeListener;
// import org.openhab.core.items.GenericItem;
// import org.openhab.core.items.ItemRegistry;
// import org.openhab.core.service.ReadyMarker;
// import org.openhab.core.service.ReadyMarkerFilter;
// import org.openhab.core.service.ReadyService;
// import org.openhab.core.thing.link.ItemChannelLink;
// import org.openhab.core.thing.link.ItemChannelLinkRegistry;
// import org.openhab.io.homekit.api.Accessory;
// import org.openhab.io.homekit.api.AccessoryServer;
// import org.openhab.io.homekit.api.AccessoryServerRegistry;
// import org.openhab.io.homekit.api.Characteristic;
// import org.openhab.io.homekit.api.Service;
// import org.osgi.service.component.annotations.Activate;
// import org.osgi.service.component.annotations.Component;
// import org.osgi.service.component.annotations.Deactivate;
// import org.osgi.service.component.annotations.Reference;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
//
// @Component(immediate = true)
// public class HomekitItemChannelLinkRegistryChangeListener
// implements RegistryChangeListener<ItemChannelLink>, ReadyService.ReadyTracker {
//
// private static final Logger logger = LoggerFactory.getLogger(HomekitItemChannelLinkRegistryChangeListener.class);
//
// private static final String HOMEKIT_THING_REGISTRY_CHANGE_LISTENER = "homekit.homekitThingRegistryChangeListener";
// private static final long INITIALIZATION_DELAY_NANOS = TimeUnit.SECONDS.toNanos(2);
//
// private final ItemRegistry itemRegistry;
// private final AccessoryServerRegistry accessoryServerRegistry;
// private final ReadyService readyService;
// private final ItemChannelLinkRegistry linkRegistry;
//
// private volatile boolean initialized = false;
// private volatile long lastUpdate = System.nanoTime();
// private @Nullable ScheduledExecutorService executor;
//
// @Activate
// public HomekitItemChannelLinkRegistryChangeListener(@Reference ItemRegistry itemRegistry,
// @Reference AccessoryServerRegistry accessoryServerRegistry,
// @Reference ItemChannelLinkRegistry itemChannelLinkRegistry, @Reference ReadyService readyService) {
// this.readyService = readyService;
// this.accessoryServerRegistry = accessoryServerRegistry;
// this.itemRegistry = itemRegistry;
// this.linkRegistry = itemChannelLinkRegistry;
// this.executor = Executors.newSingleThreadScheduledExecutor();
//
// itemChannelLinkRegistry.addRegistryChangeListener(this);
// readyService.registerTracker(this, new ReadyMarkerFilter().withType(HOMEKIT_THING_REGISTRY_CHANGE_LISTENER));
// }
//
// @Deactivate
// public void deactivate() {
// linkRegistry.removeRegistryChangeListener(this);
// readyService.unregisterTracker(this);
// }
//
// @SuppressWarnings("null")
// private synchronized void delayedInitialize() {
// if (executor == null) {
// executor = Executors.newSingleThreadScheduledExecutor();
// }
//
// if (Thread.currentThread().isInterrupted()) {
// return;
// }
//
// final long diff = System.nanoTime() - lastUpdate - INITIALIZATION_DELAY_NANOS;
// if (diff < 0) {
// executor.schedule(() -> delayedInitialize(), -diff, TimeUnit.NANOSECONDS);
// } else {
// executor.shutdown();
// executor = null;
//
// initialize();
// }
// }
//
// private synchronized void initialize() {
//
// logger.debug("Initializing StateChangeListeners");
//
// initialized = true;
//
// try {
// for (ItemChannelLink aLink : linkRegistry.getAll()) {
// added(aLink);
// }
// } catch (Exception e) {
// e.printStackTrace();
// }
// }
//
// @Override
// public void added(ItemChannelLink element) {
// for (AccessoryServer server : accessoryServerRegistry.getAll()) {
// for (Accessory accessory : server.getAccessories()) {
// for (Service service : accessory.getServices()) {
// for (@SuppressWarnings("rawtypes")
// Characteristic characteristic : service.getCharacteristics()) {
// if (element.getLinkedUID().equals(characteristic.getChannelUID())) {
// GenericItem item = (GenericItem) itemRegistry.get(element.getItemName());
// if (item != null) {
// logger.debug("Linking {} to {}", element.getLinkedUID(), characteristic.getUID());
// item.addStateChangeListener(characteristic);
// }
// }
// }
// }
// }
// }
// }
//
// @Override
// public void removed(ItemChannelLink element) {
// for (AccessoryServer server : accessoryServerRegistry.getAll()) {
// for (Accessory accessory : server.getAccessories()) {
// for (Service service : accessory.getServices()) {
// for (@SuppressWarnings("rawtypes")
// Characteristic characteristic : service.getCharacteristics()) {
// if (element.getLinkedUID().equals(characteristic.getChannelUID())) {
// GenericItem item = (GenericItem) itemRegistry.get(element.getItemName());
// if (item != null) {
// item.removeStateChangeListener(characteristic);
// }
// }
// }
// }
// }
// }
// }
//
// @Override
// public void updated(ItemChannelLink oldElement, ItemChannelLink element) {
// removed(oldElement);
// added(element);
// }
//
// @Override
// public void onReadyMarkerAdded(@NonNull ReadyMarker readyMarker) {
// logger.debug("Receiving the ready marker {}:{}", readyMarker.getType(), readyMarker.getIdentifier());
//
// initialize();
// }
//
// @Override
// public void onReadyMarkerRemoved(@NonNull ReadyMarker readyMarker) {
// // TODO Auto-generated method stub
// }
//
// }
