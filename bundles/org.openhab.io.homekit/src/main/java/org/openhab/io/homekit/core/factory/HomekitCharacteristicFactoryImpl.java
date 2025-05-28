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
 * This factory serves as the central registry and creator for all HomeKit characteristics in the system.
 *
 * <p>
 * The factory operates as a dynamic discovery and instantiation system for HomeKit characteristics. It uses
 * reflection to scan for classes annotated with {@link HomekitCharacteristicType} and maintains mappings between
 * characteristic types, their implementations, and their associated tags. This allows for flexible and extensible
 * characteristic creation without requiring explicit registration of each characteristic type.
 * </p>
 *
 * <p>
 * The factory provides a centralized mechanism for creating HomeKit characteristics through:
 * <ul>
 * <li>Annotation-based discovery of characteristic types using {@link HomekitCharacteristicType}</li>
 * <li>Dynamic instantiation of characteristic instances through reflection</li>
 * <li>Mapping between characteristic types and their implementations</li>
 * <li>Support for tag-based characteristic creation</li>
 * <li>Integration with {@link org.openhab.core.items.Item OpenHAB's item system} for state management</li>
 * </ul>
 * </p>
 *
 * <p>
 * The factory integrates with several key components:
 * <ul>
 * <li>{@link HomekitCharacteristic} for characteristic functionality and state management</li>
 * <li>{@link HomekitService} for service integration and characteristic ownership</li>
 * <li>{@link HomekitEventManager} for event handling and state updates</li>
 * <li>{@link HomekitCharacteristicType} for type annotations and metadata</li>
 * <li>{@link org.openhab.core.items.Item OpenHAB's item system} for state synchronization</li>
 * <li>{@link org.openhab.core.thing.ChannelTypeUID OpenHAB's channel type system} for characteristic configuration</li>
 * </ul>
 * </p>
 *
 * <p>
 * The factory works in conjunction with {@link HomekitServiceFactory} and {@link HomekitAccessoryFactory} to
 * create a complete HomeKit accessory hierarchy. When a service needs characteristics, it uses this factory to
 * create them, ensuring proper initialization and integration with the event system.
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@Component(service = HomekitCharacteristicFactory.class)
@NonNullByDefault
public class HomekitCharacteristicFactoryImpl implements HomekitCharacteristicFactory {
    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit CharacteristicFactory: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_CHAR = LOG_PREFIX + "Characteristic - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    protected static final String LOG_TRACE = LOG_PREFIX + "Trace - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitCharacteristicFactoryImpl.class);
    private final Map<String, Class<? extends HomekitCharacteristic<?>>> characteristicTypes = new ConcurrentHashMap<>();
    private final Map<String, String> tagToTypeMap = new ConcurrentHashMap<>();
    private final HomekitEventManager eventManager;

    /**
     * Creates a new HomekitCharacteristicFactoryImpl instance.
     *
     * <p>
     * This constructor initializes the factory with required dependencies and starts characteristic type discovery.
     * The factory will scan for annotated characteristic classes and register them during initialization.
     * Each characteristic type will be associated with its metadata based on annotation analysis.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Initializes characteristic type registry</li>
     * <li>Scans for annotated characteristic classes</li>
     * <li>Registers characteristic types and tags</li>
     * <li>Analyzes characteristic metadata</li>
     * </ul>
     *
     * @param eventManager The event manager for handling HomeKit events
     * @throws IllegalArgumentException if eventManager is null
     * @since 1.0
     */
    @Activate
    public HomekitCharacteristicFactoryImpl(@Reference HomekitEventManager eventManager) {
        this.eventManager = eventManager;
        logger.debug("{}Initializing HomekitCharacteristicFactory", LOG_INIT);
        initializeCharacteristicTypes();
    }

    /**
     * Initializes the characteristic type registry by scanning for annotated characteristic classes.
     *
     * <p>
     * This method uses reflection to discover and register all characteristic types and their associated metadata.
     * The initialization process:
     * </p>
     * <ol>
     * <li>Scans the characteristic package for {@link HomekitCharacteristicType} annotations</li>
     * <li>Analyzes each characteristic class for type and tag information</li>
     * <li>Registers characteristic types and their implementations</li>
     * <li>Builds the tag-to-type mapping for flexible characteristic creation</li>
     * </ol>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Uses Reflections library for annotation scanning</li>
     * <li>Maintains thread-safe collections for characteristic types</li>
     * <li>Analyzes characteristic annotations for metadata</li>
     * <li>Logs detailed information about discovered characteristics</li>
     * </ul>
     *
     * @throws IllegalStateException if characteristic type initialization fails
     * @since 1.0
     */
    @SuppressWarnings("unchecked")
    private void initializeCharacteristicTypes() {
        logger.debug("{}Starting characteristic type initialization", LOG_INIT);
        try {
            // Configure Reflections to scan the characteristic package
            ConfigurationBuilder config = new ConfigurationBuilder()
                    .forPackages("org.openhab.io.homekit.internal.characteristic").setScanners(Scanners.TypesAnnotated);

            Reflections reflections = new Reflections(config);
            Set<Class<?>> characteristicClasses = reflections.getTypesAnnotatedWith(HomekitCharacteristicType.class);
            logger.trace("{}Found {} annotated characteristic classes", LOG_TRACE, characteristicClasses.size());

            for (Class<?> characteristicClass : characteristicClasses) {
                if (HomekitCharacteristic.class.isAssignableFrom(characteristicClass)) {
                    HomekitCharacteristicType annotation = characteristicClass
                            .getAnnotation(HomekitCharacteristicType.class);
                    if (annotation != null) {
                        characteristicTypes.put(annotation.type(),
                                (Class<? extends HomekitCharacteristic<?>>) characteristicClass);
                        tagToTypeMap.put(annotation.tag(), annotation.type());
                        logger.debug("{}Registered characteristic type: {} -> {}", LOG_STATE, annotation.type(),
                                characteristicClass.getName());
                    }
                }
            }
            logger.info("{}Successfully initialized {} characteristic types", LOG_INIT, characteristicTypes.size());
        } catch (Exception e) {
            logger.error("{}Error initializing characteristic types: {}", LOG_ERROR, e.getMessage(), e);
            throw new IllegalStateException("Failed to initialize characteristic types", e);
        }
    }

    /**
     * Creates a new characteristic instance for the specified type and service.
     *
     * <p>
     * This method serves as the primary entry point for characteristic creation. It creates a characteristic
     * instance using the standard constructor that takes a {@link HomekitService} and {@link HomekitEventManager}.
     * The characteristic type must be previously registered during initialization.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Validates characteristic type against registered types</li>
     * <li>Uses reflection to create characteristic instance</li>
     * <li>Provides proper error handling and logging</li>
     * <li>Ensures thread-safe operation</li>
     * </ul>
     *
     * @param type The characteristic type to create
     * @param service The service that will own the characteristic
     * @return A new characteristic instance
     * @throws IllegalArgumentException if the characteristic type is not supported or creation fails
     * @since 1.0
     */
    @Override
    public HomekitCharacteristic<?> createCharacteristic(String type, HomekitService service) {
        logger.trace("{}Creating characteristic of type: {} for service: {}", LOG_TRACE, type, service.getUID());
        Class<? extends HomekitCharacteristic<?>> characteristicClass = characteristicTypes.get(type);
        if (characteristicClass == null) {
            logger.error("{}Unsupported characteristic type: {}", LOG_ERROR, type);
            throw new IllegalArgumentException("Unsupported characteristic type: " + type);
        }

        try {
            return characteristicClass.getConstructor(HomekitService.class, HomekitEventManager.class)
                    .newInstance(service, eventManager);
        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException
                | SecurityException | InvocationTargetException e) {
            logger.error("{}Error creating characteristic of type {}: {}", LOG_ERROR, type, e.getMessage(), e);
            throw new IllegalArgumentException("Failed to create characteristic of type: " + type, e);
        }
    }

    /**
     * Creates a characteristic instance from a tag.
     *
     * <p>
     * This method creates a characteristic instance using a tag instead of a characteristic type. The tag is
     * mapped to the appropriate characteristic type during initialization.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Maps tag to characteristic type</li>
     * <li>Uses standard characteristic creation</li>
     * <li>Provides proper error handling</li>
     * <li>Maintains consistent logging</li>
     * </ul>
     *
     * @param tag The characteristic tag
     * @param service The service that will own the characteristic
     * @return A new characteristic instance
     * @throws IllegalArgumentException if the tag is not supported or creation fails
     * @since 1.0
     */
    @Override
    public HomekitCharacteristic<?> createCharacteristicFromTag(String tag, HomekitService service) {
        logger.trace("{}Creating characteristic from tag: {} for service: {}", LOG_TRACE, tag, service.getUID());
        String type = tagToTypeMap.get(tag);
        if (type == null) {
            logger.error("{}Unsupported characteristic tag: {}", LOG_ERROR, tag);
            throw new IllegalArgumentException("Unsupported characteristic tag: " + tag);
        }
        return createCharacteristic(type, service);
    }

    /**
     * Creates a characteristic instance with custom arguments.
     *
     * <p>
     * This method creates a characteristic instance using a custom constructor that accepts additional arguments.
     * The characteristic type must be previously registered during initialization.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Validates characteristic type against registered types</li>
     * <li>Uses reflection to find appropriate constructor</li>
     * <li>Handles argument type matching and conversion</li>
     * <li>Provides detailed error logging</li>
     * </ul>
     *
     * @param type The characteristic type to create
     * @param args Additional arguments for characteristic creation
     * @return A new characteristic instance
     * @throws IllegalArgumentException if the characteristic type is not supported or creation fails
     * @since 1.0
     */
    @Override
    public HomekitCharacteristic<?> createCharacteristicWithArgs(String type, Object... args) {
        logger.trace("{}Creating characteristic of type: {} with {} arguments", LOG_TRACE, type, args.length);
        Class<? extends HomekitCharacteristic<?>> characteristicClass = characteristicTypes.get(type);
        if (characteristicClass == null) {
            logger.error("{}Unsupported characteristic type: {}", LOG_ERROR, type);
            throw new IllegalArgumentException("Unsupported characteristic type: " + type);
        }

        try {
            Class<?>[] argTypes = Arrays.stream(args).map(Object::getClass).toArray(Class[]::new);
            Constructor<? extends HomekitCharacteristic<?>> constructor = characteristicClass.getConstructor(argTypes);
            return constructor.newInstance(args);
        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException
                | SecurityException | InvocationTargetException e) {
            logger.error("{}Error creating characteristic of type {} with args: {}", LOG_ERROR, type, e.getMessage(),
                    e);
            throw new IllegalArgumentException("Failed to create characteristic of type: " + type, e);
        }
    }

    /**
     * Checks if a characteristic type is supported by the factory.
     *
     * <p>
     * This method verifies whether a given characteristic type has been registered during initialization.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Performs thread-safe lookup in characteristic registry</li>
     * <li>Provides trace-level logging for debugging</li>
     * <li>Maintains consistent error handling</li>
     * </ul>
     *
     * @param type The characteristic type to check
     * @return true if the type is supported, false otherwise
     * @since 1.0
     */
    @Override
    public boolean supportsCharacteristicType(String type) {
        logger.trace("{}Checking support for characteristic type: {}", LOG_TRACE, type);
        boolean supported = characteristicTypes.containsKey(type);
        logger.debug("{}Characteristic type {} is {}", LOG_STATE, type, supported ? "supported" : "not supported");
        return supported;
    }

    /**
     * Checks if a characteristic tag is supported by the factory.
     *
     * <p>
     * This method verifies whether a given tag has been mapped to a characteristic type during initialization.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Performs thread-safe lookup in tag registry</li>
     * <li>Provides trace-level logging for debugging</li>
     * <li>Maintains consistent error handling</li>
     * </ul>
     *
     * @param tag The characteristic tag to check
     * @return true if the tag is supported, false otherwise
     * @since 1.0
     */
    @Override
    public boolean supportsTag(String tag) {
        logger.trace("{}Checking support for characteristic tag: {}", LOG_TRACE, tag);
        boolean supported = tagToTypeMap.containsKey(tag);
        logger.debug("{}Characteristic tag {} is {}", LOG_STATE, tag, supported ? "supported" : "not supported");
        return supported;
    }

    /**
     * Returns the set of supported characteristic types.
     *
     * <p>
     * This method provides access to all characteristic types that have been registered during initialization.
     * The returned set is unmodifiable to prevent external modification of the registry.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Returns thread-safe unmodifiable set</li>
     * <li>Provides debug-level logging</li>
     * <li>Maintains consistent error handling</li>
     * </ul>
     *
     * @return An unmodifiable set of supported characteristic types
     * @since 1.0
     */
    @Override
    public Set<String> getSupportedCharacteristicTypes() {
        logger.debug("{}Retrieving supported characteristic types", LOG_STATE);
        Set<String> types = Collections.unmodifiableSet(new HashSet<>(characteristicTypes.keySet()));
        logger.trace("{}Found {} supported characteristic types", LOG_TRACE, types.size());
        return types;
    }

    /**
     * Returns the set of supported characteristic tags.
     *
     * <p>
     * This method provides access to all characteristic tags that have been mapped during initialization.
     * The returned set is unmodifiable to prevent external modification of the registry.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Returns thread-safe unmodifiable set</li>
     * <li>Provides debug-level logging</li>
     * <li>Maintains consistent error handling</li>
     * </ul>
     *
     * @return An unmodifiable set of supported characteristic tags
     * @since 1.0
     */
    @Override
    public Set<String> getSupportedTags() {
        logger.debug("{}Retrieving supported characteristic tags", LOG_STATE);
        Set<String> tags = Collections.unmodifiableSet(new HashSet<>(tagToTypeMap.keySet()));
        logger.trace("{}Found {} supported characteristic tags", LOG_TRACE, tags.size());
        return tags;
    }

    /**
     * Retrieves the tag associated with a characteristic type.
     *
     * <p>
     * This method looks up the tag that was mapped to a given characteristic type during initialization.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Performs reverse lookup in tag registry</li>
     * <li>Provides detailed error logging</li>
     * <li>Maintains consistent error handling</li>
     * </ul>
     *
     * @param characteristicType The characteristic type to look up
     * @return The associated tag
     * @throws IllegalArgumentException if the characteristic type is not supported
     * @since 1.0
     */
    @Override
    public String getTagFromCharacteristicType(String characteristicType) {
        logger.trace("{}Looking up tag for characteristic type: {}", LOG_TRACE, characteristicType);
        if (!supportsCharacteristicType(characteristicType)) {
            logger.error("{}Unsupported characteristic type: {}", LOG_ERROR, characteristicType);
            throw new IllegalArgumentException("Unsupported characteristic type: " + characteristicType);
        }

        String tag = tagToTypeMap.entrySet().stream().filter(entry -> entry.getValue().equals(characteristicType))
                .map(Map.Entry::getKey).findFirst().orElse(null);

        if (tag == null) {
            logger.error("{}No tag found for characteristic type: {}", LOG_ERROR, characteristicType);
            throw new IllegalArgumentException("No tag found for characteristic type: " + characteristicType);
        }

        logger.debug("{}Found tag {} for characteristic type {}", LOG_STATE, tag, characteristicType);
        return tag;
    }

    /**
     * Retrieves the characteristic type associated with a tag.
     *
     * <p>
     * This method looks up the characteristic type that was mapped to a given tag during initialization.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Performs direct lookup in tag registry</li>
     * <li>Provides detailed error logging</li>
     * <li>Maintains consistent error handling</li>
     * </ul>
     *
     * @param characteristicTag The tag to look up
     * @return The associated characteristic type
     * @throws IllegalArgumentException if the tag is not supported
     * @since 1.0
     */
    @Override
    public String getCharacteristicTypeFromTag(String characteristicTag) {
        logger.trace("{}Looking up characteristic type for tag: {}", LOG_TRACE, characteristicTag);
        String type = tagToTypeMap.get(characteristicTag);
        if (type == null) {
            logger.error("{}Unsupported characteristic tag: {}", LOG_ERROR, characteristicTag);
            throw new IllegalArgumentException("Unsupported characteristic tag: " + characteristicTag);
        }
        logger.debug("{}Found characteristic type {} for tag {}", LOG_STATE, type, characteristicTag);
        return type;
    }

    /**
     * Retrieves the set of accepted item types for a characteristic type.
     *
     * <p>
     * This method provides access to the item types that are accepted by a given characteristic type.
     * The information is obtained from the characteristic's metadata during initialization.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Retrieves characteristic class from registry</li>
     * <li>Uses reflection to access metadata</li>
     * <li>Provides detailed error logging</li>
     * <li>Maintains consistent error handling</li>
     * </ul>
     *
     * @param characteristicType The characteristic type to look up
     * @return A set of accepted item types
     * @throws IllegalArgumentException if the characteristic type is not supported
     * @since 1.0
     */
    @Override
    public Set<String> getAcceptedItemTypes(String characteristicType) {
        logger.trace("{}Retrieving accepted item types for characteristic type: {}", LOG_TRACE, characteristicType);
        Class<? extends HomekitCharacteristic<?>> characteristicClass = characteristicTypes.get(characteristicType);
        if (characteristicClass == null) {
            logger.error("{}Unsupported characteristic type: {}", LOG_ERROR, characteristicType);
            throw new IllegalArgumentException("Unsupported characteristic type: " + characteristicType);
        }
        logger.trace("{}Returning accepted item types for characteristic type: {}", LOG_TRACE, characteristicType);
        return Collections.unmodifiableSet(new HashSet<>(
                Arrays.asList(characteristicClass.getAnnotation(HomekitCharacteristicType.class).acceptedItemTypes())));
    }
}
