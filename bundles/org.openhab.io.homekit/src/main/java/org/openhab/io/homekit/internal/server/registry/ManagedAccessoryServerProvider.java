package org.openhab.io.homekit.internal.server.registry;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.AbstractManagedProvider;
import org.openhab.core.common.registry.ManagedProvider;
import org.openhab.core.io.transport.mdns.MDNSService;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyService;
import org.openhab.core.storage.StorageService;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.AccessoryCategory;
import org.openhab.io.homekit.api.hap.AccessoryServer;
import org.openhab.io.homekit.api.provider.AccessoryServerProvider;
import org.openhab.io.homekit.api.registry.AccessoryRegistry;
import org.openhab.io.homekit.api.registry.PairingRegistry;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.openhab.io.homekit.exception.HomekitServerException;
import org.openhab.io.homekit.internal.accessory.AccessoryUID;
import org.openhab.io.homekit.internal.events.HomekitEventManager;
import org.openhab.io.homekit.internal.server.AccessoryServerUID;
import org.openhab.io.homekit.internal.server.LocalAccessoryServer;
import org.openhab.io.homekit.internal.server.PersistedAccessoryServer;
import org.openhab.io.homekit.internal.server.RemoteAccessoryServer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ManagedAccessoryServerProvider} is an implementation for the {@link ManagedProvider} interface and will
 * manage
 * the lifetime of HomekitAccessoryServer
 *
 *
 * @author Karel Goderis - Initial Contribution
 *
 */
@NonNullByDefault
@Component(immediate = true, service = { AccessoryServerProvider.class,
        ManagedAccessoryServerProvider.class }, configurationPid = "org.openhab.homekit")
public class ManagedAccessoryServerProvider
        extends AbstractManagedProvider<AccessoryServer, AccessoryServerUID, PersistedAccessoryServer>
        implements AccessoryServerProvider {

    private final Logger logger = LoggerFactory.getLogger(ManagedAccessoryServerProvider.class);

    private static final String HOMEKIT_MANAGED_ACCESSORY_SERVER_PROVIDER = "homekit.managedAccessoryServerProvider";

    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "HomeKit Provider: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "Accessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    private final ReadyService readyService;
    private final AccessoryRegistry accessoryRegistry;
    private final PairingRegistry pairingRegistry;
    private final MDNSService mdnsService;
    private final HomekitEventManager eventManager;

    @Activate
    public ManagedAccessoryServerProvider(@Reference StorageService storageService,
            @Reference ReadyService readyService, @Reference AccessoryRegistry accessoryRegistry,
            @Reference PairingRegistry pairingRegistry, @Reference MDNSService mdnsService, @Reference HomekitEventManager eventManager) {
        super(storageService);
        this.readyService = readyService;
        this.accessoryRegistry = accessoryRegistry;
        this.pairingRegistry = pairingRegistry;
        this.mdnsService = mdnsService;
        this.eventManager = eventManager;

        logger.info("{}Marking Managed Accessory Server Provider as ready", LOG_STATE);
        ReadyMarker newMarker = new ReadyMarker(HOMEKIT_MANAGED_ACCESSORY_SERVER_PROVIDER, this.toString());
        this.readyService.markReady(newMarker);
    }

    @Override
    protected String getStorageName() {
        return AccessoryServer.class.getName();
    }

    @Override
    protected String keyToString(AccessoryServerUID key) {
        return key.getAsString();
    }

    @Override
    @SuppressWarnings("null")
    protected AccessoryServer toElement(String key, PersistedAccessoryServer persistableElement) {
        try {
            AccessoryServer server;
            if (persistableElement.getServerType() == PersistedAccessoryServer.ServerType.REMOTE) {
                server = new RemoteAccessoryServer(persistableElement.getCategory(),
                        persistableElement.getLocalAddress(), persistableElement.getPort(),
                        persistableElement.getPairingIdentifier(), persistableElement.getPrivateKey(),
                        accessoryRegistry, pairingRegistry, eventManager);
            } else {
                server = new LocalAccessoryServer(persistableElement.getCategory(),
                        persistableElement.getLocalAddress(), persistableElement.getPort(),
                        persistableElement.getPairingIdentifier(), persistableElement.getPrivateKey(), mdnsService,
                        accessoryRegistry, pairingRegistry, eventManager);
            }

            logger.debug("{}Created Accessory Server - UID: {}, Setup Code: {}", LOG_ACCESSORY, server.getUID(),
                    server.getSetupCode());

            if (accessoryRegistry != null) {
                Collection<String> accessoryUIDs = persistableElement.getAccessoryUIDs();
                for (String accessoryUID : accessoryUIDs) {
                    Accessory accessory = accessoryRegistry.get(new AccessoryUID(accessoryUID));
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
            logger.error("{}Error creating Accessory Server: {}", LOG_ERROR, e.getMessage(), e);
            return null;
        }
    }

    @Override
    protected @NonNull PersistedAccessoryServer toPersistableElement(AccessoryServer element) {
        PersistedAccessoryServer.ServerType serverType = element instanceof LocalAccessoryServer
                ? PersistedAccessoryServer.ServerType.LOCAL
                : PersistedAccessoryServer.ServerType.REMOTE;

        // get the accessories or an empty list
        Collection<Accessory> accessories = new ArrayList<>();
        try {
            Collection<Accessory> serverAccessories = element.getAccessories();
            if (serverAccessories != null) {
                accessories = serverAccessories;
            }
        } catch (HomekitAccessoryOperationException e) {
            logger.error("{}Error getting accessories: {}", LOG_ERROR, e.getMessage(), e);
        }

        return new PersistedAccessoryServer(element.getAddress(), element.getPort(), element.getPairingId(),
                element.getSecretKey(), element.getConfigurationIndex(), accessories, AccessoryCategory.BRIDGES,
                serverType);
    }
}
