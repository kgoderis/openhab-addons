package org.openhab.io.homekit.internal.accessory;

import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractManagedProvider;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyMarkerFilter;
import org.openhab.core.service.ReadyService;
import org.openhab.core.storage.StorageService;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingUID;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.AccessoryProvider;
import org.openhab.io.homekit.api.AccessoryRegistry;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.api.AccessoryServerRegistry;
import org.openhab.io.homekit.api.Characteristic;
import org.openhab.io.homekit.api.HomekitFactory;
import org.openhab.io.homekit.api.ManagedAccessory;
import org.openhab.io.homekit.api.ManagedCharacteristic;
import org.openhab.io.homekit.api.ManagedService;
import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.internal.server.AccessoryServerUID;
import org.openhab.io.homekit.internal.server.BridgeAccessoryServer;
import org.openhab.io.homekit.library.accessory.ThingAccessory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ManagedAccessoryProvider} is an OSGi service, that allows to add or remove Accessories at runtime by calling
 * {@link ManagedAccessoryProvider#addAccessory(ManagedAccessory)} or
 * {@link ManagedAccessoryProvider#removeAccessory(ManagedAccessory)}. An added Accessory is automatically exposed to
 * the
 * {@link AccessoryRegistry}. Persistence of added Accessories is handled by a {@link StorageService}. Accessories are
 * being restored using the given {@link HomekitFactory}s.
 *
 **/
@Component(immediate = true, service = { AccessoryProvider.class, ManagedAccessoryProvider.class })
public class ManagedAccessoryProvider
        extends AbstractManagedProvider<ManagedAccessory, AccessoryUID, PersistedAccessory>
        implements AccessoryProvider, ReadyService.ReadyTracker {

    private final Logger logger = LoggerFactory.getLogger(ManagedAccessoryProvider.class);

    static final String HOMEKIT_ACCESSORY_SERVER_REGISTRY = "homekit.accessoryServerRegistry";
    static final String HOMEKIT_MANAGED_ACCESSORY_PROVIDER = "homekit.managedAccessoryProvider";
    private static final long INITIALIZATION_DELAY_NANOS = TimeUnit.SECONDS.toNanos(5);

    private final Collection<HomekitFactory> homekitFactories = new CopyOnWriteArrayList<>();

    private final HomekitCommunicationManager homekitCommunicationManager;
    private final AccessoryServerRegistry accessoryServerRegistry;
    private final ThingRegistry thingRegistry;
    private ReadyService readyService;

    private volatile long lastUpdate = System.nanoTime();
    private @Nullable ScheduledExecutorService executor;

    @Activate
    public ManagedAccessoryProvider(@Reference StorageService storageService,
            @Reference HomekitCommunicationManager homekitCommunicationManager,
            @Reference AccessoryServerRegistry accessoryServerRegistry, @Reference ThingRegistry thingRegistry,
            @Reference ReadyService readyService) {
        super(storageService);
        this.homekitCommunicationManager = homekitCommunicationManager;
        this.accessoryServerRegistry = accessoryServerRegistry;
        this.thingRegistry = thingRegistry;
        this.readyService = readyService;

        readyService.registerTracker(this, new ReadyMarkerFilter().withType(HOMEKIT_ACCESSORY_SERVER_REGISTRY));
    }

    @Deactivate
    protected synchronized void deactivate(ComponentContext componentContext) {
        readyService.unregisterTracker(this);
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

            logger.info("Marking the Managed Accessory Provider as ready");
            ReadyMarker newMarker = new ReadyMarker(HOMEKIT_MANAGED_ACCESSORY_PROVIDER, this.toString());
            readyService.markReady(newMarker);
        }
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
    protected String getStorageName() {
        return ManagedAccessory.class.getName();
    }

    @Override
    protected @NonNull String keyToString(@NonNull AccessoryUID key) {
        return key.getAsString();
    }

    @Override
    protected ManagedAccessory toElement(@NonNull String key, @NonNull PersistedAccessory persistableElement) {

        try {
            JsonReader jsonReader = Json.createReader(new StringReader(persistableElement.getJson()));
            JsonObject jsonObject = jsonReader.readObject();
            jsonReader.close();

            AccessoryServer server = accessoryServerRegistry.get(new AccessoryServerUID(
                    BridgeAccessoryServer.class.getSimpleName(), persistableElement.getServerId()));

            ManagedAccessory accessory = null;

            if (server != null) {

                long aid = jsonObject.getJsonNumber("aid").longValue();
                JsonArray services = (JsonArray) jsonObject.get("services");

                Class<?> clazz;
                try {
                    clazz = Class.forName(persistableElement.getAccessoryClass());
                } catch (ClassNotFoundException e) {
                    logger.warn(
                            "Unable to find Accessory class {}, and will revert to the default ThingAccessory class",
                            persistableElement.getAccessoryClass());
                    clazz = ThingAccessory.class;
                }

                try {
                    accessory = (ManagedAccessory) clazz.getConstructor(HomekitCommunicationManager.class,
                            AccessoryServer.class, long.class, boolean.class)
                            .newInstance(homekitCommunicationManager, server, aid, false);
                } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                if (accessory != null) {
                    logger.debug("Created an Accessory {} of Type {} with instanceIdPool {}", accessory.getUID(),
                            clazz.getSimpleName(), accessory.getCurrentInstanceId());

                    for (JsonValue service : services) {
                        long iid = ((JsonObject) service).getJsonNumber("iid").longValue();
                        final String serviceType = ((JsonObject) service).getString("type");

                        if (accessory.getService(serviceType) == null) {

                            HomekitFactory factory = homekitFactories.stream()
                                    .filter(f -> f.supportsServiceType(serviceType)).findFirst().orElse(null);

                            if (factory != null) {
                                ManagedService newService = factory.createService(serviceType, accessory, iid, false);

                                if (newService != null) {
                                    accessory.addService(newService);
                                    JsonArray characteristics = (JsonArray) ((JsonObject) service)
                                            .get("characteristics");

                                    for (JsonValue characteristic : characteristics) {
                                        iid = ((JsonObject) characteristic).getJsonNumber("iid").longValue();
                                        final String characteristicType = ((JsonObject) characteristic)
                                                .getString("type");

                                        if (newService.getCharacteristic(characteristicType) == null) {

                                            factory = homekitFactories.stream()
                                                    .filter(f -> f.supportsCharacteristicsType(characteristicType))
                                                    .findFirst().orElse(null);

                                            if (factory != null) {
                                                ManagedCharacteristic<?> newCharacteristic = factory
                                                        .createCharacteristic(characteristicType, newService, iid);
                                                if (newCharacteristic != null) {
                                                    newService.addCharacteristic(newCharacteristic);
                                                } else {
                                                    logger.warn(
                                                            "Homekit Factory {} could not create a Characteristic of Type {}",
                                                            factory.toString(), characteristicType);
                                                }
                                            } else {
                                                logger.warn("No Homekit Factory can create a Characteristic of Type {}",
                                                        characteristicType);
                                            }
                                        } else {
                                            if (newService.getCharacteristic(characteristicType) != null) {
                                                logger.info(
                                                        "Service {} of Type {} already holds Characteristic {} of Type {}",
                                                        newService.getUID(), newService.getClass().getSimpleName(),
                                                        ((ManagedCharacteristic<?>) newService
                                                                .getCharacteristic(characteristicType)).getUID(),
                                                        newService.getCharacteristic(characteristicType).getClass()
                                                                .getSimpleName());
                                            }
                                        }
                                    }
                                } else {
                                    logger.warn("Homekit Factory {} could not create a Service of Type {}",
                                            factory.toString(), serviceType);
                                }
                            } else {
                                logger.warn("No Homekit Factory can create a Service of Type {}", serviceType);
                            }
                        } else {
                            if (accessory.getService(serviceType) != null) {
                                logger.info("Accessory {} of Type {} already holds Service {} of Type {}",
                                        accessory.getUID(), accessory.getClass().getSimpleName(),
                                        ((ManagedService) accessory.getService(serviceType)).getUID(),
                                        accessory.getService(serviceType).getClass().getSimpleName());
                            }
                        }
                    }

                    if (accessory instanceof ThingAccessory) {
                        ThingUID thingUID = new ThingUID(persistableElement.getThingUID());
                        Thing thing = thingRegistry.get(thingUID);

                        if (thing != null) {
                            HomekitFactory factory = homekitFactories.stream()
                                    .filter(f -> f.supportsThingType(thing.getThingTypeUID())).findFirst().orElse(null);

                            if (factory != null) {
                                ((ThingAccessory) accessory).setThingUID(thingUID);
                                logger.info("Linked Thing {} ({}) to Accessory {} of Type {}",
                                        persistableElement.getThingUID(), thing.getLabel(), accessory.getUID(),
                                        accessory.getClass().getSimpleName());

                                for (Channel channel : thing.getChannels()) {
                                    HashSet<String> characteristicTypes = factory
                                            .getCharacteristicTypes(channel.getChannelTypeUID());

                                    for (Service aService : accessory.getServices()) {
                                        for (Characteristic aCharacteristic : aService.getCharacteristics()) {
                                            for (String aCharacteristicType : characteristicTypes) {
                                                if (aCharacteristic.isType(aCharacteristicType)) {
                                                    ((ManagedCharacteristic<?>) aCharacteristic)
                                                            .setChannelUID(channel.getUID());
                                                    logger.debug(
                                                            "Linked Channel {} ({}) to Characteristic {} of Type {}",
                                                            channel.getUID(), channel.getLabel(),
                                                            ((ManagedCharacteristic<?>) aCharacteristic).getUID(),
                                                            aCharacteristic.getClass().getSimpleName());
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                logger.warn("There is no Homekit Factory that supports ThingType {}",
                                        persistableElement.getThingUID());
                            }
                        } else {
                            logger.warn(
                                    "The Thing {} linked to to Accessory {} could not be found in the Thing Registry",
                                    persistableElement.getThingUID(), accessory.getUID());
                            // TODO : Remove from accessory registry? what if thingregistry is not ready?
                        }
                    }

                    // server.addAccessory(accessory);
                }
            } else {
                AccessoryUID uid = new AccessoryUID(key);

                logger.warn("Accessory Server {} hosting Accessory {} was not found in the Accessory Server Registry",
                        new AccessoryServerUID(BridgeAccessoryServer.class.getSimpleName(),
                                persistableElement.getServerId()).toString(),
                        uid);

                this.remove(uid);
            }

            return accessory;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected PersistedAccessory toPersistableElement(ManagedAccessory element) {

        String thingUID = "";
        if (element instanceof ThingAccessory) {
            thingUID = ((ThingAccessory) element).getThingUID().toString();
        }
        return new PersistedAccessory(element.getClass().getName(), element.toJson().toString(),
                element.getServer().getId(), element.getCurrentInstanceId(), thingUID);
    }

    @Override
    public void onReadyMarkerAdded(@NonNull ReadyMarker readyMarker) {

        logger.debug("Receiving the ready marker {}:{}", readyMarker.getType(), readyMarker.getIdentifier());

        delayedInitialize();
    }

    @Override
    public void onReadyMarkerRemoved(@NonNull ReadyMarker readyMarker) {
        ReadyMarker newMarker = new ReadyMarker(HOMEKIT_MANAGED_ACCESSORY_PROVIDER, this.toString());
        readyService.unmarkReady(newMarker);
    }

}
