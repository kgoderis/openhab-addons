package org.openhab.io.homekit.core.factory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.accessory.HomekitAccessoryType;
import org.openhab.io.homekit.api.factory.HomekitAccessoryFactory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.factory.HomekitServiceFactory;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the HomekitAccessoryFactory that uses annotations to discover and create accessories.
 * This factory scans for classes annotated with @HomekitAccessoryType and creates instances as needed.
 */
@Component(service = HomekitAccessoryFactory.class)
@NonNullByDefault
public class HomekitAccessoryFactoryImpl implements HomekitAccessoryFactory {
    private final Logger logger = LoggerFactory.getLogger(HomekitAccessoryFactoryImpl.class);
    private final Map<String, Class<? extends HomekitAccessory>> accessoryTypes = new ConcurrentHashMap<>();
    private final Map<String, String> tagToTypeMap = new ConcurrentHashMap<>();
    private HomekitEventManager eventManager;
    private HomekitServiceFactory serviceFactory;
    private HomekitCharacteristicFactory characteristicFactory;

    public HomekitAccessoryFactoryImpl(@Reference HomekitEventManager eventManager,
            @Reference HomekitServiceFactory serviceFactory,
            @Reference HomekitCharacteristicFactory characteristicFactory) {
        this.eventManager = eventManager;
        this.serviceFactory = serviceFactory;
        this.characteristicFactory = characteristicFactory;
        initializeAccessoryTypes();
    }

    /**
     * Initializes the accessory type registry by scanning for annotated accessory classes.
     */
    @SuppressWarnings("unchecked")
    private void initializeAccessoryTypes() {
        try {
            // Configure Reflections to scan the accessory package
            ConfigurationBuilder config = new ConfigurationBuilder()
                    .forPackages("org.openhab.io.homekit.internal.accessory").setScanners(Scanners.TypesAnnotated);

            Reflections reflections = new Reflections(config);
            Set<Class<?>> accessoryClasses = reflections.getTypesAnnotatedWith(HomekitAccessoryType.class);

            for (Class<?> accessoryClass : accessoryClasses) {
                if (HomekitAccessory.class.isAssignableFrom(accessoryClass)) {
                    HomekitAccessoryType annotation = accessoryClass.getAnnotation(HomekitAccessoryType.class);
                    if (annotation != null) {
                        accessoryTypes.put(annotation.type(), (Class<? extends HomekitAccessory>) accessoryClass);
                        tagToTypeMap.put(annotation.tag(), annotation.type());
                        logger.debug("Registered accessory type: {} -> {}", annotation.type(),
                                accessoryClass.getName());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error initializing accessory types", e);
        }
    }

    @Override
    public HomekitAccessory createAccessory(String type) {
        Class<? extends HomekitAccessory> accessoryClass = accessoryTypes.get(type);
        if (accessoryClass == null) {
            throw new IllegalArgumentException("Unsupported accessory type: " + type);
        }

        try {
            return accessoryClass.getConstructor(HomekitEventManager.class).newInstance(eventManager);
        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException
                | SecurityException | InvocationTargetException e) {
            logger.error("Error creating accessory of type {}", type, e);
            throw new IllegalArgumentException("Failed to create accessory of type: " + type, e);
        }
    }

    @Override
    public HomekitAccessory createAccessoryFromTag(String tag) {
        String type = tagToTypeMap.get(tag);
        if (type == null) {
            throw new IllegalArgumentException("No accessory type found for tag: " + tag);
        }
        return createAccessory(type);
    }

    @Override
    public HomekitAccessory createAccessoryWithArgs(String type, Object... args) {
        Class<? extends HomekitAccessory> accessoryClass = accessoryTypes.get(type);
        if (accessoryClass == null) {
            throw new IllegalArgumentException("Unsupported accessory type: " + type);
        }

        try {
            Class<?>[] argTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                argTypes[i] = args[i].getClass();
            }

            Constructor<? extends HomekitAccessory> constructor = accessoryClass.getConstructor(argTypes);
            return constructor.newInstance(args);
        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException
                | SecurityException | InvocationTargetException e) {
            logger.error("Error creating accessory of type {} with args", type, e);
            throw new IllegalArgumentException("Failed to create accessory of type: " + type, e);
        }
    }

    @Override
    public HomekitAccessory createAccessoryFromTagWithValue(String tag, JsonValue value) {
        String type = tagToTypeMap.get(tag);
        if (type == null) {
            throw new IllegalArgumentException("No accessory type found for tag: " + tag);
        }

        Object[] args = new Object[4];
        args[0] = eventManager;
        args[1] = serviceFactory;
        args[2] = characteristicFactory;
        args[3] = value;

        return createAccessoryWithArgs(type, args);
    }

    public HomekitAccessory createAccessoryFromTagWithArgs(String tag, Object... args) {
        String type = tagToTypeMap.get(tag);
        if (type == null) {
            throw new IllegalArgumentException("No accessory type found for tag: " + tag);
        }
        return createAccessoryWithArgs(type, args);
    }

    @Override
    public boolean supportsAccessoryType(String type) {
        return accessoryTypes.containsKey(type);
    }

    @Override
    public boolean supportsTag(String tag) {
        return tagToTypeMap.containsKey(tag);
    }

    @Override
    public Set<String> getSupportedAccessoryTypes() {
        return Collections.unmodifiableSet(accessoryTypes.keySet());
    }

    @Override
    public Set<String> getSupportedTags() {
        return Collections.unmodifiableSet(tagToTypeMap.keySet());
    }
}
