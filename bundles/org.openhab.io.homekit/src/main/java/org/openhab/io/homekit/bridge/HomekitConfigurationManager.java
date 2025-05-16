package org.openhab.io.homekit.bridge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.service.WatchService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * Manages HomeKit configurations from various sources.
 * Handles loading, storing, and updating configurations with priority management.
 * Supports cascading configurations with wildcards.
 * Any YAML file can contain multiple sections of different types (items, things, channels).
 */
@Component(service = HomekitConfigurationManager.class)
@NonNullByDefault
public class HomekitConfigurationManager implements WatchService.WatchEventListener {
    private static final Logger logger = LoggerFactory.getLogger(HomekitConfigurationManager.class);
    private static final String CONFIG_DIR = "conf/homekit";
    
    private final MetadataRegistry metadataRegistry;
    private final WatchService watchService;
    private final Yaml yaml;
    
    // Separate stores for different configuration types
    private final Map<String, Map<String, Object>> itemConfigs = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> thingConfigs = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> channelConfigs = new ConcurrentHashMap<>();
    
    // Track which file each configuration comes from
    private final Map<String, String> itemSourceFiles = new ConcurrentHashMap<>();
    private final Map<String, String> thingSourceFiles = new ConcurrentHashMap<>();
    private final Map<String, String> channelSourceFiles = new ConcurrentHashMap<>();
    
    @Activate
    public HomekitConfigurationManager(@Reference MetadataRegistry metadataRegistry,
            @Reference WatchService watchService) {
        this.metadataRegistry = metadataRegistry;
        this.watchService = watchService;
        this.yaml = new Yaml();
        
        // Register directory for watching
        Path confDir = Paths.get(CONFIG_DIR);
        this.watchService.registerListener(this, confDir);
        
        // Initial load of all YAML files in the directory
        try (Stream<Path> paths = Files.walk(confDir, 1)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().toLowerCase().endsWith(".yaml") || 
                                path.toString().toLowerCase().endsWith(".yml"))
                 .forEach(this::processConfigFile);
        } catch (IOException e) {
            logger.error("Error scanning configuration directory: {}", e.getMessage());
        }
    }
    
    @Override
    public void processWatchEvent(WatchService.Kind kind, Path path) {
        if (kind == WatchService.Kind.CREATE || kind == WatchService.Kind.MODIFY) {
            processConfigFile(path);
        }
    }
    
    /**
     * Processes a configuration file and updates the appropriate stores
     */
    private void processConfigFile(Path file) {
        try {
            String fileName = file.getFileName().toString();
            Map<String, Object> yamlConfig = yaml.load(Files.readString(file));
            
            // Process each section in the YAML file
            if (yamlConfig.containsKey("items")) {
                processItemConfigs(yamlConfig, fileName);
            }
            if (yamlConfig.containsKey("things")) {
                processThingConfigs(yamlConfig, fileName);
            }
            if (yamlConfig.containsKey("channels")) {
                processChannelConfigs(yamlConfig, fileName);
            }
        } catch (IOException e) {
            logger.error("Error processing config file {}: {}", file, e.getMessage());
        }
    }
    
    /**
     * Processes item configurations from YAML
     */
    @SuppressWarnings("unchecked")
    private void processItemConfigs(Map<String, Object> yamlConfig, String fileName) {
        Map<String, Object> items = (Map<String, Object>) yamlConfig.get("items");
        if (items != null) {
            items.forEach((uid, config) -> {
                if (config instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> configMap = (Map<String, Object>) config;
                    itemConfigs.put(uid, configMap);
                    itemSourceFiles.put(uid, fileName);
                    updateItemMetadata(uid, configMap);
                }
            });
        }
    }
    
    /**
     * Processes thing configurations from YAML
     */
    @SuppressWarnings("unchecked")
    private void processThingConfigs(Map<String, Object> yamlConfig, String fileName) {
        Map<String, Object> things = (Map<String, Object>) yamlConfig.get("things");
        if (things != null) {
            things.forEach((uid, config) -> {
                if (config instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> configMap = (Map<String, Object>) config;
                    thingConfigs.put(uid, configMap);
                    thingSourceFiles.put(uid, fileName);
                    updateThingMetadata(uid, configMap);
                }
            });
        }
    }
    
    /**
     * Processes channel configurations from YAML
     */
    @SuppressWarnings("unchecked")
    private void processChannelConfigs(Map<String, Object> yamlConfig, String fileName) {
        Map<String, Object> channels = (Map<String, Object>) yamlConfig.get("channels");
        if (channels != null) {
            channels.forEach((uid, config) -> {
                if (config instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> configMap = (Map<String, Object>) config;
                    channelConfigs.put(uid, configMap);
                    channelSourceFiles.put(uid, fileName);
                    updateChannelMetadata(uid, configMap);
                }
            });
        }
    }
    
    /**
     * Updates item metadata in the registry
     */
    private void updateItemMetadata(String itenName, Map<String, Object> config) {
        MetadataKey key = new MetadataKey("homekit", itenName);
        String value = yaml.dump(config);
        Metadata metadata = new Metadata(key, value, config);
        metadataRegistry.add(metadata);
    }
    
    /**
     * Updates thing metadata in the registry
     */
    private void updateThingMetadata(String thingUid, Map<String, Object> config) {
        MetadataKey key = new MetadataKey("homekit", thingUid);
        String value = yaml.dump(config);
        Metadata metadata = new Metadata(key, value, config);
        metadataRegistry.add(metadata);
    }
    
    /**
     * Updates channel metadata in the registry
     */
    private void updateChannelMetadata(String channelUid, Map<String, Object> config) {
        MetadataKey key = new MetadataKey("homekit", channelUid);
        String value = yaml.dump(config);
        Metadata metadata = new Metadata(key, value, config);
        metadataRegistry.add(metadata);
    }
    
    /**
     * Gets configuration for a given UID with cascading support
     */
    public Optional<Map<String, Object>> getConfiguration(String uid, ConfigurationType type) {
        Map<String, Map<String, Object>> configs = switch (type) {
            case ITEM -> itemConfigs;
            case THING -> thingConfigs;
            case CHANNEL -> channelConfigs;
        };
        
        Map<String, String> sourceFiles = switch (type) {
            case ITEM -> itemSourceFiles;
            case THING -> thingSourceFiles;
            case CHANNEL -> channelSourceFiles;
        };
        
        // Try exact match first
        Map<String, Object> config = configs.get(uid);
        if (config != null) {
            return Optional.of(config);
        }
        
        // Try wildcard matches
        if (type == ConfigurationType.ITEM) {
            // For items, try wildcard match at the end
            String wildcardUid = uid.substring(0, uid.lastIndexOf('_') + 1) + "*";
            config = configs.get(wildcardUid);
            if (config != null) {
                String sourceFile = sourceFiles.get(wildcardUid);
                if (sourceFile != null) {
                    sourceFiles.put(uid, sourceFile);
                }
                return Optional.of(config);
            }
        } else {
            // For things and channels, try wildcard matches at any segment
            String[] segments = uid.split(":");
            for (int i = segments.length - 1; i >= 0; i--) {
                StringBuilder wildcardBuilder = new StringBuilder();
                for (int j = 0; j < segments.length; j++) {
                    if (j > 0) {
                        wildcardBuilder.append(":");
                    }
                    wildcardBuilder.append(j == i ? "*" : segments[j]);
                }
                String wildcardUid = wildcardBuilder.toString();
                config = configs.get(wildcardUid);
                if (config != null) {
                    String sourceFile = sourceFiles.get(wildcardUid);
                    if (sourceFile != null) {
                        sourceFiles.put(uid, sourceFile);
                    }
                    return Optional.of(config);
                }
            }
        }
        
        // Try global default
        config = configs.get("*");
        if (config != null) {
            String sourceFile = sourceFiles.get("*");
            if (sourceFile != null) {
                sourceFiles.put(uid, sourceFile);
            }
            return Optional.of(config);
        }
        
        return Optional.empty();
    }
    
    /**
     * Gets the effective source file for a configuration UID.
     * This will return the actual source file if it exists, or the source file
     * of the wildcard/global rule that was used to match this UID.
     */
    public Optional<String> getEffectiveSourceFile(String uid, ConfigurationType type) {
        Map<String, String> sourceFiles = switch (type) {
            case ITEM -> itemSourceFiles;
            case THING -> thingSourceFiles;
            case CHANNEL -> channelSourceFiles;
        };
        
        // First check if we have a direct source file
        String sourceFile = sourceFiles.get(uid);
        if (sourceFile != null) {
            return Optional.of(sourceFile);
        }
        
        // If not, check for wildcard matches
        if (type == ConfigurationType.ITEM) {
            // For items, check wildcard match at the end
            String wildcardUid = uid.substring(0, uid.lastIndexOf('_') + 1) + "*";
            sourceFile = sourceFiles.get(wildcardUid);
            if (sourceFile != null) {
                return Optional.of(sourceFile);
            }
        } else {
            // For things and channels, check wildcard matches at any segment
            String[] segments = uid.split(":");
            for (int i = segments.length - 1; i >= 0; i--) {
                StringBuilder wildcardBuilder = new StringBuilder();
                for (int j = 0; j < segments.length; j++) {
                    if (j > 0) {
                        wildcardBuilder.append(":");
                    }
                    wildcardBuilder.append(j == i ? "*" : segments[j]);
                }
                String wildcardUid = wildcardBuilder.toString();
                sourceFile = sourceFiles.get(wildcardUid);
                if (sourceFile != null) {
                    return Optional.of(sourceFile);
                }
            }
        }
        
        // Finally, check for global default
        return Optional.ofNullable(sourceFiles.get("*"));
    }
    
    /**
     * Updates configuration for a given UID
     */
    public void updateConfiguration(String uid, Map<String, Object> config, ConfigurationType type) {
        Map<String, Map<String, Object>> configs = switch (type) {
            case ITEM -> itemConfigs;
            case THING -> thingConfigs;
            case CHANNEL -> channelConfigs;
        };
        
        configs.put(uid, config);
        
        // Update metadata
        switch (type) {
            case ITEM -> updateItemMetadata(uid, config);
            case THING -> updateThingMetadata(uid, config);
            case CHANNEL -> updateChannelMetadata(uid, config);
        }
    }
    
    /**
     * Updates configuration for a given UID and writes to its source file
     * 
     * @param uid the unique identifier for the configuration
     * @param config the configuration map to store
     * @param type the type of configuration (ITEM, THING, CHANNEL)
     */
    public void updateConfiguration(String uid, Map<String, Object> config, ConfigurationType type, String yamlFile) {
        // First update the configuration in memory
        updateConfiguration(uid, config, type);
        
        // Update the source file mapping
        switch (type) {
            case ITEM -> itemSourceFiles.put(uid, yamlFile);
            case THING -> thingSourceFiles.put(uid, yamlFile);
            case CHANNEL -> channelSourceFiles.put(uid, yamlFile);
        }
        
        // Then update the specific YAML file
        Path configPath = Paths.get(CONFIG_DIR, yamlFile);
        try {
            Map<String, Object> yamlConfig = new HashMap<>();
            
            // Add only the sections that belong to this file
            Map<String, Object> items = new HashMap<>();
            Map<String, Object> things = new HashMap<>();
            Map<String, Object> channels = new HashMap<>();
            
            // Collect configurations for this file
            itemSourceFiles.forEach((itemUid, file) -> {
                if (file.equals(yamlFile)) {
                    items.put(itemUid, itemConfigs.get(itemUid));
                }
            });
            thingSourceFiles.forEach((thingUid, file) -> {
                if (file.equals(yamlFile)) {
                    things.put(thingUid, thingConfigs.get(thingUid));
                }
            });
            channelSourceFiles.forEach((channelUid, file) -> {
                if (file.equals(yamlFile)) {
                    channels.put(channelUid, channelConfigs.get(channelUid));
                }
            });
            
            // Add non-empty sections to the YAML config
            if (!items.isEmpty()) {
                yamlConfig.put("items", items);
            }
            if (!things.isEmpty()) {
                yamlConfig.put("things", things);
            }
            if (!channels.isEmpty()) {
                yamlConfig.put("channels", channels);
            }
            
            yaml.dump(yamlConfig, Files.newBufferedWriter(configPath));
        } catch (IOException e) {
            logger.error("Error writing YAML configuration to {}: {}", yamlFile, e.getMessage());
        }
    }
    
    /**
     * Removes configuration for a given UID
     */
    public void removeConfiguration(String uid, ConfigurationType type) {
        Map<String, Map<String, Object>> configs = switch (type) {
            case ITEM -> itemConfigs;
            case THING -> thingConfigs;
            case CHANNEL -> channelConfigs;
        };
        
        // Get the source file before removing the configuration
        String sourceFile = switch (type) {
            case ITEM -> itemSourceFiles.remove(uid);
            case THING -> thingSourceFiles.remove(uid);
            case CHANNEL -> channelSourceFiles.remove(uid);
        };
        
        configs.remove(uid);
        metadataRegistry.remove(new MetadataKey("homekit", uid));
        
        // Update the source file if it exists
        if (sourceFile != null) {
            updateConfiguration(uid, Collections.emptyMap(), type, sourceFile);
        }
    }
    
    /**
     * Gets the source file for a configuration
     */
    public Optional<String> getSourceFile(String uid, ConfigurationType type) {
        return Optional.ofNullable(switch (type) {
            case ITEM -> itemSourceFiles.get(uid);
            case THING -> thingSourceFiles.get(uid);
            case CHANNEL -> channelSourceFiles.get(uid);
        });
    }
    
    /**
     * Enum representing different types of configurations
     */
    public enum ConfigurationType {
        ITEM, THING, CHANNEL
    }
} 