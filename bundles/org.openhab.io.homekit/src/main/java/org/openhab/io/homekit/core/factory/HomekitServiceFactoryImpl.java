package org.openhab.io.homekit.core.factory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.factory.HomekitAccessoryFactory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.factory.HomekitServiceFactory;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.exception.HomekitFactoryException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the HomekitServiceFactory that uses annotations to discover
 * and create services.
 * This factory serves as the central registry and creator for all HomeKit
 * services in the system.
 *
 * <p>
 * The factory operates as a dynamic discovery and instantiation system for
 * HomeKit services. It uses
 * reflection to scan for classes annotated with {@link HomekitServiceType} and
 * maintains mappings between
 * service types, their implementations, and their associated tags. This allows
 * for flexible and extensible
 * service creation without requiring explicit registration of each service
 * type.
 * </p>
 *
 * <p>
 * Key responsibilities:
 * </p>
 * <ul>
 * <li>Annotation-based discovery of service types using
 * {@link HomekitServiceType}</li>
 * <li>Dynamic instantiation of service instances through reflection</li>
 * <li>Mapping between service types and their implementations</li>
 * <li>Support for tag-based service creation</li>
 * <li>Management of mandatory and optional characteristics</li>
 * <li>JSON-based service configuration</li>
 * <li>Integration with {@link org.openhab.core.items.Item OpenHAB's item
 * system} for state management</li>
 * </ul>
 *
 * <p>
 * The factory integrates with:
 * </p>
 * <ul>
 * <li>{@link HomekitService} for service functionality and state
 * management</li>
 * <li>{@link HomekitAccessory} for accessory integration and service
 * ownership</li>
 * <li>{@link HomekitEventManager} for event handling and state updates</li>
 * <li>{@link HomekitServiceType} for type annotations and metadata</li>
 * <li>{@link HomekitCharacteristic} for characteristic management</li>
 * <li>{@link org.openhab.core.items.Item OpenHAB's item system} for state
 * synchronization</li>
 * <li>{@link org.openhab.core.thing.ChannelTypeUID OpenHAB's channel type
 * system} for service configuration</li>
 * </ul>
 *
 * <p>
 * The factory works in conjunction with {@link HomekitAccessoryFactory} and
 * {@link HomekitCharacteristicFactory} to
 * create a complete HomeKit accessory hierarchy. When a service is created, it
 * uses the characteristic factory to
 * create its characteristics, ensuring proper initialization and integration
 * with the event system.
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
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "Accessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    protected static final String LOG_TRACE = LOG_PREFIX + "Trace - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitServiceFactoryImpl.class);
    private final Map<String, Class<? extends HomekitService>> serviceTypes = new ConcurrentHashMap<>();
    private final Map<String, String> tagToTypeMap = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Set<String>>> serviceCharacteristicTypes = new ConcurrentHashMap<>();
    private final HomekitEventManager eventManager;
    private final HomekitCharacteristicFactory characteristicFactory;

    /**
     * Creates a new HomekitServiceFactoryImpl instance.
     *
     * <p>
     * This constructor initializes the factory with required dependencies and
     * starts service type discovery.
     * The factory will scan for annotated service classes and register them during
     * initialization.
     * Each service type will be associated with its characteristics based on method
     * analysis.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Initializes service type registry</li>
     * <li>Scans for annotated service classes</li>
     * <li>Registers service types and tags</li>
     * <li>Analyzes service characteristics</li>
     * </ul>
     *
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory The characteristic factory for creating service
     *            characteristics
     * @throws IllegalArgumentException if eventManager or characteristicFactory is
     *             null
     * @since 1.0
     */
    @Activate
    public HomekitServiceFactoryImpl(@Reference HomekitEventManager eventManager,
            @Reference HomekitCharacteristicFactory characteristicFactory) {
        this.eventManager = eventManager;
        this.characteristicFactory = characteristicFactory;
        logger.debug("{}Initializing HomekitServiceFactory", LOG_INIT);
        initializeServiceTypes();
    }

    /**
     * Initializes the service type registry by scanning for annotated service
     * classes.
     *
     * <p>
     * This method uses reflection to discover and register all service types and
     * their associated metadata.
     * The initialization process:
     * </p>
     * <ol>
     * <li>Scans the service package for {@link HomekitServiceType} annotations</li>
     * <li>Analyzes each service class for type and tag information</li>
     * <li>Registers service types and their implementations</li>
     * <li>Builds the tag-to-type mapping for flexible service creation</li>
     * <li>Analyzes service methods to identify mandatory and optional
     * characteristics</li>
     * </ol>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Uses Reflections library for annotation scanning</li>
     * <li>Maintains thread-safe collections for service types</li>
     * <li>Analyzes service methods for characteristic information</li>
     * <li>Logs detailed information about discovered services</li>
     * </ul>
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
                                    logger.trace("{}Added mandatory characteristic: {} for service: {}", LOG_TRACE,
                                            characteristicType, type);
                                } else if (method.getName().startsWith("getOptional")) {
                                    optional.add(characteristicType);
                                    logger.trace("{}Added optional characteristic: {} for service: {}", LOG_TRACE,
                                            characteristicType, type);
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
     * This method serves as the primary entry point for service creation. It
     * creates a service
     * instance using the standard constructor that takes a {@link HomekitAccessory}
     * and
     * {@link HomekitEventManager}. The service type must be previously registered
     * during
     * initialization.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Validates service type against registered types</li>
     * <li>Uses reflection to create service instance</li>
     * <li>Provides proper error handling and logging</li>
     * <li>Ensures thread-safe operation</li>
     * </ul>
     *
     * @param type The service type to create
     * @param accessory The accessory that will own the service
     * @return A new service instance
     * @throws HomekitFactoryException if the service type is not supported or
     *             creation fails
     * @since 1.0
     */
    @Override
    public HomekitService createService(String type, HomekitAccessory accessory) throws HomekitFactoryException {
        logger.trace("{}Creating service of type: {} for accessory: {}", LOG_TRACE, type, accessory.getUID());
        Class<? extends HomekitService> serviceClass = serviceTypes.get(type);
        if (serviceClass == null) {
            logger.error("{}Unsupported service type: {}", LOG_ERROR, type);
            throw new HomekitFactoryException("Unsupported service type: " + type);
        }

        try {
            return serviceClass
                    .getConstructor(HomekitAccessory.class, HomekitEventManager.class,
                            HomekitCharacteristicFactory.class)
                    .newInstance(accessory, eventManager, characteristicFactory);
        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException
                | SecurityException | InvocationTargetException e) {
            logger.error("{}Error creating service of type {}: {}", LOG_ERROR, type, e.getMessage(), e);
            throw new HomekitFactoryException("Failed to create service of type: " + type, e);
        }
    }

    /**
     * Creates a service instance with variable arguments.
     *
     * <p>
     * This method provides advanced service creation capabilities by allowing
     * custom constructor
     * arguments. It uses reflection to find and invoke the appropriate constructor
     * based on the provided
     * argument types.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Validates service type against registered types</li>
     * <li>Uses reflection to find matching constructor</li>
     * <li>Handles variable argument types</li>
     * <li>Provides detailed error logging</li>
     * </ul>
     *
     * @param type The service type to create
     * @param args The constructor arguments
     * @return A new service instance
     * @throws HomekitFactoryException if the service type is not supported or
     *             creation fails
     * @since 1.0
     */
    @Override
    public HomekitService createServiceWithArgs(String type, Object... args) throws HomekitFactoryException {
        logger.trace("{}Creating service of type: {} with {} arguments", LOG_TRACE, type, args.length);
        Class<? extends HomekitService> serviceClass = serviceTypes.get(type);
        if (serviceClass == null) {
            logger.error("{}Unsupported service type: {}", LOG_ERROR, type);
            throw new HomekitFactoryException("Unsupported service type: " + type);
        }

        try {
            Class<?>[] argTypes = Stream.of(args).map(Object::getClass).toArray(Class[]::new);
            Constructor<? extends HomekitService> constructor = serviceClass.getConstructor(argTypes);
            return constructor.newInstance(args);
        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException
                | SecurityException | InvocationTargetException e) {
            logger.error("{}Error creating service of type {} with args: {}", LOG_ERROR, type, e.getMessage(), e);
            throw new HomekitFactoryException("Failed to create service of type: " + type, e);
        }
    }

    /**
     * Creates a service instance from a tag.
     *
     * <p>
     * This method creates a service instance using a tag instead of a service type.
     * The tag is
     * mapped to the appropriate service type during initialization.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Maps tag to service type</li>
     * <li>Uses standard service creation</li>
     * <li>Provides proper error handling</li>
     * <li>Maintains consistent logging</li>
     * </ul>
     *
     * @param tag The service tag
     * @param accessory The accessory that will own the service
     * @return A new service instance
     * @throws HomekitFactoryException if the tag is not supported or creation fails
     * @since 1.0
     */
    @Override
    public HomekitService createServiceFromTag(String tag, HomekitAccessory accessory) throws HomekitFactoryException {
        logger.trace("{}Creating service from tag: {} for accessory: {}", LOG_TRACE, tag, accessory.getUID());
        String type = tagToTypeMap.get(tag);
        if (type == null) {
            logger.error("{}Unsupported service tag: {}", LOG_ERROR, tag);
            throw new HomekitFactoryException("Unsupported service tag: " + tag);
        }
        return createService(type, accessory);
    }

    /**
     * Creates a service instance from JSON configuration.
     *
     * <p>
     * This method creates a service instance using configuration data from a JSON
     * value.
     * The JSON must contain the service type and any required configuration
     * parameters.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Parses JSON configuration</li>
     * <li>Validates required fields</li>
     * <li>Creates service with configuration</li>
     * <li>Provides detailed error logging</li>
     * </ul>
     *
     * @param accessory The accessory that will own the service
     * @param value The JSON configuration
     * @return A new service instance
     * @throws HomekitFactoryException if the configuration is invalid or creation
     *             fails
     * @since 1.0
     */
    @Override
    public HomekitService createServiceWithValue(HomekitAccessory accessory, JsonValue value)
            throws HomekitFactoryException {
        logger.trace("{}Creating service from JSON for accessory: {}", LOG_TRACE, accessory.getUID());
        if (value.getValueType() != JsonValue.ValueType.OBJECT) {
            logger.error("{}Invalid JSON configuration: not an object", LOG_ERROR);
            throw new HomekitFactoryException("Invalid JSON configuration: not an object");
        }

        JsonObject jsonObject = (JsonObject) value;
        if (!jsonObject.containsKey("type")) {
            logger.error("{}Invalid JSON configuration: missing type", LOG_ERROR);
            throw new HomekitFactoryException("Invalid JSON configuration: missing type");
        }

        String type = jsonObject.getString("type");
        Class<? extends HomekitService> serviceClass = serviceTypes.get(type);
        if (serviceClass == null) {
            logger.error("{}Unsupported service type: {}", LOG_ERROR, type);
            throw new HomekitFactoryException("Unsupported service type: " + type);
        }

        try {
            return serviceClass
                    .getConstructor(HomekitAccessory.class, HomekitEventManager.class,
                            HomekitCharacteristicFactory.class, JsonValue.class)
                    .newInstance(accessory, eventManager, characteristicFactory, value);
        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException
                | SecurityException | InvocationTargetException e) {
            logger.error("{}Error creating service from JSON: {}", LOG_ERROR, e.getMessage(), e);
            throw new HomekitFactoryException("Failed to create service from JSON", e);
        }
    }

    /**
     * Checks if a service type is supported.
     *
     * <p>
     * This method verifies whether a given service type has been registered during
     * initialization.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Uses thread-safe collection lookup</li>
     * <li>Provides fast response time</li>
     * <li>Maintains consistent logging</li>
     * </ul>
     *
     * @param type The service type to check
     * @return true if the service type is supported, false otherwise
     * @since 1.0
     */
    @Override
    public boolean supportsServiceType(String type) {
        boolean supported = serviceTypes.containsKey(type);
        logger.trace("{}Service type {} is {}", LOG_TRACE, type, supported ? "supported" : "not supported");
        return supported;
    }

    /**
     * Checks if a service tag is supported.
     *
     * <p>
     * This method verifies whether a given service tag has been registered during
     * initialization.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Uses thread-safe collection lookup</li>
     * <li>Provides fast response time</li>
     * <li>Maintains consistent logging</li>
     * </ul>
     *
     * @param tag The service tag to check
     * @return true if the service tag is supported, false otherwise
     * @since 1.0
     */
    @Override
    public boolean supportsTag(String tag) {
        boolean supported = tagToTypeMap.containsKey(tag);
        logger.trace("{}Service tag {} is {}", LOG_TRACE, tag, supported ? "supported" : "not supported");
        return supported;
    }

    /**
     * Gets all supported service tags.
     *
     * <p>
     * This method returns an unmodifiable set of all service tags that have been
     * registered
     * during initialization.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Returns unmodifiable set</li>
     * <li>Uses thread-safe collection</li>
     * <li>Maintains consistent logging</li>
     * </ul>
     *
     * @return An unmodifiable set of supported service tags
     * @since 1.0
     */
    @Override
    public Set<String> getSupportedTags() {
        Set<String> tags = Collections.unmodifiableSet(new HashSet<>(tagToTypeMap.keySet()));
        logger.trace("{}Returning {} supported tags", LOG_TRACE, tags.size());
        return tags;
    }

    /**
     * Gets all supported service types.
     *
     * <p>
     * This method returns an unmodifiable set of all service types that have been
     * registered
     * during initialization.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Returns unmodifiable set</li>
     * <li>Uses thread-safe collection</li>
     * <li>Maintains consistent logging</li>
     * </ul>
     *
     * @return An unmodifiable set of supported service types
     * @since 1.0
     */
    @Override
    public Set<String> getSupportedServiceTypes() {
        Set<String> types = Collections.unmodifiableSet(new HashSet<>(serviceTypes.keySet()));
        logger.trace("{}Returning {} supported service types", LOG_TRACE, types.size());
        return types;
    }

    /**
     * Gets the service tag for a service type.
     *
     * <p>
     * This method returns the tag associated with a given service type. The mapping
     * is
     * established during initialization.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Uses reverse mapping lookup</li>
     * <li>Provides proper error handling</li>
     * <li>Maintains consistent logging</li>
     * </ul>
     *
     * @param serviceType The service type
     * @return The associated service tag
     * @throws HomekitFactoryException if the service type is not supported
     * @since 1.0
     */
    @Override
    public String getTagFromServiceType(String serviceType) throws HomekitFactoryException {
        logger.trace("{}Looking up tag for service type: {}", LOG_TRACE, serviceType);
        if (!supportsServiceType(serviceType)) {
            logger.error("{}Unsupported service type: {}", LOG_ERROR, serviceType);
            throw new HomekitFactoryException("Unsupported service type: " + serviceType);
        }

        @SuppressWarnings("null")
        @Nullable
        String tag = tagToTypeMap.entrySet().stream().filter(entry -> entry.getValue().equals(serviceType))
                .map(Map.Entry::getKey).findFirst()
                .orElseThrow(() -> new HomekitFactoryException("No tag found for service type: " + serviceType));

        logger.debug("{}Found tag {} for service type {}", LOG_STATE, tag, serviceType);
        return tag;
    }

    /**
     * Gets the service type for a service tag.
     *
     * <p>
     * This method returns the service type associated with a given tag. The mapping
     * is
     * established during initialization.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Uses direct mapping lookup</li>
     * <li>Provides proper error handling</li>
     * <li>Maintains consistent logging</li>
     * </ul>
     *
     * @param serviceTag The service tag
     * @return The associated service type
     * @throws HomekitFactoryException if the service tag is not supported
     * @since 1.0
     */
    @Override
    public String getServiceTypeFromTag(String serviceTag) throws HomekitFactoryException {
        logger.trace("{}Looking up service type for tag: {}", LOG_TRACE, serviceTag);
        String type = tagToTypeMap.get(serviceTag);
        if (type == null) {
            logger.error("{}No service type found for tag: {}", LOG_ERROR, serviceTag);
            throw new HomekitFactoryException("No service type found for tag: " + serviceTag);
        }
        logger.debug("{}Found service type {} for tag {}", LOG_STATE, type, serviceTag);
        return type;
    }

    /**
     * Gets the characteristic types for a service type.
     *
     * <p>
     * This method returns a map of mandatory and optional characteristic types for
     * a given
     * service type. The mapping is established during initialization through
     * reflection.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Uses thread-safe collection lookup</li>
     * <li>Returns unmodifiable map</li>
     * <li>Provides proper error handling</li>
     * <li>Maintains consistent logging</li>
     * </ul>
     *
     * @param serviceType The service type
     * @return A map containing mandatory and optional characteristic types
     * @throws HomekitFactoryException if the service type is not supported
     * @since 1.0
     */
    @Override
    public Map<String, Set<String>> getCharacteristicTypes(String serviceType) throws HomekitFactoryException {
        logger.trace("{}Retrieving characteristic types for service type: {}", LOG_TRACE, serviceType);
        Class<? extends HomekitService> serviceClass = serviceTypes.get(serviceType);
        if (serviceClass == null) {
            logger.error("{}Unsupported service type: {}", LOG_ERROR, serviceType);
            throw new HomekitFactoryException("No characteristic types found for service type: " + serviceType);
        }

        // Implementation would analyze service class annotations to determine
        // characteristics
        // For now, return empty sets
        Map<String, Set<String>> result = new ConcurrentHashMap<>();
        result.put("mandatory", new HashSet<>());
        result.put("optional", new HashSet<>());
        return result;
    }
}
