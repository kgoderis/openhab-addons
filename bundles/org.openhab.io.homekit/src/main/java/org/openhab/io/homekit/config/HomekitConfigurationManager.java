package org.openhab.io.homekit.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.HashSet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.ConfigDescriptionProvider;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.service.WatchService;
import org.openhab.core.thing.UID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.ChannelUID;
import org.openhab.io.homekit.util.ItemUID;
import org.openhab.io.homekit.util.HomekitUID;
import org.openhab.io.homekit.core.accessory.HomekitAccessoryUID;
import org.openhab.io.homekit.core.characteristic.HomekitCharacteristicUID;
import org.openhab.io.homekit.core.service.HomekitServiceUID;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
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
@Component(service = HomekitConfigurationManager.class, configurationPid = "org.openhab.homekit")
@NonNullByDefault
public class HomekitConfigurationManager implements WatchService.WatchEventListener {
    private static final Logger logger = LoggerFactory.getLogger(HomekitConfigurationManager.class);
    private static final String CONFIG_DIR = "conf/homekit";
    
    private final WatchService watchService;
    private final ConfigDescriptionRegistry configDescriptionRegistry;
    private final Yaml yaml;
    
    // Separate stores for different configuration types
    private final Map<UID, Map<String, Object>> itemConfigs = new ConcurrentHashMap<>();
    private final Map<UID, Map<String, Object>> thingConfigs = new ConcurrentHashMap<>();
    private final Map<UID, Map<String, Object>> channelConfigs = new ConcurrentHashMap<>();
    private final Map<UID, Map<String, Object>> accessoryConfigs = new ConcurrentHashMap<>();
    private final Map<UID, Map<String, Object>> serviceConfigs = new ConcurrentHashMap<>();
    private final Map<UID, Map<String, Object>> characteristicConfigs = new ConcurrentHashMap<>();
    private final Map<UID, Map<String, Object>> profileConfigs = new ConcurrentHashMap<>();
    private final Map<UID, Map<String, Object>> bridgeConfigs = new ConcurrentHashMap<>();
    private final Map<UID, Map<String, Object>> networkConfigs = new ConcurrentHashMap<>();
    private final Map<UID, Map<String, Object>> eventConfigs = new ConcurrentHashMap<>();
    
    // Track which file each configuration comes from
    private final Map<UID, String> itemSourceFiles = new ConcurrentHashMap<>();
    private final Map<UID, String> thingSourceFiles = new ConcurrentHashMap<>();
    private final Map<UID, String> channelSourceFiles = new ConcurrentHashMap<>();
    private final Map<UID, String> accessorySourceFiles = new ConcurrentHashMap<>();
    private final Map<UID, String> serviceSourceFiles = new ConcurrentHashMap<>();
    private final Map<UID, String> characteristicSourceFiles = new ConcurrentHashMap<>();
    private final Map<UID, String> profileSourceFiles = new ConcurrentHashMap<>();
    private final Map<UID, String> bridgeSourceFiles = new ConcurrentHashMap<>();
    private final Map<UID, String> networkSourceFiles = new ConcurrentHashMap<>();
    private final Map<UID, String> eventSourceFiles = new ConcurrentHashMap<>();
    
    private final Map<UID, String> uidToYamlFile = new ConcurrentHashMap<>();
    private final Map<String, Set<UID>> yamlFileToUIDs = new ConcurrentHashMap<>();
    
    @Activate
    public HomekitConfigurationManager(@Reference WatchService watchService,
            @Reference ConfigDescriptionRegistry configDescriptionRegistry,
            Map<String, Object> config) {
        this.watchService = watchService;
        this.configDescriptionRegistry = configDescriptionRegistry;
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
        
        // Process OSGi configuration
        modified(config);
    }
    
    @Modified
    protected void modified(Map<String, Object> config) {
        if (config == null) {
            return;
        }

        config.forEach((key, value) -> {
            String[] parts = key.split("\\.");
            if (parts.length >= 3) {
                String typeStr = parts[0];
                String uidStr = parts[1];
                String configKey = parts[2];

                try {
                    ConfigurationType type = ConfigurationType.valueOf(typeStr.toUpperCase());
                    UID uid = convertToUID(uidStr, type);
                    updateConfiguration(uid, type, configKey, value);
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid configuration key format: {}", key, e);
                }
            }
        });
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
            for (ConfigurationType type : ConfigurationType.values()) {
                if (yamlConfig.containsKey(type.getYamlSection())) {
                    processConfigs(yamlConfig, fileName, type);
                }
            }
        } catch (IOException e) {
            logger.error("Error processing config file {}: {}", file, e.getMessage());
        }
    }
    
    /**
     * Converts a string UID to the appropriate UID type based on its format and section
     */
    private UID convertToUID(String uidString, ConfigurationType type) {
        if (uidString == null || uidString.isEmpty()) {
            throw new IllegalArgumentException("UID string cannot be null or empty");
        }

        // Handle wildcard UIDs
        if (uidString.equals("*")) {
            return HomekitUID.WILDCARD_UID;
        }

        String[] segments = uidString.split(":");
        
        // Handle HomeKit UIDs
        if (segments.length >= 2 && segments[0].equals("homekit")) {
            switch (segments[1]) {
                case "item":
                    return new ItemUID(uidString);
                case "accessory":
                    return new HomekitAccessoryUID(uidString);
                case "service":
                    return new HomekitServiceUID(uidString);
                case "characteristic":
                    return new HomekitCharacteristicUID(uidString);
                case "profile":
                    return new HomekitUID(uidString);
                case "bridge":
                    return new HomekitUID(uidString);
                case "network":
                    return new HomekitUID(uidString);
                case "event":
                    return new HomekitUID(uidString);
            }
        }

        // Handle based on configuration type
        switch (type) {
            case ITEM:
                return new ItemUID(uidString);
            case THING:
            case SERVICE:
                if (segments.length == 3) {
                    return new ThingUID(segments[0], segments[1], segments[2]);
                }
                break;
            case CHANNEL:
            case CHARACTERISTIC:
                if (segments.length == 4) {
                    return new ChannelUID(new ThingUID(segments[0], segments[1], segments[2]), segments[3]);
                }
                break;
            case ACCESSORY:
                return new HomekitAccessoryUID(uidString);
            case PROFILE:
                return new HomekitUID(uidString);
            case BRIDGE:
                return new HomekitUID(uidString);
            case NETWORK:
                return new HomekitUID(uidString);
            case EVENT:
                return new HomekitUID(uidString);
        }

        // Default to ThingUID if no specific type can be determined
        return new ThingUID("homekit", "unknown", uidString);
    }

    /**
     * Converts a string UID to the appropriate UID type based on its format
     */
    private UID convertToUID(String uidString) {
        // Try to determine type from the UID string first
        String[] segments = uidString.split(":");
        if (segments.length >= 2 && segments[0].equals("homekit")) {
            try {
                ConfigurationType type = ConfigurationType.valueOf(segments[1].toUpperCase());
                return convertToUID(uidString, type);
            } catch (IllegalArgumentException e) {
                // If we can't determine type from UID, try to infer from format
            }
        }

        // Try to infer type from format
        if (segments.length == 3) {
            return convertToUID(uidString, ConfigurationType.THING);
        } else if (segments.length == 4) {
            return convertToUID(uidString, ConfigurationType.CHANNEL);
        }

        // Default to ThingUID if no specific type can be determined
        return new ThingUID("homekit", "unknown", uidString);
    }
    
    /**
     * Processes configurations from YAML for a specific type
     */
    @SuppressWarnings("unchecked")
    private void processConfigs(Map<String, Object> yamlConfig, String fileName, ConfigurationType type) {
        Map<String, Object> configs = (Map<String, Object>) yamlConfig.get(type.getYamlSection());
        if (configs != null) {
            for (Map.Entry<String, Object> entry : configs.entrySet()) {
                String uidString = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> config = (Map<String, Object>) value;
                    try {
                        UID uid = convertToUID(uidString, type);
                        updateConfiguration(uid, type, config, fileName);
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid UID format in configuration: {}", uidString, e);
                    }
                }
            }
        }
    }
    
    /**
     * Writes a YAML configuration to a file
     */
    private void writeYamlFile(String yamlFile, Map<String, Object> yamlConfig) throws IOException {
        Path configPath = Paths.get(CONFIG_DIR, yamlFile);
        yaml.dump(yamlConfig, Files.newBufferedWriter(configPath));
    }
    
    /**
     * Stores configuration for a given UID in its respective YAML file
     */
    private void storeConfigs(UID uid, ConfigurationType type) {
        Map<UID, Map<String, Object>> configs = switch (type) {
            case ITEM -> itemConfigs;
            case THING -> thingConfigs;
            case CHANNEL -> channelConfigs;
            case ACCESSORY -> accessoryConfigs;
            case SERVICE -> serviceConfigs;
            case CHARACTERISTIC -> characteristicConfigs;
            case PROFILE -> profileConfigs;
            case BRIDGE -> bridgeConfigs;
            case NETWORK -> networkConfigs;
            case EVENT -> eventConfigs;
        };

        Map<String, Object> config = configs.get(uid);
        if (config != null) {
            // Try to find the effective source file, considering wildcard matches
            Optional<String> yamlFile = getEffectiveSourceFile(uid, type);
            if (yamlFile.isPresent()) {
                try {
                    Map<String, Object> yamlConfig = new HashMap<>();
                    yamlConfig.put(uid.toString(), config);
                    writeYamlFile(yamlFile.get(), yamlConfig);
                } catch (IOException e) {
                    logger.error("Failed to write configuration to file: {}", yamlFile.get(), e);
                }
            } else {
                logger.warn("No source file found for UID: {}", uid);
            }
        }
    }

    /**
     * Gets all configurations of a given type
     */
    public Map<UID, Map<String, Object>> getAllConfigurations(ConfigurationType type) {
        return switch (type) {
            case ITEM -> new HashMap<>(itemConfigs);
            case THING -> new HashMap<>(thingConfigs);
            case CHANNEL -> new HashMap<>(channelConfigs);
            case ACCESSORY -> new HashMap<>(accessoryConfigs);
            case SERVICE -> new HashMap<>(serviceConfigs);
            case CHARACTERISTIC -> new HashMap<>(characteristicConfigs);
            case PROFILE -> new HashMap<>(profileConfigs);
            case BRIDGE -> new HashMap<>(bridgeConfigs);
            case NETWORK -> new HashMap<>(networkConfigs);
            case EVENT -> new HashMap<>(eventConfigs);
        };
    }

    /**
     * Gets all source files for a given type
     */
    public Map<UID, String> getAllSourceFiles(ConfigurationType type) {
        return switch (type) {
            case ITEM -> new HashMap<>(itemSourceFiles);
            case THING -> new HashMap<>(thingSourceFiles);
            case CHANNEL -> new HashMap<>(channelSourceFiles);
            case ACCESSORY -> new HashMap<>(accessorySourceFiles);
            case SERVICE -> new HashMap<>(serviceSourceFiles);
            case CHARACTERISTIC -> new HashMap<>(characteristicSourceFiles);
            case PROFILE -> new HashMap<>(profileSourceFiles);
            case BRIDGE -> new HashMap<>(bridgeSourceFiles);
            case NETWORK -> new HashMap<>(networkSourceFiles);
            case EVENT -> new HashMap<>(eventSourceFiles);
        };
    }

    /**
     * Gets all UIDs of a given type
     */
    public Set<UID> getAllUIDs(ConfigurationType type) {
        return switch (type) {
            case ITEM -> new HashSet<>(itemConfigs.keySet());
            case THING -> new HashSet<>(thingConfigs.keySet());
            case CHANNEL -> new HashSet<>(channelConfigs.keySet());
            case ACCESSORY -> new HashSet<>(accessoryConfigs.keySet());
            case SERVICE -> new HashSet<>(serviceConfigs.keySet());
            case CHARACTERISTIC -> new HashSet<>(characteristicConfigs.keySet());
            case PROFILE -> new HashSet<>(profileConfigs.keySet());
            case BRIDGE -> new HashSet<>(bridgeConfigs.keySet());
            case NETWORK -> new HashSet<>(networkConfigs.keySet());
            case EVENT -> new HashSet<>(eventConfigs.keySet());
        };
    }

    /**
     * Gets the source file for a given UID
     */
    public Optional<String> getSourceFile(UID uid, ConfigurationType type) {
        Map<UID, String> sourceFiles = switch (type) {
            case ITEM -> itemSourceFiles;
            case THING -> thingSourceFiles;
            case CHANNEL -> channelSourceFiles;
            case ACCESSORY -> accessorySourceFiles;
            case SERVICE -> serviceSourceFiles;
            case CHARACTERISTIC -> characteristicSourceFiles;
            case PROFILE -> profileSourceFiles;
            case BRIDGE -> bridgeSourceFiles;
            case NETWORK -> networkSourceFiles;
            case EVENT -> eventSourceFiles;
        };
        return Optional.ofNullable(sourceFiles.get(uid));
    }

    /**
     * Updates the source file for a given UID
     */
    public void updateSourceFile(UID uid, ConfigurationType type, String yamlFile) {
        Map<UID, String> sourceFiles = switch (type) {
            case ITEM -> itemSourceFiles;
            case THING -> thingSourceFiles;
            case CHANNEL -> channelSourceFiles;
            case ACCESSORY -> accessorySourceFiles;
            case SERVICE -> serviceSourceFiles;
            case CHARACTERISTIC -> characteristicSourceFiles;
            case PROFILE -> profileSourceFiles;
            case BRIDGE -> bridgeSourceFiles;
            case NETWORK -> networkSourceFiles;
            case EVENT -> eventSourceFiles;
        };
        sourceFiles.put(uid, yamlFile);
    }

    /**
     * Gets the effective source file for a given UID, considering wildcard matches
     */
    private Optional<String> getEffectiveSourceFile(UID uid, ConfigurationType type) {
        // Try exact match first
        Optional<String> sourceFile = getSourceFile(uid, type);
        if (sourceFile.isPresent()) {
            return sourceFile;
        }


            // For things and channels, try wildcard matches at any segment
            String[] segments = uid.toString().split(":");
            for (int i = segments.length - 1; i >= 0; i--) {
                StringBuilder wildcardBuilder = new StringBuilder();
                for (int j = 0; j < segments.length; j++) {
                    if (j > 0) {
                        wildcardBuilder.append(":");
                    }
                    wildcardBuilder.append(j == i ? "*" : segments[j]);
                }
                String wildcardUid = wildcardBuilder.toString();
                UID wildcardUID = convertToUID(wildcardUid, type);
                sourceFile = getSourceFile(wildcardUID, type);
                if (sourceFile.isPresent()) {
                    return sourceFile;
                }
        }

        // Try global default
        UID wildcardUID = convertToUID("*", type);
        return getSourceFile(wildcardUID, type);
    }
    
    /**
     * Gets configuration for a given UID with cascading support
     */
    public Optional<Map<String, Object>> getConfiguration(UID uid, ConfigurationType type) {
        Map<UID, Map<String, Object>> configs = switch (type) {
            case ITEM -> itemConfigs;
            case THING -> thingConfigs;
            case CHANNEL -> channelConfigs;
            case ACCESSORY -> accessoryConfigs;
            case SERVICE -> serviceConfigs;
            case CHARACTERISTIC -> characteristicConfigs;
            case PROFILE -> profileConfigs;
            case BRIDGE -> bridgeConfigs;
            case NETWORK -> networkConfigs;
            case EVENT -> eventConfigs;
        };

        Map<UID, String> sourceFiles = switch (type) {
            case ITEM -> itemSourceFiles;
            case THING -> thingSourceFiles;
            case CHANNEL -> channelSourceFiles;
            case ACCESSORY -> accessorySourceFiles;
            case SERVICE -> serviceSourceFiles;
            case CHARACTERISTIC -> characteristicSourceFiles;
            case PROFILE -> profileSourceFiles;
            case BRIDGE -> bridgeSourceFiles;
            case NETWORK -> networkSourceFiles;
            case EVENT -> eventSourceFiles;
        };
        
        // Try exact match first
        Map<String, Object> config = configs.get(uid);
        if (config != null) {
            return Optional.of(config);
        }
        

            // For things and channels, try wildcard matches at any segment
            String[] segments = uid.toString().split(":");
            for (int i = segments.length - 1; i >= 0; i--) {
                StringBuilder wildcardBuilder = new StringBuilder();
                for (int j = 0; j < segments.length; j++) {
                    if (j > 0) {
                        wildcardBuilder.append(":");
                    }
                    wildcardBuilder.append(j == i ? "*" : segments[j]);
                }
                String wildcardUid = wildcardBuilder.toString();
                UID wildcardUID = convertToUID(wildcardUid, type);
                config = configs.get(wildcardUID);
                if (config != null) {
                    String sourceFile = sourceFiles.get(wildcardUID);
                    if (sourceFile != null) {
                        sourceFiles.put(uid, sourceFile);
                    }
                    return Optional.of(config);
                }
            
        }
        
        // Try global default
        UID wildcardUID = convertToUID("*", type);
        config = configs.get(wildcardUID);
        if (config != null) {
            String sourceFile = sourceFiles.get(wildcardUID);
            if (sourceFile != null) {
                sourceFiles.put(uid, sourceFile);
            }
            return Optional.of(config);
        }
        
        return Optional.empty();
    }
    
    /**
     * Updates configuration for a given UID
     */
    public void updateConfiguration(UID uid, ConfigurationType type, Map<String, Object> config) {
        Map<UID, Map<String, Object>> configs = switch (type) {
            case ITEM -> itemConfigs;
            case THING -> thingConfigs;
            case CHANNEL -> channelConfigs;
            case ACCESSORY -> accessoryConfigs;
            case SERVICE -> serviceConfigs;
            case CHARACTERISTIC -> characteristicConfigs;
            case PROFILE -> profileConfigs;
            case BRIDGE -> bridgeConfigs;
            case NETWORK -> networkConfigs;
            case EVENT -> eventConfigs;
        };
        configs.put(uid, config);
        storeConfigs(uid, type);
    }
    
    /**
     * Updates configuration for a given UID and stores the source file
     */
    public void updateConfiguration(UID uid, ConfigurationType type, Map<String, Object> config, String yamlFile) {
        updateConfiguration(uid, type, config);
        Map<UID, String> sourceFiles = switch (type) {
            case ITEM -> itemSourceFiles;
            case THING -> thingSourceFiles;
            case CHANNEL -> channelSourceFiles;
            case ACCESSORY -> accessorySourceFiles;
            case SERVICE -> serviceSourceFiles;
            case CHARACTERISTIC -> characteristicSourceFiles;
            case PROFILE -> profileSourceFiles;
            case BRIDGE -> bridgeSourceFiles;
            case NETWORK -> networkSourceFiles;
            case EVENT -> eventSourceFiles;
        };
        sourceFiles.put(uid, yamlFile);
        storeConfigs(uid, type);
    }
    
    /**
     * Removes configuration for a given UID
     */
    public void removeConfiguration(UID uid, ConfigurationType type) {
        Map<UID, Map<String, Object>> configs = switch (type) {
            case ITEM -> itemConfigs;
            case THING -> thingConfigs;
            case CHANNEL -> channelConfigs;
            case ACCESSORY -> accessoryConfigs;
            case SERVICE -> serviceConfigs;
            case CHARACTERISTIC -> characteristicConfigs;
            case PROFILE -> profileConfigs;
            case BRIDGE -> bridgeConfigs;
            case NETWORK -> networkConfigs;
            case EVENT -> eventConfigs;
        };
        configs.remove(uid);

        Map<UID, String> sourceFiles = switch (type) {
            case ITEM -> itemSourceFiles;
            case THING -> thingSourceFiles;
            case CHANNEL -> channelSourceFiles;
            case ACCESSORY -> accessorySourceFiles;
            case SERVICE -> serviceSourceFiles;
            case CHARACTERISTIC -> characteristicSourceFiles;
            case PROFILE -> profileSourceFiles;
            case BRIDGE -> bridgeSourceFiles;
            case NETWORK -> networkSourceFiles;
            case EVENT -> eventSourceFiles;
        };
        sourceFiles.remove(uid);
    }
    
    /**
     * Gets a specific configuration value for a given UID and key.
     */
    public Optional<Object> getConfiguration(UID uid, ConfigurationType type, String key) {
        return getConfiguration(uid, type)
            .map(config -> config.get(key));
    }
    
    /**
     * Updates a specific configuration key for a given UID
     */
    public void updateConfiguration(UID uid, ConfigurationType type, String key, Object value) {
        Map<UID, Map<String, Object>> configs = switch (type) {
            case ITEM -> itemConfigs;
            case THING -> thingConfigs;
            case CHANNEL -> channelConfigs;
            case ACCESSORY -> accessoryConfigs;
            case SERVICE -> serviceConfigs;
            case CHARACTERISTIC -> characteristicConfigs;
            case PROFILE -> profileConfigs;
            case BRIDGE -> bridgeConfigs;
            case NETWORK -> networkConfigs;
            case EVENT -> eventConfigs;
        };

        Map<String, Object> config = configs.computeIfAbsent(uid, k -> new HashMap<>());
        config.put(key, value);
        storeConfigs(uid, type);
    }
    
    /**
     * Enum representing different types of configurations and their YAML section identifiers
     */
    public enum ConfigurationType {
        ITEM("items"),
        THING("things"),
        CHANNEL("channels"),
        ACCESSORY("accessories"),
        SERVICE("services"),
        CHARACTERISTIC("characteristics"),
        PROFILE("profiles"),
        BRIDGE("bridge"),
        NETWORK("network"),
        EVENT("events");

        private final String yamlSection;

        ConfigurationType(String yamlSection) {
            this.yamlSection = yamlSection;
        }

        public String getYamlSection() {
            return yamlSection;
        }

        public static ConfigurationType fromYamlSection(String section) {
            for (ConfigurationType type : values()) {
                if (type.yamlSection.equals(section)) {
                    return type;
                }
            }
            return null;
        }
    }
} 