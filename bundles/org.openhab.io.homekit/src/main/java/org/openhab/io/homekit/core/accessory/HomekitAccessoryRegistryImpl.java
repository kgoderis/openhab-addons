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
package org.openhab.io.homekit.core.accessory;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.AbstractRegistry;
import org.openhab.core.common.registry.ManagedProvider;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyMarkerFilter;
import org.openhab.core.service.ReadyService;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.provider.HomekitAccessoryProvider;
import org.openhab.io.homekit.api.registry.HomekitAccessoryRegistry;
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
@Component(immediate = true, service = HomekitAccessoryRegistry.class)
public class HomekitAccessoryRegistryImpl extends AbstractRegistry<HomekitAccessory, HomekitAccessoryUID, HomekitAccessoryProvider>
        implements HomekitAccessoryRegistry, ReadyService.ReadyTracker {

    private final Logger logger = LoggerFactory.getLogger(HomekitAccessoryRegistryImpl.class);

    private static final String HOMEKIT_MANAGED_ACCESSORY_PROVIDER = "homekit.managedAccessoryProvider";
    private static final String HOMEKIT_ACCESSORY_REGISTRY = "homekit.accessoryRegistry";

    // ========== Log HomekitMessage Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit Registry: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "HomekitAccessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    private final ReadyService readyService;

    @Activate
    public HomekitAccessoryRegistryImpl(@Reference ReadyService readyService) {
        super(HomekitAccessoryProvider.class);
        this.readyService = readyService;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    @Override
    protected void setManagedProvider(ManagedProvider<HomekitAccessory, HomekitAccessoryUID> provider) {
        super.setManagedProvider(provider);
    }

    @Override
    protected void unsetManagedProvider(ManagedProvider<HomekitAccessory, HomekitAccessoryUID> provider) {
        super.unsetManagedProvider(provider);
    }

    @Override
    @Activate
    protected void activate(final BundleContext context) {
        super.activate(context);
        logger.debug("{}Activating HomekitAccessory Registry", LOG_INIT);
        readyService.registerTracker(this, new ReadyMarkerFilter().withType(HOMEKIT_MANAGED_ACCESSORY_PROVIDER));
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
        logger.debug("{}Deactivating HomekitAccessory Registry", LOG_INIT);
        readyService.unregisterTracker(this);
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

    @Override
    protected void addProvider(Provider<HomekitAccessory> provider) {
        logger.debug("{}Adding provider: {}", LOG_CONFIG, provider.toString());

        ReadyMarker newMarker = new ReadyMarker(HOMEKIT_MANAGED_ACCESSORY_PROVIDER, provider.toString());

        if (provider instanceof Provider<HomekitAccessory>) {
            if (readyService.isReady(newMarker)) {
                addProviderWithReadyMarker(provider);
            }
        } else {
            super.addProvider(provider);
        }
    }

    public synchronized void addProviderWithReadyMarker(Provider<HomekitAccessory> provider) {
        super.addProvider(provider);

        for (HomekitAccessory accessory : getAll()) {
            logger.debug("{}HomekitAccessory available in registry - UID: {}", LOG_ACCESSORY, accessory.getUID());
        }

        logger.info("{}Marking HomekitAccessory Registry as ready", LOG_STATE);
        ReadyMarker newMarker = new ReadyMarker(HOMEKIT_ACCESSORY_REGISTRY, this.toString());
        readyService.markReady(newMarker);
    }
}
