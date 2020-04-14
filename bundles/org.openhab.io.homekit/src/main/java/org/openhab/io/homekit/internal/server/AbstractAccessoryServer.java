package org.openhab.io.homekit.internal.server;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.api.AccessoryRegistry;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.api.AccessoryServerChangeListener;
import org.openhab.io.homekit.api.NotificationRegistry;
import org.openhab.io.homekit.api.Pairing;
import org.openhab.io.homekit.api.PairingRegistry;
import org.openhab.io.homekit.crypto.HomekitEncryptionEngine;
import org.openhab.io.homekit.internal.pairing.PairingImpl;
import org.openhab.io.homekit.internal.pairing.PairingUID;
import org.openhab.io.homekit.util.Byte;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;

public abstract class AbstractAccessoryServer implements AccessoryServer {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractAccessoryServer.class);

    protected static final String SERVICE_TYPE = "_hap._tcp.local.";

    protected final AccessoryRegistry accessoryRegistry;
    protected final PairingRegistry pairingRegistry;
    protected final NotificationRegistry notificationRegistry;

    protected final InetAddress address;
    protected final int port;
    protected final byte[] pairingIdentifier;
    protected final byte[] secretKey;
    protected String setupCode;

    private final Collection<AccessoryServerChangeListener> listeners = new CopyOnWriteArraySet<>();

    // TODO use service trackers

    // private @NonNullByDefault({}) ServiceTracker<ThingTypeRegistry, ThingTypeRegistry>
    // thingTypeRegistryServiceTracker;
    // /**
    // * Initializes the {@link BaseThingHandlerFactory}. If this method is overridden by a sub class, the implementing
    // * method must call <code>super.activate(componentContext)</code> first.
    // *
    // * @param componentContext component context (must not be null)
    // */
    // protected void activate(ComponentContext componentContext) {
    // bundleContext = componentContext.getBundleContext();
    // thingTypeRegistryServiceTracker = new ServiceTracker<>(bundleContext, ThingTypeRegistry.class.getName(), null);
    // thingTypeRegistryServiceTracker.open();
    // configDescriptionRegistryServiceTracker = new ServiceTracker<>(bundleContext,
    // ConfigDescriptionRegistry.class.getName(), null);
    // configDescriptionRegistryServiceTracker.open();
    // }
    //
    // /**
    // * Disposes the {@link BaseThingHandlerFactory}. If this method is overridden by a sub class, the implementing
    // * method must call <code>super.deactivate(componentContext)</code> first.
    // *
    // * @param componentContext component context (must not be null)
    // */
    // protected void deactivate(ComponentContext componentContext) {
    // for (ServiceRegistration<ConfigStatusProvider> serviceRegistration : configStatusProviders.values()) {
    // if (serviceRegistration != null) {
    // serviceRegistration.unregister();
    // }
    // }
    // for (ServiceRegistration<FirmwareUpdateHandler> serviceRegistration : firmwareUpdateHandlers.values()) {
    // if (serviceRegistration != null) {
    // serviceRegistration.unregister();
    // }
    // }
    // thingTypeRegistryServiceTracker.close();
    // configDescriptionRegistryServiceTracker.close();
    // configStatusProviders.clear();
    // firmwareUpdateHandlers.clear();
    // bundleContext = null;
    // }
    // protected @Nullable ThingType getThingTypeByUID(ThingTypeUID thingTypeUID) {
    // if (thingTypeRegistryServiceTracker == null) {
    // throw new IllegalStateException(
    // "Base thing handler factory has not been properly initialized. Did you forget to call super.activate()?");
    // }
    // ThingTypeRegistry thingTypeRegistry = thingTypeRegistryServiceTracker.getService();
    // if (thingTypeRegistry != null) {
    // return thingTypeRegistry.getThingType(thingTypeUID);
    // }
    // return null;
    // }

    public AbstractAccessoryServer(InetAddress address, int port, byte[] pairingId, byte[] privateKey,
            AccessoryRegistry accessoryRegistry, PairingRegistry pairingRegistry,
            NotificationRegistry notificationRegistry) {
        super();
        this.address = address;
        this.port = port;
        this.accessoryRegistry = accessoryRegistry;
        this.pairingRegistry = pairingRegistry;
        this.notificationRegistry = notificationRegistry;
        this.secretKey = privateKey;
        this.pairingIdentifier = pairingId;
    }

    @Override
    public InetAddress getAddress() {
        return address;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public byte[] getPairingId() {
        return pairingIdentifier;
    }

    @Override
    public String getId() {
        return (new String(getPairingId(), StandardCharsets.UTF_8)).replace(":", "");
    }

    @Override
    public byte[] getSecretKey() {
        return secretKey;
    }

    @Override
    public byte[] getDestinationPublicKey(byte @NonNull [] destinationPairingId) {
        Pairing hp = pairingRegistry.get(new PairingUID(getPairingId(), destinationPairingId));
        return hp != null ? hp.getDestinationPublicKey() : null;
    }

    @Override
    public String getSetupCode() {
        return setupCode;
    }

    @Override
    public void setSetupCode(String setupCode) {
        this.setupCode = setupCode;
    }

    @Override
    public void addChangeListener(AccessoryServerChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeChangeListener(AccessoryServerChangeListener listener) {
        listeners.remove(listener);
    }

    protected void notifyListeners() {
        for (AccessoryServerChangeListener listener : this.listeners) {
            try {
                listener.updated(this);
            } catch (Throwable throwable) {
                logger.error("Cannot inform listener {} ", listener, throwable.getMessage(), throwable);
            }
        }
    }

    // protected static SecureRandom getSecureRandom() {
    // if (secureRandom == null) {
    // synchronized (HomekitCommunicationManager.class) {
    // if (secureRandom == null) {
    // secureRandom = new SecureRandom();
    // }
    // }
    // }
    // return secureRandom;
    // }

    @Override
    public void addPairing(byte @NonNull [] destinationPairingId, byte @NonNull [] destinationPublicKey) {
        try {
            Pairing newPairing = new PairingImpl(getPairingId(), destinationPairingId, destinationPublicKey);
            Pairing oldPairing = pairingRegistry.remove(newPairing.getUID());

            if (oldPairing != null) {
                logger.debug("Removed Pairing of {} with Destination {} holding Public Key {}", getId(),
                        Byte.toHexString(oldPairing.getDestinationPairingId()),
                        Byte.toHexString(oldPairing.getDestinationPublicKey()));
            }

            pairingRegistry.add(newPairing);
            logger.debug("Paired {} with Destination {} holding Public Key {}", getId(), destinationPairingId,
                    Byte.toHexString(destinationPublicKey));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removePairing(byte @NonNull [] destinationPairingId) {
        Pairing oldPairing = pairingRegistry.remove(new PairingUID(getPairingId(), destinationPairingId));
        if (oldPairing != null) {
            logger.debug("Removed Pairing of {} with Destination {} holding Public Key {}", getId(),
                    Byte.toHexString(oldPairing.getDestinationPairingId()),
                    Byte.toHexString(oldPairing.getDestinationPublicKey()));
        } else {
            logger.warn("The Pairing Registry does not contain a Pairing for {} with Destination {}", getId(),
                    Byte.toHexString(destinationPairingId));
        }
    }

    @Override
    public Pairing getPairing(byte @NonNull [] destinationPairingId) {
        return pairingRegistry.get(new PairingUID(getPairingId(), destinationPairingId));
    }

    @Override
    public boolean isPaired() {
        return !pairingRegistry.get(getPairingId()).isEmpty();
    }

    protected static byte[] generateSecretKey() {
        EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("ed25519-sha-512");
        byte[] seed = new byte[spec.getCurve().getField().getb() / 8];
        HomekitEncryptionEngine.getSecureRandom().nextBytes(seed);
        return seed;
    }

    protected static byte[] generatePairingId() {
        int byte1 = ((HomekitEncryptionEngine.getSecureRandom().nextInt(255) + 1) | 2) & 0xFE; // Unicast locally
                                                                                               // administered MAC;
        return (Integer.toHexString(byte1).toUpperCase() + ":"
                + Stream.generate(() -> HomekitEncryptionEngine.getSecureRandom().nextInt(255) + 1).limit(5)
                        .map(i -> Integer.toHexString(i).toUpperCase()).collect(Collectors.joining(":")))
                                .getBytes(StandardCharsets.UTF_8);
    }

}