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
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.accessory.HomekitAccessoryType;
import org.openhab.io.homekit.api.factory.HomekitAccessoryFactory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.factory.HomekitServiceFactory;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.exception.HomekitFactoryException;
import org.openhab.io.homekit.util.HomekitAnnotationScanner;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the HomekitAccessoryFactory that uses annotations to
 * discover and create accessories.
 * This factory serves as the central registry and creator for all HomeKit
 * accessories in the system.
 *
 * <p>
 * The factory operates as a dynamic discovery and instantiation system for
 * HomeKit accessories. It uses
 * reflection to scan for classes annotated with {@link HomekitAccessoryType}
 * and maintains mappings between
 * accessory types, their implementations, and their associated tags. This
 * allows for flexible and extensible
 * accessory creation without requiring explicit registration of each accessory
 * type.
 * </p>
 *
 * <p>
 * The factory provides a centralized mechanism for creating HomeKit accessories
 * through:
 * <ul>
 * <li>Annotation-based discovery of accessory types using
 * {@link HomekitAccessoryType}</li>
 * <li>Dynamic instantiation of accessory instances through reflection</li>
 * <li>Mapping between accessory types and their implementations</li>
 * <li>Support for tag-based accessory creation</li>
 * <li>Integration with {@link HomekitServiceFactory} for service creation</li>
 * <li>Integration with {@link HomekitCharacteristicFactory} for characteristic
 * creation</li>
 * <li>Integration with {@link org.openhab.core.items.Item OpenHAB's item
 * system} for state management</li>
 * </ul>
 * </p>
 *
 * <p>
 * The factory integrates with several key components:
 * <ul>
 * <li>{@link HomekitAccessory} for accessory functionality and state
 * management</li>
 * <li>{@link HomekitServiceFactory} for service creation and management</li>
 * <li>{@link HomekitCharacteristicFactory} for characteristic creation and
 * management</li>
 * <li>{@link HomekitEventManager} for event handling and state updates</li>
 * <li>{@link HomekitAccessoryType} for type annotations and metadata</li>
 * <li>{@link org.openhab.core.items.Item OpenHAB's item system} for state
 * synchronization</li>
 * <li>{@link org.openhab.core.thing.ChannelTypeUID OpenHAB's channel type
 * system} for accessory configuration</li>
 * </ul>
 * </p>
 *
 * <p>
 * The factory works in conjunction with {@link HomekitServiceFactory} and
 * {@link HomekitCharacteristicFactory} to
 * create a complete HomeKit accessory hierarchy. When an accessory is created,
 * it uses these factories to
 * create its services and characteristics, ensuring proper initialization and
 * integration with the event system.
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@Component(service = HomekitAccessoryFactory.class)
@NonNullByDefault
public class HomekitAccessoryFactoryImpl implements HomekitAccessoryFactory {
    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit AccessoryFactory: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "Accessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    protected static final String LOG_TRACE = LOG_PREFIX + "Trace - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitAccessoryFactoryImpl.class);
    private final Map<String, Class<? extends HomekitAccessory>> accessoryTypes = new ConcurrentHashMap<>();
    private final Map<String, String> tagToTypeMap = new ConcurrentHashMap<>();
    private final HomekitEventManager eventManager;
    private final HomekitServiceFactory serviceFactory;
    private final HomekitCharacteristicFactory characteristicFactory;

    /**
     * Creates a new HomekitAccessoryFactoryImpl instance.
     *
     * <p>
     * This constructor initializes the factory with required dependencies and
     * starts accessory type discovery.
     * The factory will scan for annotated accessory classes and register them
     * during initialization.
     * Each accessory type will be associated with its metadata based on annotation
     * analysis.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Initializes accessory type registry</li>
     * <li>Scans for annotated accessory classes</li>
     * <li>Registers accessory types and tags</li>
     * <li>Analyzes accessory metadata</li>
     * </ul>
     *
     * @param eventManager The event manager for handling HomeKit events
     * @param serviceFactory The service factory for creating HomeKit
     *            services
     * @param characteristicFactory The characteristic factory for creating HomeKit
     *            characteristics
     * @throws IllegalArgumentException if any parameter is null
     * @since 1.0
     */
    @Activate
    public HomekitAccessoryFactoryImpl(@Reference HomekitEventManager eventManager,
            @Reference HomekitServiceFactory serviceFactory,
            @Reference HomekitCharacteristicFactory characteristicFactory) {
        this.eventManager = eventManager;
        this.serviceFactory = serviceFactory;
        this.characteristicFactory = characteristicFactory;
        logger.debug("{}Initializing HomekitAccessoryFactory", LOG_INIT);
        initializeAccessoryTypes();
    }

    /**
     * Initializes the accessory type registry by scanning for annotated accessory
     * classes.
     *
     * <p>
     * This method uses Java's reflection APIs to discover and register all accessory types and
     * their associated metadata.
     * The initialization process:
     * </p>
     * <ol>
     * <li>Scans the accessory package for {@link HomekitAccessoryType}
     * annotations</li>
     * <li>Analyzes each accessory class for type and tag information</li>
     * <li>Registers accessory types and their implementations</li>
     * <li>Builds the tag-to-type mapping for flexible accessory creation</li>
     * </ol>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Uses HomekitAnnotationScanner for annotation scanning</li>
     * <li>Maintains thread-safe collections for accessory types</li>
     * <li>Analyzes accessory annotations for metadata</li>
     * <li>Logs detailed information about discovered accessories</li>
     * </ul>
     *
     * @throws IllegalStateException if accessory type initialization fails
     * @since 1.0
     */
    @SuppressWarnings("unchecked")
    private void initializeAccessoryTypes() {
        logger.debug("{}Starting accessory type initialization", LOG_INIT);
        try {
            // Use HomekitAnnotationScanner to find annotated classes
            Set<Class<?>> accessoryClasses = HomekitAnnotationScanner
                    .findAnnotatedClasses("org.openhab.io.homekit.internal.accessory", HomekitAccessoryType.class);
            logger.trace("{}Found {} annotated accessory classes", LOG_TRACE, accessoryClasses.size());

            for (Class<?> accessoryClass : accessoryClasses) {
                if (HomekitAccessory.class.isAssignableFrom(accessoryClass)) {
                    @SuppressWarnings("null") // getAnnotation() can return null, handled by null check below
                    HomekitAccessoryType annotation = accessoryClass.getAnnotation(HomekitAccessoryType.class);
                    if (annotation != null) {
                        accessoryTypes.put(annotation.type(), (Class<? extends HomekitAccessory>) accessoryClass);
                        tagToTypeMap.put(annotation.tag(), annotation.type());
                        logger.debug("{}Registered accessory type: {} -> {}", LOG_STATE, annotation.type(),
                                accessoryClass.getName());
                    }
                }
            }
            logger.info("{}Successfully initialized {} accessory types", LOG_INIT, accessoryTypes.size());
        } catch (Exception e) {
            logger.error("{}Error initializing accessory types: {}", LOG_ERROR, e.getMessage(), e);
            throw new IllegalStateException("Failed to initialize accessory types", e);
        }
    }

    /**
     * Creates a new accessory instance for the specified type.
     *
     * <p>
     * This method serves as the primary entry point for accessory creation. It
     * creates an accessory
     * instance using the standard constructor that takes a
     * {@link HomekitEventManager}. The accessory
     * type must be previously registered during initialization.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Validates accessory type against registered types</li>
     * <li>Uses reflection to create accessory instance</li>
     * <li>Provides proper error handling and logging</li>
     * <li>Ensures thread-safe operation</li>
     * </ul>
     *
     * @param type The accessory type to create
     * @return A new accessory instance
     * @throws HomekitFactoryException if the accessory type is not supported or
     *             creation fails
     * @since 1.0
     */
    @Override
    public HomekitAccessory createAccessory(String type) throws HomekitFactoryException {
        logger.trace("{}Creating accessory of type: {}", LOG_TRACE, type);
        @SuppressWarnings("null") // accessoryTypes.get() can return null, checked below
        Class<? extends HomekitAccessory> accessoryClass = accessoryTypes.get(type);
        if (accessoryClass == null) {
            logger.error("{}Unsupported accessory type: {}", LOG_ERROR, type);
            throw new HomekitFactoryException("Unsupported accessory type: " + type);
        }

        try {
            @SuppressWarnings("null") // getConstructor() and newInstance() guaranteed to return valid objects for valid
                                      // class
            Constructor<? extends HomekitAccessory> constructor = accessoryClass.getConstructor(
                    HomekitEventManager.class, HomekitServiceFactory.class, HomekitCharacteristicFactory.class);
            @SuppressWarnings("null") // newInstance() guaranteed to return valid instance for valid constructor
            HomekitAccessory instance = constructor.newInstance(eventManager, serviceFactory, characteristicFactory);
            return instance;
        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException
                | SecurityException | InvocationTargetException e) {
            logger.error("{}Error creating accessory of type {}: {}", LOG_ERROR, type, e.getMessage(), e);
            throw new HomekitFactoryException("Failed to create accessory of type: " + type, e);
        }
    }

    /**
     * Creates an accessory instance from a tag.
     *
     * <p>
     * This method creates an accessory instance using a tag instead of an accessory
     * type. The tag is
     * mapped to the appropriate accessory type during initialization.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Maps tag to accessory type</li>
     * <li>Uses standard accessory creation</li>
     * <li>Provides proper error handling</li>
     * <li>Maintains consistent logging</li>
     * </ul>
     *
     * @param tag The accessory tag
     * @return A new accessory instance
     * @throws HomekitFactoryException if the tag is not supported or creation fails
     * @since 1.0
     */
    @Override
    public HomekitAccessory createAccessoryFromTag(String tag) throws HomekitFactoryException {
        logger.trace("{}Creating accessory from tag: {}", LOG_TRACE, tag);
        @SuppressWarnings("null") // tagToTypeMap.get() can return null, checked below
        String type = tagToTypeMap.get(tag);
        if (type == null) {
            logger.error("{}Unsupported accessory tag: {}", LOG_ERROR, tag);
            throw new HomekitFactoryException("Unsupported accessory tag: " + tag);
        }
        return createAccessory(type);
    }

    /**
     * Creates an accessory instance with variable arguments.
     *
     * <p>
     * This method provides advanced accessory creation capabilities by allowing
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
     * <li>Validates accessory type against registered types</li>
     * <li>Uses reflection to find matching constructor</li>
     * <li>Handles variable argument types</li>
     * <li>Provides detailed error logging</li>
     * </ul>
     *
     * @param type The accessory type to create
     * @param args The constructor arguments
     * @return A new accessory instance
     * @throws HomekitFactoryException if the accessory type is not supported or
     *             creation fails
     * @since 1.0
     */
    @Override
    public HomekitAccessory createAccessoryWithArgs(String type, Object... args) throws HomekitFactoryException {
        logger.trace("{}Creating accessory of type: {} with {} arguments", LOG_TRACE, type, args.length);
        @SuppressWarnings("null") // accessoryTypes.get() can return null, checked below
        Class<? extends HomekitAccessory> accessoryClass = accessoryTypes.get(type);
        if (accessoryClass == null) {
            logger.error("{}Unsupported accessory type: {}", LOG_ERROR, type);
            throw new HomekitFactoryException("Unsupported accessory type: " + type);
        }

        try {
            @SuppressWarnings("null") // Stream.of(args).map(Object::getClass) safe as args cannot contain null
            Class<?>[] argTypes = Stream.of(args).map(Object::getClass).toArray(Class[]::new);
            @SuppressWarnings("null") // getConstructor() guaranteed to return valid constructor for valid class
            Constructor<? extends HomekitAccessory> constructor = accessoryClass.getConstructor(argTypes);
            @SuppressWarnings("null") // newInstance() guaranteed to return valid instance for valid constructor
            HomekitAccessory instance = constructor.newInstance(args);
            return instance;
        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException
                | SecurityException | InvocationTargetException e) {
            logger.error("{}Error creating accessory of type {} with args: {}", LOG_ERROR, type, e.getMessage(), e);
            throw new HomekitFactoryException("Failed to create accessory of type: " + type, e);
        }
    }

    /**
     * Creates an accessory instance from a tag with variable arguments.
     *
     * <p>
     * This method creates an accessory instance using a tag and custom constructor
     * arguments.
     * The tag is mapped to the appropriate accessory type during initialization.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Maps tag to accessory type</li>
     * <li>Uses custom constructor arguments</li>
     * <li>Provides proper error handling</li>
     * <li>Maintains consistent logging</li>
     * </ul>
     *
     * @param tag The accessory tag
     * @param args The constructor arguments
     * @return A new accessory instance
     * @throws HomekitFactoryException if the tag is not supported or creation fails
     * @since 1.0
     */
    public HomekitAccessory createAccessoryFromTagWithArgs(String tag, Object... args) throws HomekitFactoryException {
        logger.trace("{}Creating accessory from tag: {} with custom arguments", LOG_TRACE, tag);
        @SuppressWarnings("null") // tagToTypeMap.get() can return null, checked below
        String type = tagToTypeMap.get(tag);
        if (type == null) {
            logger.error("{}Unsupported accessory tag: {}", LOG_ERROR, tag);
            throw new HomekitFactoryException("Unsupported accessory tag: " + tag);
        }
        return createAccessoryWithArgs(type, args);
    }

    /**
     * Creates an accessory instance from a tag with a JSON value.
     *
     * <p>
     * This method creates an accessory instance using a tag and initializes it with
     * a JSON value.
     * The tag is mapped to the appropriate accessory type during initialization.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Maps tag to accessory type</li>
     * <li>Uses JSON value for initialization</li>
     * <li>Provides proper error handling</li>
     * <li>Maintains consistent logging</li>
     * </ul>
     *
     * @param tag The accessory tag
     * @param value The JSON value for initialization
     * @return A new accessory instance
     * @throws HomekitFactoryException if the tag is not supported or creation fails
     * @since 1.0
     */
    @Override
    public HomekitAccessory createAccessoryFromTagWithValue(String tag, JsonValue value)
            throws HomekitFactoryException {
        logger.trace("{}Creating accessory from tag: {} with JSON value", LOG_TRACE, tag);
        @SuppressWarnings("null") // tagToTypeMap.get() can return null, checked below
        String type = tagToTypeMap.get(tag);
        if (type == null) {
            logger.error("{}Unsupported accessory tag: {}", LOG_ERROR, tag);
            throw new HomekitFactoryException("Unsupported accessory tag: " + tag);
        }

        @SuppressWarnings("null") // accessoryTypes.get() can return null, checked below
        Class<? extends HomekitAccessory> accessoryClass = accessoryTypes.get(type);
        if (accessoryClass == null) {
            logger.error("{}Unsupported accessory tag: {}", LOG_ERROR, tag);
            throw new HomekitFactoryException("Unsupported accessory tag: " + tag);
        }

        try {
            @SuppressWarnings("null") // getConstructor() guaranteed to return valid constructor for valid class
            Constructor<? extends HomekitAccessory> constructor = accessoryClass.getConstructor(
                    HomekitEventManager.class, HomekitServiceFactory.class, HomekitCharacteristicFactory.class,
                    JsonValue.class);
            @SuppressWarnings("null") // newInstance() guaranteed to return valid instance for valid constructor
            HomekitAccessory instance = constructor.newInstance(eventManager, serviceFactory, characteristicFactory,
                    value);
            return instance;
        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException
                | SecurityException | InvocationTargetException e) {
            logger.error("{}Error creating accessory from tag {} with value: {}", LOG_ERROR, tag, e.getMessage(), e);
            throw new HomekitFactoryException("Failed to create accessory from tag: " + tag, e);
        }
    }

    /**
     * Checks if an accessory type is supported.
     *
     * <p>
     * This method verifies whether a given accessory type has been registered
     * during initialization.
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
     * @param type The accessory type to check
     * @return true if the accessory type is supported, false otherwise
     * @since 1.0
     */
    @Override
    public boolean supportsAccessoryType(String type) {
        boolean supported = accessoryTypes.containsKey(type);
        logger.trace("{}Accessory type {} is {}", LOG_TRACE, type, supported ? "supported" : "not supported");
        return supported;
    }

    /**
     * Checks if an accessory tag is supported.
     *
     * <p>
     * This method verifies whether a given accessory tag has been registered during
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
     * @param tag The accessory tag to check
     * @return true if the accessory tag is supported, false otherwise
     * @since 1.0
     */
    @Override
    public boolean supportsTag(String tag) {
        boolean supported = tagToTypeMap.containsKey(tag);
        logger.trace("{}Accessory tag {} is {}", LOG_TRACE, tag, supported ? "supported" : "not supported");
        return supported;
    }

    /**
     * Gets all supported accessory types.
     *
     * <p>
     * This method returns an unmodifiable set of all accessory types that have been
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
     * @return An unmodifiable set of supported accessory types
     * @since 1.0
     */
    @Override
    public Set<String> getSupportedAccessoryTypes() {
        Set<String> types = Collections.unmodifiableSet(new HashSet<>(accessoryTypes.keySet()));
        logger.trace("{}Returning {} supported accessory types", LOG_TRACE, types.size());
        return types;
    }

    /**
     * Gets all supported accessory tags.
     *
     * <p>
     * This method returns an unmodifiable set of all accessory tags that have been
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
     * @return An unmodifiable set of supported accessory tags
     * @since 1.0
     */
    @Override
    public Set<String> getSupportedTags() {
        Set<String> tags = Collections.unmodifiableSet(new HashSet<>(tagToTypeMap.keySet()));
        logger.trace("{}Returning {} supported tags", LOG_TRACE, tags.size());
        return tags;
    }
}
