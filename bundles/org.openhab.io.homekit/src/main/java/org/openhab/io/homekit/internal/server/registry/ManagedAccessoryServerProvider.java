package org.openhab.io.homekit.internal.server.registry;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNull;
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
import org.openhab.io.homekit.internal.accessory.AccessoryUID;
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
@Component(immediate = true, service = { AccessoryServerProvider.class,
        ManagedAccessoryServerProvider.class }, configurationPid = "org.openhab.homekit")
public class ManagedAccessoryServerProvider
        extends AbstractManagedProvider<AccessoryServer, AccessoryServerUID, PersistedAccessoryServer>
        implements AccessoryServerProvider {

    private final Logger logger = LoggerFactory.getLogger(ManagedAccessoryServerProvider.class);

    private static final String HOMEKIT_MANAGED_ACCESSORY_SERVER_PROVIDER = "homekit.managedAccessoryServerProvider";

    private final ReadyService readyService;
    private final AccessoryRegistry accessoryRegistry;
    private final PairingRegistry pairingRegistry;
    private final MDNSService mdnsService;

    @Activate
    public ManagedAccessoryServerProvider(@Reference StorageService storageService,
            @Reference ReadyService readyService, @Reference AccessoryRegistry accessoryRegistry,
            @Reference PairingRegistry pairingRegistry, @Reference MDNSService mdnsService) {
        super(storageService);
        this.readyService = readyService;
        this.accessoryRegistry = accessoryRegistry;
        this.pairingRegistry = pairingRegistry;
        this.mdnsService = mdnsService;

        logger.warn("Marking the Managed Accessory Server Provider as ready");
        ReadyMarker newMarker = new ReadyMarker(HOMEKIT_MANAGED_ACCESSORY_SERVER_PROVIDER, this.toString());
        readyService.markReady(newMarker);
    }

    @Override
    protected String getStorageName() {
        return AccessoryServer.class.getName();
    }

    @Override
    protected @NonNull String keyToString(@NonNull AccessoryServerUID key) {
        return key.getAsString();
    }

    @Override
    protected AccessoryServer toElement(@NonNull String key, @NonNull PersistedAccessoryServer persistableElement) {
        try {

            AccessoryServer server = null;
            if (persistableElement.getServerType() == PersistedAccessoryServer.ServerType.REMOTE) {
                server = new RemoteAccessoryServer(persistableElement.getCategory(),
                        persistableElement.getLocalAddress(), persistableElement.getPort(),
                        persistableElement.getPairingIdentifier(), persistableElement.getPrivateKey(),
                        accessoryRegistry, pairingRegistry);
            } else {
                server = new LocalAccessoryServer(persistableElement.getCategory(),
                        persistableElement.getLocalAddress(), persistableElement.getPort(),
                        persistableElement.getPairingIdentifier(), persistableElement.getPrivateKey(), mdnsService,
                        accessoryRegistry, pairingRegistry);
            }

            if (server != null) {
                logger.debug("Created an Accessory Server {} with Setup Code {}", server.getUID(),
                        server.getSetupCode());

                if (accessoryRegistry != null) {
                    Collection<String> accessoryUIDs = persistableElement.getAccessoryUIDs();
                    for (String accessoryUID : accessoryUIDs) {
                        Accessory accessory = accessoryRegistry.get(new AccessoryUID(accessoryUID));
                        if (accessory != null) {
                            server.addAccessory(accessory);
                        }
                    }
                }

                return server;
            } else {
                logger.warn("Unable to create an Accessory Server");
                return null;
            }
        } catch (Exception e) {
            logger.warn("Error creating Accessory Server", e);
            return null;
        }
    }

    @Override
    protected @NonNull PersistedAccessoryServer toPersistableElement(@NonNull AccessoryServer element) {
        PersistedAccessoryServer.ServerType serverType = element instanceof LocalAccessoryServer
                ? PersistedAccessoryServer.ServerType.LOCAL
                : PersistedAccessoryServer.ServerType.REMOTE;
        return new PersistedAccessoryServer(element.getAddress(), element.getPort(), element.getPairingId(),
                element.getSecretKey(), element.getConfigurationIndex(), element.getAccessories(),
                AccessoryCategory.BRIDGES, serverType);
    }
}
