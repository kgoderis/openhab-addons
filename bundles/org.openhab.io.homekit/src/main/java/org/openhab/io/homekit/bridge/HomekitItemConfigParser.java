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

package org.openhab.io.homekit.bridge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.items.Item;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for HomeKit item configurations.
 *
 * This class implements a parser for HomeKit item configurations stored in OpenHAB metadata.
 * It handles both simple and complex accessories, including all configuration parameters
 * and characteristic mappings. The parser supports metadata-based configuration parsing,
 * characteristic mapping parsing, and parameter value type detection.
 *
 * The class integrates with:
 * - {@link org.openhab.core.items.MetadataRegistry} for metadata access and management
 * - {@link org.openhab.core.items.Item} for item information and state
 * - {@link org.openhab.core.items.Metadata} for configuration storage and retrieval
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0.0
 */
@NonNullByDefault
public class HomekitItemConfigParser {
    private static final Logger logger = LoggerFactory.getLogger(HomekitItemConfigParser.class);

    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit ItemConfigParser: ";
    private static final String LOG_PARSE = LOG_PREFIX + "Parse - ";
    private static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    private static final String LOG_CONFIG = LOG_PREFIX + "Config - ";

    private final MetadataRegistry metadataRegistry;
    private static final Pattern CONFIG_PATTERN = Pattern.compile("\\[(.*?)\\]");
    private static final Pattern MAPPING_PATTERN = Pattern.compile("([A-Z]+)=\"([^\"]+)\"");

    /**
     * Creates a new HomekitItemConfigParser instance.
     *
     * This method initializes the parser with the required metadata registry
     * for accessing item configurations.
     *
     * Key implementation details:
     * - Validates metadata registry parameter
     * - Initializes logging
     * - Sets up pattern matchers
     *
     * @param metadataRegistry The registry to use for accessing item metadata
     * @throws NullPointerException if metadataRegistry is null
     */
    public HomekitItemConfigParser(MetadataRegistry metadataRegistry) {
        this.metadataRegistry = metadataRegistry;
        logger.debug("{}Initialized with metadata registry", LOG_PREFIX + "Init - ");
    }

    /**
     * Parses the HomeKit configuration from an item.
     *
     * This method extracts and parses the HomeKit configuration from an item's metadata.
     * It processes both the service type and any additional characteristics or parameters.
     *
     * Key implementation details:
     * - Retrieves metadata from registry
     * - Parses service type and characteristics
     * - Processes configuration parameters
     * - Includes group membership information
     *
     * @param item The item to parse
     * @return Map containing the parsed configuration
     * @throws NullPointerException if item is null
     */
    public Map<String, Object> parseItemConfig(Item item) {
        logger.debug("{}Parsing configuration for item {}", LOG_PARSE, item.getName());
        Map<String, Object> config = new HashMap<>();

        // Get metadata from registry
        MetadataKey key = new MetadataKey("homekit", item.getName());
        @SuppressWarnings("null") // metadataRegistry.get() can return null, which is checked below
        Metadata metadata = metadataRegistry.get(key);

        if (metadata != null) {
            String value = metadata.getValue();
            config.put("type", item.getClass().getSimpleName());
            config.put("name", item.getName());
            String label = item.getLabel();
            if (label != null) {
                config.put("label", label);
            }

            // Parse the metadata value
            parseMetadataValue(value, config);

            // Add group membership if applicable
            if (!item.getGroupNames().isEmpty()) {
                config.put("groups", new ArrayList<>(item.getGroupNames()));
                logger.debug("{}Item {} belongs to groups: {}", LOG_PARSE, item.getName(), item.getGroupNames());
            }
        } else {
            logger.debug("{}No metadata found for item {}", LOG_PARSE, item.getName());
        }

        return config;
    }

    /**
     * Parses the metadata value string into configuration parameters.
     *
     * This method processes the raw metadata value string, extracting service type,
     * characteristics, and configuration parameters.
     *
     * Key implementation details:
     * - Splits value into service type and characteristics
     * - Extracts configuration parameters
     * - Updates the provided configuration map
     *
     * @param value The metadata value string to parse
     * @param config The configuration map to populate
     * @throws NullPointerException if value or config is null
     */
    private void parseMetadataValue(String value, Map<String, Object> config) {
        logger.debug("{}Parsing metadata value: {}", LOG_PARSE, value);

        // Split the value into service type and characteristics
        String[] parts = value.split("\\.");
        String serviceType = parts[0];
        config.put("serviceType", serviceType);
        logger.debug("{}Found service type: {}", LOG_PARSE, serviceType);

        if (parts.length > 1) {
            List<String> characteristics = new ArrayList<>();
            for (int i = 1; i < parts.length; i++) {
                characteristics.add(parts[i]);
            }
            config.put("characteristics", characteristics);
            logger.debug("{}Found characteristics: {}", LOG_PARSE, characteristics);
        }

        // Parse configuration parameters
        Matcher configMatcher = CONFIG_PATTERN.matcher(value);
        if (configMatcher.find()) {
            String configStr = configMatcher.group(1);
            Map<String, Object> params = parseConfigParameters(configStr);
            config.put("parameters", params);
            logger.debug("{}Found parameters: {}", LOG_PARSE, params);
        }
    }

    /**
     * Parses configuration parameters from a string.
     *
     * This method extracts and parses configuration parameters from a string,
     * handling both characteristic mappings and numeric values.
     *
     * Key implementation details:
     * - Parses characteristic mappings
     * - Handles numeric value conversion
     * - Processes parameter pairs
     * - Returns a map of parameter names to values
     *
     * @param configStr The configuration string to parse
     * @return Map of parameter names to values
     * @throws NullPointerException if configStr is null
     */
    private Map<String, Object> parseConfigParameters(String configStr) {
        if (configStr == null) {
            throw new NullPointerException("configStr cannot be null");
        }
        logger.debug("{}Parsing configuration parameters: {}", LOG_PARSE, configStr);
        Map<String, Object> params = new HashMap<>();

        // Parse characteristic mappings
        Matcher mappingMatcher = MAPPING_PATTERN.matcher(configStr);
        while (mappingMatcher.find()) {
            String key = mappingMatcher.group(1);
            String value = mappingMatcher.group(2);
            params.put(key, value);
            logger.debug("{}Found mapping: {} = {}", LOG_PARSE, key, value);
        }

        // Parse other parameters
        String[] paramPairs = configStr.split(",");
        for (String pair : paramPairs) {
            pair = pair.trim();
            if (!pair.contains("=")) {
                continue;
            }
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                try {
                    // Try to parse as number if possible
                    if (value.matches("-?\\d+")) {
                        params.put(key, Long.parseLong(value));
                    } else if (value.matches("-?\\d*\\.\\d+")) {
                        params.put(key, Double.parseDouble(value));
                    } else {
                        params.put(key, value);
                    }
                    logger.debug("{}Found parameter: {} = {}", LOG_PARSE, key, value);
                } catch (NumberFormatException e) {
                    logger.warn("{}Failed to parse numeric value for parameter {}: {}", LOG_ERROR, key, value);
                    params.put(key, value);
                }
            }
        }
        return params;
    }

    /**
     * Gets a filtered configuration for an item.
     *
     * This method retrieves and filters the HomeKit configuration for an item,
     * returning only the specified configuration fields.
     *
     * Key implementation details:
     * - Retrieves full configuration
     * - Filters based on specified fields
     * - Handles missing fields gracefully
     *
     * @param item The item to get configuration for
     * @return Map containing the filtered configuration
     * @throws NullPointerException if item is null
     */
    public Map<String, Object> getFilteredConfig(Item item) {
        if (item == null) {
            throw new NullPointerException("item cannot be null");
        }
        logger.debug("{}Getting filtered configuration for item {}", LOG_CONFIG, item.getName());
        Map<String, Object> fullConfig = parseItemConfig(item);
        Map<String, Object> filteredConfig = new HashMap<>();

        // Filter configuration fields
        String[] fields = { "serviceType", "characteristics", "parameters" };
        for (String field : fields) {
            if (fullConfig.containsKey(field)) {
                Object value = fullConfig.get(field);
                if (value != null) {
                    filteredConfig.put(field, value);
                }
            }
        }

        logger.debug("{}Filtered configuration for item {}: {}", LOG_CONFIG, item.getName(), filteredConfig);
        return filteredConfig;
    }
}
