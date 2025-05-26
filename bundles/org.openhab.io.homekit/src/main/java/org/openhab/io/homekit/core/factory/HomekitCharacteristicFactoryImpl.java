package org.openhab.io.homekit.core.factory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the HomekitCharacteristicFactory that uses annotations to discover and create characteristics.
 * This factory scans for classes annotated with @HomekitCharacteristicType and creates instances as needed.
 */
@Component(service = HomekitCharacteristicFactory.class)
@NonNullByDefault
public class HomekitCharacteristicFactoryImpl implements HomekitCharacteristicFactory {
    private final Logger logger = LoggerFactory.getLogger(HomekitCharacteristicFactoryImpl.class);
    private final Map<String, Class<? extends HomekitCharacteristic<?>>> characteristicTypes = new ConcurrentHashMap<>();
    private final Map<String, String> tagToTypeMap = new ConcurrentHashMap<>();
    private final HomekitEventManager eventManager;

    @Activate
    public HomekitCharacteristicFactoryImpl(@Reference HomekitEventManager eventManager) {
        this.eventManager = eventManager;
        initializeCharacteristicTypes();
    }

    /**
     * Initializes the characteristic type registry by scanning for annotated characteristic classes.
     */
    @SuppressWarnings("unchecked")
    private void initializeCharacteristicTypes() {
        try {
            // Configure Reflections to scan the characteristic package
            ConfigurationBuilder config = new ConfigurationBuilder()
                    .forPackages("org.openhab.io.homekit.internal.characteristic").setScanners(Scanners.TypesAnnotated);

            Reflections reflections = new Reflections(config);
            Set<Class<?>> characteristicClasses = reflections.getTypesAnnotatedWith(HomekitCharacteristicType.class);

            for (Class<?> characteristicClass : characteristicClasses) {
                if (HomekitCharacteristic.class.isAssignableFrom(characteristicClass)) {
                    HomekitCharacteristicType annotation = characteristicClass
                            .getAnnotation(HomekitCharacteristicType.class);
                    if (annotation != null) {
                        characteristicTypes.put(annotation.type(),
                                (Class<? extends HomekitCharacteristic<?>>) characteristicClass);
                        tagToTypeMap.put(annotation.tag(), annotation.type());
                        logger.debug("Registered characteristic type: {} -> {}", annotation.type(),
                                characteristicClass.getName());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error initializing characteristic types", e);
        }
    }

    @Override
    public HomekitCharacteristic<?> createCharacteristic(String type, HomekitService service) {
        Class<? extends HomekitCharacteristic<?>> characteristicClass = characteristicTypes.get(type);
        if (characteristicClass == null) {
            throw new IllegalArgumentException("Unsupported characteristic type: " + type);
        }

        try {
            return characteristicClass.getConstructor(HomekitService.class, HomekitEventManager.class)
                    .newInstance(service, eventManager);
        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException
                | SecurityException | InvocationTargetException e) {
            logger.error("Error creating characteristic of type {}", type, e);
            throw new IllegalArgumentException("Failed to create characteristic of type: " + type, e);
        }
    }

    @Override
    public HomekitCharacteristic<?> createCharacteristicFromTag(String tag, HomekitService service) {
        String type = tagToTypeMap.get(tag);
        if (type == null) {
            throw new IllegalArgumentException("No characteristic type found for tag: " + tag);
        }
        return createCharacteristic(type, service);
    }

    @Override
    public HomekitCharacteristic<?> createCharacteristicWithArgs(String type, Object... args) {
        Class<? extends HomekitCharacteristic<?>> characteristicClass = characteristicTypes.get(type);
        if (characteristicClass == null) {
            throw new IllegalArgumentException("Unsupported characteristic type: " + type);
        }

        try {
            Class<?>[] argTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                argTypes[i] = args[i].getClass();
            }

            Constructor<? extends HomekitCharacteristic<?>> constructor = characteristicClass.getConstructor(argTypes);
            return constructor.newInstance(args);
        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException
                | SecurityException | InvocationTargetException e) {
            logger.error("Error creating characteristic of type {} with args", type, e);
            throw new IllegalArgumentException("Failed to create characteristic of type: " + type, e);
        }
    }

    @Override
    public boolean supportsCharacteristicType(String type) {
        return characteristicTypes.containsKey(type);
    }

    @Override
    public boolean supportsTag(String tag) {
        return tagToTypeMap.containsKey(tag);
    }

    @Override
    public Set<String> getSupportedCharacteristicTypes() {
        return Collections.unmodifiableSet(characteristicTypes.keySet());
    }

    @Override
    public Set<String> getSupportedTags() {
        return Collections.unmodifiableSet(tagToTypeMap.keySet());
    }

    @Override
    public String getTagFromCharacteristicType(String characteristicType) {
        for (Map.Entry<String, String> entry : tagToTypeMap.entrySet()) {
            if (entry.getValue().equals(characteristicType)) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public String getCharacteristicTypeFromTag(String characteristicTag) {
        return tagToTypeMap.get(characteristicTag);
    }

    @Override
    public Set<String> getAcceptedItemTypes(String characteristicType) {
        Class<? extends HomekitCharacteristic<?>> characteristicClass = characteristicTypes.get(characteristicType);
        if (characteristicClass == null) {
            throw new IllegalArgumentException("Unsupported characteristic type: " + characteristicType);
        }

        HomekitCharacteristicType annotation = characteristicClass.getAnnotation(HomekitCharacteristicType.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Characteristic type " + characteristicType
                    + " does not have a HomekitCharacteristicType annotation");
        }

        return new HashSet<>(Arrays.asList(annotation.acceptedItemTypes()));
    }
}
