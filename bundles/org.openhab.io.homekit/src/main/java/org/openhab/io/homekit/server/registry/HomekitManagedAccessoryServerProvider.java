package org.openhab.io.homekit.server.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.AbstractManagedProvider;
import org.openhab.core.common.registry.ManagedProvider;
import org.openhab.core.io.transport.mdns.MDNSService;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyService;
import org.openhab.core.storage.StorageService;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.accessory.HomekitAccessoryCategory;
import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.openhab.io.homekit.api.provider.HomekitAccessoryServerProvider;
import org.openhab.io.homekit.api.registry.HomekitAccessoryRegistry;
import org.openhab.io.homekit.api.registry.HomekitPairingRegistry;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;
import org.openhab.io.homekit.core.accessory.HomekitAccessoryUID;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.openhab.io.homekit.exception.HomekitServerException;
import org.openhab.io.homekit.server.HomekitAccessoryServerUID;
import org.openhab.io.homekit.server.HomekitLocalAccessoryServer;
import org.openhab.io.homekit.server.HomekitPersistedAccessoryServer;
import org.openhab.io.homekit.server.HomekitRemoteAccessoryServer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link HomekitManagedAccessoryServerProvider} is an implementation for the {@link ManagedProvider} interface and will
 * manage
 * the lifetime of HomekitAccessoryServer
 *
 *
 * @author Karel Goderis - Initial Contribution
 *
 */
@NonNullByDefault
@Component(immediate = true, service = { HomekitAccessoryServerProvider.class,
        HomekitManagedAccessoryServerProvider.class }, configurationPid = "org.openhab.homekit")
public class HomekitManagedAccessoryServerProvider
        extends AbstractManagedProvider<HomekitAccessoryServer, HomekitAccessoryServerUID, HomekitPersistedAccessoryServer>
        implements HomekitAccessoryServerProvider {

    private final Logger logger = LoggerFactory.getLogger(HomekitManagedAccessoryServerProvider.class);

    private static final String HOMEKIT_MANAGED_ACCESSORY_SERVER_PROVIDER = "homekit.managedAccessoryServerProvider";

    // ========== Log HomekitMessage Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit Provider: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "HomekitAccessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    private final ReadyService readyService;
    private final HomekitAccessoryRegistry accessoryRegistry;
    private final HomekitPairingRegistry pairingRegistry;
    private final MDNSService mdnsService;
    private final HomekitEventManager eventManager;
    private final Set<HomekitFactory> homekitFactories;

    @Activate
    public HomekitManagedAccessoryServerProvider(@Reference StorageService storageService,
            @Reference ReadyService readyService, @Reference HomekitAccessoryRegistry accessoryRegistry,
            @Reference HomekitPairingRegistry pairingRegistry, @Reference MDNSService mdnsService,
            @Reference HomekitEventManager eventManager, @Reference Set<HomekitFactory> homekitFactories) {
        super(storageService);
        this.readyService = readyService;
        this.accessoryRegistry = accessoryRegistry;
        this.pairingRegistry = pairingRegistry;
        this.mdnsService = mdnsService;
        this.eventManager = eventManager;
        this.homekitFactories = homekitFactories;

        logger.info("{}Marking Managed HomekitAccessory Server Provider as ready", LOG_STATE);
        ReadyMarker newMarker = new ReadyMarker(HOMEKIT_MANAGED_ACCESSORY_SERVER_PROVIDER, this.toString());
        this.readyService.markReady(newMarker);
    }

    @Override
    protected String getStorageName() {
        return HomekitAccessoryServer.class.getName();
    }

    @Override
    protected String keyToString(HomekitAccessoryServerUID key) {
        return key.getAsString();
    }

    @Override
    @SuppressWarnings("null")
    protected HomekitAccessoryServer toElement(String key, HomekitPersistedAccessoryServer persistableElement) {
        try {
            HomekitAccessoryServer server;
            if (persistableElement.getServerType() == HomekitPersistedAccessoryServer.ServerType.REMOTE) {
                server = new HomekitRemoteAccessoryServer(persistableElement.getCategory(),
                        persistableElement.getLocalAddress(), persistableElement.getPort(),
                        persistableElement.getPairingIdentifier(), persistableElement.getPrivateKey(),
                        accessoryRegistry, pairingRegistry, eventManager, homekitFactories);
            } else {
                server = new HomekitLocalAccessoryServer(persistableElement.getCategory(),
                        persistableElement.getLocalAddress(), persistableElement.getPort(),
                        persistableElement.getPairingIdentifier(), persistableElement.getPrivateKey(), mdnsService,
                        accessoryRegistry, pairingRegistry, eventManager, homekitFactories);
            }

            logger.debug("{}Created HomekitAccessory Server - UID: {}, Setup Code: {}", LOG_ACCESSORY, server.getUID(),
                    server.getSetupCode());

            if (accessoryRegistry != null) {
                Collection<String> accessoryUIDs = persistableElement.getAccessoryUIDs();
                for (String accessoryUID : accessoryUIDs) {
                    HomekitAccessory accessory = accessoryRegistry.get(new HomekitAccessoryUID(accessoryUID));
                    if (accessory != null) {
                        try {
                            server.addAccessory(accessory);
                        } catch (HomekitAccessoryOperationException e) {
                            logger.error("{}Failed to add accessory {}: {}", LOG_ERROR, accessoryUID, e.getMessage(),
                                    e);
                        }
                    }
                }
            }

            return server;

        } catch (HomekitServerException e) {
            logger.error("{}Error creating HomekitAccessory Server: {}", LOG_ERROR, e.getMessage(), e);
            return null;
        }
    }

    @Override
    protected @NonNull HomekitPersistedAccessoryServer toPersistableElement(HomekitAccessoryServer element) {
        HomekitPersistedAccessoryServer.ServerType serverType = element instanceof HomekitLocalAccessoryServer
                ? HomekitPersistedAccessoryServer.ServerType.LOCAL
                : HomekitPersistedAccessoryServer.ServerType.REMOTE;

        // get the accessories or an empty list
        Collection<HomekitAccessory> accessories = new ArrayList<>();
        try {
            Collection<HomekitAccessory> serverAccessories = element.getAccessories();
            if (serverAccessories != null) {
                accessories = serverAccessories;
            }
        } catch (HomekitAccessoryOperationException e) {
            logger.error("{}Error getting accessories: {}", LOG_ERROR, e.getMessage(), e);
        }

        return new HomekitPersistedAccessoryServer(element.getAddress(), element.getPort(), element.getPairingId(),
                element.getSecretKey(), element.getConfigurationIndex(), accessories, HomekitAccessoryCategory.BRIDGES,
                serverType);
    }
}
