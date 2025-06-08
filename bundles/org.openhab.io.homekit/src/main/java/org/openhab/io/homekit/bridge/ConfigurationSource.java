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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Enum representing different sources of HomeKit configurations.
 * The order of declaration determines the priority (first = highest priority).
 *
 * Configuration Sources:
 * - ITEMS_YAML: Configuration from items.yaml file
 * - Contains item-specific HomeKit configurations
 * - Highest priority for item-related settings
 * - Supports item-level customization
 *
 * - THINGS_YAML: Configuration from things.yaml file
 * - Contains thing-specific HomeKit configurations
 * - Used for thing-level HomeKit integration
 * - Supports thing-level customization
 *
 * - CHANNELS_YAML: Configuration from channels.yaml file
 * - Contains channel-specific HomeKit configurations
 * - Used for channel-level HomeKit integration
 * - Supports channel-level customization
 *
 * - DSL_CONFIG: Configuration from DSL files
 * - Contains dynamic configuration rules
 * - Supports runtime configuration changes
 * - Used for complex configuration scenarios
 *
 * - METADATA: Configuration from metadata registry
 * - Contains metadata-based configurations
 * - Lowest priority for configuration
 * - Used for basic configuration needs
 *
 * Usage:
 * - Configuration sources are checked in order of priority
 * - Higher priority sources override lower priority ones
 * - Multiple sources can be used together
 * - Each source has specific use cases and limitations
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0.0
 */
@NonNullByDefault
public enum ConfigurationSource {
    ITEMS_YAML, // Configuration from items.yaml
    THINGS_YAML, // Configuration from things.yaml
    CHANNELS_YAML, // Configuration from channels.yaml
    DSL_CONFIG, // Configuration from DSL files
    METADATA // Configuration from metadata registry
}
