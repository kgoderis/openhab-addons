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
import org.openhab.io.homekit.api.hap.HomekitPairing;
import org.openhab.io.homekit.api.provider.HomekitPairingProvider;
import org.openhab.io.homekit.api.registry.HomekitPairingRegistry;
import org.openhab.io.homekit.util.HomekitByte;
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
@Component(immediate = true, service = HomekitPairingRegistry.class)
public class HomekitPairingRegistryImpl extends AbstractRegistry<HomekitPairing, HomekitPairingUID, HomekitPairingProvider>
        implements HomekitPairingRegistry, ReadyService.ReadyTracker {

    private final Logger logger = LoggerFactory.getLogger(HomekitPairingRegistry.class);

    // ========== Log HomekitMessage Prefixes ==========
    protected static final String LOG_PREFIX = "HomeKit HomekitPairingRegistry: ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    private static final String HOMEKIT_PAIRING_REGISTRY = "homekit.pairingRegistry";
    private static final String HOMEKIT_MANAGED_PAIRING_PROVIDER = "homekit.managedPairingProvider";
    private static final String HOMEKIT_ACCESSORY_SERVER_REGISTRY = "homekit.accessoryServerRegistry";

    private final ReadyService readyService;
    private boolean accessoryServerRegistryReady = false;
    private boolean managedPairingProviderReady = false;

    @Activate
    public HomekitPairingRegistryImpl(@Reference ReadyService readyService) {
        super(HomekitPairingProvider.class);
        this.readyService = readyService;

        readyService.registerTracker(this, new ReadyMarkerFilter().withType(HOMEKIT_MANAGED_PAIRING_PROVIDER)
                .withType(HOMEKIT_ACCESSORY_SERVER_REGISTRY));
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setManagedProvider(HomekitManagedPairingProvider provider) {
        super.setManagedProvider(provider);
    }

    protected void unsetManagedProvider(HomekitManagedPairingProvider provider) {
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
    public Collection<HomekitPairing> get(byte[] pairingId) {
        return getAll().stream().filter(p -> Arrays.equals(p.getUID().getSourcePairingId(), pairingId))
                .collect(Collectors.toList());
    }

    @Override
    protected void addProvider(Provider<HomekitPairing> provider) {

        logger.debug("{}Adding Provider {}", LOG_STATE, provider.toString());

        ReadyMarker newMarker = new ReadyMarker(HOMEKIT_MANAGED_PAIRING_PROVIDER, provider.toString());

        if (provider instanceof HomekitManagedPairingProvider) {
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
        logger.debug("{}Receiving the ready marker {}:{}", LOG_STATE, readyMarker.getType(),
                readyMarker.getIdentifier());

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
            for (HomekitPairing aPairing : getAll()) {
                logger.debug("{}HomekitPairing {} with Public Key {} is available in the HomekitPairing Registry", LOG_STATE,
                        aPairing.getUID(), HomekitByte.toHexString(aPairing.getPublicKey()));
            }

            logger.warn("{}Marking the HomekitPairing Registry as ready", LOG_WARN);
            ReadyMarker newMarker = new ReadyMarker(HOMEKIT_PAIRING_REGISTRY, this.toString());
            readyService.markReady(newMarker);
        }
    }

    @Override
    public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
        // TODO Auto-generated method stub
    }
}
