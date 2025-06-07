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
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Access Control Level Characteristic.
 * This characteristic represents the access control level of a device.
 * The levels range from 0 to 2, where 0 is the lowest level of access and 2 is the highest.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "000000E5-0000-1000-8000-0026BB765291", name = "Access Control Level", tag = "accessControlLevel", acceptedItemTypes = {
        "Number", "String" })
public class HomekitAccessControlLevelCharacteristic extends HomekitEnumCharacteristic {

    public enum AccessControlLevel {
        LEVEL_0(0),
        LEVEL_1(1),
        LEVEL_2(2);

        private final int value;

        AccessControlLevel(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static AccessControlLevel fromValue(int value) {
            for (AccessControlLevel l : values()) {
                if (l.value == value)
                    return l;
            }
            throw new IllegalArgumentException("Invalid AccessControlLevel value: " + value);
        }
    }

    public HomekitAccessControlLevelCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 3);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true)
                .withDescription("Access Control Level");
    }

    public HomekitAccessControlLevelCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        if (value == null)
            return false;
        try {
            AccessControlLevel.fromValue(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        java.util.Set<Integer> allowed = new java.util.HashSet<>();
        for (AccessControlLevel l : AccessControlLevel.values()) {
            allowed.add(l.getValue());
        }
        return allowed;
    }
}
