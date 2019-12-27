package org.openhab.io.homekit.internal.server;

import java.net.InetAddress;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractRegistry;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.net.NetworkAddressService;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyMarkerFilter;
import org.openhab.core.service.ReadyService;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.api.AccessoryServerChangeListener;
import org.openhab.io.homekit.api.AccessoryServerFactory;
import org.openhab.io.homekit.api.AccessoryServerProvider;
import org.openhab.io.homekit.api.AccessoryServerRegistry;
import org.openhab.io.homekit.library.accessory.BridgeAccessory;
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

    private final Collection<AccessoryServerFactory> serverFactories = new CopyOnWriteArrayList<>();
    private final ReadyService readyService;
    private final HomekitCommunicationManager communicationManager;
    private final NetworkAddressService networkAddressService;

    @Activate
    public AccessoryServerRegistryImpl(@Reference ReadyService readyService,
            @Reference HomekitCommunicationManager communicationManager,
            @Reference NetworkAddressService networkAddressService) {
        super(AccessoryServerProvider.class);
        this.readyService = readyService;
        this.communicationManager = communicationManager;
        this.networkAddressService = networkAddressService;

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

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addServerFactory(AccessoryServerFactory serverFactory) {
        if (serverFactory instanceof BridgeAccessoryServerFactoryImpl) {
            serverFactories.add(serverFactory);
        }
    }

    protected void removeServerFactory(AccessoryServerFactory serverFactory) {
        serverFactories.remove(serverFactory);
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
    public synchronized BridgeAccessoryServer getAvailableBridgeAccessoryServer() {
        BridgeAccessoryServer availableServer = null;
        int highestPortNumber = LOWEST_PORT_NUMBER;
        for (AccessoryServer server : getAll()) {
            if (server instanceof BridgeAccessoryServer
                    && server.getAccessories().size() < MAX_ACCESSORIES_PER_SERVER) {
                availableServer = (BridgeAccessoryServer) server;
                break;
            }
            if (server.getPort() > highestPortNumber) {
                highestPortNumber = server.getPort();
            }
        }

        logger.info("Found {} Accessory Servers, the highest Port is/will be {}", getAll().size(), highestPortNumber);

        if (availableServer == null) {
            for (AccessoryServerFactory factory : serverFactories) {
                try {
                    availableServer = (BridgeAccessoryServer) factory.createServer(
                            BridgeAccessoryServer.class.getSimpleName(),
                            InetAddress.getByName(networkAddressService.getPrimaryIpv4HostAddress()),
                            highestPortNumber++);
                    if (availableServer != null) {
                        availableServer
                                .addAccessory(new BridgeAccessory(communicationManager, availableServer, 1, true));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (availableServer != null) {
                    logger.info("Added an Accessory Server {} of Type {} running on Port {} with Setup Code {}",
                            availableServer.getUID(), availableServer.getClass().getSimpleName(),
                            availableServer.getPort(), availableServer.getSetupCode());
                    add(availableServer);
                    try {
                        availableServer.start();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    break;
                }
            }
        } else {
            if (availableServer.getAccessory(BridgeAccessory.class) == null) {
                try {
                    logger.warn(
                            "Added a Bridge Accessory to Server {} of Type {} running on Port {} with Setup Code {}",
                            availableServer.getUID(), availableServer.getClass().getSimpleName(),
                            availableServer.getPort(), availableServer.getSetupCode());
                    availableServer.addAccessory(new BridgeAccessory(communicationManager, availableServer, 1, true));
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
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

    public synchronized void addProviderWithReadyMarker(Provider<AccessoryServer> provider) {
        super.addProvider(provider);

        for (AccessoryServer aServer : getAll()) {
            logger.debug("Accessory Server {} with Setup Code {} is available in the Accessory Server Registry",
                    aServer.getUID(), aServer.getSetupCode());
            aServer.advertise();
        }

        logger.warn("Marking the Accessory Server Registry as ready");
        ReadyMarker newMarker = new ReadyMarker(HOMEKIT_ACCESSORY_SERVER_REGISTRY, this.toString());
        readyService.markReady(newMarker);
    }

    @Override
    public void updated(AccessoryServer server) {
        this.update(server);
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
