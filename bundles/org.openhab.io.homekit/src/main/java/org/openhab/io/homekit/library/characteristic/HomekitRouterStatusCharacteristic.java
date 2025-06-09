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

package org.openhab.io.homekit.library.characteristic;

import java.util.Set;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Router Status Characteristic.
 * This characteristic represents the router status (READY or NOT_READY).
 *
 * @see <a href=\"https://developer.apple.com/documentation/HomeKit\">HAP Specification</a>
 * @author Karel Goderis - Initial contribution
 */
@HomekitCharacteristicType(type = "0000020E-0000-1000-8000-0026BB765291", name = "Router Status", tag = "routerStatus", acceptedItemTypes = {
        "String" })
@NonNullByDefault
public class HomekitRouterStatusCharacteristic extends HomekitIntegerCharacteristic {
    public static final int READY = 0;
    public static final int NOT_READY = 1;

    public HomekitRouterStatusCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 0, 1, "");
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Router Status");
    }

    public HomekitRouterStatusCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is a valid router status.
     *
     * @param value the value to check
     * @return true if the value is READY or NOT_READY, false otherwise
     */
    @Override
    public boolean isAllowedValue(@Nullable Integer value) {
        return value != null && (value == READY || value == NOT_READY);
    }

    /**
     * Returns the set of allowed router status values.
     *
     * @return a set containing READY and NOT_READY
     */
    @Override
    public Set<Integer> getAllowedValues() {
        return Set.of(READY, NOT_READY);
    }
}
