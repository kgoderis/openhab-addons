// package org.openhab.io.homekit.internal.client;
//
// import java.nio.charset.StandardCharsets;
// import java.util.Base64;
// import java.util.Enumeration;
// import java.util.HashMap;
// import java.util.Map;
// import java.util.Set;
// import java.util.stream.Collectors;
// import java.util.stream.Stream;
//
// import javax.jmdns.ServiceInfo;
//
// import org.apache.commons.lang.SystemUtils;
// import org.eclipse.jdt.annotation.NonNull;
// import org.eclipse.jdt.annotation.Nullable;
// import org.openhab.core.config.discovery.DiscoveryResult;
// import org.openhab.core.config.discovery.DiscoveryResultBuilder;
// import org.openhab.core.config.discovery.mdns.MDNSDiscoveryParticipant;
// import org.openhab.core.net.NetworkAddressService;
// import org.openhab.core.thing.ThingTypeUID;
// import org.openhab.core.thing.ThingUID;
// import org.openhab.io.homekit.api.AccessoryServer;
// import org.openhab.io.homekit.api.AccessoryServerRegistry;
// import org.openhab.io.homekit.internal.server.AccessoryServerUID;
// import org.osgi.service.component.annotations.Activate;
// import org.osgi.service.component.annotations.Component;
// import org.osgi.service.component.annotations.Reference;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
//
// @Component(service = MDNSDiscoveryParticipant.class, immediate = true)
// public class HomekitAccessoryDiscoveryParticipant implements MDNSDiscoveryParticipant {
//
// private final Logger logger = LoggerFactory.getLogger(HomekitAccessoryDiscoveryParticipant.class);
//
// private static final String HAP_SERVICE_TYPE = "_hap._tcp.local.";
//
// private final AccessoryServerRegistry accessoryServerRegistry;
// private final NetworkAddressService networkAddressService;
// private final Map<String, ThingUID> cachedServices;
//
// @Activate
// public HomekitAccessoryDiscoveryParticipant(@Reference AccessoryServerRegistry accessoryServerRegistry,
// @Reference NetworkAddressService networkAddressService) {
// this.accessoryServerRegistry = accessoryServerRegistry;
// this.networkAddressService = networkAddressService;
//
// this.cachedServices = new HashMap<String, ThingUID>();
// }
//
// @Override
// public @NonNull Set<@NonNull ThingTypeUID> getSupportedThingTypeUIDs() {
// return Stream.of(HomekitBindingConstants.THING_TYPE_BRIDGE).collect(Collectors.toSet());
// }
//
// @Override
// public @NonNull String getServiceType() {
// return HAP_SERVICE_TYPE;
// }
//
// @Override
// public @Nullable DiscoveryResult createResult(@NonNull ServiceInfo service) {
//
// // logger.info("Discovery of {}:{} {} ({})",
// // service.getHostAddresses().length > 0 ? service.getHostAddresses()[0] : "N/A'", service.getPort(),
// // service.getName(), service.hasData());
// if (service.hasData() && service.getApplication().contains("hap") && service.getPort() != 0) {
//
// String id = service.getPropertyString("id");
// if (id != null) {
// logger.info(
// "Discovered a Homekit Automation Protocol participant with id '{}', having {} IPv4 and {} IPv6 addresses",
// id, service.getInet4Addresses().length, service.getInet6Addresses().length);
//
// AccessoryServer accessoryServer = accessoryServerRegistry
// .get(new AccessoryServerUID("BridgeAccessoryServer", id.replace(":", "")));
//
// if (accessoryServer != null) {
// logger.debug("Skipping the discovery for an existing openHAB Accessory Server '{}'",
// accessoryServer.getUID());
// } else {
// ThingUID uid = getThingUID(service);
//
// if (uid != null) {
// Map<String, Object> properties = new HashMap<>();
//
// if (SystemUtils.IS_OS_MAC) {
// // Use IPv4 only - see
// // https://medium.com/@quelgar/java-sockets-broken-for-ipv6-on-mac-5aae72f06b21
// if (networkAddressService.isUseIPv6()) {
// logger.warn(
// "IPv6 and MDNS dot no match well on MacOS - see
// https://medium.com/@quelgar/java-sockets-broken-for-ipv6-on-mac-5aae72f06b21");
// }
// if (service.getInet4Addresses().length > 0) {
// properties.put(HomekitAccessoryConfiguration.HOST,
// service.getInet4Addresses()[0].getHostAddress());
// }
// } else {
// if (networkAddressService.isUseIPv6()) {
// if (service.getInet6Addresses().length > 0) {
// properties.put(HomekitAccessoryConfiguration.HOST,
// service.getInet6Addresses()[0].getHostAddress());
// }
// } else {
// if (service.getInet4Addresses().length > 0) {
// properties.put(HomekitAccessoryConfiguration.HOST,
// service.getInet4Addresses()[0].getHostAddress());
// }
// }
// }
//
// if (properties.get(HomekitAccessoryConfiguration.HOST) == null) {
// logger.warn(
// "Skipping a discovered Homekit Automation Protocol participant without valid host address");
// return null;
// }
//
// properties.put(HomekitAccessoryConfiguration.PORT, service.getPort());
//
// Enumeration<String> serviceProperties = service.getPropertyNames();
// while (serviceProperties.hasMoreElements()) {
// String element = serviceProperties.nextElement();
// String value = service.getPropertyString(element);
// properties.put(element, value);
//
// if (element.equals(HomekitBindingConstants.CONFIGURATION_NUMBER_SHARP)) {
// properties.put(HomekitAccessoryConfiguration.CONFIGURATION_NUMBER, value);
// }
//
// if (element.equals(HomekitBindingConstants.DEVICE_ID)) {
// properties.put(HomekitAccessoryConfiguration.ACCESSORY_PAIRING_ID,
// Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8)));
// }
//
// }
//
// // logger.warn("Adding {} to the service cache", service.getQualifiedName());
// cachedServices.put(service.getQualifiedName(), uid);
//
// DiscoveryResultBuilder builder = DiscoveryResultBuilder.create(uid).withProperties(properties)
// .withRepresentationProperty(HomekitBindingConstants.DEVICE_ID);
//
// String category = service.getPropertyString("ci");
//
// if (category.equals("2")) {
// return builder.withLabel("Homekit Accessory Bridge").build();
// } else {
// return builder.withLabel("Homekit StandAlone Accessory").build();
// }
// }
// }
// }
// } else {
// // if (service.getPort() == 0) {
// // logger.warn("Skipping a service with port = 0");
// // }
// // if (!service.hasData()) {
// // logger.warn("Skipping a service without service data");
// // }
// // if (!service.getApplication().contains("hap")) {
// // logger.warn("Skipping a service unrelated to the Homekit Accessory Protocol");
// // }
// }
// return null;
// }
//
// @Override
// public @Nullable ThingUID getThingUID(@NonNull ServiceInfo service) {
// if (service.hasData()) {
// if (service.getApplication().contains("hap") && service.getPropertyString("id") != null
// && service.getPropertyString("ci") != null) {
// String id = service.getPropertyString("id").replace(":", "");
//
// if (service.getPropertyString("ci").contentEquals("2")) {
// return new ThingUID(HomekitBindingConstants.THING_TYPE_BRIDGE, id);
// } else {
// return new ThingUID(HomekitBindingConstants.THING_TYPE_STANDALONE_ACCESSORY, id);
// }
// }
// } else {
// if (service.getApplication().contains("hap") && cachedServices.containsKey(service.getQualifiedName())) {
// logger.warn("Removing {} from the service cache", service.getQualifiedName());
// ThingUID thingUID = cachedServices.remove(service.getQualifiedName());
// return thingUID;
// }
// }
//
// return null;
// }
// }
