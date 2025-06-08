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

package org.openhab.io.homekit.api.factory;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Enum representing the different types of factories in the HomeKit integration.
 *
 * This enum defines the types of components that can be created by factories in the HomeKit
 * integration. Each type corresponds to a specific factory interface that is responsible for
 * creating instances of that component type.
 *
 * The factory types are:
 * - ACCESSORY: Creates HomeKit accessories
 * - SERVICE: Creates HomeKit services
 * - CHARACTERISTIC: Creates HomeKit characteristics
 *
 * Key implementation details:
 * - Used for factory registration and lookup
 * - Supports component type identification
 * - Enables factory type validation
 * - Facilitates factory management
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0.0
 */
@NonNullByDefault
public enum HomekitFactoryType {
    /**
     * Factory type for creating HomeKit accessories.
     * Used by {@link org.openhab.io.homekit.api.factory.HomekitAccessoryFactory}.
     */
    ACCESSORY,

    /**
     * Factory type for creating HomeKit services.
     * Used by {@link org.openhab.io.homekit.api.factory.HomekitServiceFactory}.
     */
    SERVICE,

    /**
     * Factory type for creating HomeKit characteristics.
     * Used by {@link org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory}.
     */
    CHARACTERISTIC
}
