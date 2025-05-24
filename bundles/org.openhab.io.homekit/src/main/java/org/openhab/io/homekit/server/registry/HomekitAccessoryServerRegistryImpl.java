package org.openhab.io.homekit.server.registry;

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
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.accessory.HomekitAccessoryCategory;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.api.factory.HomekitAccessoryFactory;
import org.openhab.io.homekit.api.provider.HomekitAccessoryServerProvider;
import org.openhab.io.homekit.api.registry.HomekitAccessoryRegistry;
import org.openhab.io.homekit.api.registry.HomekitAccessoryServerRegistry;
import org.openhab.io.homekit.api.registry.HomekitPairingRegistry;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;
import org.openhab.io.homekit.api.uid.HomekitAccessoryServerUID;
import org.openhab.io.homekit.api.uid.HomekitAccessoryUID;
import org.openhab.io.homekit.event.core.HomekitEventSubscription;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.event.model.server.HomekitAccessoryServerEvent;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.openhab.io.homekit.exception.HomekitServerException;
import org.openhab.io.homekit.server.HomekitRemoteAccessoryServer;
import org.openhab.io.homekit.util.HomekitUID;
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
@Component(immediate = true, service = HomekitAccessoryServerRegistry.class)
public class HomekitAccessoryServerRegistryImpl
        extends AbstractRegistry<HomekitAccessoryServer, HomekitAccessoryServerUID, HomekitAccessoryServerProvider>
        implements HomekitAccessoryServerRegistry, ReadyService.ReadyTracker {

    private static final String HOMEKIT_ACCESSORY_SERVER_REGISTRY = "homekit.accessoryServerRegistry";
    private static final String HOMEKIT_MANAGED_ACCESSORY_SERVER_PROVIDER = "homekit.managedAccessoryServerProvider";
    private static final int MAX_ACCESSORIES_PER_SERVER = 150;
    private static final int LOWEST_PORT_NUMBER = 9000;

    protected static final String LOG_PREFIX = "Homekit Registry: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "HomekitAccessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    private final HomekitUID subscriberUID = new HomekitUID("registry:");

    private final Logger logger = LoggerFactory.getLogger(HomekitAccessoryServerRegistryImpl.class);

    private final ReadyService readyService;
    private final NetworkAddressService networkAddressService;
    private final HomekitAccessoryRegistry accessoryRegistry;
    private final HomekitPairingRegistry pairingRegistry;
    private final HomekitEventManager eventManager;
    private final Set<HomekitEventSubscription> eventSubscriptions = new HashSet<>();
    private final HomekitAccessoryFactory accessoryFactory;

    @Activate
    public HomekitAccessoryServerRegistryImpl(@Reference ReadyService readyService,
            @Reference NetworkAddressService networkAddressService,
            @Reference HomekitAccessoryRegistry accessoryRegistry, @Reference HomekitPairingRegistry pairingRegistry,
            @Reference HomekitEventManager eventManager, @Reference HomekitAccessoryFactory accessoryFactory) {
        super(HomekitAccessoryServerProvider.class);
        this.readyService = readyService;
        this.networkAddressService = networkAddressService;
        this.accessoryRegistry = accessoryRegistry;
        this.pairingRegistry = pairingRegistry;
        this.eventManager = eventManager;
        this.accessoryFactory = accessoryFactory;
    }

    @Override
    @Activate
    protected void activate(final BundleContext context) {
        super.activate(context);
        logger.debug("{}Activating HomekitAccessory Server Registry", LOG_INIT);
        readyService.registerTracker(this, new ReadyMarkerFilter().withType(HOMEKIT_MANAGED_ACCESSORY_SERVER_PROVIDER));
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setManagedProvider(HomekitManagedAccessoryServerProvider provider) {
        super.setManagedProvider(provider);
    }

    protected void unsetManagedProvider(HomekitManagedAccessoryServerProvider provider) {
        super.unsetManagedProvider(provider);
    }

    @Override
    protected void addProvider(Provider<HomekitAccessoryServer> provider) {
        logger.debug("{}Adding provider: {}", LOG_CONFIG, provider.toString());

        ReadyMarker newMarker = new ReadyMarker(HOMEKIT_MANAGED_ACCESSORY_SERVER_PROVIDER, provider.toString());

        if (provider instanceof HomekitManagedAccessoryServerProvider) {
            if (readyService.isReady(newMarker)) {
                addProviderWithReadyMarker(provider);
            }
        } else {
            super.addProvider(provider);
        }
    }

    @Override
    @Nullable
    public synchronized HomekitAccessoryServer getAvailableBridgeAccessoryServer() {
        HomekitAccessoryServer availableServer = null;
        int highestPortNumber = LOWEST_PORT_NUMBER;
        try {
            for (HomekitAccessoryServer server : getAll()) {
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

        logger.info("{}Found {} HomekitAccessory Servers, highest port: {}", LOG_STATE, getAll().size(),
                highestPortNumber);

        if (availableServer == null) {
            try {
                availableServer = new HomekitRemoteAccessoryServer(HomekitAccessoryCategory.BRIDGES,
                        InetAddress.getByName(networkAddressService.getPrimaryIpv4HostAddress()), highestPortNumber++,
                        accessoryRegistry, pairingRegistry, eventManager, accessoryFactory);
            } catch (UnknownHostException | HomekitServerException e) {
                logger.error("{}Failed to create HomekitRemoteAccessoryServer: {}", LOG_ERROR, e.getMessage(), e);
                return null;
            }
        } else {
            try {
                if (availableServer.getAccessory(1) == null) {
                    try {
                        logger.info(
                                "{}Adding Bridge HomekitAccessory to Server - UID: {}, Type: {}, Port: {}, Setup Code: {}",
                                LOG_ACCESSORY, availableServer.getUID(), availableServer.getClass().getSimpleName(),
                                availableServer.getPort(), availableServer.getSetupCode());
                        HomekitAccessory bridgeAccessory = accessoryFactory.createAccessoryFromTag("generic");
                        bridgeAccessory.assignToServer(availableServer);
                    } catch (Exception e) {
                        logger.error("{}Error adding bridge accessory: {}", LOG_ERROR, e.getMessage(), e);
                    }
                }

                logger.info(
                        "{}Found HomekitAccessory Server - UID: {}, Type: {}, Port: {}, Setup Code: {}, Accessories: {}",
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

    public void handleAccessoryServerEvent(HomekitAccessoryServerEvent event) {
        switch (event.getType()) {
            case SERVER_STATE_CHANGED -> {
                if (event.getServer() != null && event.getServer().isPresent()) {
                    this.update(event.getServer().get());
                }
            }
            default -> {
                // No Op
            }
        }
    }

    public synchronized void addProviderWithReadyMarker(Provider<HomekitAccessoryServer> provider) {
        super.addProvider(provider);

        for (HomekitAccessoryServer aServer : getAll()) {
            logger.debug("{}HomekitAccessory Server available - UID: {}, Setup Code: {}", LOG_ACCESSORY,
                    aServer.getUID(), aServer.getSetupCode());
            if (aServer instanceof HomekitAccessoryServer accessoryServer) {
                accessoryServer.advertise();
            }
        }

        logger.info("{}Marking HomekitAccessory Server Registry as ready", LOG_STATE);
        ReadyMarker newMarker = new ReadyMarker(HOMEKIT_ACCESSORY_SERVER_REGISTRY, this.toString());
        readyService.markReady(newMarker);
    }

    @Override
    public void added(Provider<HomekitAccessoryServer> provider, HomekitAccessoryServer element) {

        eventSubscriptions.add(eventManager.subscribe(HomekitEventType.SERVER_STATE_CHANGED, (UID) element.getUID(),
                subscriberUID, event -> handleAccessoryServerEvent((HomekitAccessoryServerEvent) event)));
        super.added(provider, element);
    }

    @Override
    public void removed(Provider<HomekitAccessoryServer> provider, HomekitAccessoryServer element) {
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

    public HomekitAccessoryServer getAccessoryServer(HomekitAccessoryUID accessoryUID) {
        return getAll().stream()
                .filter(server -> {
                    try {
                        return server.getAccessories().stream()
                                .anyMatch(accessory -> accessory.getUID().equals(accessoryUID));
                    } catch (HomekitAccessoryOperationException e) {
                        return false;
                    }
                })
                .findFirst()
                .orElse(null);
    }
}
