package org.openhab.io.homekit.network.pairing;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.AbstractManagedProvider;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyService;
import org.openhab.core.storage.StorageService;
import org.openhab.io.homekit.api.provider.HomekitPairingProvider;
import org.openhab.io.homekit.api.registry.HomekitPairingRegistry;
import org.openhab.io.homekit.protocol.pairing.HomekitPairing;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * {@link ManagedPairingProvider} is an OSGi service, that allows to add or remove
 * pairings at runtime by calling {@link ManagedPairingProvider#addAccessory(HomekitPairing)} or
 * {@link ManagedPairingProvider#removeAccessory(HomekitPairing)}. An added HomekitPairing is automatically
 * exposed to the {@link HomekitPairingRegistry}. Persistence of added HomekitPairings is handled by
 * a {@link StorageService}.
 *
 **/
@NonNullByDefault
@Component(immediate = true, service = { HomekitPairingProvider.class, HomekitManagedPairingProvider.class })
public class HomekitManagedPairingProvider extends
        AbstractManagedProvider<HomekitPairing, HomekitPairingUID, HomekitPairing> implements HomekitPairingProvider {

    private static final String HOMEKIT_MANAGED_PAIRING_PROVIDER = "homekit.managedPairingProvider";

    private final ReadyService readyService;

    @Activate
    public HomekitManagedPairingProvider(@Reference StorageService storageService,
            @Reference ReadyService readyService) {
        super(storageService);
        this.readyService = readyService;

        ReadyMarker newMarker = new ReadyMarker(HOMEKIT_MANAGED_PAIRING_PROVIDER, this.toString());
        readyService.markReady(newMarker);
    }

    @Override
    protected String getStorageName() {
        return HomekitPairing.class.getName();
    }

    @Override
    protected @NonNull String keyToString(HomekitPairingUID key) {
        return key.getAsString();
    }

    @Override
    protected HomekitPairing toElement(String key, HomekitPairing persistableElement) {
        return persistableElement;
    }

    @Override
    protected HomekitPairing toPersistableElement(HomekitPairing element) {
        return element;
    }
}
