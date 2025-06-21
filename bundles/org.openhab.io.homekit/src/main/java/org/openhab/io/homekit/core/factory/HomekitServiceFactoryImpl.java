/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.openhab.io.homekit.core.factory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.factory.HomekitServiceFactory;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.api.uid.HomekitAccessoryUID;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.openhab.io.homekit.exception.HomekitFactoryException;
import org.openhab.io.homekit.util.HomekitAnnotationScanner;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
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
 * <li>{@link org.openhab.core.items.Item OpenHAB's item system} for state
 * synchronization</li>
 * <li>{@link org.openhab.core.thing.ChannelTypeUID OpenHAB's channel type
 * system} for service configuration</li>
 * </ul>
 *
 * <p>
 * The factory works in conjunction with {@link org.openhab.io.homekit.api.factory.HomekitAccessoryFactory} and
 * {@link org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory} to
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

    private static final Logger logger = LoggerFactory.getLogger(HomekitServiceFactoryImpl.class);
    private final Map<String, Class<? extends HomekitService>> serviceTypes = new ConcurrentHashMap<>();
    private final Map<String, String> tagToTypeMap = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Set<String>>> serviceCharacteristicTypes = new ConcurrentHashMap<>();
    private final HomekitEventManager eventManager;
    private final HomekitCharacteristicFactory characteristicFactory;
    private final Set<String> mandatoryCharacteristics = new HashSet<>();
    private final Set<String> optionalCharacteristics = new HashSet<>();

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
     * This method uses Java's reflection APIs to discover and register all service types and
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
     * <li>Uses HomekitAnnotationScanner for annotation scanning</li>
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
            // Use HomekitAnnotationScanner to find annotated classes
            Set<Class<?>> serviceClasses = HomekitAnnotationScanner
                    .findAnnotatedClasses("org.openhab.io.homekit.library.service", HomekitServiceType.class);
            logger.trace("{}Found {} annotated service classes", LOG_TRACE, serviceClasses.size());

            for (Class<?> serviceClass : serviceClasses) {
                if (HomekitService.class.isAssignableFrom(serviceClass)) {
                    @SuppressWarnings("null") // getAnnotation() can return null, handled by null check below
                    HomekitServiceType annotation = serviceClass.getAnnotation(HomekitServiceType.class);
                    if (annotation != null) {
                        String type = annotation.type();
                        serviceTypes.put(type, (Class<? extends HomekitService>) serviceClass);
                        tagToTypeMap.put(annotation.tag(), type);

                        // Initialize characteristic types map using reflection
                        Map<String, Set<String>> characteristicTypes = new ConcurrentHashMap<>();
                        Set<String> mandatory = new HashSet<>();
                        Set<String> optional = new HashSet<>();

                        // Get characteristics directly from the service instance
                        try {
                            HomekitService service = createService(type, new DummyAccessory());
                            Set<HomekitCharacteristic<?>> characteristics = service.getCharacteristics();

                            for (HomekitCharacteristic<?> characteristic : characteristics) {
                                String characteristicType = characteristic.getType();
                                if (characteristic.isMandatory()) {
                                    mandatory.add(characteristicType);
                                    logger.trace("{}Added mandatory characteristic: {} for service: {}", LOG_TRACE,
                                            characteristicType, type);
                                } else {
                                    optional.add(characteristicType);
                                    logger.trace("{}Added optional characteristic: {} for service: {}", LOG_TRACE,
                                            characteristicType, type);
                                }
                            }
                        } catch (HomekitFactoryException e) {
                            logger.error("{}Error creating service for characteristic discovery: {}", LOG_ERROR,
                                    e.getMessage());
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
        @SuppressWarnings("null") // serviceTypes.get() can return null, checked below
        Class<? extends HomekitService> serviceClass = serviceTypes.get(type);
        if (serviceClass == null) {
            logger.error("{}Unsupported service type: {}", LOG_ERROR, type);
            throw new HomekitFactoryException("Unsupported service type: " + type);
        }

        try {
            @SuppressWarnings("null") // getConstructor() guaranteed to return valid constructor for valid class
            Constructor<? extends HomekitService> constructor = serviceClass.getConstructor(HomekitAccessory.class,
                    HomekitEventManager.class, HomekitCharacteristicFactory.class);
            @SuppressWarnings("null") // newInstance() guaranteed to return valid instance for valid constructor
            HomekitService instance = constructor.newInstance(accessory, eventManager, characteristicFactory);
            return instance;
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
        @SuppressWarnings("null") // serviceTypes.get() can return null, checked below
        Class<? extends HomekitService> serviceClass = serviceTypes.get(type);
        if (serviceClass == null) {
            logger.error("{}Unsupported service type: {}", LOG_ERROR, type);
            throw new HomekitFactoryException("Unsupported service type: " + type);
        }

        try {
            @SuppressWarnings("null") // Stream.of(args).map(Object::getClass) safe as args cannot contain null
            Class<?>[] argTypes = Stream.of(args).map(Object::getClass).toArray(Class[]::new);
            @SuppressWarnings("null") // getConstructor() guaranteed to return valid constructor for valid class
            Constructor<? extends HomekitService> constructor = serviceClass.getConstructor(argTypes);
            @SuppressWarnings("null") // newInstance() guaranteed to return valid instance for valid constructor
            HomekitService instance = constructor.newInstance(args);
            return instance;
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
        @SuppressWarnings("null") // tagToTypeMap.get() can return null, checked below
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
        @SuppressWarnings("null") // serviceTypes.get() can return null, checked below
        Class<? extends HomekitService> serviceClass = serviceTypes.get(type);
        if (serviceClass == null) {
            logger.error("{}Unsupported service type: {}", LOG_ERROR, type);
            throw new HomekitFactoryException("Unsupported service type: " + type);
        }

        try {
            @SuppressWarnings("null") // getConstructor() guaranteed to return valid constructor for valid class
            Constructor<? extends HomekitService> constructor = serviceClass.getConstructor(HomekitAccessory.class,
                    HomekitEventManager.class, HomekitCharacteristicFactory.class, JsonValue.class);
            @SuppressWarnings("null") // newInstance() guaranteed to return valid instance for valid constructor
            HomekitService instance = constructor.newInstance(accessory, eventManager, characteristicFactory, value);
            return instance;
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
    @NonNull
    public Set<String> getSupportedTags() {
        @NonNull
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
    @NonNull
    public Set<String> getSupportedServiceTypes() {
        @NonNull
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

        @SuppressWarnings("null") // entrySet().getValue() guaranteed non-null for valid entries
        String tag = tagToTypeMap.entrySet().stream().filter(entry -> {
            @SuppressWarnings("null") // entry.getValue() guaranteed non-null for valid map entries
            String value = entry.getValue();
            return value.equals(serviceType);
        }).map(entry -> {
            @SuppressWarnings("null") // entry.getKey() guaranteed non-null for valid map entries
            String key = entry.getKey();
            return key;
        }).findFirst().orElseThrow(() -> new HomekitFactoryException("No tag found for service type: " + serviceType));

        logger.trace("{}Found tag {} for service type {}", LOG_STATE, tag, serviceType);
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
        @SuppressWarnings("null") // tagToTypeMap.get() can return null, checked below
        String type = tagToTypeMap.get(serviceTag);
        if (type == null) {
            logger.error("{}No service type found for tag: {}", LOG_ERROR, serviceTag);
            throw new HomekitFactoryException("No service type found for tag: " + serviceTag);
        }
        logger.trace("{}Found service type {} for tag {}", LOG_STATE, type, serviceTag);
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
        @SuppressWarnings("null") // serviceCharacteristicTypes.get() can return null, checked below
        Map<String, Set<String>> characteristicTypes = serviceCharacteristicTypes.get(serviceType);
        if (characteristicTypes == null) {
            logger.error("{}Unsupported service type: {}", LOG_ERROR, serviceType);
            throw new HomekitFactoryException("No characteristic types found for service type: " + serviceType);
        }

        // Return unmodifiable copies of the sets to prevent external modification
        Map<String, Set<String>> result = new ConcurrentHashMap<>();
        @SuppressWarnings("null") // characteristicTypes.get() guaranteed non-null after null check above
        Set<String> mandatory = characteristicTypes.get("mandatory");
        @SuppressWarnings("null") // characteristicTypes.get() guaranteed non-null after null check above
        Set<String> optional = characteristicTypes.get("optional");

        // Create new non-null sets from the existing ones
        Set<String> mandatorySet = new HashSet<>(mandatory != null ? mandatory : Collections.emptySet());
        Set<String> optionalSet = new HashSet<>(optional != null ? optional : Collections.emptySet());

        result.put("mandatory", Collections.unmodifiableSet(mandatorySet));
        result.put("optional", Collections.unmodifiableSet(optionalSet));

        logger.trace("{}Found {} mandatory and {} optional characteristics for service type {}", LOG_STATE,
                mandatorySet.size(), optionalSet.size(), serviceType);
        return result;
    }

    private void discoverCharacteristics() {
        for (String type : getSupportedServiceTypes()) {
            try {
                // Get the service class from the registry
                Class<? extends HomekitService> serviceClass = serviceTypes.get(type);
                if (serviceClass == null) {
                    logger.warn("{}Service class not found for type: {}", LOG_WARN, type);
                    continue;
                }

                // Get the service type annotation
                HomekitServiceType serviceTypeAnnotation = serviceClass.getAnnotation(HomekitServiceType.class);
                if (serviceTypeAnnotation == null) {
                    logger.warn("{}Service type annotation not found for: {}", LOG_WARN, type);
                    continue;
                }

                // Create a minimal service instance
                Constructor<? extends HomekitService> constructor = serviceClass.getDeclaredConstructor(
                        HomekitAccessory.class, HomekitEventManager.class, HomekitCharacteristicFactory.class);
                HomekitService service = constructor.newInstance(null, eventManager, characteristicFactory);

                // Get characteristics from the service
                Set<HomekitCharacteristic<?>> characteristics = service.getCharacteristics();
                if (characteristics == null || characteristics.isEmpty()) {
                    logger.warn("{}No characteristics found for service: {}", LOG_WARN, type);
                    continue;
                }

                // Add characteristics to the appropriate sets
                for (HomekitCharacteristic<?> characteristic : characteristics) {
                    String characteristicType = characteristic.getType();
                    if (characteristicType == null) {
                        logger.warn("{}Characteristic type is null for service: {}", LOG_WARN, type);
                        continue;
                    }

                    if (characteristic.isMandatory()) {
                        mandatoryCharacteristics.add(characteristicType);
                        logger.trace("{}Added mandatory characteristic: {} for service: {}", LOG_TRACE,
                                characteristicType, type);
                    } else {
                        optionalCharacteristics.add(characteristicType);
                        logger.trace("{}Added optional characteristic: {} for service: {}", LOG_TRACE,
                                characteristicType, type);
                    }
                }
            } catch (Exception e) {
                logger.error("{}Error discovering characteristics for service: {}", LOG_ERROR, type, e);
            }
        }
    }

    private static class DummyAccessory implements HomekitAccessory {
        private final String label = "Dummy Accessory";
        private final String manufacturer = "openHAB";
        private final String model = "Dummy";
        private final String serialNumber = "DUMMY-123";
        private final long accessoryId = 1;
        private final String pairingId = "DUMMY";
        private boolean orphaned = false;
        private boolean assigned = false;
        private boolean extensible = true;

        @Override
        public HomekitAccessoryUID getUID() {
            return new org.openhab.io.homekit.core.accessory.HomekitAccessoryUIDImpl(pairingId, accessoryId);
        }

        @Override
        public long getAccessoryId() {
            return accessoryId;
        }

        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public String getSerialNumber() {
            return serialNumber;
        }

        @Override
        public String getModel() {
            return model;
        }

        @Override
        public String getManufacturer() {
            return manufacturer;
        }

        @Override
        public boolean isExtensible() {
            return extensible;
        }

        @Override
        public boolean isAssigned() {
            return assigned;
        }

        @Override
        public void addService(HomekitService service) {
        }

        @Override
        public void addServices() {
        }

        @Override
        public void removeService(HomekitService service) {
        }

        @Override
        public Collection<HomekitService> getServices() {
            return Collections.emptyList();
        }

        @Override
        public Optional<HomekitService> getService(String serviceType) {
            return Optional.empty();
        }

        @Override
        public Optional<HomekitService> getPrimaryService() {
            return Optional.empty();
        }

        @Override
        public void assignToServer(HomekitAccessoryServer server) throws HomekitAccessoryOperationException {
            throw new HomekitAccessoryOperationException("DummyAccessory cannot be assigned");
        }

        @Override
        public JsonObject toJson() {
            return Json.createObjectBuilder().build();
        }

        @Override
        public JsonObject toReducedJson() {
            return Json.createObjectBuilder().build();
        }

        @Override
        public void identify() {
        }

        @Override
        public long getNextAvailableInstanceId() {
            return 1;
        }

        @Override
        public HomekitAccessory withLabel(String label) {
            return this;
        }

        @Override
        public HomekitAccessory withSerialNumber(String serialNumber) {
            return this;
        }

        @Override
        public HomekitAccessory withModel(String model) {
            return this;
        }

        @Override
        public HomekitAccessory withManufacturer(String manufacturer) {
            return this;
        }

        @Override
        public HomekitAccessory withExtensible(boolean isExtensible) {
            this.extensible = isExtensible;
            return this;
        }

        @Override
        public void setOrphaned(boolean orphaned) {
            this.orphaned = orphaned;
        }

        @Override
        public boolean isOrphaned() {
            return orphaned;
        }

        @Override
        public int compareTo(HomekitAccessory other) {
            return Long.compare(this.accessoryId, other.getAccessoryId());
        }
    }
}
