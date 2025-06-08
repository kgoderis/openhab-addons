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

package org.openhab.io.homekit.api.listener;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;

/**
 * Interface for listening to changes in the HomeKit accessory registry.
 *
 * This interface defines the contract for components that need to be notified of changes
 * in the HomeKit accessory registry, including the addition and removal of accessories.
 *
 * The interface provides:
 * - Accessory addition notifications
 * - Accessory removal notifications
 * - Registry change tracking
 *
 * Key implementation details:
 * - Registry-based event routing
 * - Thread-safe event handling
 * - Component state tracking
 * - Event propagation
 *
 * The interface integrates with:
 * - {@link org.openhab.core.common.registry.RegistryChangeListener} for registry change events
 * - {@link org.openhab.io.homekit.api.accessory.HomekitAccessory} for accessory events
 * - {@link org.openhab.io.homekit.api.registry.HomekitAccessoryRegistry} for registry management
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0.0
 */
@NonNullByDefault
public interface HomekitAccessoryRegistryChangeListener extends RegistryChangeListener<HomekitAccessory> {

}
