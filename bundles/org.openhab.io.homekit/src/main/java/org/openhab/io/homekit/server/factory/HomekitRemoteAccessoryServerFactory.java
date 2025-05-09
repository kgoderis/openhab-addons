package org.openhab.io.homekit.server.factory;
// package org.openhab.io.homekit.internal.server.factory;

// import java.net.InetAddress;
// import java.security.InvalidAlgorithmParameterException;
// import java.util.Arrays;
// import java.util.HashMap;
// import java.util.Hashtable;
// import java.util.Map;

// import org.eclipse.jdt.annotation.NonNull;
// import org.eclipse.jdt.annotation.NonNullByDefault;
// import org.eclipse.jdt.annotation.Nullable;
// import org.openhab.core.common.SafeCaller;
// import org.openhab.core.config.discovery.mdns.MDNSDiscoveryParticipant;
// import org.openhab.core.io.transport.mdns.MDNSService;
// import org.openhab.core.net.NetworkAddressService;
// import org.openhab.io.homekit.api.factory.AccessoryServerFactory;
// import org.openhab.io.homekit.api.hap.HomekitAccessoryServer;
// import org.openhab.io.homekit.api.registry.HomekitAccessoryRegistry;
// import org.openhab.io.homekit.api.registry.HomekitPairingRegistry;
// import org.openhab.io.homekit.internal.client.AccessoryServerConfigurationChangeParticipant;
// import org.openhab.io.homekit.internal.server.HomekitAccessoryServerUID;
// import org.openhab.io.homekit.internal.server.HomekitRemoteAccessoryServer;
// import org.osgi.framework.BundleContext;
// import org.osgi.framework.ServiceRegistration;
// import org.osgi.service.component.ComponentContext;
// import org.osgi.service.component.annotations.Activate;
// import org.osgi.service.component.annotations.Component;
// import org.osgi.service.component.annotations.Deactivate;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// @Component(immediate = true, service = { AccessoryServerFactory.class })
// @NonNullByDefault
// public class HomekitRemoteAccessoryServerFactory implements AccessoryServerFactory {

// private final Logger logger = LoggerFactory.getLogger(RemoteAccessoryServerFactory.class);

// @Nullable
// private MDNSService mdnsService;
// @Nullable
// private HomekitAccessoryRegistry accessoryRegistry;
// @Nullable
// private HomekitPairingRegistry pairingRegistry;
// @Nullable
// private NetworkAddressService networkAddressService;
// @Nullable
// private SafeCaller safeCaller;

// protected final @NonNullByDefault({}) BundleContext bundleContext;
// private final Map<HomekitAccessoryServerUID, @Nullable ServiceRegistration<?>> mdnsServiceRegs = new HashMap<>();

// @Activate
// public RemoteAccessoryServerFactory(ComponentContext componentContext, @Nullable MDNSService mdnsService,
// @Nullable HomekitAccessoryRegistry accessoryRegistry, @Nullable HomekitPairingRegistry pairingRegistry,
// @Nullable NetworkAddressService networkAddressService, @Nullable SafeCaller safeCaller) {
// super();
// this.bundleContext = componentContext.getBundleContext();

// this.mdnsService = mdnsService;
// this.accessoryRegistry = accessoryRegistry;
// this.pairingRegistry = pairingRegistry;
// this.networkAddressService = networkAddressService;
// this.safeCaller = safeCaller;
// }

// @Deactivate
// public void deactivate() {
// for (HomekitAccessoryServerUID serverUID : mdnsServiceRegs.keySet()) {
// ServiceRegistration<?> serviceReg = this.mdnsServiceRegs.remove(serverUID);
// if (serviceReg != null) {
// AccessoryServerConfigurationChangeParticipant service = (AccessoryServerConfigurationChangeParticipant)
// getBundleContext()
// .getService(serviceReg.getReference());
// serviceReg.unregister();
// }
// }
// }

// @Override
// public @Nullable HomekitAccessoryServer createServer(@NonNull String factoryType, InetAddress address, int port) {
// if (Arrays.stream(getSupportedServerTypes()).anyMatch(factoryType::equals)) {
// HomekitRemoteAccessoryServer newBridge = null;

// try {
// newBridge = new HomekitRemoteAccessoryServer(address, port, accessoryRegistry, pairingRegistry);
// if (newBridge != null) {
// logger.debug("Created an HomekitAccessory Server {} of Type {} running at {}:{}", newBridge.getUID(),
// newBridge.getClass().getSimpleName(), address.toString(), port);
// registerHomekitMDNSParticipant(newBridge);

// }
// return newBridge;
// } catch (InvalidAlgorithmParameterException e) {
// e.printStackTrace();
// return null;
// } catch (Exception e) {
// e.printStackTrace();
// }
// }

// return null;
// }

// @Override
// public @Nullable HomekitAccessoryServer createServer(@NonNull String factoryType, InetAddress address, int port, byte[] id,
// byte[] privateKey, int configurationIndex) {

// if (Arrays.stream(getSupportedServerTypes()).anyMatch(factoryType::equals)) {
// HomekitRemoteAccessoryServer newBridge = null;

// try {
// newBridge = new HomekitRemoteAccessoryServer(address, port, id, privateKey, accessoryRegistry,
// pairingRegistry);
// if (newBridge != null) {
// logger.debug("Created an HomekitAccessory Server {} of Type {} running at {}:{}", newBridge.getUID(),
// newBridge.getClass().getSimpleName(), address.toString(), port);
// registerHomekitMDNSParticipant(newBridge);
// }
// return newBridge;
// } catch (Exception e) {
// e.printStackTrace();
// }
// }

// return null;
// }

// @Override
// public String @NonNull [] getSupportedServerTypes() {
// return new String[] { HomekitRemoteAccessoryServer.class.getSimpleName() };
// }

// private synchronized void registerHomekitMDNSParticipant(HomekitRemoteAccessoryServer participant) {
// AccessoryServerConfigurationChangeParticipant mdnsParticipant = new AccessoryServerConfigurationChangeParticipant(
// participant);
// this.mdnsServiceRegs.put(participant.getUID(), getBundleContext().registerService(
// MDNSDiscoveryParticipant.class.getName(), mdnsParticipant, new Hashtable<String, Object>()));
// }

// protected BundleContext getBundleContext() {
// final BundleContext bundleContext = this.bundleContext;
// if (bundleContext != null) {
// return bundleContext;
// } else {
// throw new IllegalStateException(
// "The bundle context is missing (it seems your thing handler factory is used but not active).");
// }
// }
// }
