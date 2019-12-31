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
package org.openhab.io.homekit.internal.accessory;

import java.util.Collection;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.AbstractRegistry;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyMarkerFilter;
import org.openhab.core.service.ReadyService;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.api.AccessoryProvider;
import org.openhab.io.homekit.api.AccessoryRegistry;
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
 * Stores the created Accessories
 *
 * @author Karel Goderis - Initial Contribution
 */
@NonNullByDefault
@Component(immediate = true, service = AccessoryRegistry.class)
public class AccessoryRegistryImpl extends AbstractRegistry<Accessory, AccessoryUID, AccessoryProvider>
        implements AccessoryRegistry, ReadyService.ReadyTracker {

    private final Logger logger = LoggerFactory.getLogger(AccessoryRegistry.class);

    private static final String HOMEKIT_MANAGED_ACCESSORY_PROVIDER = "homekit.managedAccessoryProvider";
    private static final String HOMEKIT_ACCESSORY_REGISTRY = "homekit.accessoryRegistry";

    private final ReadyService readyService;

    @Activate
    public AccessoryRegistryImpl(@Reference ReadyService readyService) {
        super(AccessoryProvider.class);
        this.readyService = readyService;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setManagedProvider(ManagedAccessoryProvider provider) {
        super.setManagedProvider(provider);
    }

    protected void unsetManagedProvider(ManagedAccessoryProvider provider) {
        super.unsetManagedProvider(provider);
    }

    @Override
    @Activate
    protected void activate(final BundleContext context) {
        super.activate(context);

        readyService.registerTracker(this, new ReadyMarkerFilter().withType(HOMEKIT_MANAGED_ACCESSORY_PROVIDER));
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();

        readyService.unregisterTracker(this);
    }

    @Override
    public Collection<Accessory> get(String serverId) {
        return getAll().stream().filter(a -> a.getServer().getId().equals(serverId)).collect(Collectors.toList());
    }

    @Override
    public void added(Provider<Accessory> provider, Accessory element) {
        super.added(provider, element);
        element.getServer().advertise();
    }

    @Override
    public void removed(Provider<Accessory> provider, Accessory element) {
        super.removed(provider, element);
        element.getServer().advertise();
    }

    @Override
    public void updated(Provider<Accessory> provider, Accessory oldElement, Accessory element) {
        super.updated(provider, oldElement, element);
        element.getServer().advertise();
    }

    @Override
    public void onReadyMarkerAdded(ReadyMarker readyMarker) {

        logger.debug("Receiving the ready marker {}:{}", readyMarker.getType(), readyMarker.getIdentifier());

        if (getManagedProvider().isPresent()) {
            addProviderWithReadyMarker(getManagedProvider().get());
        }
    }

    @Override
    public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
    }

    @Override
    protected void addProvider(Provider<Accessory> provider) {
        // if (provider instanceof ManagedAccessoryProvider) {
        // // Skip, only do this when we get a readyMarker
        // logger.warn("Delaying adding the Managed Accessory Provider");
        // } else {
        // super.addProvider(provider);
        // }

        logger.debug("Adding Provider {}", provider.toString());

        ReadyMarker newMarker = new ReadyMarker(HOMEKIT_MANAGED_ACCESSORY_PROVIDER, provider.toString());

        if (provider instanceof ManagedAccessoryProvider) {
            if (readyService.isReady(newMarker)) {
                addProviderWithReadyMarker(provider);
            }
        } else {
            super.addProvider(provider);
        }
    }

    public synchronized void addProviderWithReadyMarker(Provider<Accessory> provider) {
        super.addProvider(provider);

        for (Accessory accessory : getAll()) {
            logger.debug("Accessory {} is available in the Accessory Registry", accessory.getUID());
        }

        logger.warn("Marking the Accessory Registry as ready");
        ReadyMarker newMarker = new ReadyMarker(HOMEKIT_ACCESSORY_REGISTRY, this.toString());
        readyService.markReady(newMarker);
    }

    // @Override
    // public @Nullable Accessory get(ThingUID uid) {
    // return getAll().stream().filter(a -> a.getThingUID() == uid).findFirst().get();
    // }

}
