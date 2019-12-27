package org.openhab.io.homekit.internal.server;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.common.registry.AbstractManagedProvider;
import org.openhab.core.common.registry.ManagedProvider;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyService;
import org.openhab.core.storage.StorageService;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.api.AccessoryServerFactory;
import org.openhab.io.homekit.api.AccessoryServerProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
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

    private final Collection<AccessoryServerFactory> serverFactories = new CopyOnWriteArrayList<>();
    private final ReadyService readyService;

    @Activate
    public ManagedAccessoryServerProvider(@Reference StorageService storageService,
            @Reference ReadyService readyService) {
        super(storageService);
        this.readyService = readyService;
    }

    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
    public void addServerFactory(AccessoryServerFactory serverFactory) {
        serverFactories.add(serverFactory);

        logger.debug("Added an Accessory Server Factory that supports {}",
                Arrays.toString(serverFactory.getSupportedServerTypes()));

        if (Arrays.stream(serverFactory.getSupportedServerTypes())
                .anyMatch(BridgeAccessoryServer.class.getSimpleName()::equals)) {
            logger.warn("Marking the Managed Accessory Server Provider as ready");
            ReadyMarker newMarker = new ReadyMarker(HOMEKIT_MANAGED_ACCESSORY_SERVER_PROVIDER, this.toString());
            readyService.markReady(newMarker);
        }
    }

    public void removeServerFactory(AccessoryServerFactory serverFactory) {
        serverFactories.remove(serverFactory);
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

        for (AccessoryServerFactory factory : serverFactories) {

            AccessoryServer server = factory.createServer(BridgeAccessoryServer.class.getSimpleName(),
                    persistableElement.getLocalAddress(), persistableElement.getPort(),
                    persistableElement.getPairingIdentifier(), persistableElement.getSalt(),
                    persistableElement.getPrivateKey(), persistableElement.getConfigurationIndex());

            if (server != null) {
                logger.debug("Created an Accessory Server {} with Setup Code {}", server.getUID(),
                        server.getSetupCode());
                return server;
            } else {
                logger.warn("Unable to create an Accessory Server of Type {}",
                        BridgeAccessoryServer.class.getSimpleName());
                return null;
            }
        }

        logger.warn("There is no Acessory Server Factory for Accessory Servers of Type '{}'",
                BridgeAccessoryServer.class.getSimpleName());

        return null;
    }

    @Override
    protected PersistedAccessoryServer toPersistableElement(AccessoryServer element) {
        return new PersistedAccessoryServer(element.getLocalAddress(), element.getPort(), element.getId(),
                element.getSalt(), element.getPrivateKey(), element.getConfigurationIndex());
    }

}
