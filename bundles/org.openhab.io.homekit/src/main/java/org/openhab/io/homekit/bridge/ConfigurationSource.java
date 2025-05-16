package org.openhab.io.homekit.bridge;

/**
 * Enum representing different sources of HomeKit configurations.
 * The order of declaration determines the priority (first = highest priority).
 */
public enum ConfigurationSource {
    ITEMS_YAML,     // Configuration from items.yaml
    THINGS_YAML,    // Configuration from things.yaml
    CHANNELS_YAML,  // Configuration from channels.yaml
    DSL_CONFIG,     // Configuration from DSL files
    METADATA        // Configuration from metadata registry
} 