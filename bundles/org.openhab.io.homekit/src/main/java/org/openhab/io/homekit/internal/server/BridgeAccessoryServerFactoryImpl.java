package org.openhab.io.homekit.internal.server;

import java.math.BigInteger;
import java.net.InetAddress;
import java.security.InvalidAlgorithmParameterException;
import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.SafeCaller;
import org.openhab.core.io.transport.mdns.MDNSService;
import org.openhab.core.net.NetworkAddressService;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.AccessoryRegistry;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.api.AccessoryServerFactory;
import org.openhab.io.homekit.api.NotificationRegistry;
import org.openhab.io.homekit.api.PairingRegistry;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = { AccessoryServerFactory.class })
@NonNullByDefault
public class BridgeAccessoryServerFactoryImpl implements AccessoryServerFactory {

    private final Logger logger = LoggerFactory.getLogger(BridgeAccessoryServerFactoryImpl.class);

    // private volatile SecureRandom secureRandom;

    @Reference
    @Nullable
    private MDNSService mdnsService;
    @Reference
    @Nullable
    private AccessoryRegistry accessoryRegistry;
    @Reference
    @Nullable
    private PairingRegistry pairingRegistry;
    @Reference
    @Nullable
    private NetworkAddressService networkAddressService;
    @Reference
    @Nullable
    private NotificationRegistry notificationRegistry;
    @Reference
    @Nullable
    private HomekitCommunicationManager communicationManager;
    @Reference
    @Nullable
    private SafeCaller safeCaller;

    // @Activate
    // public BridgeAccessoryServerFactoryImpl() {
    // super();
    //
    //// secureRandom = new SecureRandom();
    // }

    @Override
    public @Nullable AccessoryServer createServer(@NonNull String factoryType, InetAddress localAddress, int port) {
        // try {
        // AccessoryServer server = createServer(factoryType, localAddress, port, generatePairingId(),
        // generateSalt(),
        // generatePrivateKey(), 1);

        if (Arrays.stream(getSupportedServerTypes()).anyMatch(factoryType::equals)) {
            BridgeAccessoryServer newBridge = null;

            try {
                newBridge = new BridgeAccessoryServer(localAddress, port, mdnsService, accessoryRegistry,
                        pairingRegistry, notificationRegistry, communicationManager, safeCaller);
                if (newBridge != null) {
                    logger.debug("Created an Accessory Server {} of Type {} running at {}:{}", newBridge.getUID(),
                            newBridge.getClass().getSimpleName(), localAddress.toString(), port);
                }
                return newBridge;
            } catch (InvalidAlgorithmParameterException e) {
                e.printStackTrace();
                return null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;

        // return server;
        // } catch (InvalidAlgorithmParameterException e) {
        // return null;
        // }
    }

    @Override
    public @Nullable AccessoryServer createServer(@NonNull String factoryType, InetAddress localAddress, int port,
            byte[] id, BigInteger salt, byte[] privateKey, int configurationIndex) {

        if (Arrays.stream(getSupportedServerTypes()).anyMatch(factoryType::equals)) {
            BridgeAccessoryServer newBridge = null;

            try {
                newBridge = new BridgeAccessoryServer(localAddress, port, id, privateKey, mdnsService,
                        accessoryRegistry, pairingRegistry, notificationRegistry, communicationManager, safeCaller);
                if (newBridge != null) {
                    logger.debug("Created an Accessory Server {} of Type {} running at {}:{}", newBridge.getUID(),
                            newBridge.getClass().getSimpleName(), localAddress.toString(), port);
                }
                return newBridge;
            } catch (InvalidAlgorithmParameterException e) {
                e.printStackTrace();
                return null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public String @NonNull [] getSupportedServerTypes() {
        return new String[] { BridgeAccessoryServer.class.getSimpleName() };
    }

    //
    // @Override
    // public byte[] generatePrivateKey() throws InvalidAlgorithmParameterException {
    // EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("ed25519-sha-512");
    // byte[] seed = new byte[spec.getCurve().getField().getb() / 8];
    // secureRandom.nextBytes(seed);
    // return seed;
    // }
    //
    // @Override
    // public byte[] generatePairingId() {
    // int byte1 = ((secureRandom.nextInt(255) + 1) | 2) & 0xFE; // Unicast locally administered MAC;
    // return (Integer.toHexString(byte1).toUpperCase() + ":"
    // + Stream.generate(() -> secureRandom.nextInt(255) + 1).limit(5)
    // .map(i -> Integer.toHexString(i).toUpperCase()).collect(Collectors.joining(":")))
    // .getBytes(StandardCharsets.UTF_8);
    // }
}
