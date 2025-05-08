package org.openhab.io.homekit.internal.accessory;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractManagedProvider;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyMarkerFilter;
import org.openhab.core.service.ReadyService;
import org.openhab.core.storage.StorageService;
import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.openhab.io.homekit.api.hap.HomekitAccessory;
import org.openhab.io.homekit.api.provider.HomekitAccessoryProvider;
import org.openhab.io.homekit.api.registry.HomekitAccessoryRegistry;
import org.openhab.io.homekit.api.registry.HomekitAccessoryServerRegistry;
import org.openhab.io.homekit.exception.HomekitException;
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
 * {@link HomekitPersistedAccessoryProvider} is an OSGi service, that allows to add or remove Accessories at runtime by calling
 * {@link HomekitPersistedAccessoryProvider#addAccessory(HomekitAccessory)} or
 * {@link HomekitPersistedAccessoryProvider#removeAccessory(HomekitAccessory)}. An added HomekitAccessory is automatically exposed to
 * the
 * {@link HomekitAccessoryRegistry}. Persistence of added Accessories is handled by a {@link StorageService}. Accessories are
 * being restored using the given {@link HomekitFactory}s.
 *
 **/
@NonNullByDefault
@Component(immediate = true, service = { HomekitPersistedAccessoryProvider.class, HomekitPersistedAccessoryProvider.class })
public class HomekitPersistedAccessoryProvider
        extends AbstractManagedProvider<HomekitAccessory, HomekitAccessoryUID, HomekitPersistedAccessory>
        implements HomekitAccessoryProvider, ReadyService.ReadyTracker {

    // ========== Constants ==========
    private static final Logger logger = LoggerFactory.getLogger(HomekitPersistedAccessoryProvider.class);

    // ========== Log HomekitMessage Prefixes ==========
    private static final String LOG_PREFIX = "Homekit HomekitAccessory Provider: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_ACCESSORY = LOG_PREFIX + "HomekitAccessory - ";
    private static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    private static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    static final String HOMEKIT_ACCESSORY_SERVER_REGISTRY = "homekit.accessoryServerRegistry";
    static final String HOMEKIT_MANAGED_ACCESSORY_PROVIDER = "homekit. HomekitAccessoryProvider";
    private static final long INITIALIZATION_DELAY_NANOS = TimeUnit.SECONDS.toNanos(5);

    private final Collection<HomekitFactory> homekitFactories = new CopyOnWriteArrayList<>();

    private final ReadyService readyService;

    private volatile long lastUpdate = System.nanoTime();
    private @Nullable ScheduledExecutorService executor;

    @Activate
    public HomekitPersistedAccessoryProvider(@Reference StorageService storageService,
            @Reference HomekitAccessoryServerRegistry accessoryServerRegistry, @Reference ReadyService readyService) {
        super(storageService);
        this.readyService = readyService;

        final HomekitPersistedAccessoryProvider self = this;
        readyService.registerTracker(self, new ReadyMarkerFilter().withType(HOMEKIT_ACCESSORY_SERVER_REGISTRY));
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

            logger.info("{}Marking the Managed HomekitAccessory Provider as ready", LOG_INIT);
            ReadyMarker newMarker = new ReadyMarker(HOMEKIT_MANAGED_ACCESSORY_PROVIDER, this.toString());
            readyService.markReady(newMarker);
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addHomekitFactory(HomekitFactory factory) {
        homekitFactories.add(factory);
        logger.info("{}Added a Homekit Factory for Thing Types {}", LOG_INIT,
                Arrays.toString(factory.getSupportedThingTypes()));
        lastUpdate = System.nanoTime();
    }

    protected void removeHomekitFactory(HomekitFactory factory) {
        homekitFactories.remove(factory);
    }

    @Override
    protected String getStorageName() {
        return org.openhab.io.homekit.api.hap.HomekitAccessory.class.getName();
    }

    @Override
    protected @NonNull String keyToString(HomekitAccessoryUID key) {
        return key.toString();
    }

    @Override
    protected @Nullable HomekitAccessory toElement(String key, HomekitPersistedAccessory persistableElement) {

        HomekitAccessory accessory = null;

        try {
            JsonObject jsonObject = null;
            try (JsonReader jsonReader = Json.createReader(new StringReader(persistableElement.getJson()))) {
                jsonObject = jsonReader.readObject();
            }

            Class<?> loadedClass = null;
            try {
                loadedClass = Class.forName(persistableElement.getAccessoryClass());
            } catch (ClassNotFoundException e) {
                logger.warn("Could not find accessory class {}. Falling back to HomekitGenericAccessory.",
                        persistableElement.getAccessoryClass());
            }

            if (loadedClass == null) {
                loadedClass = HomekitBaseAccessory.class;
            }

            if (loadedClass != null) {
                if (HomekitAccessory.class.isAssignableFrom(loadedClass)) {
                    final Class<? extends HomekitAccessory> clazz = loadedClass.asSubclass(HomekitAccessory.class);

                    @SuppressWarnings("null")
                    HomekitFactory factory = homekitFactories.stream().filter(f -> f.supportsAccessoryClass(clazz))
                            .findFirst().orElseThrow(
                                    () -> new IllegalStateException("No HomekitFactory found for accessory class"));

                    if (factory != null) {
                        accessory = factory.createAccessory(clazz, (JsonValue) jsonObject);
                    }
                } else {
                    logger.warn("Class {} is not an HomekitAccessory", loadedClass.getName());
                }

                if (accessory == null) {
                    logger.warn("Could not create accessory for class {}", loadedClass.getName());
                }
            }

            // if (accessory != null) {
            // logger.debug("Created an HomekitAccessory {} of Type {} ", accessory.getUID(),
            // accessory.getClass().getSimpleName());

            // for (JsonValue service : services) {
            // long iid = ((JsonObject) service).getJsonNumber("iid").longValue();
            // final String serviceType = ((JsonObject) service).getString("type");

            // if (accessory.getService(serviceType) == null) {

            // HomekitFactory factory = homekitFactories.stream()
            // .filter(f -> f.supportsServiceType(serviceType)).findFirst().orElse(null);

            // if (factory != null) {
            // HomekitService newService = factory.createService(serviceType, (HomekitAccessory) accessory, iid, false);

            // if (newService != null) {
            // accessory.addService(newService);
            // JsonArray characteristics = (JsonArray) ((JsonObject) service).get("characteristics");

            // for (JsonValue characteristic : characteristics) {
            // iid = ((JsonObject) characteristic).getJsonNumber("iid").longValue();
            // final String characteristicType = ((JsonObject) characteristic).getString("type");

            // if (newService.getCharacteristic(characteristicType) == null) {

            // factory = homekitFactories.stream()
            // .filter(f -> f.supportsCharacteristicsType(characteristicType))
            // .findFirst().orElse(null);

            // if (factory != null) {
            // HomekitCharacteristic<?> newCharacteristic = factory
            // .createCharacteristic(characteristicType, newService, iid);
            // if (newCharacteristic != null) {
            // newService.addCharacteristic(newCharacteristic);
            // } else {
            // logger.warn(
            // "Homekit Factory {} could not create a HomekitCharacteristic of Type {}",
            // factory.toString(), characteristicType);
            // }
            // } else {
            // logger.warn("No Homekit Factory can create a HomekitCharacteristic of Type {}",
            // characteristicType);
            // }
            // } else {
            // if (newService.getCharacteristic(characteristicType) != null) {
            // logger.info(
            // "HomekitService {} of Type {} already holds HomekitCharacteristic {} of Type {}",
            // newService.getUID(), newService.getClass().getSimpleName(),
            // ((HomekitCharacteristic<?>) newService
            // .getCharacteristic(characteristicType)).getUID(),
            // newService.getCharacteristic(characteristicType).getClass()
            // .getSimpleName());
            // }
            // }
            // }
            // } else {
            // logger.warn("Homekit Factory {} could not create a HomekitService of Type {}",
            // factory.toString(), serviceType);
            // }
            // } else {
            // logger.warn("No Homekit Factory can create a HomekitService of Type {}", serviceType);
            // }
            // } else {
            // if (accessory.getService(serviceType) != null) {
            // logger.info("HomekitAccessory {} of Type {} already holds HomekitService {} of Type {}",
            // accessory.getUID(), accessory.getClass().getSimpleName(),
            // ((HomekitService) accessory.getService(serviceType)).getUID(),
            // accessory.getService(serviceType).getClass().getSimpleName());
            // }
            // }
            // }

            // if (accessory instanceof ThingAccessory) {
            // ThingUID thingUID = new ThingUID(persistableElement.getThingUID());
            // Thing thing = thingRegistry.get(thingUID);

            // if (thing != null) {
            // HomekitFactory factory = homekitFactories.stream()
            // .filter(f -> f.supportsThingType(thing.getThingTypeUID())).findFirst().orElse(null);

            // if (factory != null) {
            // ((ThingAccessory) accessory).setThingUID(thingUID);
            // logger.info("Linked Thing {} ({}) to HomekitAccessory {} of Type {}",
            // persistableElement.getThingUID(), thing.getLabel(), accessory.getUID(),
            // accessory.getClass().getSimpleName());

            // for (Channel channel : thing.getChannels()) {
            // HashSet<String> characteristicTypes = factory
            // .getCharacteristicTypes(channel.getChannelTypeUID());

            // for (HomekitService aService : accessory.getServices()) {
            // for (HomekitCharacteristic aCharacteristic : aService.getCharacteristics()) {
            // for (String aCharacteristicType : characteristicTypes) {
            // if (aCharacteristic.isType(aCharacteristicType)) {
            // ((HomekitCharacteristic<?>) aCharacteristic)
            // .setChannelUID(channel.getUID());
            // logger.debug(
            // "Linked Channel {} ({}) to HomekitCharacteristic {} of Type {}",
            // channel.getUID(), channel.getLabel(),
            // ((HomekitCharacteristic<?>) aCharacteristic).getUID(),
            // aCharacteristic.getClass().getSimpleName());
            // }
            // }
            // }
            // }
            // } else {
            // logger.warn("There is no Homekit Factory that supports ThingType {}",
            // persistableElement.getThingUID());
            // }
            // } else {
            // logger.warn(
            // "The Thing {} linked to to HomekitAccessory {} could not be found in the Thing Registry",
            // persistableElement.getThingUID(), accessory.getUID());
            // // TODO : Remove from accessory registry? what if thingregistry is not ready?
            // }
            // }
        } catch (HomekitException e) {
            logger.error("Error creating accessory: {}", e.getMessage());
        }

        return accessory;

        // } else {
        // HomekitAccessoryUID uid = new HomekitAccessoryUID(key);

        // logger.warn("HomekitAccessory Server {} hosting HomekitAccessory {} was not found in the HomekitAccessory Server Registry",
        // persistableElement.getServerUID(), uid);

        // this.remove(uid);
        // }

        // return null;
    }

    @Override
    protected @NonNull HomekitPersistedAccessory toPersistableElement(HomekitAccessory element) {
        return new HomekitPersistedAccessory(element.getClass().getName(), element.toJson().toString());
    }

    @Override
    public void onReadyMarkerAdded(ReadyMarker readyMarker) {
        logger.debug("{}Ready marker added: {}", LOG_INIT, readyMarker);
        delayedInitialize();
    }

    @Override
    public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
        logger.debug("{}Ready marker removed: {}", LOG_INIT, readyMarker);
        ReadyMarker newMarker = new ReadyMarker(HOMEKIT_MANAGED_ACCESSORY_PROVIDER, this.toString());
        readyService.unmarkReady(newMarker);
    }
}
