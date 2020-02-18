/**
 *
 */
package org.openhab.io.homekit;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyMarkerFilter;
import org.openhab.core.service.ReadyService;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingRegistryChangeListener;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.api.AccessoryRegistry;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.api.AccessoryServerRegistry;
import org.openhab.io.homekit.api.HomekitFactory;
import org.openhab.io.homekit.internal.client.HomekitBindingConstants;
import org.openhab.io.homekit.library.accessory.ThingAccessory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author kgoderis
 *
 */
@Component(immediate = true, service = { ThingRegistryChangeListener.class })
public class HomekitThingRegistryChangeListener implements ThingRegistryChangeListener, ReadyService.ReadyTracker {

    private final Logger logger = LoggerFactory.getLogger(HomekitThingRegistryChangeListener.class);

    private static final String HOMEKIT_ACCESSORY_REGISTRY = "homekit.accessoryRegistry";
    private static final String HOMEKIT_THING_REGISTRY_CHANGE_LISTENER = "homekit.homekitThingRegistryChangeListener";
    private static final long INITIALIZATION_DELAY_NANOS = TimeUnit.SECONDS.toNanos(5);

    private final ThingRegistry thingRegistry;
    private final AccessoryRegistry accessoryRegistry;
    private final AccessoryServerRegistry serverRegistry;
    private final ReadyService readyService;
    private final Collection<HomekitFactory> homekitFactories = new CopyOnWriteArrayList<>();

    private volatile boolean initialized = false;
    private volatile long lastUpdate = System.nanoTime();
    private @Nullable ScheduledExecutorService executor;

    @Activate
    public HomekitThingRegistryChangeListener(@Reference ThingRegistry thingRegistry,
            @Reference AccessoryRegistry accessoryRegistry, @Reference AccessoryServerRegistry serverRegistry,
            @Reference ReadyService readyService) {
        this.thingRegistry = thingRegistry;
        this.accessoryRegistry = accessoryRegistry;
        this.serverRegistry = serverRegistry;
        this.readyService = readyService;
        this.executor = Executors.newSingleThreadScheduledExecutor();

        thingRegistry.addRegistryChangeListener(this);
        readyService.registerTracker(this, new ReadyMarkerFilter().withType(HOMEKIT_ACCESSORY_REGISTRY));
    }

    @Deactivate
    public void deactivate() {
        thingRegistry.removeRegistryChangeListener(this);
    }

    @SuppressWarnings("null")
    private synchronized void delayedInitialize() {
        if (executor == null) {
            executor = Executors.newSingleThreadScheduledExecutor();
        }

        if (Thread.currentThread().isInterrupted()) {
            return;
        }

        final long diff = System.nanoTime() - lastUpdate - INITIALIZATION_DELAY_NANOS;
        if (diff < 0) {
            executor.schedule(() -> delayedInitialize(), -diff, TimeUnit.NANOSECONDS);
        } else {
            executor.shutdown();
            executor = null;

            initialize();
        }
    }

    private synchronized void initialize() {
        logger.debug("Initializing Accessories");

        initialized = true;

        for (Thing aThing : thingRegistry.getAll()) {
            logger.debug("Initializing an Accessory for Thing {}", aThing.getUID());
            added(aThing);
        }

        logger.debug("Advertising Accessory Servers");

        for (AccessoryServer server : serverRegistry.getAll()) {
            logger.debug("Advertising {} with Setup Code {} ", server.getUID(), server.getSetupCode());
            server.advertise();
        }

        logger.info("Marking the Homekit ThingRegistry Change Listener as ready");
        ReadyMarker newMarker = new ReadyMarker(HOMEKIT_THING_REGISTRY_CHANGE_LISTENER, this.toString());
        readyService.markReady(newMarker);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addHomekitFactory(HomekitFactory factory) {
        homekitFactories.add(factory);
        logger.info("Added a HomeKit Factory for Thing Types {}", Arrays.toString(factory.getSupportedThingTypes()));
        lastUpdate = System.nanoTime();
    }

    protected void removeHomekitFactory(HomekitFactory factory) {
        homekitFactories.remove(factory);
    }

    @Override
    public void added(@NonNull Thing thing) {
        if (!initialized) {
            return;
        }

        if (HomekitBindingConstants.BINDING_ID.equals(thing.getUID().getBindingId())) {
            return;
        }

        boolean accessoryExists = false;
        Accessory foundAccessory = null;
        for (Accessory accessory : accessoryRegistry.getAll()) {
            if (accessory instanceof ThingAccessory) {
                if (((ThingAccessory) accessory).getThingUID() != null) {
                    if (((ThingAccessory) accessory).getThingUID().toString().equals(thing.getUID().toString())) {
                        accessoryExists = true;
                        foundAccessory = accessory;
                        break;
                    }
                }
            }
        }

        if (!accessoryExists) {
            HomekitFactory factory = homekitFactories.stream().filter(f -> f.supportsThingType(thing.getThingTypeUID()))
                    .findFirst().orElse(null);
            AccessoryServer server = serverRegistry.getAvailableBridgeAccessoryServer();

            if (factory != null) {
                if (server != null) {
                    Accessory accessory;
                    try {
                        accessory = factory.createAccessory(thing, server);
                        if (accessory != null) {
                            logger.debug("Added an Accessory {} of Type {} for Thing {}", accessory.getUID(),
                                    accessory.getClass().getSimpleName(), thing.getUID());
                            server.addAccessory(accessory);
                        }
                    } catch (Exception e) {
                        logger.error("Exception adding an Accessory for Thing {} : {}", thing.getUID(), e.getMessage());
                    }
                } else {
                    logger.warn("There is no Accessory Server available");
                }
            } else {
                logger.warn("No Accessory Factory supports ThingType {}", thing.getThingTypeUID());
            }
        } else {
            logger.warn("The Accessory Registry already containts a Thing Accessory {} for Thing {}",
                    foundAccessory.getUID(), thing.getUID());
        }
    }

    @Override
    public void removed(@NonNull Thing thing) {
        if (!initialized) {
            return;
        }

        for (Accessory accessory : accessoryRegistry.getAll()) {
            if (accessory instanceof ThingAccessory) {
                if (((ThingAccessory) accessory).getThingUID().toString().equals(thing.getUID().toString())) {
                    AccessoryServer server = accessory.getServer();
                    server.removeAccessory(accessory);
                }
            }
        }
    }

    @Override
    public void updated(@NonNull Thing oldElement, @NonNull Thing element) {
        removed(oldElement);
        added(element);
    }

    @Override
    public void onReadyMarkerAdded(@NonNull ReadyMarker readyMarker) {
        logger.debug("Receiving the ready marker {}:{}", readyMarker.getType(), readyMarker.getIdentifier());

        delayedInitialize();
    }

    @Override
    public void onReadyMarkerRemoved(@NonNull ReadyMarker readyMarker) {
        // TODO Auto-generated method stub
    }
}
