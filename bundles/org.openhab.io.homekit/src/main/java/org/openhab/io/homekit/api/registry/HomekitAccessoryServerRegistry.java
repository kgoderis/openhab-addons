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

package org.openhab.io.homekit.api.registry;

import java.util.Optional;

import org.openhab.core.common.registry.Registry;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;
import org.openhab.io.homekit.api.uid.HomekitAccessoryServerUID;
import org.openhab.io.homekit.api.uid.HomekitAccessoryUID;

/**
 * Registry for managing HomeKit accessory servers in the system.
 *
 * This interface defines the contract for components that need to track and
 * manage HomeKit
 * accessory servers from different providers. It extends the OpenHAB Registry
 * interface to provide
 * a centralized registry for all HomeKit accessory servers in the system.
 *
 * The registry provides:
 * - Centralized server management
 * - Provider-based server discovery
 * - Change notification support
 * - Server lifecycle tracking
 * - Bridge server management
 *
 * Key implementation details:
 * - Thread-safe registry operations
 * - Provider-based discovery system
 * - Change listener support
 * - UID-based server identification
 * - Bridge server prioritization
 *
 * The interface integrates with:
 * - {@link org.openhab.core.common.registry.Registry} for registry
 * functionality
 * - {@link org.openhab.io.homekit.api.server.HomekitAccessoryServer} for server
 * management
 * - {@link org.openhab.io.homekit.api.provider.HomekitAccessoryServerProvider}
 * for server discovery
 * -
 * {@link org.openhab.io.homekit.api.listener.HomekitAccessoryServerChangeListener}
 * for change notifications
 * - {@link org.openhab.io.homekit.api.uid.HomekitAccessoryServerUID} for server
 * identification
 * - {@link org.openhab.io.homekit.api.uid.HomekitAccessoryUID} for accessory
 * identification
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0.0
 */
public interface HomekitAccessoryServerRegistry extends Registry<HomekitAccessoryServer, HomekitAccessoryServerUID> {

    /**
     * Gets the first available bridge accessory server.
     * Bridge servers are used to manage multiple accessories through a single
     * connection.
     *
     * @return the first available bridge server, or empty if none are available
     * @since 1.0.0
     */
    public Optional<HomekitAccessoryServer> getAvailableBridgeAccessoryServer();

    /**
     * Gets the accessory server associated with a specific accessory.
     * This method is used to find which server is managing a particular accessory.
     *
     * @param accessoryUID the UID of the accessory to look up
     * @return the server managing the accessory, or empty if not found
     * @since 1.0.0
     */
    public Optional<HomekitAccessoryServer> getAccessoryServer(HomekitAccessoryUID accessoryUID);
}
