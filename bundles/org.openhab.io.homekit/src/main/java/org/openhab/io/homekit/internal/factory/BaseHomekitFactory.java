package org.openhab.io.homekit.internal.factory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.json.JsonObject;
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
import org.openhab.io.homekit.library.service.ThingService;
import org.openhab.io.homekit.util.UUID5;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseHomekitFactory implements HomekitFactory {

    protected static final Logger logger = LoggerFactory.getLogger(BaseHomekitFactory.class);

    // 1. Metadata classes and mappers
    private static class ServiceMetadata {
        final String serviceType;
        final String tag;
        final Class<? extends Service> serviceClass;
        final String instanceType;

        ServiceMetadata(Class<? extends Service> serviceClass, String serviceType, String tag, String instanceType) {
            this.serviceClass = serviceClass;
            this.serviceType = serviceType;
            this.tag = tag;
            this.instanceType = instanceType;
        }
    }

    private static class CharacteristicMetadata {
        final String characteristicType;
        final String tag;
        final Class<? extends Characteristic<?>> characteristicClass;
        final String acceptedItemType;
        final String instanceType;
        final ChannelTypeUID channelTypeUID;

        CharacteristicMetadata(Class<? extends Characteristic<?>> characteristicClass, String characteristicType, String tag, 
                String acceptedItemType, String instanceType, ChannelTypeUID channelTypeUID) {
            this.characteristicClass = characteristicClass;
            this.characteristicType = characteristicType;
            this.tag = tag;
            this.acceptedItemType = acceptedItemType;
            this.instanceType = instanceType;
            this.channelTypeUID = channelTypeUID;
        }
    }

    private final Map<ThingTypeUID, Class<? extends Accessory>> thingTypeAccessoryClassMapper = new HashMap<>();
    private final Map<ThingTypeUID, Set<String>> thingTypeServiceTypesMapper = new HashMap<>();
    private final Map<ChannelTypeUID, Set<String>> channelTypeCharacteristicTypesMapper = new HashMap<>();
    private final Map<String, ServiceMetadata> serviceMetadataMapper = new HashMap<>();
    private final Map<String, CharacteristicMetadata> characteristicMetadataMapper = new HashMap<>();
    private final Map<String, Set<Class<? extends Service>>> tagServiceClassMapper = new HashMap<>();
    private final Map<String, Set<Class<? extends Characteristic<?>>>> tagCharacteristicClassMapper = new HashMap<>();

    // 2. Constructor and initialization
    protected BaseHomekitFactory() {
        initializeMappers();
    }

    protected abstract void initializeMappers();

    // 3. Metadata population methods
    private void populateServiceMetadata(Class<? extends Service> serviceClass) {
        if (serviceClass == null) {
            logger.error("Cannot populate metadata for null service class");
            return;
        }

        try {
            Method getTypeMethod = serviceClass.getMethod("getType");
            Method getTagMethod = serviceClass.getMethod("getTag");
            Method getInstanceTypeMethod = serviceClass.getMethod("getInstanceType");
            
            String type = (String) getTypeMethod.invoke(null);
            String tag = (String) getTagMethod.invoke(null);
            String instanceType = (String) getInstanceTypeMethod.invoke(null);
            
            if (type == null || type.isEmpty()) {
                logger.warn("Service {} has empty or null type, using generated UUID", serviceClass.getSimpleName());
                type = UUID5.fromNamespaceAndString(UUID5.NAMESPACE_SERVICE, serviceClass.getName()).toString();
            }
            
            if (tag == null || tag.isEmpty()) {
                logger.warn("Service {} has empty or null tag, using type as tag", serviceClass.getSimpleName());
                tag = type;
            }
            
            if (instanceType == null || instanceType.isEmpty()) {
                logger.warn("Service {} has empty or null instance type, using default", serviceClass.getSimpleName());
                instanceType = "default";
            }
            
            registerServiceMetadata(serviceClass, type, tag, instanceType);
        } catch (NoSuchMethodException e) {
            logger.error("Service {} is missing required methods: {}", serviceClass.getSimpleName(), e.getMessage());
        } catch (IllegalAccessException e) {
            logger.error("Cannot access methods for service {}: {}", serviceClass.getSimpleName(), e.getMessage());
        } catch (InvocationTargetException e) {
            logger.error("Error invoking methods for service {}: {}", serviceClass.getSimpleName(), e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error populating service metadata for {}: {}", serviceClass.getSimpleName(), e.getMessage());
        }
    }

    private void populateCharacteristicMetadata(Class<? extends Characteristic<?>> characteristicClass) {
        if (characteristicClass == null) {
            logger.error("Cannot populate metadata for null characteristic class");
            return;
        }

        try {
            Method getTypeMethod = characteristicClass.getMethod("getType");
            Method getTagMethod = characteristicClass.getMethod("getTag");
            Method getAcceptedItemTypeMethod = characteristicClass.getMethod("getAcceptedItemType");
            Method getInstanceTypeMethod = characteristicClass.getMethod("getInstanceType");
            Method getChannelTypeUIDMethod = characteristicClass.getMethod("getChannelTypeUID");
            
            String type = (String) getTypeMethod.invoke(null);
            String tag = (String) getTagMethod.invoke(null);
            String acceptedItemType = (String) getAcceptedItemTypeMethod.invoke(null);
            String instanceType = (String) getInstanceTypeMethod.invoke(null);
            ChannelTypeUID channelTypeUID = (ChannelTypeUID) getChannelTypeUIDMethod.invoke(null);
            
            if (type == null || type.isEmpty()) {
                logger.warn("Characteristic {} has empty or null type, using generated UUID", characteristicClass.getSimpleName());
                type = UUID5.fromNamespaceAndString(UUID5.NAMESPACE_CHARACTERISTIC, characteristicClass.getName()).toString();
            }
            
            if (tag == null || tag.isEmpty()) {
                logger.warn("Characteristic {} has empty or null tag, using type as tag", characteristicClass.getSimpleName());
                tag = type;
            }
            
            if (acceptedItemType == null || acceptedItemType.isEmpty()) {
                logger.warn("Characteristic {} has empty or null accepted item type, using default", characteristicClass.getSimpleName());
                acceptedItemType = "default";
            }
            
            if (instanceType == null || instanceType.isEmpty()) {
                logger.warn("Characteristic {} has empty or null instance type, using default", characteristicClass.getSimpleName());
                instanceType = "default";
            }
            
            if (channelTypeUID == null) {
                logger.warn("Characteristic {} has null channel type UID, using generated one", characteristicClass.getSimpleName());
                channelTypeUID = new ChannelTypeUID(HomekitBindingConstants.BINDING_ID, type);
            }
            
            registerCharacteristicMetadata(characteristicClass, type, tag, acceptedItemType, instanceType, channelTypeUID);
        } catch (NoSuchMethodException e) {
            logger.error("Characteristic {} is missing required methods: {}", characteristicClass.getSimpleName(), e.getMessage());
        } catch (IllegalAccessException e) {
            logger.error("Cannot access methods for characteristic {}: {}", characteristicClass.getSimpleName(), e.getMessage());
        } catch (InvocationTargetException e) {
            logger.error("Error invoking methods for characteristic {}: {}", characteristicClass.getSimpleName(), e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error populating characteristic metadata for {}: {}", characteristicClass.getSimpleName(), e.getMessage());
        }
    }

    protected void registerServiceMetadata(Class<? extends Service> serviceClass, String serviceType, 
            String tag, String instanceType) {
        ServiceMetadata metadata = new ServiceMetadata(serviceClass, serviceType, tag, instanceType);
        serviceMetadataMapper.put(serviceType, metadata);
        tagServiceClassMapper.computeIfAbsent(tag, k -> new HashSet<>()).add(serviceClass);
    }

    protected void registerCharacteristicMetadata(Class<? extends Characteristic<?>> characteristicClass, 
            String characteristicType, String tag, String acceptedItemType, String instanceType, ChannelTypeUID channelTypeUID) {
        CharacteristicMetadata metadata = new CharacteristicMetadata(characteristicClass, characteristicType, tag, 
                acceptedItemType, instanceType, channelTypeUID);
        characteristicMetadataMapper.put(characteristicType, metadata);
        tagCharacteristicClassMapper.computeIfAbsent(tag, k -> new HashSet<>()).add(characteristicClass);
    }

    // 4. Service-related methods
    @Override
    public void addService(@NonNull ThingTypeUID thingTypeUID, @NonNull Class<@NonNull ? extends Service> serviceClass) {
        String serviceType = getServiceType(serviceClass);
        addService(thingTypeUID, serviceType);
        if (!serviceMetadataMapper.containsKey(serviceType)) {
            populateServiceMetadata(serviceClass);
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
        Set<String> currentTypes = thingTypeServiceTypesMapper.get(thingType);
        if (currentTypes == null) {
            currentTypes = new HashSet<String>();
        }
        currentTypes.add(serviceType);
        thingTypeServiceTypesMapper.put(thingType, currentTypes);
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

    // 5. Characteristic-related methods
    @Override
    public void addCharacteristic(@NonNull ChannelTypeUID channelTypeUID,
            @NonNull Class<@NonNull ? extends Characteristic<?>> characteristicClass) {
        String characteristicType = getCharacteristicType(characteristicClass);
        addCharacteristic(channelTypeUID, characteristicType);
        if (!characteristicMetadataMapper.containsKey(characteristicType)) {
            populateCharacteristicMetadata(characteristicClass);
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
        Set<String> currentTypes = channelTypeCharacteristicTypesMapper.get(channelTypeUID);
        if (currentTypes == null) {
            currentTypes = new HashSet<String>();
        }
        currentTypes.add(characteristicType);
        channelTypeCharacteristicTypesMapper.put(channelTypeUID, currentTypes);
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
        thingTypeAccessoryClassMapper.put(thingTypeUID, accessoryClass);
    }

    @Override
    public @Nullable Accessory createAccessory(Class<? extends Accessory> accessoryClass, AccessoryServer server,
            long instanceId) {
        try {
            Accessory accessory = accessoryClass.getConstructor(AccessoryServer.class, long.class).newInstance(server,
                    instanceId);
            logger.debug("Created an Accessory {} of Type {}, with instanceId {}", accessory.getUID(),
                    accessory.getClass().getSimpleName(), accessory.getAccessoryId());
            return accessory;
        } catch (NoSuchMethodException e) {
            logger.warn("Accessory {} is missing a valid constructor of type (AccessoryServer.class, long.class)",
                    accessoryClass.getSimpleName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public @Nullable Accessory createAccessory(@NonNull Thing thing, @NonNull LocalAccessoryServer server) throws Exception {
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

            Set<String> serviceTypes = thingTypeServiceTypesMapper.get(thingTypeUID);
            if (serviceTypes != null) {
                for (String serviceType : serviceTypes) {
                    if (thingAccessory.getService(serviceType) == null && thingAccessory.isExtensible()) {
                        thingAccessory.addService(createService(serviceType, thingAccessory, true, thingAccessory.getLabel()));
                    }

                    Service service = thingAccessory.getService(serviceType);
                    if (service != null) {
                        for (Channel channel : thing.getChannels()) {
                            Set<String> characteristicTypes = channelTypeCharacteristicTypesMapper.get(channel.getChannelTypeUID());
                            if (characteristicTypes != null) {
                                for (String characteristicType : characteristicTypes) {
                                    if (service.getCharacteristic(characteristicType) == null && service.isExtensible()) {
                                        service.addCharacteristic(createCharacteristic(characteristicType, service));
                                    }

                                    Characteristic<?> characteristic = service.getCharacteristic(characteristicType);
                                    if (characteristic != null) {
                                        characteristic.setChannelUID(channel.getUID());
                                        logger.debug("Linked Channel {} to Characteristic {} of Type {}", channel.getUID(),
                                                characteristic.getUID(), characteristic.getClass().getSimpleName());
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

    public  Class<? extends Service> getService(String serviceType) {
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
                .filter(m -> m.serviceClass.equals(serviceClass))
                .findFirst()
                .orElse(null);
        if (metadata != null) {
            return metadata.type;
        }
        return UUID5.fromNamespaceAndString(UUID5.NAMESPACE_SERVICE, serviceClass.getName()).toString();
    }

    private String getCharacteristicType(Class<? extends Characteristic<?>> characteristicClass) {
        CharacteristicMetadata metadata = characteristicMetadataMapper.values().stream()
                .filter(m -> m.characteristicClass.equals(characteristicClass))
                .findFirst()
                .orElse(null);
        if (metadata != null) {
            return metadata.type;
        }
        return UUID5.fromNamespaceAndString(UUID5.NAMESPACE_CHARACTERISTIC, characteristicClass.getName()).toString();
    }

    @Override
    public String getServiceInstanceType(@NonNull String characteristicType) {
        CharacteristicMetadata metadata = characteristicMetadataMapper.get(characteristicType);
        ServiceMetadata serviceMetadata = serviceMetadataMapper.get(metadata.serviceType);
        if (serviceMetadata != null) {
            return serviceMetadata.instanceType;
        }
        logger.warn("No instance type found for characteristic type: {}", characteristicType);
        return null;
    }

    @Override
    public String getCharacteristicAcceptedItemType(@NonNull String characteristicType) {
        CharacteristicMetadata metadata = characteristicMetadataMapper.get(characteristicType);
        if (metadata != null) {
            return metadata.acceptedItemType;
        }
        logger.warn("No accepted item type found for characteristic type: {}", characteristicType);
        return null;
    }

    // 8. Tag-related methods
    @Override
    public String getTagFromServiceType(@NonNull String serviceType) {
        ServiceMetadata metadata = serviceMetadataMapper.get(serviceType);
        if (metadata != null) {
            return metadata.tag;
        }
        logger.warn("No tag found for service type: {}", serviceType);
        return null;
    }

    @Override
    public String getTagFromCharacteristicType(@NonNull String characteristicType) {
        CharacteristicMetadata metadata = characteristicMetadataMapper.get(characteristicType);
        if (metadata != null) {
            return metadata.tag;
        }
        logger.warn("No tag found for characteristic type: {}", characteristicType);
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
        logger.warn("No channel type UID found for characteristic type: {}", characteristicType);
        return null;
    }

    @Override
    public HashSet<String> getCharacteristicTypes(@Nullable ChannelTypeUID channelTypeUID) {
        return channelTypeCharacteristicTypesMapper.get(channelTypeUID);
    }

    // 10. Support check methods
    @Override
    public boolean supportsThingType(@NonNull ThingTypeUID thingTypeUID) {
        return thingTypeServiceTypesMapper.containsKey(thingTypeUID);
    }

    @Override
    public ThingTypeUID @NonNull [] getSupportedThingTypes() {
        return thingTypeServiceTypesMapper.keySet().toArray(new ThingTypeUID[0]);
    }

    @Override
    public boolean supportsServiceType(@NonNull String serviceType) {
        return serviceMetadataMapper.containsKey(serviceType);
    }

    @Override
    public Set<String> getSupportedServiceTypes() {
        return serviceMetadataMapper.keySet();
    }
    
    @Override
    public boolean supportsCharacteristicsType(@NonNull String characteristicsType) {
        return characteristicMetadataMapper.containsKey(characteristicsType);
    }

    @Override
    public Set<String> getSupportedCharacteristicTypes() {
        return characteristicMetadataMapper.keySet();
    }
}
