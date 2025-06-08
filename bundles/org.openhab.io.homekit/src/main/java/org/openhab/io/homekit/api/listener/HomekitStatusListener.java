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
import org.openhab.core.thing.Bridge;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.service.HomekitService;

/**
 * Interface for listening to HomeKit status changes.
 *
 * This interface defines the contract for components that need to be notified
 * of changes
 * in the HomeKit system's status, including the addition, removal, and state
 * changes of
 * accessories, services, and characteristics.
 *
 * The interface provides:
 * - Accessory lifecycle monitoring
 * - Service lifecycle monitoring
 * - Characteristic lifecycle monitoring
 * - State change notifications
 *
 * Key implementation details:
 * - Bridge-based event routing
 * - Thread-safe event handling
 * - Component state tracking
 * - Event propagation
 *
 * The interface integrates with:
 * - {@link org.openhab.core.thing.Bridge} for bridge-based event routing
 * - {@link org.openhab.io.homekit.api.accessory.HomekitAccessory} for accessory
 * events
 * - {@link org.openhab.io.homekit.api.service.HomekitService} for service
 * events
 * - {@link org.openhab.io.homekit.api.characteristic.HomekitCharacteristic} for
 * characteristic events
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0.0
 */
@NonNullByDefault
public interface HomekitStatusListener {
    /**
     * Called when a new accessory is added to the bridge.
     * This method is invoked when a new HomeKit accessory is registered with the
     * system.
     *
     * @param bridge The bridge that owns the accessory
     * @param accessory The newly added accessory
     * @since 1.0.0
     */
    void onAccessoryAdded(Bridge bridge, HomekitAccessory accessory);

    /**
     * Called when an accessory is removed from the bridge.
     * This method is invoked when a HomeKit accessory is unregistered from the
     * system.
     *
     * @param bridge The bridge that owned the accessory
     * @param accessory The removed accessory
     * @since 1.0.0
     */
    void onAccessoryRemoved(Bridge bridge, HomekitAccessory accessory);

    /**
     * Called when a new service is added to an accessory.
     * This method is invoked when a new HomeKit service is added to an accessory.
     *
     * @param bridge The bridge that owns the accessory
     * @param service The newly added service
     * @since 1.0.0
     */
    void onServiceAdded(Bridge bridge, HomekitService service);

    /**
     * Called when a service is removed from an accessory.
     * This method is invoked when a HomeKit service is removed from an accessory.
     *
     * @param bridge The bridge that owns the accessory
     * @param service The removed service
     * @since 1.0.0
     */
    void onServiceRemoved(Bridge bridge, HomekitService service);

    /**
     * Called when a new characteristic is added to a service.
     * This method is invoked when a new HomeKit characteristic is added to a
     * service.
     *
     * @param bridge The bridge that owns the accessory
     * @param characteristic The newly added characteristic
     * @since 1.0.0
     */
    void onCharacteristicAdded(Bridge bridge, HomekitCharacteristic<?> characteristic);

    /**
     * Called when a characteristic is removed from a service.
     * This method is invoked when a HomeKit characteristic is removed from a
     * service.
     *
     * @param bridge The bridge that owns the accessory
     * @param characteristic The removed characteristic
     * @since 1.0.0
     */
    void onCharacteristicRemoved(Bridge bridge, HomekitCharacteristic<?> characteristic);

    /**
     * Called when a characteristic's state changes.
     * This method is invoked when the state of a HomeKit characteristic changes.
     *
     * @param bridge The bridge that owns the accessory
     * @param characteristic The characteristic whose state changed
     * @since 1.0.0
     */
    void onCharacteristicStateChanged(Bridge bridge, HomekitCharacteristic<?> characteristic);
}
