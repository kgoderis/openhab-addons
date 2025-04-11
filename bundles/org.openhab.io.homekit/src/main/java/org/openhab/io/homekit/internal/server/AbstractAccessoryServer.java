package org.openhab.io.homekit.internal.server;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.AccessoryServer;
import org.openhab.io.homekit.api.hap.Pairing;
import org.openhab.io.homekit.api.listener.AccessoryServerChangeListener;
import org.openhab.io.homekit.api.registry.AccessoryRegistry;
import org.openhab.io.homekit.api.registry.PairingRegistry;
import org.openhab.io.homekit.internal.accessory.AccessoryState;
import org.openhab.io.homekit.internal.events.AccessoryServerEvent;
import org.openhab.io.homekit.internal.pairing.HomekitPairing;
import org.openhab.io.homekit.internal.pairing.PairingUID;
import org.openhab.io.homekit.util.Byte;
import org.openhab.io.homekit.util.HomekitKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public abstract class AbstractAccessoryServer implements AccessoryServer {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractAccessoryServer.class);

    protected static final String SERVICE_TYPE = "_hap._tcp.local.";

    protected final AccessoryRegistry accessoryRegistry;
    protected final PairingRegistry pairingRegistry;

    protected final InetAddress address;
    protected final int port;
    protected final byte[] pairingIdentifier;
    protected final byte[] secretKey;
    protected String setupCode;
    protected int configurationIndex = 1;

    private final Collection<AccessoryServerChangeListener> changeListeners = new CopyOnWriteArraySet<>();

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
            AccessoryRegistry accessoryRegistry, PairingRegistry pairingRegistry) {
        super();
        this.address = address;
        this.port = port;
        this.accessoryRegistry = accessoryRegistry;
        this.pairingRegistry = pairingRegistry;
        this.secretKey = privateKey;
        this.pairingIdentifier = pairingId;
        this.setupCode = "";
    }

    protected AccessoryState currentState = AccessoryState.UNPAIRED;

    protected synchronized void setState(AccessoryState newState) {
        if (!currentState.equals(newState)) {
            logger.debug("Accessory state changing from {} to {}", currentState, newState);
            AccessoryServerEvent event = new AccessoryServerEvent(this, null, null, null, newState.getEventType());
            currentState = newState;

            for (AccessoryServerChangeListener listener : changeListeners) {
                listener.onAccessoryServerEvent(event);
            }
        }
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
    public AccessoryServerUID getUID() {
        return new AccessoryServerUID(new String(getPairingId(), StandardCharsets.UTF_8).replace(":", ""));
    }

    @Override
    public byte[] getSecretKey() {
        return secretKey;
    }

    @Override
    public byte[] getPublicKey(byte @NonNull [] destinationPairingId) {
        Pairing hp = pairingRegistry.get(new PairingUID(getPairingId(), destinationPairingId));
        return hp != null ? hp.getPublicKey() : null;
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
        changeListeners.add(listener);
    }

    @Override
    public void removeChangeListener(AccessoryServerChangeListener listener) {
        changeListeners.remove(listener);
    }

    protected void notifyListeners() {
        AccessoryServerEvent event = new AccessoryServerEvent(this, null, null, null,
                AccessoryServerEvent.AccessoryServerEventType.SERVER_UPDATED);
        for (AccessoryServerChangeListener listener : this.changeListeners) {
            try {
                listener.onAccessoryServerEvent(event);
            } catch (Throwable throwable) {
                logger.error("Cannot inform listener {} ", listener, throwable.getMessage(), throwable);
            }
        }
    }

    @Override
    public Collection<Accessory> getAccessories() {
        return Collections.unmodifiableList(accessories.stream()
                .sorted((o1, o2) -> Long.valueOf(o1.getAccessoryId()).compareTo(Long.valueOf(o2.getAccessoryId())))
                .collect(Collectors.toList()));
    }

    @Override
    public @Nullable Accessory getAccessory(int acessoryId) {
        return accessories.stream().filter(accessory -> accessory.getAccessoryId() == acessoryId).findFirst()
                .orElse(null);
    }

    // Collection to track accessories locally
    private final Collection<Accessory> accessories = new CopyOnWriteArraySet<>();

    @Override
    public void addAccessory(Accessory accessory) {
        logger.debug("Adding Accessory {} of Type {} to Accessory Server {}", accessory.getUID(),
                accessory.getClass().getSimpleName(), this.getUID());

        if (accessories.add(accessory)) {
            // Increment configuration index when accessories change
            configurationIndex++;

            advertise();

            // Notify listeners of accessory addition
            AccessoryServerEvent event = new AccessoryServerEvent(this, accessory, null, null,
                    AccessoryServerEvent.AccessoryServerEventType.ACCESSORY_ADDED);
            for (AccessoryServerChangeListener listener : changeListeners) {
                try {
                    listener.onAccessoryServerEvent(event);
                } catch (Throwable throwable) {
                    logger.error("Cannot inform listener {} of accessory addition", listener, throwable);
                }
            }
        }
    }

    @Override
    public void removeAccessory(Accessory accessory) {
        logger.debug("Removing Accessory {} from Accessory Server {}", accessory.getUID(), this.getUID());

        if (accessories.remove(accessory)) {
            // Increment configuration index when accessories change
            configurationIndex++;

            advertise();

            // Notify listeners of accessory removal
            AccessoryServerEvent event = new AccessoryServerEvent(this, accessory, null, null,
                    AccessoryServerEvent.AccessoryServerEventType.ACCESSORY_REMOVED);
            for (AccessoryServerChangeListener listener : changeListeners) {
                try {
                    listener.onAccessoryServerEvent(event);
                } catch (Throwable throwable) {
                    logger.error("Cannot inform listener {} of accessory removal", listener, throwable);
                }
            }
        }
    }

    // @Override
    // public @Nullable Accessory getAccessory(Class<? extends Accessory> accessoryClass) {
    // Collection<Accessory> accessories = accessoryRegistry.get(getId());
    // for (Accessory accessory : accessories) {
    // if (accessory.getClass() == accessoryClass) {
    // return accessory;
    // }
    // }
    // return null;
    // }

    // @Override
    // public void addAccessory(Accessory accessory) {
    // logger.debug("Adding Accessory {} of Type {} to Accessory Server {}", accessory.getUID(),
    // accessory.getClass().getSimpleName(), this.getUID());
    // // if (accessory.getAccessoryId() <= 1 && !(accessory instanceof BridgeAccessory)) {
    // // throw new IndexOutOfBoundsException("The ID of an accessory used in a bridge must be greater than 1");
    // // }

    // if (accessoryRegistry.update(accessory) == null) {
    // logger.debug("Adding Accessory {} of Type {} to the Accessory Registry", accessory.getUID(),
    // accessory.getClass().getSimpleName(), this.getUID());
    // accessoryRegistry.add(accessory);
    // }
    // }

    // @Override
    // public void removeAccessory(Accessory accessory) {
    // accessoryRegistry.remove(accessory.getUID());
    // }

    @Override
    public void addPairing(byte @NonNull [] pairingId, byte @NonNull [] publicKey) {
        try {
            Pairing newPairing = new HomekitPairing(getPairingId(), pairingId, publicKey);
            Pairing oldPairing = pairingRegistry.remove(newPairing.getUID());

            if (oldPairing != null) {
                logger.debug("Removed Pairing of {} with Destination {} holding Public Key {}", getUID(),
                        Byte.toHexString(oldPairing.getDestinationId()), Byte.toHexString(oldPairing.getPublicKey()));
                setState(AccessoryState.DISCONNECTED);
            }

            pairingRegistry.add(newPairing);
            logger.debug("Paired {} with Destination {} holding Public Key {}", getUID(), pairingId,
                    Byte.toHexString(publicKey));
            setState(AccessoryState.PAIRED);
        } catch (Exception e) {
            e.printStackTrace();
            setState(AccessoryState.UNKNOWN);
        }
    }

    @Override
    public Pairing getPairing(byte @NonNull [] pairingId) {
        return pairingRegistry.get(new PairingUID(getPairingId(), pairingId));
    }

    @Override
    public Collection<Pairing> getPairings() {
        return pairingRegistry.get(getPairingId());
    }

    @Override
    public void removePairing(byte @NonNull [] pairingId) {
        Pairing oldPairing = pairingRegistry.remove(new PairingUID(getPairingId(), pairingId));
        if (oldPairing != null) {
            logger.debug("Removed Pairing of {} with Destination {} holding Public Key {}", getUID(),
                    Byte.toHexString(oldPairing.getDestinationId()), Byte.toHexString(oldPairing.getPublicKey()));
            setState(AccessoryState.DISCONNECTED);
        } else {
            logger.warn("The Pairing Registry does not contain a Pairing for {} with Destination {}", getUID(),
                    Byte.toHexString(pairingId));
            setState(AccessoryState.PAIRING_MISSING);
        }
    }

    @Override
    public boolean isPaired() {
        boolean paired = !pairingRegistry.get(getPairingId()).isEmpty();
        if (paired) {
            setState(AccessoryState.PAIRED);
        } else {
            setState(AccessoryState.UNPAIRED);
        }
        return paired;
    }

    protected static byte[] generateSecretKey() {
        return HomekitKeyGenerator.generateSecretKey();
    }

    protected static byte[] generatePairingId() {
        return HomekitKeyGenerator.generateHexidecimalId();
    }

    @Override
    public int getConfigurationIndex() {
        return configurationIndex;
    }

    @Override
    public void setConfigurationIndex(int configurationIndex) {
        this.configurationIndex = configurationIndex;
    }

    @Override
    public void factoryReset() {
        // TODO Auto-generated method stub
        // TODO Remove all crypto keys
        // TODO id is a unique random number, regenerate
        setState(AccessoryState.RESET);
    }

    // Add a method to handle connection state
    protected void handleConnection(boolean connected) {
        if (connected) {
            setState(AccessoryState.CONNECTED);
        } else {
            setState(AccessoryState.DISCONNECTED);
        }
    }

    // Add a method to handle pairing verification
    protected void handlePairingVerification(boolean verified) {
        if (verified) {
            setState(AccessoryState.PAIR_VERIFIED);
        } else {
            setState(AccessoryState.PAIRED);
        }
    }
}
