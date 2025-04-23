package org.openhab.io.homekit.internal.factory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.AccessoryServer;
import org.openhab.io.homekit.api.hap.Characteristic;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.client.HomekitBindingConstants;
import org.openhab.io.homekit.util.UUID5;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractHomekitFactory implements HomekitFactory {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractHomekitFactory.class);

    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "HomeKit Factory: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_METADATA = LOG_PREFIX + "Metadata - ";
    protected static final String LOG_REGISTRY = LOG_PREFIX + "Registry - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";

    // ========== Error Messages ==========
    protected static final String ERROR_PREFIX = "HomeKit Factory Error: ";
    protected static final String ERROR_METADATA_POPULATION = ERROR_PREFIX + "Failed to populate metadata: %s";
    protected static final String ERROR_SERVICE_METADATA = ERROR_PREFIX + "Service metadata error: %s";
    protected static final String ERROR_CHARACTERISTIC_METADATA = ERROR_PREFIX + "Characteristic metadata error: %s";
    protected static final String ERROR_TYPE_LOOKUP = ERROR_PREFIX + "Type lookup error: %s";

    // 1. Metadata classes and mappers
    private static class ServiceMetadata {
        final String serviceType;
        final String tag;
        final Class<? extends Service> serviceClass;

        ServiceMetadata(Class<? extends Service> serviceClass, String serviceType, String tag) {
            this.serviceClass = serviceClass;
            this.serviceType = serviceType;
            this.tag = tag;
        }
    }

    private static class CharacteristicMetadata {
        final String characteristicType;
        final String tag;
        final Class<? extends Characteristic<?>> characteristicClass;
        final String acceptedItemType;
        final ChannelTypeUID channelTypeUID;

        CharacteristicMetadata(Class<? extends Characteristic<?>> characteristicClass, String characteristicType,
                String tag, String acceptedItemType, ChannelTypeUID channelTypeUID) {
            this.characteristicClass = characteristicClass;
            this.characteristicType = characteristicType;
            this.tag = tag;
            this.acceptedItemType = acceptedItemType;
            this.channelTypeUID = channelTypeUID;
        }
    }

    private final Map<ThingTypeUID, Class<? extends Accessory>> thingTypeAccessoryClassMapper = new HashMap<>();
    private final Map<ThingTypeUID, Set<String>> thingTypeServiceTypeMapper = new HashMap<>();
    private final Map<ChannelTypeUID, Set<String>> channelTypeCharacteristicTypeMapper = new HashMap<>();
    private final Map<String, ServiceMetadata> serviceMetadataMapper = new HashMap<>();
    private final Map<String, CharacteristicMetadata> characteristicMetadataMapper = new HashMap<>();
    private final Map<String, Set<Class<? extends Service>>> tagServiceClassMapper = new HashMap<>();
    private final Map<String, Set<Class<? extends Characteristic<?>>>> tagCharacteristicClassMapper = new HashMap<>();

    // 2. Constructor and initialization
    protected AbstractHomekitFactory() {
        logger.debug("{}Initializing HomeKit factory", LOG_INIT);
        initializeMappers();
        logger.debug("{}HomeKit factory initialization completed", LOG_INIT);
    }

    protected abstract void initializeMappers();

    // 3. Metadata population methods
    private void populateServiceMetadata(Class<? extends Service> serviceClass) throws MetadataException {
        if (serviceClass == null) {
            throw new MetadataException("Service class cannot be null");
        }

        try {
            Method getTypeMethod = serviceClass.getMethod("getType");
            Method getTagMethod = serviceClass.getMethod("getTag");

            String type = (String) getTypeMethod.invoke(null);
            String tag = (String) getTagMethod.invoke(null);

            if (type == null || type.isEmpty()) {
                logger.warn("{}Service {} has empty or null type, using generated UUID", LOG_METADATA,
                        serviceClass.getSimpleName());
                type = UUID5.fromNamespaceAndString(UUID5.NAMESPACE_SERVICE, serviceClass.getName()).toString();
            }

            if (tag == null || tag.isEmpty()) {
                logger.warn("{}Service {} has empty or null tag, using type as tag", LOG_METADATA,
                        serviceClass.getSimpleName());
                tag = type;
            }

            registerServiceMetadata(serviceClass, type, tag);
            logger.debug("{}Successfully populated metadata for service {}", LOG_METADATA,
                    serviceClass.getSimpleName());
        } catch (NoSuchMethodException e) {
            String message = String.format("Service %s is missing required methods: %s", serviceClass.getSimpleName(),
                    e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new MetadataException(message, e);
        } catch (IllegalAccessException e) {
            String message = String.format("Cannot access methods for service %s: %s", serviceClass.getSimpleName(),
                    e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new MetadataException(message, e);
        } catch (InvocationTargetException e) {
            String message = String.format("Error invoking methods for service %s: %s", serviceClass.getSimpleName(),
                    e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new MetadataException(message, e);
        } catch (Exception e) {
            String message = String.format("Unexpected error populating service metadata for %s: %s",
                    serviceClass.getSimpleName(), e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new MetadataException(message, e);
        }
    }

    private void populateCharacteristicMetadata(Class<? extends Characteristic<?>> characteristicClass)
            throws MetadataException {
        if (characteristicClass == null) {
            throw new MetadataException("Characteristic class cannot be null");
        }

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
                logger.warn("{}Characteristic {} has empty or null type, using generated UUID", LOG_METADATA,
                        characteristicClass.getSimpleName());
                type = UUID5.fromNamespaceAndString(UUID5.NAMESPACE_CHARACTERISTIC, characteristicClass.getName())
                        .toString();
            }

            if (tag == null || tag.isEmpty()) {
                logger.warn("{}Characteristic {} has empty or null tag, using type as tag", LOG_METADATA,
                        characteristicClass.getSimpleName());
                tag = type;
            }

            if (acceptedItemType == null || acceptedItemType.isEmpty()) {
                logger.warn("{}Characteristic {} has empty or null accepted item type, using default", LOG_METADATA,
                        characteristicClass.getSimpleName());
                acceptedItemType = "default";
            }

            if (channelTypeUID == null) {
                logger.warn("{}Characteristic {} has null channel type UID, using generated one", LOG_METADATA,
                        characteristicClass.getSimpleName());
                channelTypeUID = new ChannelTypeUID(HomekitBindingConstants.BINDING_ID, type);
            }

            registerCharacteristicMetadata(characteristicClass, type, tag, acceptedItemType, channelTypeUID);
            logger.debug("{}Successfully populated metadata for characteristic {}", LOG_METADATA,
                    characteristicClass.getSimpleName());
        } catch (NoSuchMethodException e) {
            String message = String.format("Characteristic %s is missing required methods: %s",
                    characteristicClass.getSimpleName(), e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new MetadataException(message, e);
        } catch (IllegalAccessException e) {
            String message = String.format("Cannot access methods for characteristic %s: %s",
                    characteristicClass.getSimpleName(), e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new MetadataException(message, e);
        } catch (InvocationTargetException e) {
            String message = String.format("Error invoking methods for characteristic %s: %s",
                    characteristicClass.getSimpleName(), e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new MetadataException(message, e);
        } catch (Exception e) {
            String message = String.format("Unexpected error populating characteristic metadata for %s: %s",
                    characteristicClass.getSimpleName(), e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new MetadataException(message, e);
        }
    }

    protected void registerServiceMetadata(Class<? extends Service> serviceClass, String serviceType, String tag) {
        logger.debug("{}Registering service metadata - Class: {}, Type: {}, Tag: {}", LOG_METADATA,
                serviceClass.getSimpleName(), serviceType, tag);
        ServiceMetadata metadata = new ServiceMetadata(serviceClass, serviceType, tag);
        serviceMetadataMapper.put(serviceType, metadata);
        tagServiceClassMapper.computeIfAbsent(tag, k -> new HashSet<>()).add(serviceClass);
        logger.debug("{}Service metadata registered successfully", LOG_METADATA);
    }

    protected void registerCharacteristicMetadata(Class<? extends Characteristic<?>> characteristicClass,
            String characteristicType, String tag, String acceptedItemType, ChannelTypeUID channelTypeUID) {
        logger.debug(
                "{}Registering characteristic metadata - Class: {}, Type: {}, Tag: {}, AcceptedItemType: {}, ChannelTypeUID: {}",
                LOG_METADATA, characteristicClass.getSimpleName(), characteristicType, tag, acceptedItemType,
                channelTypeUID);
        CharacteristicMetadata metadata = new CharacteristicMetadata(characteristicClass, characteristicType, tag,
                acceptedItemType, channelTypeUID);
        characteristicMetadataMapper.put(characteristicType, metadata);
        tagCharacteristicClassMapper.computeIfAbsent(tag, k -> new HashSet<>()).add(characteristicClass);
        logger.debug("{}Characteristic metadata registered successfully", LOG_METADATA);
    }

    // 4. Service-related methods
    @Override
    public void addService(@NonNull ThingTypeUID thingTypeUID, @NonNull Class<@NonNull ? extends Service> serviceClass)
            throws RegistrationException {
        logger.debug("{}Adding service to thing type - ThingType: {}, ServiceClass: {}", LOG_REGISTRY, thingTypeUID,
                serviceClass.getSimpleName());
        try {
            String serviceType = getServiceType(serviceClass);
            addService(thingTypeUID, serviceType);
            if (!serviceMetadataMapper.containsKey(serviceType)) {
                populateServiceMetadata(serviceClass);
            }
            logger.debug("{}Service added successfully", LOG_REGISTRY);
        } catch (MetadataException e) {
            String message = String.format("Failed to add service %s to thing type %s: %s",
                    serviceClass.getSimpleName(), thingTypeUID, e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new RegistrationException(message, e);
        } catch (Exception e) {
            String message = String.format("Unexpected error adding service %s to thing type %s: %s",
                    serviceClass.getSimpleName(), thingTypeUID, e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new RegistrationException(message, e);
        }
    }

    @Override
    public void addService(@NonNull Class<@NonNull ? extends Service> serviceClass) {
        String serviceType = getServiceType(serviceClass);
        addService(serviceType, serviceClass);
        if (!serviceMetadataMapper.containsKey(serviceType)) {
            populateServiceMetadata(serviceClass);
        }
    }

    @Override
    public void addService(@NonNull ThingTypeUID thingType, @NonNull String serviceType) {
        logger.debug("{}Adding service type to thing type - ThingType: {}, ServiceType: {}", LOG_REGISTRY, thingType,
                serviceType);
        Set<String> currentTypes = thingTypeServiceTypeMapper.get(thingType);
        if (currentTypes == null) {
            currentTypes = new HashSet<String>();
            logger.debug("{}Creating new service type set for thing type: {}", LOG_REGISTRY, thingType);
        }
        currentTypes.add(serviceType);
        thingTypeServiceTypeMapper.put(thingType, currentTypes);
        logger.debug("{}Service type added successfully", LOG_REGISTRY);
    }

    @Override
    public void addService(@NonNull String serviceType, @NonNull Class<@NonNull ? extends Service> serviceClass) {
        if (!serviceMetadataMapper.containsKey(serviceType)) {
            populateServiceMetadata(serviceClass);
        }
    }

    @Override
    public void addServiceWithTag(String tag, Class<? extends Service> serviceClass) {
        tagServiceClassMapper.computeIfAbsent(tag, k -> new HashSet<>()).add(serviceClass);
        populateServiceMetadata(serviceClass);
    }

    @Override
    public void addService(ThingTypeUID thingTypeUID) throws RegistrationException {
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
            throw new RegistrationException(message, e);
        }
    }

    // Helper method for getting default service types
    private Set<String> getDefaultServiceTypes(ThingTypeUID thingTypeUID) {
        // Implementation depends on your specific requirements
        return new HashSet<>();
    }

    // 5. Characteristic-related methods
    @Override
    public void addCharacteristic(@NonNull ChannelTypeUID channelTypeUID,
            @NonNull Class<@NonNull ? extends Characteristic<?>> characteristicClass) throws RegistrationException {
        logger.debug("{}Adding characteristic to channel type - ChannelType: {}, CharacteristicClass: {}", LOG_REGISTRY,
                channelTypeUID, characteristicClass.getSimpleName());
        try {
            String characteristicType = getCharacteristicType(characteristicClass);
            addCharacteristic(channelTypeUID, characteristicType);
            if (!characteristicMetadataMapper.containsKey(characteristicType)) {
                populateCharacteristicMetadata(characteristicClass);
            }
            logger.debug("{}Characteristic added successfully", LOG_REGISTRY);
        } catch (MetadataException e) {
            String message = String.format("Failed to add characteristic %s to channel type %s: %s",
                    characteristicClass.getSimpleName(), channelTypeUID, e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new RegistrationException(message, e);
        } catch (Exception e) {
            String message = String.format("Unexpected error adding characteristic %s to channel type %s: %s",
                    characteristicClass.getSimpleName(), channelTypeUID, e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new RegistrationException(message, e);
        }
    }

    @Override
    public void addCharacteristic(@NonNull Class<@NonNull ? extends Characteristic<?>> characteristicClass) {
        String characteristicType = getCharacteristicType(characteristicClass);
        addCharacteristic(characteristicType, characteristicClass);
        if (!characteristicMetadataMapper.containsKey(characteristicType)) {
            populateCharacteristicMetadata(characteristicClass);
        }
    }

    @Override
    public void addCharacteristic(@NonNull ChannelTypeUID channelTypeUID, @NonNull String characteristicType) {
        logger.debug("{}Adding characteristic type to channel type - ChannelType: {}, CharacteristicType: {}",
                LOG_REGISTRY, channelTypeUID, characteristicType);
        Set<String> currentTypes = channelTypeCharacteristicTypeMapper.get(channelTypeUID);
        if (currentTypes == null) {
            currentTypes = new HashSet<String>();
            logger.debug("{}Creating new characteristic type set for channel type: {}", LOG_REGISTRY, channelTypeUID);
        }
        currentTypes.add(characteristicType);
        channelTypeCharacteristicTypeMapper.put(channelTypeUID, currentTypes);
        logger.debug("{}Characteristic type added successfully", LOG_REGISTRY);
    }

    @Override
    public void addCharacteristic(@NonNull String characteristicType,
            @NonNull Class<@NonNull ? extends Characteristic<?>> characteristicClass) {
        if (!characteristicMetadataMapper.containsKey(characteristicType)) {
            populateCharacteristicMetadata(characteristicClass);
        }
    }

    @Override
    public void addCharacteristicWithTag(String tag, Class<? extends Characteristic<?>> characteristicClass) {
        tagCharacteristicClassMapper.computeIfAbsent(tag, k -> new HashSet<>()).add(characteristicClass);
        populateCharacteristicMetadata(characteristicClass);
    }

    // 6. Accessory-related methods
    @Override
    public void addAccessory(@NonNull ThingTypeUID thingTypeUID,
            @NonNull Class<? extends org.openhab.io.homekit.api.hap.Accessory> accessoryClass) {
        logger.debug("{}Adding accessory to thing type - ThingType: {}, AccessoryClass: {}", LOG_REGISTRY, thingTypeUID,
                accessoryClass.getSimpleName());
        thingTypeAccessoryClassMapper.put(thingTypeUID, accessoryClass);
        logger.debug("{}Accessory added successfully", LOG_REGISTRY);
    }

    @Override
    public @Nullable Accessory createAccessory(Class<? extends Accessory> accessoryClass, AccessoryServer server,
            long instanceId) throws RegistrationException {
        logger.debug("{}Creating accessory - Class: {}, Server: {}, InstanceId: {}", LOG_REGISTRY,
                accessoryClass.getSimpleName(), server, instanceId);
        try {
            Accessory accessory = accessoryClass.getConstructor(AccessoryServer.class, long.class).newInstance(server,
                    instanceId);
            logger.debug("{}Created an Accessory {} of Type {}, with instanceId {}", LOG_REGISTRY, accessory.getUID(),
                    accessory.getClass().getSimpleName(), accessory.getAccessoryId());
            return accessory;
        } catch (NoSuchMethodException e) {
            String message = String.format(
                    "Accessory %s is missing a valid constructor of type (AccessoryServer.class, long.class)",
                    accessoryClass.getSimpleName());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new RegistrationException(message, e);
        } catch (Exception e) {
            String message = String.format("Failed to create accessory %s: %s", accessoryClass.getSimpleName(),
                    e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new RegistrationException(message, e);
        }
    }

    @Override
    public @Nullable Accessory createAccessory(@NonNull Thing thing, @NonNull LocalAccessoryServer server)
            throws Exception {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        Class<? extends Accessory> accessoryClass = thingTypeAccessoryClassMapper.get(thingTypeUID);

        if (accessoryClass == null) {
            accessoryClass = ThingAccessory.class;
        }

        Accessory accessory = createAccessory(accessoryClass, server, server.getInstanceId(), true);

        if (accessory != null && (accessory instanceof ThingAccessory)) {
            ThingAccessory thingAccessory = (ThingAccessory) accessory;
            thingAccessory.setThingUID(thing.getUID());
            logger.info("Linked Thing {} to Accessory {} of Type {}", thing.getUID(), accessory.getUID(),
                    accessory.getClass().getSimpleName());

            Set<String> serviceTypes = thingTypeServiceTypeMapper.get(thingTypeUID);
            if (serviceTypes != null) {
                for (String serviceType : serviceTypes) {
                    if (thingAccessory.getService(serviceType) == null && thingAccessory.isExtensible()) {
                        thingAccessory.addService(
                                createService(serviceType, thingAccessory, true, thingAccessory.getLabel()));
                    }

                    Service service = thingAccessory.getService(serviceType);
                    if (service != null) {
                        for (Channel channel : thing.getChannels()) {
                            Set<String> characteristicTypes = channelTypeCharacteristicTypeMapper
                                    .get(channel.getChannelTypeUID());
                            if (characteristicTypes != null) {
                                for (String characteristicType : characteristicTypes) {
                                    if (service.getCharacteristic(characteristicType) == null
                                            && service.isExtensible()) {
                                        service.addCharacteristic(createCharacteristic(characteristicType, service));
                                    }

                                    Characteristic<?> characteristic = service.getCharacteristic(characteristicType);
                                    if (characteristic != null) {
                                        characteristic.setChannelUID(channel.getUID());
                                        logger.debug("Linked Channel {} to Characteristic {} of Type {}",
                                                channel.getUID(), characteristic.getUID(),
                                                characteristic.getClass().getSimpleName());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return accessory;
    }

    @Override
    public @Nullable Service createService(String serviceType, Accessory accessory, boolean extend, String serviceName)
            throws MetadataException {
        try {
            Class<? extends Service> serviceClass = getService(serviceType);
            return serviceClass.getConstructor(Accessory.class, long.class, boolean.class, String.class)
                    .newInstance(accessory, 0L, extend, serviceName);
        } catch (Exception e) {
            String message = String.format("Failed to create service %s: %s", serviceType, e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new MetadataException(message, e);
        }
    }

    @Override
    public @Nullable Service createService(String serviceType, Accessory accessory, long instanceId, boolean extend,
            String serviceName) throws MetadataException {
        try {
            Class<? extends Service> serviceClass = getService(serviceType);
            return serviceClass.getConstructor(Accessory.class, long.class, boolean.class, String.class)
                    .newInstance(accessory, instanceId, extend, serviceName);
        } catch (Exception e) {
            String message = String.format("Failed to create service %s with instanceId %d: %s", serviceType,
                    instanceId, e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new MetadataException(message, e);
        }
    }

    @Override
    public @Nullable Service createService(String serviceType, Accessory accessory, long instanceId, boolean extend)
            throws MetadataException {
        try {
            Class<? extends Service> serviceClass = getService(serviceType);
            return serviceClass.getConstructor(Accessory.class, long.class, boolean.class, String.class)
                    .newInstance(accessory, instanceId, extend, serviceType);
        } catch (Exception e) {
            String message = String.format("Failed to create service %s with instanceId %d: %s", serviceType,
                    instanceId, e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new MetadataException(message, e);
        }
    }

    @Override
    public @Nullable Service createService(Accessory accessory, JsonValue value) throws MetadataException {
        try {
            String serviceType = value.asJsonObject().getString("type");
            Class<? extends Service> serviceClass = getService(serviceType);
            return serviceClass.getConstructor(Accessory.class, JsonValue.class, String.class).newInstance(accessory,
                    value, serviceType);
        } catch (Exception e) {
            String message = String.format("Failed to create service from JSON: %s", e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new MetadataException(message, e);
        }
    }

    @Override
    public @Nullable Characteristic<?> createCharacteristic(String characteristicsType, Service service)
            throws MetadataException {
        try {
            Class<? extends Characteristic<?>> characteristicClass = getCharacteristic(characteristicsType);
            return characteristicClass.getConstructor(Service.class, long.class).newInstance(service, 0L);
        } catch (Exception e) {
            String message = String.format("Failed to create characteristic %s: %s", characteristicsType,
                    e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new MetadataException(message, e);
        }
    }

    @Override
    public @Nullable Characteristic<?> createCharacteristic(String characteristicsType, Service service,
            long instanceId) throws MetadataException {
        try {
            Class<? extends Characteristic<?>> characteristicClass = getCharacteristic(characteristicsType);
            return characteristicClass.getConstructor(Service.class, long.class).newInstance(service, instanceId);
        } catch (Exception e) {
            String message = String.format("Failed to create characteristic %s with instanceId %d: %s",
                    characteristicsType, instanceId, e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new MetadataException(message, e);
        }
    }

    @Override
    public @Nullable Characteristic<?> createCharacteristic(Service service, JsonValue value) throws MetadataException {
        try {
            String characteristicType = value.asJsonObject().getString("type");
            Class<? extends Characteristic<?>> characteristicClass = getCharacteristic(characteristicType);
            return characteristicClass.getConstructor(Service.class, JsonValue.class).newInstance(service, value);
        } catch (Exception e) {
            String message = String.format("Failed to create characteristic from JSON: %s", e.getMessage());
            logger.error("{}{}", LOG_ERROR, message, e);
            throw new MetadataException(message, e);
        }
    }

    public Class<? extends Service> getService(String serviceType) {
        ServiceMetadata metadata = serviceMetadataMapper.get(serviceType);
        if (metadata != null) {
            return metadata.serviceClass;
        }
        return null;
    }

    public Class<? extends Characteristic<?>> getCharacteristic(String characteristicType) {
        CharacteristicMetadata metadata = characteristicMetadataMapper.get(characteristicType);
        if (metadata != null) {
            return metadata.characteristicClass;
        }
        return null;
    }

    // 7. Type conversion and lookup methods
    private String getServiceType(Class<? extends Service> serviceClass) {
        ServiceMetadata metadata = serviceMetadataMapper.values().stream()
                .filter(m -> m.serviceClass.equals(serviceClass)).findFirst().orElse(null);
        if (metadata != null) {
            return metadata.serviceType;
        }
        return UUID5.fromNamespaceAndString(UUID5.NAMESPACE_SERVICE, serviceClass.getName()).toString();
    }

    private String getCharacteristicType(Class<? extends Characteristic<?>> characteristicClass) {
        CharacteristicMetadata metadata = characteristicMetadataMapper.values().stream()
                .filter(m -> m.characteristicClass.equals(characteristicClass)).findFirst().orElse(null);
        if (metadata != null) {
            return metadata.characteristicType;
        }
        return UUID5.fromNamespaceAndString(UUID5.NAMESPACE_CHARACTERISTIC, characteristicClass.getName()).toString();
    }

    @Override
    public String getServiceInstanceType(@NonNull String characteristicType) {
        CharacteristicMetadata metadata = characteristicMetadataMapper.get(characteristicType);
        ServiceMetadata serviceMetadata = serviceMetadataMapper.get(metadata.serviceType);
        if (serviceMetadata != null) {
            return serviceMetadata.serviceType;
        }
        logger.warn("{}No service type found for characteristic type: {}", LOG_ERROR, characteristicType);
        return null;
    }

    @Override
    public String getCharacteristicAcceptedItemType(@NonNull String characteristicType) {
        CharacteristicMetadata metadata = characteristicMetadataMapper.get(characteristicType);
        if (metadata != null) {
            return metadata.acceptedItemType;
        }
        logger.warn("{}No accepted item type found for characteristic type: {}", LOG_ERROR, characteristicType);
        return null;
    }

    // 8. Tag-related methods
    @Override
    public String getTagFromServiceType(@NonNull String serviceType) {
        ServiceMetadata metadata = serviceMetadataMapper.get(serviceType);
        if (metadata != null) {
            return metadata.tag;
        }
        logger.warn("{}No tag found for service type: {}", LOG_ERROR, serviceType);
        return null;
    }

    @Override
    public String getTagFromCharacteristicType(@NonNull String characteristicType) {
        CharacteristicMetadata metadata = characteristicMetadataMapper.get(characteristicType);
        if (metadata != null) {
            return metadata.tag;
        }
        logger.warn("{}No tag found for characteristic type: {}", LOG_ERROR, characteristicType);
        return null;
    }

    @Override
    public String getServiceTypeFromTag(@NonNull String tag) {
        Set<Class<? extends Service>> serviceClasses = tagServiceClassMapper.get(tag);
        if (serviceClasses != null && !serviceClasses.isEmpty()) {
            Class<? extends Service> serviceClass = serviceClasses.iterator().next();
            for (Map.Entry<String, ServiceMetadata> entry : serviceMetadataMapper.entrySet()) {
                if (entry.getValue().serviceClass.equals(serviceClass)) {
                    return entry.getKey();
                }
            }
        }
        logger.warn("No service type found for tag: {}", tag);
        return null;
    }

    @Override
    public String getCharacteristicTypeFromTag(@NonNull String tag) {
        Set<Class<? extends Characteristic<?>>> characteristicClasses = tagCharacteristicClassMapper.get(tag);
        if (characteristicClasses != null && !characteristicClasses.isEmpty()) {
            Class<? extends Characteristic<?>> characteristicClass = characteristicClasses.iterator().next();
            for (Map.Entry<String, CharacteristicMetadata> entry : characteristicMetadataMapper.entrySet()) {
                if (entry.getValue().characteristicClass.equals(characteristicClass)) {
                    return entry.getKey();
                }
            }
        }
        logger.warn("No characteristic type found for tag: {}", tag);
        return null;
    }

    // 9. Channel-related methods
    @Override
    public @Nullable ChannelTypeUID getChannelTypeUID(@NonNull String characteristicType) {
        CharacteristicMetadata metadata = characteristicMetadataMapper.get(characteristicType);
        if (metadata != null) {
            return metadata.channelTypeUID;
        }
        logger.warn("{}No channel type UID found for characteristic type: {}", LOG_ERROR, characteristicType);
        return null;
    }

    @Override
    public HashSet<String> getCharacteristicTypes(@Nullable ChannelTypeUID channelTypeUID) {
        return channelTypeCharacteristicTypeMapper.get(channelTypeUID);
    }

    // 10. Support check methods
    @Override
    public boolean supportsThingType(@NonNull ThingTypeUID thingTypeUID) {
        boolean supported = thingTypeServiceTypeMapper.containsKey(thingTypeUID);
        logger.debug("{}Checking thing type support - ThingType: {}, Supported: {}", LOG_REGISTRY, thingTypeUID,
                supported);
        return supported;
    }

    @Override
    public ThingTypeUID @NonNull [] getSupportedThingTypes() {
        ThingTypeUID[] supportedTypes = thingTypeServiceTypeMapper.keySet().toArray(new ThingTypeUID[0]);
        logger.debug("{}Retrieved supported thing types - Count: {}", LOG_REGISTRY, supportedTypes.length);
        return supportedTypes;
    }

    @Override
    public boolean supportsServiceType(@NonNull String serviceType) {
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
    public boolean supportsCharacteristicsType(@NonNull String characteristicsType) {
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
}
