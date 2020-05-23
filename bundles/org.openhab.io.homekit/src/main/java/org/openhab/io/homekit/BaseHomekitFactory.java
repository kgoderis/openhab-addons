package org.openhab.io.homekit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.api.Characteristic;
import org.openhab.io.homekit.api.HomekitFactory;
import org.openhab.io.homekit.api.LocalAccessoryServer;
import org.openhab.io.homekit.api.ManagedAccessory;
import org.openhab.io.homekit.api.ManagedCharacteristic;
import org.openhab.io.homekit.api.ManagedService;
import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.internal.accessory.GenericAccessory;
import org.openhab.io.homekit.library.accessory.ThingAccessory;
import org.openhab.io.homekit.library.service.ThingService;
import org.openhab.io.homekit.util.UUID5;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseHomekitFactory implements HomekitFactory {

    protected static final Logger logger = LoggerFactory.getLogger(BaseHomekitFactory.class);

    protected HomekitServiceTracker<@NonNull HomekitCommunicationManager> communicationManagerTracker;

    HashMap<ThingTypeUID, Class<? extends ManagedAccessory>> thingTypeAccessoryClassMapper = new HashMap<ThingTypeUID, Class<? extends ManagedAccessory>>();
    HashMap<ThingTypeUID, HashSet<String>> thingTypeServiceTypesMapper = new HashMap<ThingTypeUID, @NonNull HashSet<String>>();
    HashMap<ChannelTypeUID, HashSet<String>> channelTypeCharacteristicTypesMapper = new HashMap<ChannelTypeUID, @NonNull HashSet<String>>();
    HashMap<String, Class<? extends ManagedService>> serviceTypeServiceClassMapper = new HashMap<String, Class<@NonNull ? extends ManagedService>>();
    HashMap<String, Class<? extends ManagedCharacteristic<?>>> characteristicTypeCharacteristicClassMapper = new HashMap<String, Class<@NonNull ? extends ManagedCharacteristic<?>>>();

    public BaseHomekitFactory() {
        communicationManagerTracker = HomekitServiceTracker.supply(HomekitCommunicationManager.class, getClass());
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

    @Override
    public @Nullable ManagedAccessory createAccessory(@NonNull Thing thing, @NonNull LocalAccessoryServer server)
            throws Exception {

        ThingTypeUID thingType = thing.getThingTypeUID();

        Class<? extends ManagedAccessory> accessoryClass = thingTypeAccessoryClassMapper.get(thingType);

        if (accessoryClass == null) {
            accessoryClass = ThingAccessory.class;
        }

        ManagedAccessory accessory = createAccessory(accessoryClass, server, server.getInstanceId(), true);

        if (accessory != null && (accessory instanceof ThingAccessory)) {
            ThingAccessory thingAccessory = (ThingAccessory) accessory;
            thingAccessory.setThingUID(thing.getUID());
            logger.info("Linked Thing {} to Accessory {} of Type {}", thing.getUID(), accessory.getUID(),
                    accessory.getClass().getSimpleName());

            HashSet<String> serviceTypes = thingTypeServiceTypesMapper.get(thingType);
            for (String serviceType : serviceTypes) {
                if (thingAccessory.getService(serviceType) == null && thingAccessory.isExtensible()) {
                    thingAccessory
                            .addService(createService(serviceType, thingAccessory, true, thingAccessory.getLabel()));
                }

                ManagedService service = (ManagedService) thingAccessory.getService(serviceType);

                if (service != null) {
                    for (Channel channel : thing.getChannels()) {
                        HashSet<String> characteristicTypes = channelTypeCharacteristicTypesMapper
                                .get(channel.getChannelTypeUID());

                        for (String characteristicType : characteristicTypes) {
                            if (service.getCharacteristic(characteristicType) == null && service.isExtensible()) {
                                service.addCharacteristic(createCharacteristic(characteristicType, service));
                            }

                            ManagedCharacteristic<?> characteristic = (ManagedCharacteristic<?>) service
                                    .getCharacteristic(characteristicType);
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

        return accessory;
    }

    @Override
    public @Nullable ManagedAccessory createAccessory(Class<? extends ManagedAccessory> accessoryClass,
            AccessoryServer server, long instanceId, boolean extend) {
        try {
            ManagedAccessory accessory = accessoryClass
                    .getConstructor(HomekitCommunicationManager.class, AccessoryServer.class, long.class, boolean.class)
                    .newInstance(communicationManagerTracker.get(), server, instanceId, extend);
            logger.debug("Created an Accessory {} of Type {}, with instanceId {}", accessory.getUID(),
                    accessory.getClass().getSimpleName(), accessory.getId());
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
    public @Nullable GenericAccessory createAccessory(Class<? extends GenericAccessory> accessoryClass,
            AccessoryServer server, long instanceId) {
        try {
            GenericAccessory accessory = accessoryClass.getConstructor(AccessoryServer.class, long.class)
                    .newInstance(server, instanceId);
            logger.debug("Created an Accessory {} of Type {}, with instanceId {}", accessory.getUID(),
                    accessory.getClass().getSimpleName(), accessory.getId());
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
    public @Nullable ManagedService createService(@NonNull String serviceType, @NonNull ManagedAccessory accessory,
            long instanceId, boolean extend, @NonNull String serviceName) {
        Class<? extends ManagedService> serviceClass = serviceTypeServiceClassMapper.get(serviceType);
        if (serviceClass != null) {
            try {
                ManagedService service = serviceClass
                        .getConstructor(HomekitCommunicationManager.class, ManagedAccessory.class, long.class,
                                boolean.class, String.class)
                        .newInstance(communicationManagerTracker.get(), accessory, instanceId, extend, serviceName);
                logger.debug(
                        "Created a Service {} of Type {} (HAP Type {}, Name {}) for Accessory {} of Type {}, with instanceId {}",
                        service.getUID(), service.getClass().getSimpleName(), service.getInstanceType(), serviceName,
                        accessory.getUID(), accessory.getClass().getSimpleName(), service.getId());
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
    public @Nullable ManagedService createService(@NonNull String serviceType, @NonNull ManagedAccessory accessory,
            boolean extend, @NonNull String serviceName) {
        Class<? extends ManagedService> serviceClass = serviceTypeServiceClassMapper.get(serviceType);
        if (serviceClass != null) {
            return createService(serviceType, accessory, accessory.getInstanceId(), extend, serviceName);
        }

        return null;
    }

    @Override
    public @Nullable ManagedService createService(@NonNull String serviceType, @NonNull ManagedAccessory accessory,
            long instanceId, boolean extend) {
        Class<? extends ManagedService> serviceClass = serviceTypeServiceClassMapper.get(serviceType);
        if (serviceClass != null) {
            return createService(serviceType, accessory, instanceId, extend, serviceClass.getSimpleName());
        }

        return null;
    }

    @Override
    public @Nullable ManagedCharacteristic<?> createCharacteristic(@NonNull String characteristicType,
            @NonNull ManagedService service, long instanceId) {
        Class<? extends ManagedCharacteristic<?>> characteristicsClass = characteristicTypeCharacteristicClassMapper
                .get(characteristicType);
        if (characteristicsClass != null) {
            try {

                ManagedCharacteristic<?> characteristic = characteristicsClass
                        .getConstructor(HomekitCommunicationManager.class, ManagedService.class, long.class)
                        .newInstance(communicationManagerTracker.get(), service, instanceId);
                logger.debug(
                        "Created a Characteristic {} of Type {} (HAP Type {}) for Service {} of Type {}, with instanceId {}",
                        characteristic.getUID(), characteristic.getClass().getSimpleName(),
                        characteristic.getInstanceType(), service.getUID(), service.getClass().getSimpleName(),
                        characteristic.getId());
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
    public @Nullable ManagedCharacteristic<?> createCharacteristic(@NonNull String characteristicType,
            @NonNull ManagedService service) {
        Class<? extends ManagedCharacteristic<?>> characteristicsClass = characteristicTypeCharacteristicClassMapper
                .get(characteristicType);
        if (characteristicsClass != null) {
            return createCharacteristic(characteristicType, service,
                    ((ManagedAccessory) service.getAccessory()).getInstanceId());
        }
        return null;
    }

    @Override
    public void addAccessory(@NonNull ThingTypeUID thingType,
            @NonNull Class<? extends @NonNull ManagedAccessory> accessoryClass) {
        thingTypeAccessoryClassMapper.put(thingType, accessoryClass);
    }

    @Override
    public void addService(@NonNull ThingTypeUID type) {
        addService(type, ThingService.class);
    }

    @Override
    public void addService(@NonNull ThingTypeUID thingType,
            @NonNull Class<@NonNull ? extends ManagedService> serviceClass) {

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
    public void addService(@NonNull Class<@NonNull ? extends ManagedService> serviceClass) {

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
    }

    @Override
    public void addService(@NonNull String serviceType,
            @NonNull Class<@NonNull ? extends ManagedService> serviceClass) {
        serviceTypeServiceClassMapper.put(serviceType, serviceClass);
    }

    @Override
    public void addCharacteristic(@NonNull ChannelTypeUID channelType,
            @NonNull Class<@NonNull ? extends ManagedCharacteristic<?>> characteristicClass) {

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
    public void addCharacteristic(@NonNull Class<@NonNull ? extends ManagedCharacteristic<?>> characteristicClass) {

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
    public void addCharacteristic(@NonNull String characateristicUID,
            @NonNull Class<@NonNull ? extends ManagedCharacteristic<?>> characteristicClass) {
        characteristicTypeCharacteristicClassMapper.put(characateristicUID, characteristicClass);
    }

    @Override
    public HashSet<String> getCharacteristicTypes(@Nullable ChannelTypeUID channelType) {
        return channelTypeCharacteristicTypesMapper.get(channelType);
    }

    @Override
    public Set<String> getSupportedCharacteristicTypes() {
        Set<String> result = new HashSet<String>();
        for (Class<? extends ManagedCharacteristic<?>> characteristicClass : characteristicTypeCharacteristicClassMapper
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
        Class<? extends ManagedCharacteristic<?>> characteristicClass = characteristicTypeCharacteristicClassMapper
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
        Class<? extends ManagedCharacteristic<?>> characteristicClass = characteristicTypeCharacteristicClassMapper
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
    public Class<? extends Characteristic> getCharacteristic(@NonNull String characteristicType) {
        return characteristicTypeCharacteristicClassMapper.get(characteristicType);
    }

    @Override
    public Class<? extends Service> getService(@NonNull String serviceType) {
        return serviceTypeServiceClassMapper.get(serviceType);
    }

}
