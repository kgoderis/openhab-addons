package org.openhab.io.homekit.internal.factory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.openhab.io.homekit.api.hap.HomekitAccessory;
import org.openhab.io.homekit.api.hap.HomekitAccessoryServer;
import org.openhab.io.homekit.api.hap.HomekitCharacteristic;
import org.openhab.io.homekit.api.hap.HomekitService;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.openhab.io.homekit.exception.HomekitFactoryException;
import org.openhab.io.homekit.exception.HomekitRegistrationException;
import org.openhab.io.homekit.internal.accessory.HomekitGenericAccessory;
import org.openhab.io.homekit.internal.client.HomekitBindingConstants;
import org.openhab.io.homekit.util.HomekitUUID5;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public abstract class AbstractHomekitFactory implements HomekitFactory {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractHomekitFactory.class);

    // ========== Log HomekitMessage Prefixes ==========
    protected static final String LOG_PREFIX = "HomeKit Factory: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_METADATA = LOG_PREFIX + "Metadata - ";
    protected static final String LOG_REGISTRY = LOG_PREFIX + "Registry - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";

    // ========== Error Messages ==========
    protected static final String ERROR_PREFIX = "HomeKit Factory Error: ";
    protected static final String ERROR_METADATA_POPULATION = ERROR_PREFIX + "Failed to populate metadata: %s";
    protected static final String ERROR_SERVICE_METADATA = ERROR_PREFIX + "HomekitService metadata error: %s";
    protected static final String ERROR_CHARACTERISTIC_METADATA = ERROR_PREFIX + "HomekitCharacteristic metadata error: %s";
    protected static final String ERROR_TYPE_LOOKUP = ERROR_PREFIX + "Type lookup error: %s";

    // 1. Metadata classes and mappers

    private static class AccessoryMetadata {
        private final Class<? extends HomekitAccessory> accessoryClass;
        private final String label;

        public AccessoryMetadata(Class<? extends HomekitAccessory> accessoryClass, String label) {
            this.accessoryClass = accessoryClass;
            this.label = label;
        }

        public Class<? extends HomekitAccessory> getAccessoryClass() {
            return accessoryClass;
        }

        public String getLabel() {
            return label;
        }
    }

    private static class ServiceMetadata {
        final String serviceType;
        final String tag;
        final Class<? extends HomekitService> serviceClass;

        ServiceMetadata(Class<? extends HomekitService> serviceClass, String serviceType, String tag) {
            this.serviceClass = serviceClass;
            this.serviceType = serviceType;
            this.tag = tag;
        }
    }

    private static class CharacteristicMetadata {
        final String characteristicType;
        final String tag;
        final Class<? extends HomekitCharacteristic<?>> characteristicClass;
        final String acceptedItemType;
        final ChannelTypeUID channelTypeUID;

        CharacteristicMetadata(Class<? extends HomekitCharacteristic<?>> characteristicClass, String characteristicType,
                String tag, String acceptedItemType, ChannelTypeUID channelTypeUID) {
            this.characteristicClass = characteristicClass;
            this.characteristicType = characteristicType;
            this.tag = tag;
            this.acceptedItemType = acceptedItemType;
            this.channelTypeUID = channelTypeUID;
        }
    }

    private final Map<ThingTypeUID, Class<? extends HomekitAccessory>> thingTypeAccessoryClassMapper = new HashMap<>();
    private final Map<ThingTypeUID, Set<String>> thingTypeServiceTypeMapper = new HashMap<>();
    private final Map<ChannelTypeUID, Set<String>> channelTypeCharacteristicTypeMapper = new HashMap<>();
    private final Map<String, AccessoryMetadata> accessoryMetadataMapper = new HashMap<>();
    private final Map<String, @Nullable ServiceMetadata> serviceMetadataMapper = new HashMap<>();
    private final Map<String, @Nullable CharacteristicMetadata> characteristicMetadataMapper = new HashMap<>();
    private final Map<String, Set<Class<? extends HomekitService>>> tagServiceClassMapper = new HashMap<>();
    private final Map<String, Set<Class<? extends HomekitCharacteristic<?>>>> tagCharacteristicClassMapper = new HashMap<>();

    // 2. Constructor and initialization
    protected AbstractHomekitFactory() {
        logger.debug("{}Initializing HomeKit factory", LOG_INIT);
        try {
            doInitializeMappers();
        } catch (HomekitFactoryException e) {
            throw new RuntimeException("Failed to initialize HomeKit factory: " + e.getMessage(), e);
        }
        logger.debug("{}HomeKit factory initialization completed", LOG_INIT);
    }

    private void doInitializeMappers() throws HomekitFactoryException {
        initializeMappers();
    }

    protected abstract void initializeMappers() throws HomekitFactoryException;

    // 3. Metadata population methods
    private void populateAccessoryMetadata(Class<? extends HomekitAccessory> accessoryClass) throws HomekitFactoryException {
        try {
            Method getLabelMethod = accessoryClass.getMethod("getLabel");
            String label = (String) getLabelMethod.invoke(null);
            registerAccessoryMetadata(accessoryClass, label);
        } catch (NoSuchMethodException e) {
            String message = String.format("HomekitAccessory %s is missing required methods: %s",
                    accessoryClass.getSimpleName(), e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitFactoryException(message, e);
        } catch (IllegalAccessException e) {
            String message = String.format("Cannot access methods for accessory %s: %s", accessoryClass.getSimpleName(),
                    e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitFactoryException(message, e);
        } catch (InvocationTargetException e) {
            String message = String.format("Error invoking methods for accessory %s: %s",
                    accessoryClass.getSimpleName(), e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitFactoryException(message, e);
        } catch (SecurityException | IllegalArgumentException e) {
            String message = String.format("Unexpected error populating accessory metadata for %s: %s",
                    accessoryClass.getSimpleName(), e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitFactoryException(message, e);
        }
    }

    private void populateServiceMetadata(Class<? extends HomekitService> serviceClass) throws HomekitFactoryException {
        try {
            Method getTypeMethod = serviceClass.getMethod("getType");
            Method getTagMethod = serviceClass.getMethod("getTag");

            String type = (String) getTypeMethod.invoke(null);
            String tag = (String) getTagMethod.invoke(null);

            if (type == null || type.isEmpty()) {
                logger.warn("{}HomekitService {} has empty or null type, using generated UUID", LOG_METADATA,
                        serviceClass.getSimpleName());
                type = HomekitUUID5.fromNamespaceAndString(HomekitUUID5.NAMESPACE_SERVICE, serviceClass.getName()).toString();
            }

            if (tag == null || tag.isEmpty()) {
                logger.warn("{}HomekitService {} has empty or null tag, using type as tag", LOG_METADATA,
                        serviceClass.getSimpleName());
                tag = type;
            }

            registerServiceMetadata(serviceClass, type, tag);
            logger.debug("{}Successfully populated metadata for service {}", LOG_METADATA,
                    serviceClass.getSimpleName());
        } catch (NoSuchMethodException e) {
            String message = String.format("HomekitService %s is missing required methods: %s", serviceClass.getSimpleName(),
                    e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitFactoryException(message, e);
        } catch (IllegalAccessException e) {
            String message = String.format("Cannot access methods for service %s: %s", serviceClass.getSimpleName(),
                    e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitFactoryException(message, e);
        } catch (InvocationTargetException e) {
            String message = String.format("Error invoking methods for service %s: %s", serviceClass.getSimpleName(),
                    e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitFactoryException(message, e);
        } catch (SecurityException | IllegalArgumentException e) {
            String message = String.format("Unexpected error populating service metadata for %s: %s",
                    serviceClass.getSimpleName(), e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitFactoryException(message, e);
        }
    }

    private void populateCharacteristicMetadata(Class<? extends HomekitCharacteristic<?>> characteristicClass)
            throws HomekitFactoryException {
        try {
            Method getTypeMethod = characteristicClass.getMethod("getType");
            Method getTagMethod = characteristicClass.getMethod("getTag");
            Method getAcceptedItemTypeMethod = characteristicClass.getMethod("getAcceptedItemType");
            Method getChannelTypeUIDMethod = characteristicClass.getMethod("getChannelTypeUID");

            String type = (String) getTypeMethod.invoke(null);
            String tag = (String) getTagMethod.invoke(null);
            String acceptedItemType = (String) getAcceptedItemTypeMethod.invoke(null);
            ChannelTypeUID channelTypeUID = (ChannelTypeUID) getChannelTypeUIDMethod.invoke(null);

            if (type == null || type.isEmpty()) {
                logger.warn("{}HomekitCharacteristic {} has empty or null type, using generated UUID", LOG_METADATA,
                        characteristicClass.getSimpleName());
                type = HomekitUUID5.fromNamespaceAndString(HomekitUUID5.NAMESPACE_CHARACTERISTIC, characteristicClass.getName())
                        .toString();
            }

            if (tag == null || tag.isEmpty()) {
                logger.warn("{}HomekitCharacteristic {} has empty or null tag, using type as tag", LOG_METADATA,
                        characteristicClass.getSimpleName());
                tag = type;
            }

            if (acceptedItemType == null || acceptedItemType.isEmpty()) {
                logger.warn("{}HomekitCharacteristic {} has empty or null accepted item type, using default", LOG_METADATA,
                        characteristicClass.getSimpleName());
                acceptedItemType = "default";
            }

            if (channelTypeUID == null) {
                logger.warn("{}HomekitCharacteristic {} has null channel type UID, using generated one", LOG_METADATA,
                        characteristicClass.getSimpleName());
                channelTypeUID = new ChannelTypeUID(HomekitBindingConstants.BINDING_ID, type);
            }

            registerCharacteristicMetadata(characteristicClass, type, tag, acceptedItemType, channelTypeUID);
            logger.debug("{}Successfully populated metadata for characteristic {}", LOG_METADATA,
                    characteristicClass.getSimpleName());
        } catch (NoSuchMethodException e) {
            String message = String.format("HomekitCharacteristic %s is missing required methods: %s",
                    characteristicClass.getSimpleName(), e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitFactoryException(message, e);
        } catch (IllegalAccessException e) {
            String message = String.format("Cannot access methods for characteristic %s: %s",
                    characteristicClass.getSimpleName(), e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitFactoryException(message, e);
        } catch (InvocationTargetException e) {
            String message = String.format("Error invoking methods for characteristic %s: %s",
                    characteristicClass.getSimpleName(), e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitFactoryException(message, e);
        } catch (SecurityException | IllegalArgumentException e) {
            String message = String.format("Unexpected error populating characteristic metadata for %s: %s",
                    characteristicClass.getSimpleName(), e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitFactoryException(message, e);
        }
    }

    protected void registerAccessoryMetadata(Class<? extends HomekitAccessory> accessoryClass, String label) {
        logger.debug("{}Registering accessory metadata - Class: {}, Label: {}", LOG_METADATA,
                accessoryClass.getSimpleName(), label);
        AccessoryMetadata metadata = new AccessoryMetadata(accessoryClass, label);
        accessoryMetadataMapper.put(accessoryClass.getName(), metadata);
        logger.debug("{}HomekitAccessory metadata registered successfully", LOG_METADATA);
    }

    protected void registerServiceMetadata(Class<? extends HomekitService> serviceClass, String serviceType, String tag) {
        logger.debug("{}Registering service metadata - Class: {}, Type: {}, Tag: {}", LOG_METADATA,
                serviceClass.getSimpleName(), serviceType, tag);
        ServiceMetadata metadata = new ServiceMetadata(serviceClass, serviceType, tag);
        serviceMetadataMapper.put(serviceType, metadata);
        @Nullable
        Set<Class<? extends HomekitService>> services = tagServiceClassMapper.get(tag);
        if (services == null || services.isEmpty()) {
            services = new HashSet<>();
            tagServiceClassMapper.put(tag, services);
        }
        services.add(serviceClass);
        logger.debug("{}HomekitService metadata registered successfully", LOG_METADATA);
    }

    protected void registerCharacteristicMetadata(Class<? extends HomekitCharacteristic<?>> characteristicClass,
            String characteristicType, String tag, String acceptedItemType, ChannelTypeUID channelTypeUID) {
        logger.debug(
                "{}Registering characteristic metadata - Class: {}, Type: {}, Tag: {}, AcceptedItemType: {}, ChannelTypeUID: {}",
                LOG_METADATA, characteristicClass.getSimpleName(), characteristicType, tag, acceptedItemType,
                channelTypeUID);
        CharacteristicMetadata metadata = new CharacteristicMetadata(characteristicClass, characteristicType, tag,
                acceptedItemType, channelTypeUID);
        characteristicMetadataMapper.put(characteristicType, metadata);
        @Nullable
        Set<Class<? extends HomekitCharacteristic<?>>> characteristics = tagCharacteristicClassMapper.get(tag);
        if (characteristics == null || characteristics.isEmpty()) {
            characteristics = new HashSet<>();
            tagCharacteristicClassMapper.put(tag, characteristics);
        }
        characteristics.add(characteristicClass);
        logger.debug("{}HomekitCharacteristic metadata registered successfully", LOG_METADATA);
    }

    // 4. HomekitService-related methods
    @Override
    public void addService(ThingTypeUID thingTypeUID, Class<@NonNull ? extends HomekitService> serviceClass)
            throws HomekitRegistrationException {
        logger.debug("{}Adding service to thing type - ThingType: {}, ServiceClass: {}", LOG_REGISTRY, thingTypeUID,
                serviceClass.getSimpleName());
        try {
            String serviceType = getServiceType(serviceClass);
            addService(thingTypeUID, serviceType);
            if (!serviceMetadataMapper.containsKey(serviceType)) {
                populateServiceMetadata(serviceClass);
            }
            logger.debug("{}HomekitService added successfully", LOG_REGISTRY);
        } catch (HomekitFactoryException e) {
            String message = String.format("Failed to add service %s to thing type %s: %s",
                    serviceClass.getSimpleName(), thingTypeUID, e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitRegistrationException(message, e);
        } catch (SecurityException | IllegalArgumentException e) {
            String message = String.format("Unexpected error adding service %s to thing type %s: %s",
                    serviceClass.getSimpleName(), thingTypeUID, e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitRegistrationException(message, e);
        }
    }

    @Override
    public void addService(Class<@NonNull ? extends HomekitService> serviceClass) throws HomekitRegistrationException {
        String serviceType = getServiceType(serviceClass);
        addService(serviceType, serviceClass);
        if (!serviceMetadataMapper.containsKey(serviceType)) {
            try {
                populateServiceMetadata(serviceClass);
            } catch (HomekitFactoryException e) {
                throw new HomekitRegistrationException("Failed to populate service metadata: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void addService(ThingTypeUID thingType, String serviceType) {
        logger.debug("{}Adding service type to thing type - ThingType: {}, ServiceType: {}", LOG_REGISTRY, thingType,
                serviceType);
        @Nullable
        Set<String> currentTypes = thingTypeServiceTypeMapper.get(thingType);
        if (currentTypes == null || currentTypes.isEmpty()) {
            currentTypes = new HashSet<>();
            logger.debug("{}Creating new service type set for thing type: {}", LOG_REGISTRY, thingType);
        }
        currentTypes.add(serviceType);
        thingTypeServiceTypeMapper.put(thingType, currentTypes);
        logger.debug("{}HomekitService type added successfully", LOG_REGISTRY);
    }

    @Override
    public void addService(String serviceType, Class<@NonNull ? extends HomekitService> serviceClass)
            throws HomekitRegistrationException {
        if (!serviceMetadataMapper.containsKey(serviceType)) {
            try {
                populateServiceMetadata(serviceClass);
            } catch (HomekitFactoryException e) {
                throw new HomekitRegistrationException("Failed to populate service metadata: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void addServiceWithTag(String tag, Class<? extends HomekitService> serviceClass)
            throws HomekitRegistrationException {
        @Nullable
        Set<Class<? extends HomekitService>> services = tagServiceClassMapper.get(tag);
        if (services == null || services.isEmpty()) {
            services = new HashSet<>();
            tagServiceClassMapper.put(tag, services);
        }
        services.add(serviceClass);
        try {
            populateServiceMetadata(serviceClass);
        } catch (HomekitFactoryException e) {
            throw new HomekitRegistrationException("Failed to populate service metadata: " + e.getMessage(), e);
        }
    }

    @Override
    public void addService(ThingTypeUID thingTypeUID) throws HomekitRegistrationException {
        logger.debug("{}Adding default services to thing type - ThingType: {}", LOG_REGISTRY, thingTypeUID);
        try {
            Set<String> serviceTypes = getDefaultServiceTypes(thingTypeUID);
            for (String serviceType : serviceTypes) {
                addService(thingTypeUID, serviceType);
            }
            logger.debug("{}Default services added successfully", LOG_REGISTRY);
        } catch (Exception e) {
            String message = String.format("Failed to add default services to thing type %s: %s", thingTypeUID,
                    e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitRegistrationException(message, e);
        }
    }

    // 5. HomekitCharacteristic-related methods
    @Override
    public void addCharacteristic(ChannelTypeUID channelTypeUID,
            Class<@NonNull ? extends HomekitCharacteristic<?>> characteristicClass) throws HomekitRegistrationException {
        logger.debug("{}Adding characteristic to channel type - ChannelType: {}, CharacteristicClass: {}", LOG_REGISTRY,
                channelTypeUID, characteristicClass.getSimpleName());
        try {
            String characteristicType = getCharacteristicType(characteristicClass);
            addCharacteristic(channelTypeUID, characteristicType);
            if (!characteristicMetadataMapper.containsKey(characteristicType)) {
                populateCharacteristicMetadata(characteristicClass);
            }
            logger.debug("{}HomekitCharacteristic added successfully", LOG_REGISTRY);
        } catch (HomekitFactoryException e) {
            String message = String.format("Failed to add characteristic %s to channel type %s: %s",
                    characteristicClass.getSimpleName(), channelTypeUID, e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitRegistrationException(message, e);
        } catch (SecurityException | IllegalArgumentException e) {
            String message = String.format("Unexpected error adding characteristic %s to channel type %s: %s",
                    characteristicClass.getSimpleName(), channelTypeUID, e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitRegistrationException(message, e);
        }
    }

    @Override
    public void addCharacteristic(Class<@NonNull ? extends HomekitCharacteristic<?>> characteristicClass)
            throws HomekitRegistrationException {
        String characteristicType = getCharacteristicType(characteristicClass);
        addCharacteristic(characteristicType, characteristicClass);
        if (!characteristicMetadataMapper.containsKey(characteristicType)) {
            try {
                populateCharacteristicMetadata(characteristicClass);
            } catch (HomekitFactoryException e) {
                throw new HomekitRegistrationException("Failed to populate characteristic metadata: " + e.getMessage(),
                        e);
            }
        }
    }

    @Override
    public void addCharacteristic(ChannelTypeUID channelTypeUID, String characteristicType) {
        logger.debug("{}Adding characteristic type to channel type - ChannelType: {}, CharacteristicType: {}",
                LOG_REGISTRY, channelTypeUID, characteristicType);
        @Nullable
        Set<String> currentTypes = channelTypeCharacteristicTypeMapper.get(channelTypeUID);
        if (currentTypes == null || currentTypes.isEmpty()) {
            currentTypes = new HashSet<>();
            logger.debug("{}Creating new characteristic type set for channel type: {}", LOG_REGISTRY, channelTypeUID);
        }
        currentTypes.add(characteristicType);
        channelTypeCharacteristicTypeMapper.put(channelTypeUID, currentTypes);
        logger.debug("{}HomekitCharacteristic type added successfully", LOG_REGISTRY);
    }

    @Override
    public void addCharacteristic(String characteristicType,
            Class<@NonNull ? extends HomekitCharacteristic<?>> characteristicClass) throws HomekitRegistrationException {
        if (!characteristicMetadataMapper.containsKey(characteristicType)) {
            try {
                populateCharacteristicMetadata(characteristicClass);
            } catch (HomekitFactoryException e) {
                throw new HomekitRegistrationException("Failed to populate characteristic metadata: " + e.getMessage(),
                        e);
            }
        }
    }

    @Override
    public void addCharacteristicWithTag(String tag, Class<? extends HomekitCharacteristic<?>> characteristicClass)
            throws HomekitRegistrationException {
        @Nullable
        Set<Class<? extends HomekitCharacteristic<?>>> characteristics = tagCharacteristicClassMapper.get(tag);
        if (characteristics == null || characteristics.isEmpty()) {
            characteristics = new HashSet<>();
            tagCharacteristicClassMapper.put(tag, characteristics);
        }
        characteristics.add(characteristicClass);
        try {
            populateCharacteristicMetadata(characteristicClass);
        } catch (HomekitFactoryException e) {
            throw new HomekitRegistrationException("Failed to populate characteristic metadata: " + e.getMessage(), e);
        }
    }

    // 6. HomekitAccessory-related methods
    @Override
    public void addAccessory(ThingTypeUID thingTypeUID,
            Class<? extends org.openhab.io.homekit.api.hap.HomekitAccessory> accessoryClass)
            throws HomekitRegistrationException {
        logger.debug("{}Adding accessory to thing type - ThingType: {}, AccessoryClass: {}", LOG_REGISTRY, thingTypeUID,
                accessoryClass.getSimpleName());
        thingTypeAccessoryClassMapper.put(thingTypeUID, accessoryClass);
        addAccessory(accessoryClass);
        logger.debug("{}HomekitAccessory added successfully", LOG_REGISTRY);
    }

    @Override
    public void addAccessory(Class<? extends HomekitAccessory> accessoryClass) throws HomekitRegistrationException {
        logger.debug("{}Adding accessory - Class: {}", LOG_REGISTRY, accessoryClass.getSimpleName());
        try {
            if (!accessoryMetadataMapper.containsKey(accessoryClass.getName())) {
                populateAccessoryMetadata(accessoryClass);
            }
            logger.debug("{}HomekitAccessory added successfully", LOG_REGISTRY);
        } catch (HomekitFactoryException e) {
            String message = String.format("Failed to add accessory %s: %s", accessoryClass.getSimpleName(),
                    e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitRegistrationException(message, e);
        }
    }

    @Override
    public @Nullable HomekitAccessory createAccessory(Class<? extends HomekitAccessory> accessoryClass, HomekitAccessoryServer server,
            long instanceId) throws HomekitFactoryException {
        logger.debug("{}Creating accessory - Class: {}, Server: {}, InstanceId: {}", LOG_REGISTRY,
                accessoryClass.getSimpleName(), server, instanceId);
        try {
            Constructor<? extends HomekitAccessory> constructor = accessoryClass.getConstructor(HomekitAccessoryServer.class,
                    long.class);
            if (constructor == null) {
                throw new NoSuchMethodException("Constructor not found");
            }
            @Nullable
            HomekitAccessory accessory = constructor.newInstance(server, instanceId);
            logger.debug("{}Created an HomekitAccessory {} of Type {}, with instanceId {}", LOG_REGISTRY, accessory.getUID(),
                    accessory.getClass().getSimpleName(), accessory.getAccessoryId());
            return accessory;
        } catch (NoSuchMethodException e) {
            String message = String.format(
                    "HomekitAccessory %s is missing a valid constructor of type (HomekitAccessoryServer.class, long.class)",
                    accessoryClass.getSimpleName());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitRegistrationException(message, e);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            String message = String.format("Failed to create accessory %s: %s", accessoryClass.getSimpleName(),
                    e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitRegistrationException(message, e);
        }
    }

    @Override
    public HomekitAccessory createAccessory(Class<? extends HomekitAccessory> accessoryClass, HomekitAccessoryServer server, long instanceId,
            boolean extend) throws HomekitFactoryException {
        logger.debug("{}Creating accessory - Class: {}, Server: {}, InstanceId: {}", LOG_REGISTRY,
                accessoryClass.getSimpleName(), server, instanceId);
        try {
            Constructor<? extends HomekitAccessory> constructor = accessoryClass.getConstructor(HomekitAccessoryServer.class,
                    long.class, boolean.class);
            if (constructor == null) {
                throw new NoSuchMethodException("Constructor not found");
            }
            @Nullable
            HomekitAccessory accessory = constructor.newInstance(server, instanceId, extend);
            logger.debug("{}Created an HomekitAccessory {} of Type {}, with instanceId {}", LOG_REGISTRY, accessory.getUID(),
                    accessory.getClass().getSimpleName(), accessory.getAccessoryId());
            return accessory;
        } catch (NoSuchMethodException e) {
            String message = String.format(
                    "HomekitAccessory %s is missing a valid constructor of type (HomekitAccessoryServer.class, long.class)",
                    accessoryClass.getSimpleName());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitRegistrationException(message, e);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            String message = String.format("Failed to create accessory %s: %s", accessoryClass.getSimpleName(),
                    e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitRegistrationException(message, e);
        }
    }

    @SuppressWarnings("unused")
    @Override
    public @Nullable HomekitAccessory createAccessory(Thing thing, HomekitAccessoryServer server) throws HomekitFactoryException {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        @Nullable
        Class<? extends HomekitAccessory> accessoryClass = thingTypeAccessoryClassMapper.get(thingTypeUID);

        // Fallback to HomekitGenericAccessory if no mapping exists
        if (accessoryClass == null) {
            accessoryClass = HomekitGenericAccessory.class;
        }

        HomekitAccessory accessory = null;
        try {
            accessory = createAccessory(accessoryClass, server, server.getNextAvailableAccessoryId(), true);
        } catch (HomekitAccessoryOperationException | HomekitFactoryException e) {
            logger.error("{}Failed to create accessory for thing {}: {}", LOG_ERROR, thing.getUID(), e.getMessage(), e);
            return null;
        }

        // TODO : Elaborate for the two ThingTypes. Traverse in each case.

        // if (accessory != null && (accessory instanceof HomekitGenericAccessory)) {
        // logger.info("Creating HomekitAccessory {} of Type {} for Thing {}", accessory.getUID(),
        // accessory.getClass().getSimpleName(),thing.getUID());

        // Set<String> serviceTypes = thingTypeServiceTypeMapper.get(thingTypeUID);
        // if (serviceTypes != null) {
        // for (String serviceType : serviceTypes) {
        // if (accessory.getService(serviceType) == null && accessory.isExtensible()) {
        // accessory.addService(
        // createService(serviceType, accessory, true, accessory.getLabel()));
        // }

        // HomekitService service = accessory.getService(serviceType);
        // if (service != null) {
        // for (Channel channel : thing.getChannels()) {
        // Set<String> characteristicTypes = channelTypeCharacteristicTypeMapper
        // .get(channel.getChannelTypeUID());
        // if (characteristicTypes != null) {
        // for (String characteristicType : characteristicTypes) {
        // if (service.getCharacteristic(characteristicType) == null
        // && service.isExtensible()) {
        // service.addCharacteristic(createCharacteristic(characteristicType, service));
        // }

        // HomekitCharacteristic<?> characteristic = service.getCharacteristic(characteristicType);
        // if (characteristic != null) {
        // characteristic.setChannelUID(channel.getUID());
        // logger.debug("Linked Channel {} to HomekitCharacteristic {} of Type {}",
        // channel.getUID(), characteristic.getUID(),
        // characteristic.getClass().getSimpleName());
        // }
        // }
        // }
        // }
        // }
        // }
        // }
        // }

        logger.debug("{}Created an HomekitAccessory {} of Type {}, with instanceId {}", LOG_REGISTRY, accessory.getUID(),
                accessory.getClass().getSimpleName(), accessory.getAccessoryId());
        return accessory;
    }

    @Override
    public @Nullable HomekitAccessory createAccessory(Class<? extends HomekitAccessory> accessoryClass, JsonValue value)
            throws HomekitFactoryException {
        logger.debug("{}Creating accessory - Class: {}, Value: {}", LOG_REGISTRY, accessoryClass.getSimpleName(),
                value.toString());
        try {
            Constructor<? extends HomekitAccessory> constructor = accessoryClass.getConstructor(JsonValue.class);
            if (constructor == null) {
                throw new NoSuchMethodException("Constructor not found");
            }
            @Nullable
            HomekitAccessory accessory = constructor.newInstance(value);
            logger.debug("{}Created an HomekitAccessory {} of Type {}, with instanceId {}", LOG_REGISTRY, accessory.getUID(),
                    accessory.getClass().getSimpleName(), accessory.getAccessoryId());
            return accessory;
        } catch (NoSuchMethodException e) {
            String message = String.format("HomekitAccessory %s is missing a valid constructor of type (JsonValue.class)",
                    accessoryClass.getSimpleName());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitRegistrationException(message, e);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            String message = String.format("Failed to create accessory %s: %s", accessoryClass.getSimpleName(),
                    e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitRegistrationException(message, e);
        }
    }

    @Override
    public @Nullable HomekitService createService(String serviceType, HomekitAccessory accessory, boolean extend, String serviceName)
            throws HomekitFactoryException {
        try {
            Class<? extends HomekitService> serviceClass = getService(serviceType);
            return serviceClass.getConstructor(HomekitAccessory.class, long.class, boolean.class, String.class)
                    .newInstance(accessory, 0L, extend, serviceName);
        } catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
                | InvocationTargetException | NoSuchMethodException e) {
            String message = String.format("Failed to create service %s: %s", serviceType, e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitFactoryException(message, e);
        }
    }

    @Override
    public @Nullable HomekitService createService(String serviceType, HomekitAccessory accessory, long instanceId, boolean extend,
            String serviceName) throws HomekitFactoryException {
        try {
            Class<? extends HomekitService> serviceClass = getService(serviceType);
            return serviceClass.getConstructor(HomekitAccessory.class, long.class, boolean.class, String.class)
                    .newInstance(accessory, instanceId, extend, serviceName);
        } catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
                | InvocationTargetException | NoSuchMethodException e) {
            String message = String.format("Failed to create service %s with instanceId %d: %s", serviceType,
                    instanceId, e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitFactoryException(message, e);
        }
    }

    @Override
    public @Nullable HomekitService createService(String serviceType, HomekitAccessory accessory, long instanceId, boolean extend)
            throws HomekitFactoryException {
        try {
            Class<? extends HomekitService> serviceClass = getService(serviceType);
            return serviceClass.getConstructor(HomekitAccessory.class, long.class, boolean.class, String.class)
                    .newInstance(accessory, instanceId, extend, serviceType);
        } catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
                | InvocationTargetException | NoSuchMethodException e) {
            String message = String.format("Failed to create service %s with instanceId %d: %s", serviceType,
                    instanceId, e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitFactoryException(message, e);
        }
    }

    @Override
    public @Nullable HomekitService createService(HomekitAccessory accessory, JsonValue value) throws HomekitFactoryException {
        try {
            String serviceType = value.asJsonObject().getString("type");
            Class<? extends HomekitService> serviceClass = getService(serviceType);
            return serviceClass.getConstructor(HomekitAccessory.class, JsonValue.class, String.class).newInstance(accessory,
                    value, serviceType);
        } catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
                | InvocationTargetException | NoSuchMethodException e) {
            String message = String.format("Failed to create service from JSON: %s", e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitFactoryException(message, e);
        }
    }

    @Override
    public @Nullable HomekitCharacteristic<?> createCharacteristic(String characteristicsType, HomekitService service)
            throws HomekitFactoryException {
        try {
            Class<? extends HomekitCharacteristic<?>> characteristicClass = getCharacteristic(characteristicsType);
            return characteristicClass.getConstructor(HomekitService.class, long.class).newInstance(service, 0L);
        } catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
                | InvocationTargetException | NoSuchMethodException e) {
            String message = String.format("Failed to create characteristic %s: %s", characteristicsType,
                    e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitFactoryException(message, e);
        }
    }

    @Override
    public @Nullable HomekitCharacteristic<?> createCharacteristic(String characteristicsType, HomekitService service,
            long instanceId) throws HomekitFactoryException {
        try {
            Class<? extends HomekitCharacteristic<?>> characteristicClass = getCharacteristic(characteristicsType);
            return characteristicClass.getConstructor(HomekitService.class, long.class).newInstance(service, instanceId);
        } catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
                | InvocationTargetException | NoSuchMethodException e) {
            String message = String.format("Failed to create characteristic %s with instanceId %d: %s",
                    characteristicsType, instanceId, e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitFactoryException(message, e);
        }
    }

    @Override
    public @Nullable HomekitCharacteristic<?> createCharacteristic(HomekitService service, JsonValue value)
            throws HomekitFactoryException {
        try {
            String characteristicType = value.asJsonObject().getString("type");
            Class<? extends HomekitCharacteristic<?>> characteristicClass = getCharacteristic(characteristicType);
            return characteristicClass.getConstructor(HomekitService.class, JsonValue.class).newInstance(service, value);
        } catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
                | InvocationTargetException | NoSuchMethodException e) {
            String message = String.format("Failed to create characteristic from JSON: %s", e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new HomekitFactoryException(message, e);
        }
    }

    @Override
    public Class<? extends HomekitService> getService(String serviceType) {
        @Nullable
        ServiceMetadata metadata = serviceMetadataMapper.get(serviceType);
        if (metadata != null) {
            return metadata.serviceClass;
        }
        throw new IllegalArgumentException("No service found for type: " + serviceType);
    }

    @Override
    public Class<? extends HomekitCharacteristic<?>> getCharacteristic(String characteristicType) {
        @Nullable
        CharacteristicMetadata metadata = characteristicMetadataMapper.get(characteristicType);
        if (metadata != null) {
            return metadata.characteristicClass;
        }
        throw new IllegalArgumentException("No characteristic found for type: " + characteristicType);
    }

    // 7. Type conversion and lookup methods
    private String getServiceType(Class<? extends HomekitService> serviceClass) {
        @Nullable
        ServiceMetadata metadata = serviceMetadataMapper.values().stream()
                .filter(m -> m != null && m.serviceClass != null && m.serviceClass.equals(serviceClass)).findFirst()
                .orElse((ServiceMetadata) null);
        if (metadata != null) {
            return metadata.serviceType;
        }
        return HomekitUUID5.fromNamespaceAndString(HomekitUUID5.NAMESPACE_SERVICE, serviceClass.getName()).toString();
    }

    private String getCharacteristicType(Class<? extends HomekitCharacteristic<?>> characteristicClass) {
        @Nullable
        CharacteristicMetadata metadata = (@Nullable CharacteristicMetadata) characteristicMetadataMapper.values()
                .stream().filter(m -> m != null && m.characteristicClass != null
                        && m.characteristicClass.equals(characteristicClass))
                .findFirst().orElse(null);
        if (metadata != null) {
            return metadata.characteristicType;
        }
        return HomekitUUID5.fromNamespaceAndString(HomekitUUID5.NAMESPACE_CHARACTERISTIC, characteristicClass.getName()).toString();
    }

    @Override
    public String getCharacteristicAcceptedItemType(String characteristicType) {
        @Nullable
        CharacteristicMetadata metadata = characteristicMetadataMapper.get(characteristicType);
        if (metadata != null) {
            return metadata.acceptedItemType;
        }
        logger.warn("{}No accepted item type found for characteristic type: {}", LOG_ERROR, characteristicType);
        throw new IllegalArgumentException(
                "No accepted item type found for characteristic type: " + characteristicType);
    }

    // 8. Tag-related methods
    @Override
    public String getTagFromServiceType(String serviceType) {
        @Nullable
        ServiceMetadata metadata = serviceMetadataMapper.get(serviceType);
        if (metadata != null) {
            return metadata.tag;
        }
        logger.warn("{}No tag found for service type: {}", LOG_ERROR, serviceType);
        throw new IllegalArgumentException("No tag found for service type: " + serviceType);
    }

    @Override
    public String getTagFromCharacteristicType(String characteristicType) {
        @Nullable
        CharacteristicMetadata metadata = characteristicMetadataMapper.get(characteristicType);
        if (metadata != null) {
            return metadata.tag;
        }
        logger.warn("{}No tag found for characteristic type: {}", LOG_ERROR, characteristicType);
        throw new IllegalArgumentException("No tag found for characteristic type: " + characteristicType);
    }

    @Override
    public @Nullable String getServiceTypeFromTag(String tag) {
        @Nullable
        Set<Class<? extends HomekitService>> serviceClasses = tagServiceClassMapper.get(tag);
        if (serviceClasses != null && !serviceClasses.isEmpty()) {
            @Nullable
            Class<? extends HomekitService> firstServiceClass = serviceClasses.iterator().next();
            for (Map.Entry<String, @Nullable ServiceMetadata> entry : serviceMetadataMapper.entrySet()) {
                @Nullable
                ServiceMetadata s = entry.getValue();
                if (s != null && s.serviceClass != null && s.serviceClass.equals(firstServiceClass)) {
                    return entry.getKey();
                }
            }
        }
        logger.warn("No service type found for tag: {}", tag);
        throw new IllegalArgumentException("No service type found for tag: " + tag);
    }

    @Override
    public @Nullable String getCharacteristicTypeFromTag(String tag) {
        @Nullable
        Set<Class<? extends HomekitCharacteristic<?>>> characteristicClasses = tagCharacteristicClassMapper.get(tag);
        if (characteristicClasses != null && !characteristicClasses.isEmpty()) {
            @Nullable
            Class<? extends HomekitCharacteristic<?>> firstCharacteristicClass = characteristicClasses.iterator().next();
            for (Map.Entry<String, @Nullable CharacteristicMetadata> entry : characteristicMetadataMapper.entrySet()) {
                @Nullable
                CharacteristicMetadata e = entry.getValue();
                if (e != null && e.characteristicClass != null
                        && e.characteristicClass.equals(firstCharacteristicClass)) {
                    return entry.getKey();
                }

            }

        }
        logger.warn("No characteristic type found for tag: {}", tag);
        throw new IllegalArgumentException("No service type found for tag: " + tag);
    }

    // 9. Channel-related methods
    @Override
    public @Nullable ChannelTypeUID getChannelTypeUID(String characteristicType) {
        @Nullable
        CharacteristicMetadata metadata = characteristicMetadataMapper.get(characteristicType);
        if (metadata != null) {
            return metadata.channelTypeUID;
        }
        logger.warn("{}No channel type UID found for characteristic type: {}", LOG_ERROR, characteristicType);
        throw new IllegalArgumentException("No channel type UID found for characteristic type: " + characteristicType);
    }

    @Override
    public @Nullable Set<String> getCharacteristicTypes(ChannelTypeUID channelTypeUID) {
        return channelTypeCharacteristicTypeMapper.get(channelTypeUID);
    }

    // 10. Support check methods
    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        boolean supported = thingTypeServiceTypeMapper.containsKey(thingTypeUID);
        logger.debug("{}Checking thing type support - ThingType: {}, Supported: {}", LOG_REGISTRY, thingTypeUID,
                supported);
        return supported;
    }

    // 11. HomekitAccessory-related methods
    @Override
    public boolean supportsAccessoryClass(Class<? extends HomekitAccessory> accessoryClass) {
        return accessoryMetadataMapper.containsKey(accessoryClass.getName());
    }

    @SuppressWarnings("null")
    @Override
    public Set<Class<? extends HomekitAccessory>> getSupportedAccessoryClasses() {
        return accessoryMetadataMapper.values().stream().map(metadata -> metadata.getAccessoryClass())
                .filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public ThingTypeUID @NonNull [] getSupportedThingTypes() {
        ThingTypeUID[] supportedTypes = thingTypeServiceTypeMapper.keySet().toArray(ThingTypeUID[]::new);
        logger.debug("{}Retrieved supported thing types - Count: {}", LOG_REGISTRY, supportedTypes.length);
        return supportedTypes;
    }

    @Override
    public boolean supportsServiceType(String serviceType) {
        boolean supported = serviceMetadataMapper.containsKey(serviceType);
        logger.debug("{}Checking service type support - ServiceType: {}, Supported: {}", LOG_REGISTRY, serviceType,
                supported);
        return supported;
    }

    @Override
    public Set<String> getSupportedServiceTypes() {
        Set<String> supportedTypes = serviceMetadataMapper.keySet();
        logger.debug("{}Retrieved supported service types - Count: {}", LOG_REGISTRY, supportedTypes.size());
        return supportedTypes;
    }

    @Override
    public boolean supportsCharacteristicsType(String characteristicsType) {
        boolean supported = characteristicMetadataMapper.containsKey(characteristicsType);
        logger.debug("{}Checking characteristic type support - CharacteristicType: {}, Supported: {}", LOG_REGISTRY,
                characteristicsType, supported);
        return supported;
    }

    @Override
    public Set<String> getSupportedCharacteristicTypes() {
        Set<String> supportedTypes = characteristicMetadataMapper.keySet();
        logger.debug("{}Retrieved supported characteristic types - Count: {}", LOG_REGISTRY, supportedTypes.size());
        return supportedTypes;
    }

    // Helper method for getting default service types
    private Set<String> getDefaultServiceTypes(ThingTypeUID thingTypeUID) {
        // Implementation depends on your specific requirements
        return new HashSet<>();
    }
}
