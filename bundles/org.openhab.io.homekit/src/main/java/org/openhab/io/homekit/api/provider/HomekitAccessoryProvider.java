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

package org.openhab.io.homekit.api.provider;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Provider;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;

/**
 * Interface for providing HomeKit accessories to the system.
 * <p>
 * This interface defines the contract for components that provide HomeKit accessories
 * to the system. It extends the OpenHAB Provider interface to enable dynamic discovery
 * and management of HomeKit accessories.
 * </p>
 * <p>
 * The interface provides:
 * <ul>
 * <li>Dynamic accessory discovery</li>
 * <li>Accessory lifecycle management</li>
 * <li>Accessory state tracking</li>
 * <li>Accessory configuration management</li>
 * </ul>
 * </p>
 * <p>
 * Key implementation details:
 * <ul>
 * <li>Provider-based discovery system</li>
 * <li>Thread-safe accessory management</li>
 * <li>Dynamic accessory registration</li>
 * <li>State synchronization</li>
 * </ul>
 * </p>
 * <p>
 * The interface integrates with:
 * <ul>
 * <li>{@link org.openhab.core.common.registry.Provider} for provider functionality</li>
 * <li>{@link org.openhab.io.homekit.api.accessory.HomekitAccessory} for accessory management</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0.0
 */
@NonNullByDefault
public interface HomekitAccessoryProvider extends Provider<HomekitAccessory> {

}
