package org.openhab.io.homekit.internal.pairing;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.common.registry.AbstractManagedProvider;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyService;
import org.openhab.core.storage.StorageService;
import org.openhab.io.homekit.api.Pairing;
import org.openhab.io.homekit.api.PairingProvider;
import org.openhab.io.homekit.api.PairingRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * {@link ManagedPairingProvider} is an OSGi service, that allows to add or remove
 * pairings at runtime by calling {@link ManagedPairingProvider#addAccessory(Pairing)} or
 * {@link ManagedPairingProvider#removeAccessory(Pairing)}. An added HomekitPairing is automatically
 * exposed to the {@link PairingRegistry}. Persistence of added HomekitPairings is handled by
 * a {@link StorageService}.
 *
 **/
@Component(immediate = true, service = { PairingProvider.class, ManagedPairingProvider.class })
public class ManagedPairingProvider extends AbstractManagedProvider<Pairing, PairingUID, Pairing>
        implements PairingProvider {

    private static final String HOMEKIT_MANAGED_PAIRING_PROVIDER = "homekit.managedPairingProvider";

    private final ReadyService readyService;

    @Activate
    public ManagedPairingProvider(@Reference StorageService storageService, @Reference ReadyService readyService) {
        super(storageService);
        this.readyService = readyService;

        ReadyMarker newMarker = new ReadyMarker(HOMEKIT_MANAGED_PAIRING_PROVIDER, this.toString());
        readyService.markReady(newMarker);
    }

    @Override
    protected String getStorageName() {
        return Pairing.class.getName();
    }

    @Override
    protected @NonNull String keyToString(@NonNull PairingUID key) {
        return key.getAsString();
    }

    @Override
    protected Pairing toElement(@NonNull String key, @NonNull Pairing persistableElement) {
        return persistableElement;
    }

    @Override
    protected Pairing toPersistableElement(Pairing element) {
        return element;
    }

}
