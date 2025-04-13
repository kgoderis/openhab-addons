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
import org.openhab.io.homekit.library.service.ThingService;
import org.openhab.io.homekit.util.UUID5;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseHomekitFactory implements HomekitFactory {

    protected static final Logger logger = LoggerFactory.getLogger(BaseHomekitFactory.class);

    private HashMap<ThingTypeUID, Class<? extends Accessory>> thingTypeAccessoryClassMapper = new HashMap<ThingTypeUID, Class<? extends Accessory>>();
    private HashMap<ThingTypeUID, HashSet<String>> thingTypeServiceTypesMapper = new HashMap<ThingTypeUID, @NonNull HashSet<String>>();
    private HashMap<ChannelTypeUID, HashSet<String>> channelTypeCharacteristicTypesMapper = new HashMap<ChannelTypeUID, @NonNull HashSet<String>>();
    private HashMap<String, Class<? extends Service>> serviceTypeServiceClassMapper = new HashMap<String, Class<@NonNull ? extends Service>>();
    private HashMap<String, Class<? extends Characteristic<?>>> characteristicTypeCharacteristicClassMapper = new HashMap<String, Class<@NonNull ? extends Characteristic<?>>>();
    private Map<String, HashSet<Class<? extends Service>>> tagServiceClassMapper = new HashMap<>();
    private Map<String, HashSet<Class<? extends Characteristic<?>>>> tagCharacteristicClassMapper = new HashMap<>();

    public BaseHomekitFactory() {
    }

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
        return serviceTypeServiceClassMapper.containsKey(serviceType);
    }

    @Override
    public boolean supportsCharacteristicsType(@NonNull String characteristicsType) {
        return characteristicTypeCharacteristicClassMapper.containsKey(characteristicsType);
    }

    // @Override
    // public @Nullable Accessory createAccessory(@NonNull Thing thing, @NonNull LocalAccessoryServer server)
    // throws Exception {

    // ThingTypeUID thingType = thing.getThingTypeUID();

    // Class<? extends Accessory> accessoryClass = thingTypeAccessoryClassMapper.get(thingType);

    // if (accessoryClass == null) {
    // accessoryClass = ThingAccessory.class;
    // }

    // Accessory accessory = createAccessory(accessoryClass, server, server.getInstanceId(), true);

    // if (accessory != null && (accessory instanceof ThingAccessory)) {
    // ThingAccessory thingAccessory = (ThingAccessory) accessory;
    // thingAccessory.setThingUID(thing.getUID());
    // logger.info("Linked Thing {} to Accessory {} of Type {}", thing.getUID(), accessory.getUID(),
    // accessory.getClass().getSimpleName());

    // HashSet<String> serviceTypes = thingTypeServiceTypesMapper.get(thingType);
    // for (String serviceType : serviceTypes) {
    // if (thingAccessory.getService(serviceType) == null && thingAccessory.isExtensible()) {
    // thingAccessory
    // .addService(createService(serviceType, thingAccessory, true, thingAccessory.getLabel()));
    // }

    // Service service = (Service) thingAccessory.getService(serviceType);

    // if (service != null) {
    // for (Channel channel : thing.getChannels()) {
    // HashSet<String> characteristicTypes = channelTypeCharacteristicTypesMapper
    // .get(channel.getChannelTypeUID());

    // for (String characteristicType : characteristicTypes) {
    // if (service.getCharacteristic(characteristicType) == null && service.isExtensible()) {
    // service.addCharacteristic(createCharacteristic(characteristicType, service));
    // }

    // Characteristic<?> characteristic = (Characteristic<?>) service
    // .getCharacteristic(characteristicType);
    // if (characteristic != null) {
    // characteristic.setChannelUID(channel.getUID());
    // logger.debug("Linked Channel {} to Characteristic {} of Type {}", channel.getUID(),
    // characteristic.getUID(), characteristic.getClass().getSimpleName());
    // }
    // }
    // }
    // }
    // }

    // return accessory;
    // }

    @Override
    public @Nullable Accessory createAccessory(Class<? extends Accessory> accessoryClass,
            @NonNull AccessoryServer server, long instanceId, boolean extend) {
        try {
            Accessory accessory = accessoryClass.getConstructor(AccessoryServer.class, long.class, boolean.class)
                    .newInstance(server, instanceId, extend);
            logger.debug("Created an Accessory {} of Type {}, with instanceId {}", accessory.getUID(),
                    accessory.getClass().getSimpleName(), accessory.getAccessoryId());
            return accessory;
        } catch (NoSuchMethodException e) {
            logger.warn(
                    "Accessory {} is missing a valid constructor of type (HomekitCommunicationManager.class, AccessoryServer.class, long.class, boolean.class)",
                    accessoryClass.getSimpleName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
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
    public @Nullable Service createService(@NonNull String serviceType, @NonNull Accessory accessory, long instanceId,
            boolean extend, @NonNull String serviceName) {
        Class<? extends Service> serviceClass = serviceTypeServiceClassMapper.get(serviceType);
        if (serviceClass != null) {
            try {
                Service service = serviceClass.getConstructor(Accessory.class, long.class, boolean.class, String.class)
                        .newInstance(accessory, instanceId, extend, serviceName);
                logger.debug(
                        "Created a Service {} of Type {} (HAP Type {}, Name {}) for Accessory {} of Type {}, with instanceId {}",
                        service.getUID(), service.getClass().getSimpleName(), service.getInstanceType(), serviceName,
                        accessory.getUID(), accessory.getClass().getSimpleName(), service.getInstanceId());
                return service;
            } catch (NoSuchMethodException e) {
                logger.warn(
                        "Service {} is missing a valid constructor of type (HomekitCommunicationManager.class, Accessory.class, long.class, boolean.class, String.class))",
                        serviceClass.getSimpleName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public @Nullable Service createService(@NonNull String serviceType, @NonNull Accessory accessory, boolean extend,
            @NonNull String serviceName) {
        Class<? extends Service> serviceClass = serviceTypeServiceClassMapper.get(serviceType);
        if (serviceClass != null) {
            return createService(serviceType, accessory, accessory.getNextAvailableInstanceId(), extend, serviceName);
        }

        return null;
    }

    @Override
    public @Nullable Service createService(@NonNull String serviceType, @NonNull Accessory accessory, long instanceId,
            boolean extend) {
        Class<? extends Service> serviceClass = serviceTypeServiceClassMapper.get(serviceType);
        if (serviceClass != null) {
            return createService(serviceType, accessory, instanceId, extend, serviceClass.getSimpleName());
        }

        return null;
    }

    /**
     * Creates a service instance from a JSON value.
     * 
     * @param accessory The accessory to create the service for
     * @param value The JSON value containing service data
     * @return The created service instance, or null if creation fails
     */
    @Override
    public @Nullable Service createService(@NonNull Accessory accessory, @NonNull JsonValue value) {
        JsonObject jsonObject = (JsonObject) value;
        String type = jsonObject.getString("type");
        long instanceId = jsonObject.getInt("iid");

        Class<? extends Service> serviceClass = serviceTypeServiceClassMapper.get(type);
        if (serviceClass == null) {
            logger.warn("Unknown service type: {}", type);
            return null;
        }

        try {
            Constructor<? extends Service> constructor = serviceClass.getDeclaredConstructor(Accessory.class,
                    long.class, boolean.class);
            constructor.setAccessible(true);
            Service service = constructor.newInstance(accessory, instanceId, false);
            logger.debug("Created service of type {} with instanceId {}", type, instanceId);
            return service;
        } catch (Exception e) {
            logger.error("Failed to create service of type {}: {}", type, e.getMessage());
            return null;
        }
    }

    protected @Nullable Service createServiceByTag(String tag, Accessory accessory, long instanceId, boolean extend) {
        HashSet<Class<? extends Service>> serviceClasses = tagServiceClassMapper.get(tag);
        if (serviceClasses != null && !serviceClasses.isEmpty()) {
            if (serviceClasses != null && !serviceClasses.isEmpty()) {
                Class<? extends Service> serviceClass = serviceClasses.iterator().next();
                try {
                    Constructor<? extends Service> constructor = serviceClass.getConstructor(Accessory.class,
                            long.class, boolean.class, String.class);
                    Service service = constructor.newInstance(accessory, instanceId, extend);
                    return service;
                } catch (NoSuchMethodException | IllegalAccessException | InstantiationException
                        | InvocationTargetException e) {
                    logger.warn("Could not create service for tag {} and class {}", tag, serviceClass.getName(), e);
                }
            }
            logger.warn("No service found with matching tag {}", tag);
        }
        return null;
    }

    @Override
    public @Nullable Characteristic<?> createCharacteristic(@NonNull String characteristicType,
            @NonNull Service service, long instanceId) {
        Class<? extends Characteristic<?>> characteristicsClass = characteristicTypeCharacteristicClassMapper
                .get(characteristicType);
        if (characteristicsClass != null) {
            try {
                Characteristic<?> characteristic = characteristicsClass.getConstructor(Service.class, long.class)
                        .newInstance(service, instanceId);
                logger.debug(
                        "Created a Characteristic {} of Type {} (HAP Type {}) for Service {} of Type {}, with instanceId {}",
                        characteristic.getUID(), characteristic.getClass().getSimpleName(),
                        characteristic.getInstanceType(), service.getUID(), service.getClass().getSimpleName(),
                        characteristic.getInstanceId());
                return characteristic;
            } catch (NoSuchMethodException e) {
                logger.warn(
                        "Characteristic {} is missing a valid constructor of type (HomekitCommunicationManager.class, Service.class, long.class)",
                        characteristicsClass.getSimpleName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public @Nullable Characteristic<?> createCharacteristic(@NonNull String characteristicType,
            @NonNull Service service) {
        Class<? extends Characteristic<?>> characteristicsClass = characteristicTypeCharacteristicClassMapper
                .get(characteristicType);
        if (characteristicsClass != null) {
            return createCharacteristic(characteristicType, service,
                    ((Accessory) service.getAccessory()).getNextAvailableInstanceId());
        }
        return null;
    }

    /**
     * Creates a characteristic instance from a JSON value.
     * 
     * @param service The service to create the characteristic for
     * @param value The JSON value containing characteristic data
     * @return The created characteristic instance, or null if creation fails
     */
    public @Nullable Characteristic<?> createCharacteristic(@NonNull Service service, @NonNull JsonValue value) {
        JsonObject jsonObject = (JsonObject) value;
        String type = jsonObject.getString("type");
        long instanceId = jsonObject.getInt("iid");

        Class<? extends Characteristic<?>> characteristicClass = characteristicTypeCharacteristicClassMapper.get(type);
        if (characteristicClass == null) {
            logger.warn("Unknown characteristic type: {}", type);
            return null;
        }

        try {
            Constructor<? extends Characteristic<?>> constructor = characteristicClass
                    .getDeclaredConstructor(Service.class, long.class);
            constructor.setAccessible(true);
            Characteristic<?> characteristic = constructor.newInstance(service, instanceId);
            logger.debug("Created characteristic of type {} with instanceId {}", type, instanceId);
            return characteristic;
        } catch (Exception e) {
            logger.error("Failed to create characteristic of type {}: {}", type, e.getMessage());
            return null;
        }
    }

    protected @Nullable Characteristic<?> createCharacteristicByTag(String tag, Service service, long instanceId) {
        HashSet<Class<? extends Characteristic<?>>> characteristicClasses = tagCharacteristicClassMapper.get(tag);
        if (characteristicClasses != null && !characteristicClasses.isEmpty()) {
            Class<? extends Characteristic<?>> characteristicClass = characteristicClasses.iterator().next();
            try {
                Constructor<? extends Characteristic<?>> constructor = characteristicClass.getConstructor(Service.class,
                        long.class);
                return constructor.newInstance(service, instanceId);
            } catch (NoSuchMethodException | IllegalAccessException | InstantiationException
                    | InvocationTargetException e) {
                logger.warn("Could not create characteristic for tag {}", tag, e);
            }
        }
        return null;
    }

    @Override
    public void addAccessory(@NonNull ThingTypeUID thingType,
            @NonNull Class<? extends org.openhab.io.homekit.api.hap.Accessory> accessoryClass) {
        thingTypeAccessoryClassMapper.put(thingType, accessoryClass);
    }

    @Override
    public void addService(@NonNull ThingTypeUID type) {
        addService(type, ThingService.class);
    }

    @Override
    public void addService(@NonNull ThingTypeUID thingType, @NonNull Class<@NonNull ? extends Service> serviceClass) {

        String serviceType = UUID5.fromNamespaceAndString(UUID5.NAMESPACE_SERVICE, serviceClass.getName()).toString();
        try {
            Method method = serviceClass.getMethod("getType");
            Object o = method.invoke(null);
            serviceType = (String) o;
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
            // No Op - we revert back to the previously generated type
        } catch (NoSuchMethodException e) {
            logger.warn("{} does not define a getType() method", serviceClass.getName());
        }

        addService(thingType, serviceType);
        addService(serviceType, serviceClass);
    }

    @Override
    public void addService(@NonNull Class<@NonNull ? extends Service> serviceClass) {

        String serviceType = UUID5.fromNamespaceAndString(UUID5.NAMESPACE_SERVICE, serviceClass.getName()).toString();
        try {
            Method method = serviceClass.getMethod("getType");
            Object o = method.invoke(null);
            serviceType = (String) o;
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
            // No Op - we revert back to the previously generated type
        } catch (NoSuchMethodException e) {
            logger.warn("{} does not define a getType() method", serviceClass.getName());
        }

        addService(serviceType, serviceClass);
    }

    @Override
    public void addService(@NonNull ThingTypeUID thingType, @NonNull String serviceType) {
        HashSet<String> currentTypes = thingTypeServiceTypesMapper.get(thingType);
        if (currentTypes == null) {
            currentTypes = new HashSet<String>();
        }
        currentTypes.add(serviceType);
        thingTypeServiceTypesMapper.put(thingType, currentTypes);

        // TODO : Modify to add service tag to a mapper structure
    }

    @Override
    public void addService(@NonNull String serviceType, @NonNull Class<@NonNull ? extends Service> serviceClass) {
        serviceTypeServiceClassMapper.put(serviceType, serviceClass);

        try {
            Method tagMethod = serviceClass.getMethod("getTag");
            String tag = (String) tagMethod.invoke(null);
            tagServiceClassMapper.computeIfAbsent(tag, k -> new HashSet<>()).add(serviceClass);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            logger.warn("Could not get service tag for class {}", serviceClass.getName(), e);
        }
    }

    // @Override
    // public void addServiceWithTag(String tag, Class<? extends Service> serviceClass) {
    // tagServiceClassMapper.computeIfAbsent(tag, k -> new HashSet<>()).add(serviceClass);
    // try {
    // Method method = serviceClass.getMethod("getType");
    // String serviceType = (String) method.invoke(null);
    // serviceTypeServiceClassMapper.put(serviceType, serviceClass);
    // addService(serviceClass);
    // } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
    // logger.warn("Could not get service type for class {}", serviceClass.getName(), e);
    // }
    // }

    @Override
    public void addCharacteristic(@NonNull ChannelTypeUID channelType,
            @NonNull Class<@NonNull ? extends Characteristic<?>> characteristicClass) {

        String characteristicType = UUID5
                .fromNamespaceAndString(UUID5.NAMESPACE_CHARACTERISTIC, characteristicClass.getName()).toString();
        try {
            Method method = characteristicClass.getMethod("getType");
            Object o = method.invoke(null);
            characteristicType = (String) o;
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
            // No Op - we revert back to the previously generated type
        } catch (NoSuchMethodException e) {
            logger.warn("{} does not define a getType() method", characteristicClass.getName());
        }

        addCharacteristic(channelType, characteristicType);
        addCharacteristic(characteristicType, characteristicClass);
    }

    @Override
    public void addCharacteristic(@NonNull Class<@NonNull ? extends Characteristic<?>> characteristicClass) {

        String characteristicType = UUID5
                .fromNamespaceAndString(UUID5.NAMESPACE_CHARACTERISTIC, characteristicClass.getName()).toString();
        try {
            Method method = characteristicClass.getMethod("getType");
            Object o = method.invoke(null);
            characteristicType = (String) o;
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
            // No Op - we revert back to the previously generated type
        } catch (NoSuchMethodException e) {
            logger.warn("{} does not define a getType() method", characteristicClass.getName());
        }

        addCharacteristic(characteristicType, characteristicClass);
    }

    @Override
    public void addCharacteristic(@NonNull ChannelTypeUID channelType, @NonNull String characteristicType) {
        HashSet<String> currentTypes = channelTypeCharacteristicTypesMapper.get(channelType);
        if (currentTypes == null) {
            currentTypes = new HashSet<String>();
        }
        currentTypes.add(characteristicType);
        channelTypeCharacteristicTypesMapper.put(channelType, currentTypes);
    }

    @Override
    public void addCharacteristic(@NonNull String characteristicType,
            @NonNull Class<@NonNull ? extends Characteristic<?>> characteristicClass) {
        characteristicTypeCharacteristicClassMapper.put(characteristicType, characteristicClass);

        try {
            Method tagMethod = characteristicClass.getMethod("getTag");
            String tag = (String) tagMethod.invoke(null);
            HashSet<Class<? extends Characteristic<?>>> characteristics = tagCharacteristicClassMapper.get(tag);
            if (characteristics == null) {
                characteristics = new HashSet<>();
            }
            characteristics.add(characteristicClass);
            tagCharacteristicClassMapper.put(tag, characteristics);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            logger.debug("Characteristic {} does not define a getTag() method", characteristicClass.getName());
        }
    }

    @Override
    public HashSet<String> getCharacteristicTypes(@Nullable ChannelTypeUID channelType) {
        return channelTypeCharacteristicTypesMapper.get(channelType);
    }

    @Override
    public Set<String> getSupportedCharacteristicTypes() {
        Set<String> result = new HashSet<String>();
        for (Class<? extends Characteristic<?>> characteristicClass : characteristicTypeCharacteristicClassMapper
                .values()) {
            try {
                Method method = characteristicClass.getMethod("getType");
                result.add((String) method.invoke(null));
            } catch (NoSuchMethodException e) {
                logger.warn("Characteristic {} is missing the method getAcceptedItemType()",
                        characteristicClass.getSimpleName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    @Override
    public Set<String> getSupportedServiceTypes() {
        return serviceTypeServiceClassMapper.values().stream().map(s -> s.getSimpleName()).collect(Collectors.toSet());
    }

    @Override
    public String getCharacteristicAcceptedItemType(@NonNull String characteristicType) {
        Class<? extends Characteristic<?>> characteristicClass = characteristicTypeCharacteristicClassMapper
                .get(characteristicType);
        if (characteristicClass != null) {
            try {
                Method method = characteristicClass.getMethod("getAcceptedItemType");
                return (String) method.invoke(null, null);
            } catch (NoSuchMethodException e) {
                logger.warn("Characteristic {} is missing the method getAcceptedItemType()",
                        characteristicClass.getSimpleName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public @Nullable ChannelTypeUID getChannelTypeUID(@NonNull String characteristicType) {
        Class<? extends Characteristic<?>> characteristicClass = characteristicTypeCharacteristicClassMapper
                .get(characteristicType);
        if (characteristicClass != null) {
            try {
                Method method = characteristicClass.getMethod("getChannelTypeUID");
                return (ChannelTypeUID) method.invoke(null);
            } catch (NoSuchMethodException e) {
                logger.warn("Characteristic {} is missing the method getChannelTypeUID()",
                        characteristicClass.getSimpleName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public Class<? extends Characteristic<?>> getCharacteristic(@NonNull String characteristicType) {
        return characteristicTypeCharacteristicClassMapper.get(characteristicType);
    }

    @Override
    public Class<? extends Service> getService(@NonNull String serviceType) {
        return serviceTypeServiceClassMapper.get(serviceType);
    }

    @Override
    public boolean isCharacteristicSupported(@NonNull String characteristicType) {
        return characteristicTypeCharacteristicClassMapper.containsKey(characteristicType);
    }

    @Override
    public boolean isServiceSupported(@NonNull String serviceType) {
        return serviceTypeServiceClassMapper.containsKey(serviceType);
    }

    @Override
    public String getServiceTypeFromTag(@NonNull String tag) {
        HashSet<Class<? extends Service>> serviceClasses = tagServiceClassMapper.get(tag);
        if (serviceClasses != null && !serviceClasses.isEmpty()) {
            Class<? extends Service> serviceClass = serviceClasses.iterator().next();
            for (String serviceType : serviceTypeServiceClassMapper.keySet()) {
                if (serviceTypeServiceClassMapper.get(serviceType).equals(serviceClass)) {
                    return serviceType;
                }
            }
        }
        return null; // Return empty string instead of null
    }

    @Override
    public String getCharacteristicTypeFromTag(@NonNull String tag) {
        HashSet<Class<? extends Characteristic<?>>> characteristicClasses = tagCharacteristicClassMapper.get(tag);
        if (characteristicClasses != null && !characteristicClasses.isEmpty()) {
            Class<? extends Characteristic<?>> characteristicClass = characteristicClasses.iterator().next();
            for (String characteristicType : characteristicTypeCharacteristicClassMapper.keySet()) {
                if (characteristicTypeCharacteristicClassMapper.get(characteristicType).equals(characteristicClass)) {
                    return characteristicType;
                }
            }
        }
        return null; // Return empty string instead of null
    }

    @Override
    public String getTagFromServiceType(@NonNull String serviceType) {
        Class<? extends Service> serviceClass = serviceTypeServiceClassMapper.get(serviceType);
        if (serviceClass != null) {
            for (Map.Entry<String, HashSet<Class<? extends Service>>> entry : tagServiceClassMapper.entrySet()) {
                if (entry.getValue().contains(serviceClass)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }
}
