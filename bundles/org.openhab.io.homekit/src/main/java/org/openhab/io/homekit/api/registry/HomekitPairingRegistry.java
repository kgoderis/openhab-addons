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

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Registry;
import org.openhab.io.homekit.api.uid.HomekitPairingUID;
import org.openhab.io.homekit.protocol.pairing.HomekitPairing;

/**
 * Registry for managing HomeKit pairing information in the system.
 * <p>
 * This interface defines the contract for components that need to track and manage HomeKit
 * pairing configurations from different providers. It extends the OpenHAB Registry interface to provide
 * a centralized registry for all HomeKit pairing information in the system.
 * </p>
 * <p>
 * The registry provides:
 * <ul>
 * <li>Centralized pairing management</li>
 * <li>Provider-based pairing discovery</li>
 * <li>Change notification support</li>
 * <li>Pairing lifecycle tracking</li>
 * <li>Pairing ID lookup</li>
 * </ul>
 * </p>
 * <p>
 * Key implementation details:
 * <ul>
 * <li>Thread-safe registry operations</li>
 * <li>Provider-based discovery system</li>
 * <li>Change listener support</li>
 * <li>UID-based pairing identification</li>
 * <li>Pairing ID validation</li>
 * </ul>
 * </p>
 * <p>
 * The interface integrates with:
 * <ul>
 * <li>{@link org.openhab.core.common.registry.Registry} for registry functionality</li>
 * <li>{@link org.openhab.io.homekit.protocol.pairing.HomekitPairing} for pairing management</li>
 * <li>{@link org.openhab.io.homekit.api.provider.HomekitPairingProvider} for pairing discovery</li>
 * <li>{@link org.openhab.io.homekit.api.listener.HomekitPairingChangeListener} for change notifications</li>
 * <li>{@link org.openhab.io.homekit.api.uid.HomekitPairingUID} for pairing identification</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0.0
 */
@NonNullByDefault
public interface HomekitPairingRegistry extends Registry<HomekitPairing, HomekitPairingUID> {

    /**
     * Gets all pairing configurations for a specific accessory pairing ID.
     * This method is used to find all pairing configurations associated with a particular
     * accessory, which is useful for managing multiple pairings for the same device.
     *
     * @param pairingId the pairing ID of the accessory
     * @return a collection of pairing configurations, or an empty collection if none are found
     * @since 1.0.0
     */
    Collection<HomekitPairing> get(byte[] pairingId);
}
