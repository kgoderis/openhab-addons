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
import org.openhab.io.homekit.protocol.pairing.HomekitPairing;

/**
 * Interface for providing HomeKit pairing information to the system.
 * <p>
 * This interface defines the contract for components that provide HomeKit pairing
 * information to the system. It extends the OpenHAB Provider interface to enable
 * dynamic discovery and management of HomeKit pairing configurations.
 * </p>
 * <p>
 * The interface provides:
 * <ul>
 * <li>Dynamic pairing discovery</li>
 * <li>Pairing lifecycle management</li>
 * <li>Pairing state tracking</li>
 * <li>Pairing configuration management</li>
 * </ul>
 * </p>
 * <p>
 * Key implementation details:
 * <ul>
 * <li>Provider-based discovery system</li>
 * <li>Thread-safe pairing management</li>
 * <li>Dynamic pairing registration</li>
 * <li>State synchronization</li>
 * </ul>
 * </p>
 * <p>
 * The interface integrates with:
 * <ul>
 * <li>{@link org.openhab.core.common.registry.Provider} for provider functionality</li>
 * <li>{@link org.openhab.io.homekit.protocol.pairing.HomekitPairing} for pairing management</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0.0
 */
@NonNullByDefault
public interface HomekitPairingProvider extends Provider<HomekitPairing> {

}
