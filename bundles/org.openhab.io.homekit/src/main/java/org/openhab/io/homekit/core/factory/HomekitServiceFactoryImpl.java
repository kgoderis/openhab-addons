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
import org.openhab.io.homekit.api.factory.HomekitServiceFactory;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.api.service.HomekitServiceType;
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
 * Implementation of the HomekitServiceFactory that uses annotations to discover and create services.
 * This factory scans for classes annotated with @HomekitServiceType and creates instances as needed.
 */
@Component(service = HomekitServiceFactory.class)
@NonNullByDefault
public class HomekitServiceFactoryImpl implements HomekitServiceFactory {
    private final Logger logger = LoggerFactory.getLogger(HomekitServiceFactoryImpl.class);
    private final Map<String, Class<? extends HomekitService>> serviceTypes = new ConcurrentHashMap<>();
    private final Map<String, String> tagToTypeMap = new ConcurrentHashMap<>();
    private final HomekitEventManager eventManager;

    @Activate
    public HomekitServiceFactoryImpl(@Reference HomekitEventManager eventManager) {
        this.eventManager = eventManager;
        initializeServiceTypes();
    }

    /**
     * Initializes the service type registry by scanning for annotated service classes.
     */
    @SuppressWarnings("unchecked")
    private void initializeServiceTypes() {
        try {
            // Configure Reflections to scan the service package
            ConfigurationBuilder config = new ConfigurationBuilder()
                    .forPackages("org.openhab.io.homekit.internal.service").setScanners(Scanners.TypesAnnotated);

            Reflections reflections = new Reflections(config);
            Set<Class<?>> serviceClasses = reflections.getTypesAnnotatedWith(HomekitServiceType.class);

            for (Class<?> serviceClass : serviceClasses) {
                if (HomekitService.class.isAssignableFrom(serviceClass)) {
                    HomekitServiceType annotation = serviceClass.getAnnotation(HomekitServiceType.class);
                    if (annotation != null) {
                        serviceTypes.put(annotation.type(), (Class<? extends HomekitService>) serviceClass);
                        tagToTypeMap.put(annotation.tag(), annotation.type());
                        logger.debug("Registered service type: {} -> {}", annotation.type(), serviceClass.getName());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error initializing service types", e);
        }
    }

    @Override
    public HomekitService createService(String type, HomekitAccessory accessory) {
        Class<? extends HomekitService> serviceClass = serviceTypes.get(type);
        if (serviceClass == null) {
            throw new IllegalArgumentException("Unsupported service type: " + type);
        }

        try {
            return serviceClass.getConstructor(HomekitAccessory.class, HomekitEventManager.class).newInstance(accessory,
                    eventManager);
        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException
                | SecurityException | InvocationTargetException e) {
            logger.error("Error creating service of type {}", type, e);
            throw new IllegalArgumentException("Failed to create service of type: " + type, e);
        }
    }

    /**
     * Creates a service instance with variable arguments.
     * This method allows for more flexible service creation by accepting any number of constructor arguments.
     *
     * @param type the service type identifier
     * @param args the constructor arguments
     * @return a new service instance
     * @throws IllegalArgumentException if the service type is not supported or creation fails
     */
    @Override
    public HomekitService createServiceWithArgs(String type, Object... args) {
        Class<? extends HomekitService> serviceClass = serviceTypes.get(type);
        if (serviceClass == null) {
            throw new IllegalArgumentException("Unsupported service type: " + type);
        }

        try {
            Class<?>[] argTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                argTypes[i] = args[i].getClass();
            }

            Constructor<? extends HomekitService> constructor = serviceClass.getConstructor(argTypes);
            return constructor.newInstance(args);
        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException
                | SecurityException | InvocationTargetException e) {
            logger.error("Error creating service of type {} with args", type, e);
            throw new IllegalArgumentException("Failed to create service of type: " + type, e);
        }
    }

    @Override
    public HomekitService createServiceFromTag(String tag, HomekitAccessory accessory) {
        String type = tagToTypeMap.get(tag);
        if (type == null) {
            throw new IllegalArgumentException("No service type found for tag: " + tag);
        }
        return createService(type, accessory);
    }

    @Override
    public boolean supportsServiceType(String type) {
        return serviceTypes.containsKey(type);
    }

    /**
     * Checks if the factory supports creating services with the given tag.
     *
     * @param tag the service tag to check
     * @return true if the tag is supported, false otherwise
     */
    @Override
    public boolean supportsTag(String tag) {
        return tagToTypeMap.containsKey(tag);
    }

    /**
     * Returns a set of all supported service tags.
     *
     * @return an unmodifiable set of supported service tags
     */
    @Override
    public Set<String> getSupportedTags() {
        return Collections.unmodifiableSet(tagToTypeMap.keySet());
    }

    @Override
    public Set<String> getSupportedServiceTypes() {
        return Collections.unmodifiableSet(serviceTypes.keySet());
    }

    /**
     * Creates a service instance from a JSON value.
     * This method extracts the service type from the JSON value and creates the appropriate service.
     *
     * @param accessory the accessory that will own the service
     * @param value the JSON value containing service configuration
     * @return a new service instance
     * @throws IllegalArgumentException if the service type is not supported or creation fails
     */
    public HomekitService createService(HomekitAccessory accessory, JsonValue value) {
        if (value == null || !value.getValueType().equals(JsonValue.ValueType.OBJECT)) {
            throw new IllegalArgumentException("Invalid JSON value for service creation");
        }

        String type = value.asJsonObject().getString("type", null);
        if (type == null) {
            throw new IllegalArgumentException("Service type not found in JSON value");
        }

        Class<? extends HomekitService> serviceClass = serviceTypes.get(type);
        if (serviceClass == null) {
            throw new IllegalArgumentException("Unsupported service type: " + type);
        }

        try {
            return serviceClass.getConstructor(HomekitAccessory.class, JsonValue.class).newInstance(accessory, value);
        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException
                | SecurityException | InvocationTargetException e) {
            logger.error("Error creating service from JSON value", e);
            throw new IllegalArgumentException("Failed to create service from JSON value", e);
        }
    }

    @Override
    public String getTagFromServiceType(String serviceType) {
        for (Map.Entry<String, String> entry : tagToTypeMap.entrySet()) {
            if (entry.getValue().equals(serviceType)) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public String getServiceTypeFromTag(String serviceTag) {
        return tagToTypeMap.get(serviceTag);
    }
}
