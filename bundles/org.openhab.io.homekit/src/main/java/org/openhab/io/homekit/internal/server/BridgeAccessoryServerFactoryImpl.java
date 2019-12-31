package org.openhab.io.homekit.internal.server;

import java.math.BigInteger;
import java.net.InetAddress;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.srp6.SRP6Routines;

import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;

@Component(immediate = true, service = { AccessoryServerFactory.class })
@NonNullByDefault
public class BridgeAccessoryServerFactoryImpl implements AccessoryServerFactory {

    private final Logger logger = LoggerFactory.getLogger(BridgeAccessoryServerFactoryImpl.class);

    private volatile SecureRandom secureRandom;

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

    @Activate
    public BridgeAccessoryServerFactoryImpl() {
        super();

        secureRandom = new SecureRandom();
    }

    @Override
    public @Nullable AccessoryServer createServer(@NonNull String factoryType, InetAddress localAddress, int port) {
        try {
            AccessoryServer server = createServer(factoryType, localAddress, port, generatePairingId(), generateSalt(),
                    generatePrivateKey(), 1);
            return server;
        } catch (InvalidAlgorithmParameterException e) {
            return null;
        }
    }

    @Override
    public @Nullable AccessoryServer createServer(@NonNull String factoryType, InetAddress localAddress, int port,
            String id, BigInteger salt, byte[] privateKey, int configurationIndex) {

        if (Arrays.stream(getSupportedServerTypes()).anyMatch(factoryType::equals)) {
            BridgeAccessoryServer newBridge = null;

            try {
                newBridge = new BridgeAccessoryServer(localAddress, port, id, salt, privateKey, mdnsService,
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

    @Override
    public BigInteger generateSalt() {
        return new BigInteger(SRP6Routines.generateRandomSalt(16));
    }

    @Override
    public byte[] generatePrivateKey() throws InvalidAlgorithmParameterException {
        EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("ed25519-sha-512");
        byte[] seed = new byte[spec.getCurve().getField().getb() / 8];
        secureRandom.nextBytes(seed);
        return seed;
    }

    @Override
    public String generatePairingId() {
        int byte1 = ((secureRandom.nextInt(255) + 1) | 2) & 0xFE; // Unicast locally administered MAC;
        return Integer.toHexString(byte1).toUpperCase() + ":" + Stream.generate(() -> secureRandom.nextInt(255) + 1)
                .limit(5).map(i -> Integer.toHexString(i).toUpperCase()).collect(Collectors.joining(":"));
        // return Integer.toHexString(byte1) + Stream.generate(() -> secureRandom.nextInt(255) + 1).limit(5)
        // .map(i -> Integer.toHexString(i)).collect(Collectors.joining(""));
    }
}
