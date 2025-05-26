package org.openhab.io.homekit.core.factory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashSet;
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
 * It maintains registries of service types, tags, and their associated characteristics.
 *
 * <p>
 * The factory uses reflection to discover and register service types during initialization.
 * Each service type is associated with its mandatory and optional characteristics, which are
 * determined by analyzing the service class methods.
 * </p>
 *
 * <p>
 * The factory supports creating services in multiple ways:
 * <ul>
 *   <li>Direct creation from a service type</li>
 *   <li>Creation from a service tag</li>
 *   <li>Creation with variable constructor arguments</li>
 *   <li>Creation from JSON configuration</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@Component(service = HomekitServiceFactory.class)
@NonNullByDefault
public class HomekitServiceFactoryImpl implements HomekitServiceFactory {
    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit ServiceFactory: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "HomekitAccessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    protected static final String LOG_TRACE = LOG_PREFIX + "Trace - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitServiceFactoryImpl.class);
    private final Map<String, Class<? extends HomekitService>> serviceTypes = new ConcurrentHashMap<>();
    private final Map<String, String> tagToTypeMap = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Set<String>>> serviceCharacteristicTypes = new ConcurrentHashMap<>();
    private final HomekitEventManager eventManager;

    /**
     * Creates a new HomekitServiceFactoryImpl instance.
     * Initializes the factory with required dependencies and starts service type discovery.
     *
     * <p>
     * The factory will scan for annotated service classes and register them during initialization.
     * Each service type will be associated with its characteristics based on method analysis.
     * </p>
     *
     * @param eventManager The event manager for handling HomeKit events
     * @throws IllegalArgumentException if eventManager is null
     * @since 1.0
     */
    @Activate
    public HomekitServiceFactoryImpl(@Reference HomekitEventManager eventManager) {
        this.eventManager = eventManager;
        logger.debug("{}Initializing HomekitServiceFactory", LOG_INIT);
        initializeServiceTypes();
    }

    /**
     * Initializes the service type registry by scanning for annotated service classes.
     * Uses reflection to discover and register all service types and their characteristics.
     *
     * <p>
     * The initialization process:
     * <ol>
     *   <li>Scans the service package for @HomekitServiceType annotations</li>
     *   <li>Analyzes each service class for characteristic methods</li>
     *   <li>Registers service types and their characteristics</li>
     *   <li>Builds the tag-to-type mapping</li>
     * </ol>
     * </p>
     *
     * @throws IllegalStateException if service type initialization fails
     * @since 1.0
     */
    @SuppressWarnings("unchecked")
    private void initializeServiceTypes() {
        logger.debug("{}Starting service type initialization", LOG_INIT);
        try {
            // Configure Reflections to scan the service package
            ConfigurationBuilder config = new ConfigurationBuilder()
                    .forPackages("org.openhab.io.homekit.internal.service").setScanners(Scanners.TypesAnnotated);

            Reflections reflections = new Reflections(config);
            Set<Class<?>> serviceClasses = reflections.getTypesAnnotatedWith(HomekitServiceType.class);
            logger.trace("{}Found {} annotated service classes", LOG_TRACE, serviceClasses.size());

            for (Class<?> serviceClass : serviceClasses) {
                if (HomekitService.class.isAssignableFrom(serviceClass)) {
                    HomekitServiceType annotation = serviceClass.getAnnotation(HomekitServiceType.class);
                    if (annotation != null) {
                        String type = annotation.type();
                        serviceTypes.put(type, (Class<? extends HomekitService>) serviceClass);
                        tagToTypeMap.put(annotation.tag(), type);
                        
                        // Initialize characteristic types map using reflection
                        Map<String, Set<String>> characteristicTypes = new ConcurrentHashMap<>();
                        Set<String> mandatory = new HashSet<>();
                        Set<String> optional = new HashSet<>();
                        
                        // Get all methods that return HomekitCharacteristic
                        for (java.lang.reflect.Method method : serviceClass.getMethods()) {
                            if (method.getReturnType().getName().contains("HomekitCharacteristic")) {
                                String characteristicType = method.getName().replace("get", "");
                                if (method.getName().startsWith("getMandatory")) {
                                    mandatory.add(characteristicType);
                                    logger.trace("{}Added mandatory characteristic: {} for service: {}", 
                                        LOG_TRACE, characteristicType, type);
                                } else if (method.getName().startsWith("getOptional")) {
                                    optional.add(characteristicType);
                                    logger.trace("{}Added optional characteristic: {} for service: {}", 
                                        LOG_TRACE, characteristicType, type);
                                }
                            }
                        }
                        
                        characteristicTypes.put("mandatory", mandatory);
                        characteristicTypes.put("optional", optional);
                        serviceCharacteristicTypes.put(type, characteristicTypes);
                        
                        logger.debug("{}Registered service type: {} -> {}", LOG_STATE, type, serviceClass.getName());
                    }
                }
            }
            logger.info("{}Successfully initialized {} service types", LOG_INIT, serviceTypes.size());
        } catch (Exception e) {
            logger.error("{}Error initializing service types: {}", LOG_ERROR, e.getMessage(), e);
            throw new IllegalStateException("Failed to initialize service types", e);
        }
    }

    /**
     * Creates a new service instance for the specified type and accessory.
     *
     * <p>
     * The service is created using the standard constructor that takes an accessory
     * and event manager. The service type must be previously registered during
     * initialization.
     * </p>
     *
     * @param type The service type to create
     * @param accessory The accessory that will own the service
     * @return A new service instance
     * @throws IllegalArgumentException if the service type is not supported or creation fails
     * @since 1.0
     */
    @Override
    public HomekitService createService(String type, HomekitAccessory accessory) {
        logger.trace("{}Creating service of type: {} for accessory: {}", LOG_TRACE, type, accessory.getUID());
        Class<? extends HomekitService> serviceClass = serviceTypes.get(type);
        if (serviceClass == null) {
            logger.error("{}Unsupported service type: {}", LOG_ERROR, type);
            throw new IllegalArgumentException("Unsupported service type: " + type);
        }

        try {
            return serviceClass.getConstructor(HomekitAccessory.class, HomekitEventManager.class)
                    .newInstance(accessory, eventManager);
        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException
                | SecurityException | InvocationTargetException e) {
            logger.error("{}Error creating service of type {}: {}", LOG_ERROR, type, e.getMessage(), e);
            throw new IllegalArgumentException("Failed to create service of type: " + type, e);
        }
    }

    /**
     * Creates a service instance with variable arguments.
     * This method allows for more flexible service creation by accepting any number of constructor arguments.
     *
     * <p>
     * The method will:
     * <ol>
     *   <li>Determine the appropriate constructor based on argument types</li>
     *   <li>Create a new service instance using the constructor</li>
     *   <li>Return the created service</li>
     * </ol>
     * </p>
     *
     * @param type The service type to create
     * @param args The constructor arguments
     * @return A new service instance
     * @throws IllegalArgumentException if the service type is not supported or creation fails
     * @since 1.0
     */
    @Override
    public HomekitService createServiceWithArgs(String type, Object... args) {
        logger.trace("{}Creating service of type: {} with {} arguments", LOG_TRACE, type, args.length);
        Class<? extends HomekitService> serviceClass = serviceTypes.get(type);
        if (serviceClass == null) {
            logger.error("{}Unsupported service type: {}", LOG_ERROR, type);
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
            logger.error("{}Error creating service of type {} with args: {}", LOG_ERROR, type, e.getMessage(), e);
            throw new IllegalArgumentException("Failed to create service of type: " + type, e);
        }
    }

    /**
     * Creates a service instance from a service tag.
     *
     * <p>
     * The method will:
     * <ol>
     *   <li>Look up the service type for the given tag</li>
     *   <li>Create a new service instance using the found type</li>
     *   <li>Return the created service</li>
     * </ol>
     * </p>
     *
     * @param tag The service tag to create from
     * @param accessory The accessory that will own the service
     * @return A new service instance
     * @throws IllegalArgumentException if the tag is not supported or service creation fails
     * @since 1.0
     */
    @Override
    public HomekitService createServiceFromTag(String tag, HomekitAccessory accessory) {
        logger.trace("{}Creating service from tag: {} for accessory: {}", LOG_TRACE, tag, accessory.getUID());
        String type = tagToTypeMap.get(tag);
        if (type == null) {
            logger.error("{}No service type found for tag: {}", LOG_ERROR, tag);
            throw new IllegalArgumentException("No service type found for tag: " + tag);
        }
        return createService(type, accessory);
    }

    /**
     * Checks if the factory supports a specific service type.
     *
     * @param type The service type to check
     * @return true if the type is supported, false otherwise
     * @since 1.0
     */
    @Override
    public boolean supportsServiceType(String type) {
        boolean supported = serviceTypes.containsKey(type);
        logger.trace("{}Service type {} support check: {}", LOG_TRACE, type, supported);
        return supported;
    }

    /**
     * Checks if the factory supports creating services with the given tag.
     *
     * @param tag The service tag to check
     * @return true if the tag is supported, false otherwise
     * @since 1.0
     */
    @Override
    public boolean supportsTag(String tag) {
        boolean supported = tagToTypeMap.containsKey(tag);
        logger.trace("{}Service tag {} support check: {}", LOG_TRACE, tag, supported);
        return supported;
    }

    /**
     * Returns a set of all supported service tags.
     *
     * @return An unmodifiable set of supported service tags
     * @since 1.0
     */
    @Override
    public Set<String> getSupportedTags() {
        logger.trace("{}Getting supported tags, count: {}", LOG_TRACE, tagToTypeMap.size());
        return Collections.unmodifiableSet(tagToTypeMap.keySet());
    }

    /**
     * Returns a set of all supported service types.
     *
     * @return An unmodifiable set of supported service types
     * @since 1.0
     */
    @Override
    public Set<String> getSupportedServiceTypes() {
        logger.trace("{}Getting supported service types, count: {}", LOG_TRACE, serviceTypes.size());
        return Collections.unmodifiableSet(serviceTypes.keySet());
    }

    /**
     * Creates a service instance from a JSON value.
     * This method extracts the service type from the JSON value and creates the appropriate service.
     *
     * <p>
     * The JSON value must contain:
     * <ul>
     *   <li>A "type" field with the service type</li>
     *   <li>Any additional configuration required by the service</li>
     * </ul>
     * </p>
     *
     * @param accessory The accessory that will own the service
     * @param value The JSON value containing service configuration
     * @return A new service instance
     * @throws IllegalArgumentException if the JSON value is invalid or service creation fails
     * @since 1.0
     */
    public HomekitService createService(HomekitAccessory accessory, JsonValue value) {
        logger.trace("{}Creating service from JSON for accessory: {}", LOG_TRACE, accessory.getUID());
        if (value == null || !value.getValueType().equals(JsonValue.ValueType.OBJECT)) {
            logger.error("{}Invalid JSON value for service creation", LOG_ERROR);
            throw new IllegalArgumentException("Invalid JSON value for service creation");
        }

        String type = value.asJsonObject().getString("type", null);
        if (type == null) {
            logger.error("{}Service type not found in JSON value", LOG_ERROR);
            throw new IllegalArgumentException("Service type not found in JSON value");
        }

        Class<? extends HomekitService> serviceClass = serviceTypes.get(type);
        if (serviceClass == null) {
            logger.error("{}Unsupported service type: {}", LOG_ERROR, type);
            throw new IllegalArgumentException("Unsupported service type: " + type);
        }

        try {
            return serviceClass.getConstructor(HomekitAccessory.class, JsonValue.class)
                    .newInstance(accessory, value);
        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException
                | SecurityException | InvocationTargetException e) {
            logger.error("{}Error creating service from JSON value: {}", LOG_ERROR, e.getMessage(), e);
            throw new IllegalArgumentException("Failed to create service from JSON value", e);
        }
    }

    /**
     * Gets the service tag for a given service type.
     *
     * @param serviceType The service type to get the tag for
     * @return The service tag, or null if not found
     * @since 1.0
     */
    @Override
    public String getTagFromServiceType(String serviceType) {
        logger.trace("{}Getting tag for service type: {}", LOG_TRACE, serviceType);
        for (Map.Entry<String, String> entry : tagToTypeMap.entrySet()) {
            if (entry.getValue().equals(serviceType)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Gets the service type for a given service tag.
     *
     * @param serviceTag The service tag to get the type for
     * @return The service type, or null if not found
     * @since 1.0
     */
    @Override
    public String getServiceTypeFromTag(String serviceTag) {
        logger.trace("{}Getting service type for tag: {}", LOG_TRACE, serviceTag);
        return tagToTypeMap.get(serviceTag);
    }

    /**
     * Gets the mandatory and optional characteristic types for a given service type.
     *
     * <p>
     * The returned map contains two sets:
     * <ul>
     *   <li>"mandatory" - Set of required characteristic types</li>
     *   <li>"optional" - Set of optional characteristic types</li>
     * </ul>
     * </p>
     *
     * @param serviceType The service type to get characteristics for
     * @return A map containing sets of mandatory and optional characteristic types
     * @throws IllegalArgumentException if the service type is not supported
     * @since 1.0
     */
    @Override
    public Map<String, Set<String>> getCharacteristicTypes(String serviceType) {
        logger.trace("{}Getting characteristic types for service: {}", LOG_TRACE, serviceType);
        Map<String, Set<String>> types = serviceCharacteristicTypes.get(serviceType);
        if (types == null) {
            logger.error("{}Unsupported service type: {}", LOG_ERROR, serviceType);
            throw new IllegalArgumentException("Unsupported service type: " + serviceType);
        }
        return Collections.unmodifiableMap(types);
    }
}
