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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Registry;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.uid.HomekitAccessoryUID;

/**
 * Registry for managing HomeKit accessories in the system.
 * <p>
 * This interface defines the contract for components that need to track and manage HomeKit
 * accessories from different providers. It extends the OpenHAB Registry interface to provide
 * a centralized registry for all HomeKit accessories in the system.
 * </p>
 * <p>
 * The registry provides:
 * <ul>
 * <li>Centralized accessory management</li>
 * <li>Provider-based accessory discovery</li>
 * <li>Change notification support</li>
 * <li>Accessory lifecycle tracking</li>
 * </ul>
 * </p>
 * <p>
 * Key implementation details:
 * <ul>
 * <li>Thread-safe registry operations</li>
 * <li>Provider-based discovery system</li>
 * <li>Change listener support</li>
 * <li>UID-based accessory identification</li>
 * </ul>
 * </p>
 * <p>
 * The interface integrates with:
 * <ul>
 * <li>{@link org.openhab.core.common.registry.Registry} for registry functionality</li>
 * <li>{@link org.openhab.io.homekit.api.accessory.HomekitAccessory} for accessory management</li>
 * <li>{@link org.openhab.io.homekit.api.provider.HomekitAccessoryProvider} for accessory discovery</li>
 * <li>{@link org.openhab.io.homekit.api.listener.HomekitAccessoryChangeListener} for change notifications</li>
 * <li>{@link org.openhab.io.homekit.api.uid.HomekitAccessoryUID} for accessory identification</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0.0
 */
@NonNullByDefault
public interface HomekitAccessoryRegistry extends Registry<HomekitAccessory, HomekitAccessoryUID> {

    // /**
    // * Returns a list of HomekitAccessories for a given serverId or an empty list if no HomekitAccessory was found
    // *
    // * @param serverId the id uniquely identifying the HomekitServer
    // * @return list of HomekitAccessories for a given serverId or an empty list if no HomekitAccessory was found
    // */
    // Collection<HomekitAccessory> get(String serverId);
}
