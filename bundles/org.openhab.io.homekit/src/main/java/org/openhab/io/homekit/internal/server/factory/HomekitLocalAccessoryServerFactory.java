// package org.openhab.io.homekit.internal.server.factory;

// import java.net.InetAddress;
// import java.security.InvalidAlgorithmParameterException;
// import java.util.Arrays;

// import org.eclipse.jdt.annotation.NonNull;
// import org.eclipse.jdt.annotation.NonNullByDefault;
// import org.eclipse.jdt.annotation.Nullable;
// import org.openhab.core.common.SafeCaller;
// import org.openhab.core.io.transport.mdns.MDNSService;
// import org.openhab.core.net.NetworkAddressService;
// import org.openhab.io.homekit.api.factory.AccessoryServerFactory;
// import org.openhab.io.homekit.api.hap.HomekitAccessoryServer;
// import org.openhab.io.homekit.api.registry.HomekitAccessoryRegistry;
// import org.openhab.io.homekit.api.registry.HomekitPairingRegistry;
// import org.openhab.io.homekit.internal.server.BridgeLocalAccessoryServer;
// import org.osgi.service.component.annotations.Activate;
// import org.osgi.service.component.annotations.Component;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// @Component(immediate = true, service = { AccessoryServerFactory.class })
// @NonNullByDefault
// public class HomekitLocalAccessoryServerFactory implements AccessoryServerFactory {

// private final Logger logger = LoggerFactory.getLogger(LocalAccessoryServerFactory.class);

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

// @Activate
// public LocalAccessoryServerFactory(@Nullable MDNSService mdnsService, @Nullable HomekitAccessoryRegistry accessoryRegistry,
// @Nullable HomekitPairingRegistry pairingRegistry, @Nullable NetworkAddressService networkAddressService,
// @Nullable SafeCaller safeCaller) {
// super();

// this.mdnsService = mdnsService;
// this.accessoryRegistry = accessoryRegistry;
// this.pairingRegistry = pairingRegistry;
// this.networkAddressService = networkAddressService;
// this.safeCaller = safeCaller;
// }

// @Override
// public @Nullable HomekitAccessoryServer createServer(@NonNull String factoryType, InetAddress localAddress, int port) {
// if (Arrays.stream(getSupportedServerTypes()).anyMatch(factoryType::equals)) {
// BridgeLocalAccessoryServer newBridge = null;

// try {
// newBridge = new BridgeLocalAccessoryServer(localAddress, port, mdnsService, accessoryRegistry,
// pairingRegistry, safeCaller);
// if (newBridge != null) {
// logger.debug("Created an HomekitAccessory Server {} of Type {} running at {}:{}", newBridge.getUID(),
// newBridge.getClass().getSimpleName(), localAddress.toString(), port);
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
// public @Nullable HomekitAccessoryServer createServer(@NonNull String factoryType, InetAddress localAddress, int port,
// byte[] id, byte[] privateKey, int configurationIndex) {

// if (Arrays.stream(getSupportedServerTypes()).anyMatch(factoryType::equals)) {
// BridgeLocalAccessoryServer newBridge = null;

// try {
// newBridge = new BridgeLocalAccessoryServer(localAddress, port, id, privateKey, mdnsService,
// accessoryRegistry, pairingRegistry, safeCaller);
// if (newBridge != null) {
// logger.debug("Created an HomekitAccessory Server {} of Type {} running at {}:{}", newBridge.getUID(),
// newBridge.getClass().getSimpleName(), localAddress.toString(), port);
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
// public String @NonNull [] getSupportedServerTypes() {
// return new String[] { BridgeLocalAccessoryServer.class.getSimpleName() };
// }
// }
