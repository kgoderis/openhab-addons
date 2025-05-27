package org.openhab.io.homekit.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.service.WatchService;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.core.accessory.HomekitAccessoryUIDImpl;
import org.openhab.io.homekit.core.characteristic.HomekitCharacteristicUIDImpl;
import org.openhab.io.homekit.core.service.HomekitServiceUIDImpl;
import org.openhab.io.homekit.util.HomekitUID;
import org.openhab.io.homekit.util.ItemUID;
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
 *
 * The class integrates with:
 * - {@link org.openhab.core.config.core.ConfigDescriptionRegistry} for configuration validation
 * - {@link org.openhab.core.service.WatchService} for file system monitoring
 * - {@link org.openhab.core.thing.UID} for component identification
 * - {@link org.yaml.snakeyaml.Yaml} for YAML parsing
 *
 * Configuration Types:
 * - Items: OpenHAB item configurations
 * - Things: OpenHAB thing configurations
 * - Channels: OpenHAB channel configurations
 * - Accessories: HomeKit accessory configurations
 * - Services: HomeKit service configurations
 * - Characteristics: HomeKit characteristic configurations
 * - Profiles: HomeKit profile configurations
 * - Bridge: HomeKit bridge configurations
 * - Network: Network-related configurations
 * - Events: Event-related configurations
 *
 * Key Features:
 * - YAML-based configuration management
 * - File system monitoring for changes
 * - Configuration cascading with wildcards
 * - Multiple configuration sources
 * - Type-safe configuration access
 * - Source file tracking
 * - Configuration validation
 *
 * Usage Patterns:
 * - Loading configurations: Use {@link #processConfigFile(Path)}
 * - Updating configurations: Use {@link #updateConfiguration(UID, ConfigurationType, Map)}
 * - Retrieving configurations: Use {@link #getConfiguration(UID, ConfigurationType)}
 * - Managing source files: Use {@link #getSourceFile(UID, ConfigurationType)}
 * - Handling configuration changes: Use {@link #modified(Map)}
 *
 * File Management:
 * - Monitors configuration directory for changes
 * - Supports multiple YAML files
 * - Tracks configuration sources
 * - Handles file updates and deletions
 * - Maintains configuration consistency
 *
 * Configuration Priority:
 * - OSGi configuration takes highest priority
 * - YAML file configurations are merged
 * - Wildcard configurations provide defaults
 * - Specific configurations override wildcards
 *
 * @author Karel Goderis - Initial contribution
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
            @Reference ConfigDescriptionRegistry configDescriptionRegistry, Map<String, Object> config) {
        this.watchService = watchService;
        this.configDescriptionRegistry = configDescriptionRegistry;
        this.yaml = new Yaml();

        // Register directory for watching
        Path confDir = Paths.get(CONFIG_DIR);
        this.watchService.registerListener(this, confDir);

        // Initial load of all YAML files in the directory
        try (Stream<Path> paths = Files.walk(confDir, 1)) {
            paths.filter(Files::isRegularFile).filter(path -> path.toString().toLowerCase().endsWith(".yaml")
                    || path.toString().toLowerCase().endsWith(".yml")).forEach(this::processConfigFile);
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
     * Processes a configuration file and updates the appropriate configuration stores.
     * This method reads a YAML file, parses its contents, and updates the configuration
     * stores based on the sections found in the file.
     *
     * Key implementation details:
     * - Reads and parses YAML file content
     * - Processes each configuration type section
     * - Updates configuration stores
     * - Handles file reading errors
     *
     * @param file The path to the configuration file to process
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
     * Converts a string UID to the appropriate UID type based on its format and section.
     * This method handles various UID formats including HomeKit-specific UIDs and wildcards.
     *
     * Key implementation details:
     * - Handles wildcard UIDs
     * - Processes HomeKit-specific UIDs
     * - Supports different configuration types
     * - Validates UID format
     *
     * @param uidString The UID string to convert
     * @param type The configuration type to determine the appropriate UID class
     * @return The converted UID object
     * @throws IllegalArgumentException if the UID string is null, empty, or invalid
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
                    return new HomekitAccessoryUIDImpl(uidString);
                case "service":
                    return new HomekitServiceUIDImpl(uidString);
                case "characteristic":
                    return new HomekitCharacteristicUIDImpl(uidString);
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
                return new HomekitAccessoryUIDImpl(uidString);
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
     * Converts a string UID to the appropriate UID type based on its format.
     * This method attempts to determine the UID type from the string format.
     *
     * Key implementation details:
     * - Analyzes UID string segments
     * - Attempts to determine type from format
     * - Falls back to default UID type
     *
     * @param uidString The UID string to convert
     * @return The converted UID object
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
     * Processes configurations from YAML for a specific type.
     * This method extracts and processes configuration entries for a given type.
     *
     * Key implementation details:
     * - Extracts type-specific configurations
     * - Converts UIDs
     * - Updates configuration stores
     * - Handles invalid configurations
     *
     * @param yamlConfig The YAML configuration map
     * @param fileName The name of the configuration file
     * @param type The type of configurations to process
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
     * Writes a YAML configuration to a file.
     * This method serializes the configuration map to YAML format and writes it to disk.
     *
     * Key implementation details:
     * - Serializes configuration to YAML
     * - Creates file if not exists
     * - Handles file writing errors
     *
     * @param yamlFile The name of the YAML file to write
     * @param yamlConfig The configuration map to write
     * @throws IOException if there is an error writing the file
     */
    private void writeYamlFile(String yamlFile, Map<String, Object> yamlConfig) throws IOException {
        Path configPath = Paths.get(CONFIG_DIR, yamlFile);
        yaml.dump(yamlConfig, Files.newBufferedWriter(configPath));
    }

    /**
     * Stores configuration for a given UID in its respective YAML file.
     * This method manages the persistence of configurations to YAML files.
     *
     * Key implementation details:
     * - Determines target configuration store
     * - Finds effective source file
     * - Writes configuration to file
     * - Handles file writing errors
     *
     * @param uid The UID of the configuration to store
     * @param type The type of configuration to store
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
     * Gets all configurations of a given type.
     * This method returns a copy of all configurations for the specified type.
     *
     * @param type The type of configurations to retrieve
     * @return A map of UIDs to their configurations
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
     * Gets all source files for a given type.
     * This method returns a copy of all source files for the specified type.
     *
     * @param type The type of source files to retrieve
     * @return A map of UIDs to their source files
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
     * Gets all UIDs of a given type.
     * This method returns a copy of all UIDs for the specified type.
     *
     * @param type The type of UIDs to retrieve
     * @return A set of UIDs
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
     * Gets the source file for a given UID.
     * This method returns the source file associated with the UID.
     *
     * @param uid The UID to look up
     * @param type The type of the UID
     * @return An optional containing the source file if found
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
     * Updates the source file for a given UID.
     * This method associates a source file with a UID.
     *
     * @param uid The UID to update
     * @param type The type of the UID
     * @param yamlFile The source file to associate
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
     * Gets the effective source file for a given UID, considering wildcard matches.
     * This method handles cascading configurations with wildcards.
     *
     * Key implementation details:
     * - Tries exact match first
     * - Attempts wildcard matches
     * - Falls back to global default
     *
     * @param uid The UID to look up
     * @param type The type of the UID
     * @return An optional containing the effective source file if found
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
     * Gets configuration for a given UID with cascading support.
     * This method handles configuration inheritance and wildcard matching.
     *
     * Key implementation details:
     * - Tries exact match first
     * - Attempts wildcard matches
     * - Falls back to global default
     * - Updates source file tracking
     *
     * @param uid The UID to look up
     * @param type The type of configuration to retrieve
     * @return An optional containing the configuration if found
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
     * Updates configuration for a given UID.
     * This method stores the configuration and persists it to the appropriate file.
     *
     * @param uid The UID to update
     * @param type The type of configuration to update
     * @param config The configuration to store
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
     * Updates configuration for a given UID and stores the source file.
     * This method handles both configuration and source file updates.
     *
     * @param uid The UID to update
     * @param type The type of configuration to update
     * @param config The configuration to store
     * @param yamlFile The source file to associate
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
     * Removes configuration for a given UID.
     * This method removes both the configuration and its source file association.
     *
     * @param uid The UID to remove
     * @param type The type of configuration to remove
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
     * This method retrieves a single configuration value.
     *
     * @param uid The UID to look up
     * @param type The type of configuration to retrieve
     * @param key The configuration key to retrieve
     * @return An optional containing the configuration value if found
     */
    public Optional<Object> getConfiguration(UID uid, ConfigurationType type, String key) {
        return getConfiguration(uid, type).map(config -> config.get(key));
    }

    /**
     * Updates a specific configuration key for a given UID.
     * This method updates a single configuration value and persists the change.
     *
     * @param uid The UID to update
     * @param type The type of configuration to update
     * @param key The configuration key to update
     * @param value The value to store
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
     * Enum representing different types of configurations and their YAML section identifiers.
     * This enum defines the supported configuration types and their YAML section names.
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
