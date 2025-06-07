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

import javax.json.JsonValue;

import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitLongCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Active Identifier Characteristic.
 * This characteristic represents the identifier of the currently active input or source.
 * The value is a 32-bit unsigned integer (0 to 0xFFFFFFFF) that identifies the active input.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "000000E7-0000-1000-8000-0026BB765291", name = "Active Identifier", tag = "activeIdentifier", acceptedItemTypes = {
        "Number" })
public class HomekitActiveIdentifierCharacteristic extends HomekitLongCharacteristic {

    public HomekitActiveIdentifierCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 0L, 0xFFFFFFFFL, 1L);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true)
                .withDescription("Active Identifier");
    }

    public HomekitActiveIdentifierCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Long value) {
        return value != null && value >= 0L && value <= 0xFFFFFFFFL;
    }

    @Override
    public java.util.Set<Long> getAllowedValues() {
        return java.util.Collections.emptySet(); // No specific allowed values, just a range
    }
}
