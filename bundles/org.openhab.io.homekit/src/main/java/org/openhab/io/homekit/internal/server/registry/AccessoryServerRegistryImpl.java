package org.openhab.io.homekit.internal.server.registry;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.SafeCaller;
import org.openhab.core.common.registry.AbstractRegistry;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.io.transport.mdns.MDNSService;
import org.openhab.core.net.NetworkAddressService;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyMarkerFilter;
import org.openhab.core.service.ReadyService;
import org.openhab.io.homekit.api.hap.AccessoryCategory;
import org.openhab.io.homekit.api.hap.AccessoryServer;
import org.openhab.io.homekit.api.listener.AccessoryServerChangeListener;
import org.openhab.io.homekit.api.provider.AccessoryServerProvider;
import org.openhab.io.homekit.api.registry.AccessoryRegistry;
import org.openhab.io.homekit.api.registry.AccessoryServerRegistry;
import org.openhab.io.homekit.api.registry.PairingRegistry;
import org.openhab.io.homekit.internal.events.AccessoryServerEvent;
import org.openhab.io.homekit.internal.server.AccessoryServerUID;
import org.openhab.io.homekit.library.accessory.BridgeAccessory;
import org.openhab.io.homekit.internal.server.RemoteAccessoryServer;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores the created HomekitServers
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = AccessoryServerRegistry.class)
public class AccessoryServerRegistryImpl
        extends AbstractRegistry<AccessoryServer, AccessoryServerUID, AccessoryServerProvider>
        implements AccessoryServerRegistry, ProviderChangeListener<AccessoryServer>, ReadyService.ReadyTracker,
        AccessoryServerChangeListener {

    private final Logger logger = LoggerFactory.getLogger(AccessoryServerRegistryImpl.class);

    private static final String HOMEKIT_ACCESSORY_SERVER_REGISTRY = "homekit.accessoryServerRegistry";
    private static final String HOMEKIT_MANAGED_ACCESSORY_SERVER_PROVIDER = "homekit.managedAccessoryServerProvider";
    private static final int MAX_ACCESSORIES_PER_SERVER = 150;
    private static final int LOWEST_PORT_NUMBER = 9000;

    private final ReadyService readyService;
    private final NetworkAddressService networkAddressService;
    private final MDNSService mdnsService;
    private final AccessoryRegistry accessoryRegistry;
    private final PairingRegistry pairingRegistry;
    private final SafeCaller safeCaller;

    @Activate
    public AccessoryServerRegistryImpl(@Reference ReadyService readyService,
            @Reference NetworkAddressService networkAddressService,
            @Reference MDNSService mdnsService,
            @Reference AccessoryRegistry accessoryRegistry,
            @Reference PairingRegistry pairingRegistry,
            @Reference SafeCaller safeCaller) {
        super(AccessoryServerProvider.class);
        this.readyService = readyService;
        this.networkAddressService = networkAddressService;
        this.mdnsService = mdnsService;
        this.accessoryRegistry = accessoryRegistry;
        this.pairingRegistry = pairingRegistry;
        this.safeCaller = safeCaller;
        
        readyService.registerTracker(this, new ReadyMarkerFilter().withType(HOMEKIT_MANAGED_ACCESSORY_SERVER_PROVIDER));
    }

    @Override
    @Activate
    protected void activate(final BundleContext context) {
        super.activate(context);
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setManagedProvider(ManagedAccessoryServerProvider provider) {
        super.setManagedProvider(provider);
    }

    protected void unsetManagedProvider(ManagedAccessoryServerProvider provider) {
        super.unsetManagedProvider(provider);
    }

    @Override
    protected void addProvider(Provider<AccessoryServer> provider) {
        logger.debug("Adding Provider {}", provider.toString());

        ReadyMarker newMarker = new ReadyMarker(HOMEKIT_MANAGED_ACCESSORY_SERVER_PROVIDER, provider.toString());

        if (provider instanceof ManagedAccessoryServerProvider) {
            if (readyService.isReady(newMarker)) {
                addProviderWithReadyMarker(provider);
            }
        } else {
            super.addProvider(provider);
        }
    }

    @Override
    @Nullable
    public synchronized AccessoryServer getAvailableBridgeAccessoryServer() {
        AccessoryServer availableServer = null;
        int highestPortNumber = LOWEST_PORT_NUMBER;
        for (AccessoryServer server : getAll()) {
            if (server.getAccessories().size() < MAX_ACCESSORIES_PER_SERVER) {
                availableServer = server;
                break;
            }
            if (server.getPort() > highestPortNumber) {
                highestPortNumber = server.getPort();
            }
        }

        logger.info("Found {} Accessory Servers, the highest Port is/will be {}", getAll().size(), highestPortNumber);

        if (availableServer == null) {
            try {
                availableServer = new RemoteAccessoryServer(AccessoryCategory.BRIDGES, 
                    InetAddress.getByName(networkAddressService.getPrimaryIpv4HostAddress()), 
                    highestPortNumber++, 
                    accessoryRegistry, 
                    pairingRegistry);
            } catch (UnknownHostException e) {
                logger.error("Failed to create RemoteAccessoryServer", e);
                return null;
            }
        } else {
            if (availableServer.getAccessory(1) == null) {
                try {
                    logger.warn(
                            "Added a Bridge Accessory to Server {} of Type {} running on Port {} with Setup Code {}",
                            availableServer.getUID(), availableServer.getClass().getSimpleName(),
                            availableServer.getPort(), availableServer.getSetupCode());
                    BridgeAccessory bridgeAccessory = new BridgeAccessory(1, true);
                    availableServer.addAccessory(bridgeAccessory);
                } catch (Exception e) {
                    logger.error("Error adding bridge accessory", e);
                }
            }

            logger.info(
                    "Found an Accessory Server {} of Type {} running on Port {} with Setup Code {}, currently hosting {} Accessories",
                    availableServer.getUID(), availableServer.getClass().getSimpleName(), availableServer.getPort(),
                    availableServer.getSetupCode(), availableServer.getAccessories().size());
        }

        return availableServer;
    }

    @Override
    public void onReadyMarkerAdded(ReadyMarker readyMarker) {
        logger.debug("Receiving the ready marker {}:{}", readyMarker.getType(), readyMarker.getIdentifier());

        if (getManagedProvider().isPresent()) {
            addProviderWithReadyMarker(getManagedProvider().get());
        }
    }

    @Override
    public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onAccessoryServerEvent(AccessoryServerEvent event) {
        switch (event.getType()) {
            case SERVER_UPDATED:
                this.update(event.getServer());
                break;
            case ACCESSORY_ADDED:
            case ACCESSORY_REMOVED:
            case SERVICE_ADDED:
            case SERVICE_REMOVED:
            case CHARACTERISTIC_ADDED:
            case CHARACTERISTIC_REMOVED:
            case CHARACTERISTIC_STATE_CHANGED:
                // No Op
                break;
        }
    }

    public synchronized void addProviderWithReadyMarker(Provider<AccessoryServer> provider) {
        super.addProvider(provider);

        for (AccessoryServer aServer : getAll()) {
            logger.debug("Accessory Server {} with Setup Code {} is available in the Accessory Server Registry",
                    aServer.getUID(), aServer.getSetupCode());
            if (aServer instanceof AccessoryServer) {
                ((AccessoryServer) aServer).advertise();
            }
        }

        logger.warn("Marking the Accessory Server Registry as ready");
        ReadyMarker newMarker = new ReadyMarker(HOMEKIT_ACCESSORY_SERVER_REGISTRY, this.toString());
        readyService.markReady(newMarker);
    }

    @Override
    public void added(Provider<AccessoryServer> provider, AccessoryServer element) {
        element.addChangeListener(this);
        super.added(provider, element);
    }

    @Override
    public void removed(Provider<AccessoryServer> provider, AccessoryServer element) {
        element.removeChangeListener(this);
        super.removed(provider, element);
    }
}
