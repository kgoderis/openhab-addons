package org.openhab.io.homekit.internal.server.registry;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractRegistry;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.net.NetworkAddressService;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyMarkerFilter;
import org.openhab.core.service.ReadyService;
import org.openhab.io.homekit.api.hap.AccessoryCategory;
import org.openhab.io.homekit.api.hap.AccessoryServer;
import org.openhab.io.homekit.api.provider.AccessoryServerProvider;
import org.openhab.io.homekit.api.registry.AccessoryRegistry;
import org.openhab.io.homekit.api.registry.AccessoryServerRegistry;
import org.openhab.io.homekit.api.registry.PairingRegistry;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.openhab.io.homekit.exception.HomekitServerException;
import org.openhab.io.homekit.internal.events.AccessoryServerEvent;
import org.openhab.io.homekit.internal.events.HomekitEventManager;
import org.openhab.io.homekit.internal.events.HomekitEventSubscription;
import org.openhab.io.homekit.internal.events.HomekitEventType;
import org.openhab.io.homekit.internal.server.AccessoryServerUID;
import org.openhab.io.homekit.internal.server.RemoteAccessoryServer;
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
        implements AccessoryServerRegistry, ReadyService.ReadyTracker {

    private static final String HOMEKIT_ACCESSORY_SERVER_REGISTRY = "homekit.accessoryServerRegistry";
    private static final String HOMEKIT_MANAGED_ACCESSORY_SERVER_PROVIDER = "homekit.managedAccessoryServerProvider";
    private static final int MAX_ACCESSORIES_PER_SERVER = 150;
    private static final int LOWEST_PORT_NUMBER = 9000;

    protected static final String LOG_PREFIX = "HomeKit Registry: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "Accessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    private final Logger logger = LoggerFactory.getLogger(AccessoryServerRegistryImpl.class);

    private final ReadyService readyService;
    private final NetworkAddressService networkAddressService;
    private final AccessoryRegistry accessoryRegistry;
    private final PairingRegistry pairingRegistry;
    private final HomekitEventManager eventManager;
    private final Set<HomekitEventSubscription> eventSubscriptions = new HashSet<>();

    @Activate
    public AccessoryServerRegistryImpl(@Reference ReadyService readyService,
            @Reference NetworkAddressService networkAddressService, @Reference AccessoryRegistry accessoryRegistry,
            @Reference PairingRegistry pairingRegistry, @Reference HomekitEventManager eventManager) {
        super(AccessoryServerProvider.class);
        this.readyService = readyService;
        this.networkAddressService = networkAddressService;
        this.accessoryRegistry = accessoryRegistry;
        this.pairingRegistry = pairingRegistry;
        this.eventManager = eventManager;
    }

    @Override
    @Activate
    protected void activate(final BundleContext context) {
        super.activate(context);
        logger.debug("{}Activating Accessory Server Registry", LOG_INIT);
        readyService.registerTracker(this, new ReadyMarkerFilter().withType(HOMEKIT_MANAGED_ACCESSORY_SERVER_PROVIDER));
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
        logger.debug("{}Adding provider: {}", LOG_CONFIG, provider.toString());

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
        try {
            for (AccessoryServer server : getAll()) {
                if (server.getAccessories().size() < MAX_ACCESSORIES_PER_SERVER) {
                    availableServer = server;
                    break;
                }
                if (server.getPort() > highestPortNumber) {
                    highestPortNumber = server.getPort();
                }
            }
        } catch (HomekitAccessoryOperationException e) {
            logger.error("{}Error accessing server accessories: {}", LOG_ERROR, e.getMessage(), e);
            return null;
        }

        logger.info("{}Found {} Accessory Servers, highest port: {}", LOG_STATE, getAll().size(), highestPortNumber);

        if (availableServer == null) {
            try {
                availableServer = new RemoteAccessoryServer(AccessoryCategory.BRIDGES,
                        InetAddress.getByName(networkAddressService.getPrimaryIpv4HostAddress()), highestPortNumber++,
                        accessoryRegistry, pairingRegistry, eventManager);
            } catch (UnknownHostException | HomekitServerException e) {
                logger.error("{}Failed to create RemoteAccessoryServer: {}", LOG_ERROR, e.getMessage(), e);
                return null;
            }
        } else {
            try {
                if (availableServer.getAccessory(1) == null) {
                    try {
                        logger.info("{}Adding Bridge Accessory to Server - UID: {}, Type: {}, Port: {}, Setup Code: {}",
                                LOG_ACCESSORY, availableServer.getUID(), availableServer.getClass().getSimpleName(),
                                availableServer.getPort(), availableServer.getSetupCode());
                        BridgeAccessory bridgeAccessory = new BridgeAccessory(availableServer, true);
                        availableServer.addAccessory(bridgeAccessory);
                    } catch (Exception e) {
                        logger.error("{}Error adding bridge accessory: {}", LOG_ERROR, e.getMessage(), e);
                    }
                }

                logger.info("{}Found Accessory Server - UID: {}, Type: {}, Port: {}, Setup Code: {}, Accessories: {}",
                        LOG_STATE, availableServer.getUID(), availableServer.getClass().getSimpleName(),
                        availableServer.getPort(), availableServer.getSetupCode(),
                        availableServer.getAccessories().size());
            } catch (HomekitAccessoryOperationException e) {
                logger.error("{}Error accessing server accessories: {}", LOG_ERROR, e.getMessage(), e);
            }
        }

        return availableServer;
    }

    @Override
    public void onReadyMarkerAdded(ReadyMarker readyMarker) {
        logger.debug("{}Ready marker added - Type: {}, Identifier: {}", LOG_STATE, readyMarker.getType(),
                readyMarker.getIdentifier());

        if (getManagedProvider().isPresent()) {
            addProviderWithReadyMarker(getManagedProvider().get());
        }
    }

    @Override
    public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
        logger.debug("{}Ready marker removed - Type: {}, Identifier: {}", LOG_STATE, readyMarker.getType(),
                readyMarker.getIdentifier());
    }

    public void handleAccessoryServerEvent(AccessoryServerEvent event) {
        switch (event.getType()) {
            case SERVER_UPDATED -> this.update(event.getServer());
            default -> {
                // No Op
            }
        }
    }

    public synchronized void addProviderWithReadyMarker(Provider<AccessoryServer> provider) {
        super.addProvider(provider);

        for (AccessoryServer aServer : getAll()) {
            logger.debug("{}Accessory Server available - UID: {}, Setup Code: {}", LOG_ACCESSORY, aServer.getUID(),
                    aServer.getSetupCode());
            if (aServer instanceof AccessoryServer accessoryServer) {
                accessoryServer.advertise();
            }
        }

        logger.info("{}Marking Accessory Server Registry as ready", LOG_STATE);
        ReadyMarker newMarker = new ReadyMarker(HOMEKIT_ACCESSORY_SERVER_REGISTRY, this.toString());
        readyService.markReady(newMarker);
    }

    @Override
    public void added(Provider<AccessoryServer> provider, AccessoryServer element) {

        eventSubscriptions.add(eventManager.subscribe(HomekitEventType.SERVER_STATE_CHANGED,
                element.getUID().toString(), event -> handleAccessoryServerEvent((AccessoryServerEvent) event)));
        super.added(provider, element);
    }

    @Override
    public void removed(Provider<AccessoryServer> provider, AccessoryServer element) {
        try {
            // get all the subscriptions for this accessory server
            List<HomekitEventSubscription> subscriptions = eventSubscriptions.stream()
                    .filter(subscription -> subscription.getPublisherUID().equals(element.getUID().toString()))
                    .collect(Collectors.toList());

            // unsubscribe from the events
            subscriptions.forEach(subscription -> eventManager.unsubscribe(subscription.getEventType(),
                    subscription.getPublisherUID(), subscription.getSubscriber()));

            super.removed(provider, element);
        } catch (Exception e) {
            logger.error("{}Error removing change listener: {}", LOG_ERROR, e.getMessage(), e);
        }
    }
}
