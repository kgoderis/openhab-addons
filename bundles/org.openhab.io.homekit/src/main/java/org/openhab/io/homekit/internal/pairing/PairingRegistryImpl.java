/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.io.homekit.internal.pairing;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.AbstractRegistry;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyMarkerFilter;
import org.openhab.core.service.ReadyService;
import org.openhab.io.homekit.api.Pairing;
import org.openhab.io.homekit.api.PairingProvider;
import org.openhab.io.homekit.api.PairingRegistry;
import org.openhab.io.homekit.util.Byte;
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
 * Stores the created HomekitPairing
 *
 * @author Karel Goderis - Initial Contribution
 */
@NonNullByDefault
@Component(immediate = true, service = PairingRegistry.class)
public class PairingRegistryImpl extends AbstractRegistry<Pairing, PairingUID, PairingProvider>
        implements PairingRegistry, ReadyService.ReadyTracker {

    private final Logger logger = LoggerFactory.getLogger(PairingRegistryImpl.class);

    private static final String HOMEKIT_PAIRING_REGISTRY = "homekit.pairingRegistry";
    private static final String HOMEKIT_MANAGED_PAIRING_PROVIDER = "homekit.managedPairingProvider";
    private static final String HOMEKIT_ACCESSORY_SERVER_REGISTRY = "homekit.accessoryServerRegistry";

    private final ReadyService readyService;
    private boolean accessoryServerRegistryReady = false;
    private boolean managedPairingProviderReady = false;

    @Activate
    public PairingRegistryImpl(@Reference ReadyService readyService) {
        super(PairingProvider.class);
        this.readyService = readyService;

        readyService.registerTracker(this, new ReadyMarkerFilter().withType(HOMEKIT_MANAGED_PAIRING_PROVIDER)
                .withType(HOMEKIT_ACCESSORY_SERVER_REGISTRY));
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setManagedProvider(ManagedPairingProvider provider) {
        super.setManagedProvider(provider);
    }

    protected void unsetManagedProvider(ManagedPairingProvider provider) {
        super.unsetManagedProvider(provider);
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

    @Override
    public Collection<Pairing> get(byte[] pairingId) {
        return getAll().stream().filter(p -> Arrays.equals(p.getUID().getAccessoryPairingId(), pairingId))
                .collect(Collectors.toList());
    }

    @Override
    protected void addProvider(Provider<Pairing> provider) {

        logger.debug("Adding Provider {}", provider.toString());

        ReadyMarker newMarker = new ReadyMarker(HOMEKIT_MANAGED_PAIRING_PROVIDER, provider.toString());

        if (provider instanceof ManagedPairingProvider) {
            if (readyService.isReady(newMarker)) {
                super.addProvider(provider);
                managedPairingProviderReady = true;
            }
        } else {
            super.addProvider(provider);
        }
    }

    @Override
    public void onReadyMarkerAdded(ReadyMarker readyMarker) {
        logger.debug("Receiving the ready marker {}:{}", readyMarker.getType(), readyMarker.getIdentifier());

        if (readyMarker.getType() == HOMEKIT_ACCESSORY_SERVER_REGISTRY) {
            accessoryServerRegistryReady = true;
        }

        if (readyMarker.getType() == HOMEKIT_MANAGED_PAIRING_PROVIDER) {
            if (getManagedProvider().isPresent()) {
                super.addProvider(getManagedProvider().get());
            }
            managedPairingProviderReady = true;
        }

        if (accessoryServerRegistryReady && managedPairingProviderReady) {
            for (Pairing aPairing : getAll()) {
                logger.debug("Pairing {} with Public Key {} is available in the Pairing Registry", aPairing.getUID(),
                        Byte.toHexString(aPairing.getDestinationPublicKey()));
            }

            logger.warn("Marking the Pairing Registry as ready");
            ReadyMarker newMarker = new ReadyMarker(HOMEKIT_PAIRING_REGISTRY, this.toString());
            readyService.markReady(newMarker);
        }

    }

    @Override
    public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
        // TODO Auto-generated method stub

    }
}
