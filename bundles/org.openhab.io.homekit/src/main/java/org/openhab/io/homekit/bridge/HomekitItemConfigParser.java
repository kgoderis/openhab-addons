package org.openhab.io.homekit.bridge;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openhab.core.items.Item;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for HomeKit item configurations that handles both simple and complex accessories,
 * including all configuration parameters and characteristic mappings.
 */
public class HomekitItemConfigParser {
    private final Logger logger = LoggerFactory.getLogger(HomekitItemConfigParser.class);
    private final MetadataRegistry metadataRegistry;
    private static final Pattern CONFIG_PATTERN = Pattern.compile("\\[(.*?)\\]");
    private static final Pattern MAPPING_PATTERN = Pattern.compile("([A-Z]+)=\"([^\"]+)\"");

    public HomekitItemConfigParser(MetadataRegistry metadataRegistry) {
        this.metadataRegistry = metadataRegistry;
    }

    /**
     * Parses the HomeKit configuration from an item.
     * @param item The item to parse
     * @return Map containing the parsed configuration
     */
    public Map<String, Object> parseItemConfig(Item item) {
        Map<String, Object> config = new HashMap<>();
        
        // Get metadata from registry
        MetadataKey key = new MetadataKey("homekit", item.getName());
        Metadata metadata = metadataRegistry.get(key);
        
        if (metadata != null) {
            String value = metadata.getValue();
            config.put("type", item.getClass().getSimpleName());
            config.put("name", item.getName());
            config.put("label", item.getLabel());
            
            // Parse the metadata value
            parseMetadataValue(value, config);
            
            // Add group membership if applicable
            if (item.getGroupNames() != null && !item.getGroupNames().isEmpty()) {
                config.put("groups", new ArrayList<>(item.getGroupNames()));
            }
        }
        
        return config;
    }

    /**
     * Parses the metadata value string into configuration parameters.
     * @param value The metadata value string
     * @param config The configuration map to populate
     */
    private void parseMetadataValue(String value, Map<String, Object> config) {
        // Split the value into service type and characteristics
        String[] parts = value.split("\\.");
        String serviceType = parts[0];
        config.put("serviceType", serviceType);
        
        if (parts.length > 1) {
            List<String> characteristics = new ArrayList<>();
            for (int i = 1; i < parts.length; i++) {
                characteristics.add(parts[i]);
            }
            config.put("characteristics", characteristics);
        }
        
        // Parse configuration parameters
        Matcher configMatcher = CONFIG_PATTERN.matcher(value);
        if (configMatcher.find()) {
            String configStr = configMatcher.group(1);
            Map<String, Object> params = parseConfigParameters(configStr);
            config.put("parameters", params);
        }
    }

    /**
     * Parses configuration parameters from a string.
     * @param configStr The configuration string
     * @return Map of parameter names to values
     */
    private Map<String, Object> parseConfigParameters(String configStr) {
        Map<String, Object> params = new HashMap<>();
        
        // Parse characteristic mappings
        Matcher mappingMatcher = MAPPING_PATTERN.matcher(configStr);
        while (mappingMatcher.find()) {
            String key = mappingMatcher.group(1);
            String value = mappingMatcher.group(2);
            params.put(key, value);
        }
        
        // Parse other parameters
        String[] paramPairs = configStr.split(",");
        for (String pair : paramPairs) {
            pair = pair.trim();
            if (!pair.contains("=")) continue;
            
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                
                // Try to parse as number if possible
                try {
                    if (value.contains(".")) {
                        params.put(key, Double.parseDouble(value));
                    } else {
                        params.put(key, Integer.parseInt(value));
                    }
                } catch (NumberFormatException e) {
                    // If not a number, store as string
                    params.put(key, value);
                }
            }
        }
        
        return params;
    }

    /**
     * Gets a filtered configuration containing only serviceType and parameters.
     * @param item The item to get configuration for
     * @return Map containing only serviceType and parameters
     */
    public Map<String, Object> getFilteredConfig(Item item) {
        Map<String, Object> fullConfig = parseItemConfig(item);
        Map<String, Object> filteredConfig = new HashMap<>();
        
        if (fullConfig.containsKey("serviceType")) {
            filteredConfig.put("serviceType", fullConfig.get("serviceType"));
        }
        
        if (fullConfig.containsKey("parameters")) {
            filteredConfig.put("parameters", fullConfig.get("parameters"));
        }
        
        return filteredConfig;
    }
} 