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
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;

/**
 * Interface for providing HomeKit accessory servers to the system.
 * <p>
 * This interface defines the contract for components that provide HomeKit accessory servers
 * to the system. It extends the OpenHAB Provider interface to enable dynamic discovery
 * and management of HomeKit accessory servers.
 * </p>
 * <p>
 * The interface provides:
 * <ul>
 * <li>Dynamic server discovery</li>
 * <li>Server lifecycle management</li>
 * <li>Server state tracking</li>
 * <li>Server configuration management</li>
 * </ul>
 * </p>
 * <p>
 * Key implementation details:
 * <ul>
 * <li>Provider-based discovery system</li>
 * <li>Thread-safe server management</li>
 * <li>Dynamic server registration</li>
 * <li>State synchronization</li>
 * </ul>
 * </p>
 * <p>
 * The interface integrates with:
 * <ul>
 * <li>{@link org.openhab.core.common.registry.Provider} for provider functionality</li>
 * <li>{@link org.openhab.io.homekit.api.server.HomekitAccessoryServer} for server management</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0.0
 */
@NonNullByDefault
public interface HomekitAccessoryServerProvider extends Provider<HomekitAccessoryServer> {

}
