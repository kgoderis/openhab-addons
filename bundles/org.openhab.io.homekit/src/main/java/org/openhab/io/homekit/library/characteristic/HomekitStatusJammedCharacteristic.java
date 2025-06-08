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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Status Jammed Characteristic.
 * This characteristic indicates if the accessory is jammed (e.g., a lock or door).
 * The status can be one of: NOT_JAMMED (0) or JAMMED (1).
 * This is used to report when a mechanical device is stuck or unable to operate properly.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000078-0000-1000-8000-0026BB765291", name = "Status Jammed", tag = "statusJammed", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitStatusJammedCharacteristic extends HomekitEnumCharacteristic {
    public enum StatusJammed {
        NOT_JAMMED(0),
        JAMMED(1);

        private final int value;

        StatusJammed(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static StatusJammed fromValue(int value) {
            for (StatusJammed status : values()) {
                if (status.value == value) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Invalid Status Jammed value: " + value);
        }
    }

    public HomekitStatusJammedCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 2);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Status Jammed");
    }

    public HomekitStatusJammedCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        if (value == null)
            return false;
        try {
            StatusJammed.fromValue(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        java.util.Set<Integer> allowed = new java.util.HashSet<>();
        for (StatusJammed status : StatusJammed.values()) {
            allowed.add(status.getValue());
        }
        return allowed;
    }
}
