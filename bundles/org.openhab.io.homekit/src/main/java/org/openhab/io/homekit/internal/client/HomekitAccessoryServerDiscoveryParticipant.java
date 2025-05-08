// package org.openhab.io.homekit.internal.client;

// import java.net.InetAddress;
// import java.net.UnknownHostException;
// import java.nio.charset.StandardCharsets;
// import java.util.Arrays;
// import java.util.Base64;
// import java.util.Collection;
// import java.util.Collections;
// import java.util.Enumeration;
// import java.util.HashMap;
// import java.util.Map;
// import java.util.Set;
// import java.util.concurrent.CopyOnWriteArrayList;

// import javax.jmdns.ServiceInfo;

// import org.apache.commons.lang.SystemUtils;
// import org.eclipse.jdt.annotation.NonNull;
// import org.eclipse.jdt.annotation.Nullable;
// import org.openhab.core.config.discovery.DiscoveryResult;
// import org.openhab.core.config.discovery.DiscoveryResultBuilder;
// import org.openhab.core.config.discovery.mdns.MDNSDiscoveryParticipant;
// import org.openhab.core.net.NetworkAddressService;
// import org.openhab.core.thing.ThingTypeUID;
// import org.openhab.core.thing.ThingUID;
// import org.openhab.io.homekit.api.factory.AccessoryServerFactory;
// import org.openhab.io.homekit.api.hap.HomekitAccessoryServer;
// import org.openhab.io.homekit.api.registry.HomekitAccessoryServerRegistry;
// import org.openhab.io.homekit.internal.server.HomekitAccessoryServerUID;
// import org.openhab.io.homekit.internal.server.StandAloneRemoteAccessoryServer;
// import org.openhab.io.homekit.internal.server.registry.HomekitManagedAccessoryServerProvider;
// import org.osgi.service.component.annotations.Activate;
// import org.osgi.service.component.annotations.Component;
// import org.osgi.service.component.annotations.Reference;
// import org.osgi.service.component.annotations.ReferenceCardinality;
// import org.osgi.service.component.annotations.ReferencePolicy;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// @Component(service = MDNSDiscoveryParticipant.class, immediate = true)
// public class HomekitAccessoryServerDiscoveryParticipant implements MDNSDiscoveryParticipant {

// private final Logger logger = LoggerFactory.getLogger(AccessoryServerDiscoveryParticipant.class);

// private static final String HAP_SERVICE_TYPE = "_hap._tcp.local.";

// private final HomekitManagedAccessoryServerProvider managedAccessoryServerProvider;
// private final NetworkAddressService networkAddressService;
// private final Map<String, ThingUID> cachedServices;
// private final Collection<AccessoryServerFactory> serverFactories = new CopyOnWriteArrayList<>();
// private final HomekitAccessoryServerRegistry accessoryServerRegistry;

// @Activate
// public AccessoryServerDiscoveryParticipant(@Reference HomekitManagedAccessoryServerProvider managedAccessoryServerProvider,
// @Reference NetworkAddressService networkAddressService,
// @Reference HomekitAccessoryServerRegistry accessoryServerRegistry) {
// this.managedAccessoryServerProvider = managedAccessoryServerProvider;
// this.networkAddressService = networkAddressService;
// this.cachedServices = new HashMap<String, ThingUID>();
// this.accessoryServerRegistry = accessoryServerRegistry;
// }

// @Override
// public @NonNull Set<@NonNull ThingTypeUID> getSupportedThingTypeUIDs() {
// return Collections.emptySet();
// }

// @Override
// public @NonNull String getServiceType() {
// return HAP_SERVICE_TYPE;
// }

// @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
// public void addServerFactory(AccessoryServerFactory serverFactory) {
// serverFactories.add(serverFactory);

// logger.debug("Added an HomekitAccessory Server Factory that supports {}",
// Arrays.toString(serverFactory.getSupportedServerTypes()));
// }

// public void removeServerFactory(AccessoryServerFactory serverFactory) {
// serverFactories.remove(serverFactory);
// }

// @Override
// public @Nullable DiscoveryResult createResult(@NonNull ServiceInfo service) {

// if (service.hasData() && service.getApplication().contains("hap") && service.getPort() != 0) {

// String id = service.getPropertyString("id");
// if (id != null) {
// logger.info(
// "Discovered a Homekit Automation Protocol participant with id '{}', having {} IPv4 and {} IPv6 addresses",
// id, service.getInet4Addresses().length, service.getInet6Addresses().length);

// String hostAddress = null;
// int port = 0;

// Map<String, Object> properties = new HashMap<>();

// if (SystemUtils.IS_OS_MAC) {
// // Use IPv4 only - see
// // https://medium.com/@quelgar/java-sockets-broken-for-ipv6-on-mac-5aae72f06b21
// if (networkAddressService.isUseIPv6()) {
// logger.warn(
// "IPv6 and MDNS dot no match well on MacOS - see
// https://medium.com/@quelgar/java-sockets-broken-for-ipv6-on-mac-5aae72f06b21");
// }
// if (service.getInet4Addresses().length > 0) {
// hostAddress = service.getInet4Addresses()[0].getHostAddress();
// }
// } else {
// if (networkAddressService.isUseIPv6()) {
// if (service.getInet6Addresses().length > 0) {
// hostAddress = service.getInet6Addresses()[0].getHostAddress();
// }
// } else {
// if (service.getInet4Addresses().length > 0) {
// hostAddress = service.getInet4Addresses()[0].getHostAddress();
// }
// }
// }

// if (hostAddress == null) {
// logger.warn(
// "Skipping a discovered Homekit Automation Protocol participant without valid host address");
// return null;
// }

// port = service.getPort();

// HomekitAccessoryServer accessoryServer = accessoryServerRegistry
// .get(new HomekitAccessoryServerUID(id.replace(":", "")));

// if (accessoryServer == null) {
// for (AccessoryServerFactory factory : serverFactories) {
// try {
// HomekitAccessoryServer server = factory.createServer(
// StandAloneRemoteAccessoryServer.class.getSimpleName(),
// InetAddress.getByName(hostAddress), port);
// if (server != null) {
// managedAccessoryServerProvider.add(server);
// logger.debug("Created a Remote HomekitAccessory Server {} with Setup Code {}", server.getUID(),
// server.getSetupCode());
// break; // Exit loop once server is created successfully
// }
// } catch (UnknownHostException e) {
// logger.warn("Failed to create server: {}", e.getMessage());
// }
// }
// } else {
// logger.debug("The HomekitAccessory Server Registry already contains an HomekitAccessory Server with Id '{}'",
// accessoryServer.getUID());
// }

// Enumeration<String> serviceProperties = service.getPropertyNames();
// while (serviceProperties.hasMoreElements()) {
// String element = serviceProperties.nextElement();
// String value = service.getPropertyString(element);
// // properties.put(element, value);
// //
// // if (element.equals(HomekitBindingConstants.CONFIGURATION_NUMBER_SHARP)) {
// // properties.put(HomekitAccessoryConfiguration.CONFIGURATION_NUMBER, value);
// // }

// if (element.equals(HomekitBindingConstants.DEVICE_ID)) {
// properties.put(HomekitAccessoryConfiguration.ACCESSORY_PAIRING_ID,
// Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8)));
// }
// }

// ThingUID uid = getThingUID(service);
// cachedServices.put(service.getQualifiedName(), uid);

// if (uid != null) {

// DiscoveryResultBuilder builder = DiscoveryResultBuilder.create(uid).withProperties(properties)
// .withRepresentationProperty(HomekitBindingConstants.DEVICE_ID);

// String category = service.getPropertyString("ci");

// if (category.equals("2")) {
// return builder.withLabel("Homekit HomekitAccessory Bridge").build();
// } else {
// return builder.withLabel("Homekit StandAlone HomekitAccessory").build();
// }
// }
// }
// }

// return null;
// }

// @Override
// public @Nullable ThingUID getThingUID(@NonNull ServiceInfo service) {
// if (service.hasData()) {
// if (service.getApplication().contains("hap") && service.getPropertyString("id") != null
// && service.getPropertyString("ci") != null) {
// String id = service.getPropertyString("id").replace(":", "");

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

// return null;
// }
// }
