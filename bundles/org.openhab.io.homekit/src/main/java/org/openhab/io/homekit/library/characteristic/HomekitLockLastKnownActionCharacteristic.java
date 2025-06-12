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

import java.util.HashSet;
import java.util.Set;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Lock Last Known Action Characteristic.
 * <p>
 * This characteristic represents the last known action performed on a lock, such as secured or unsecured by various
 * means. The value is an integer corresponding to a specific action as defined by the HAP specification.
 * <p>
 * See the HomeKit Accessory Protocol (HAP) specification for details: https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis - Initial Contribution
 */
@HomekitCharacteristicType(type = "0000001C-0000-1000-8000-0026BB765291", name = "Lock Last Known Action", tag = "lockLastKnownAction", acceptedItemTypes = {
        "Number" })
@NonNullByDefault
public class HomekitLockLastKnownActionCharacteristic extends HomekitIntegerCharacteristic {

    /**
     * Enum representing the possible last known actions for a lock.
     */
    public enum LockLastKnownAction {
        SECURED_PHYSICALLY_INTERIOR(0),
        UNSECURED_PHYSICALLY_INTERIOR(1),
        SECURED_PHYSICALLY_EXTERIOR(2),
        UNSECURED_PHYSICALLY_EXTERIOR(3),
        SECURED_BY_KEYPAD(4),
        UNSECURED_BY_KEYPAD(5),
        SECURED_REMOTELY(6),
        UNSECURED_REMOTELY(7),
        SECURED_BY_AUTO_SECURE_TIMEOUT(8),
        SECURED_PHYSICALLY(9),
        UNSECURED_PHYSICALLY(10);

        private final int code;

        LockLastKnownAction(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static LockLastKnownAction fromCode(int code) {
            for (LockLastKnownAction a : values()) {
                if (a.code == code)
                    return a;
            }
            throw new IllegalArgumentException("Invalid code: " + code);
        }
    }

    /**
     * Constructs a new Lock Last Known Action characteristic.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param instanceId the instance ID for this characteristic
     */
    public HomekitLockLastKnownActionCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 0, 10, "");
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("Lock Last Known Action");
    }

    /**
     * Constructs a new Lock Last Known Action characteristic from a JSON value.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param value the JSON value to initialize the characteristic with
     */
    public HomekitLockLastKnownActionCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is an allowed lock last known action.
     *
     * @param value the value to check
     * @return true if the value is allowed, false otherwise
     */
    @Override
    public boolean isAllowedValue(@Nullable Integer value) {
        return value != null && value >= 0 && value <= 10;
    }

    /**
     * Returns the set of allowed lock last known action values.
     *
     * @return the set of allowed values
     */
    @Override
    public Set<Integer> getAllowedValues() {
        Set<Integer> set = new HashSet<>();
        for (LockLastKnownAction a : LockLastKnownAction.values()) {
            set.add(a.getCode());
        }
        return set;
    }
}
