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

/**
 *
 */
package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Status Low Battery Characteristic.
 * This characteristic indicates if the accessory has a low battery.
 * The status can be one of: BATTERY_LEVEL_NORMAL (0) or BATTERY_LEVEL_LOW (1).
 * This is used to report the battery status of the accessory.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000079-0000-1000-8000-0026BB765291", name = "Status Low Battery", tag = "statusLowBattery", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitStatusLowBatteryCharacteristic extends HomekitEnumCharacteristic {

    public enum StatusLowBattery {
        BATTERY_LEVEL_NORMAL(0),
        BATTERY_LEVEL_LOW(1);

        private final int value;

        StatusLowBattery(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static StatusLowBattery fromValue(int value) {
            for (StatusLowBattery status : values()) {
                if (status.value == value) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Invalid Status Low Battery value: " + value);
        }
    }

    public HomekitStatusLowBatteryCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 2);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Status Low Battery");
    }

    public HomekitStatusLowBatteryCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public JsonObject toEventJson() {
        return super.toEventJson();
    }

    @Override
    public JsonValue toValueJson(@Nullable Integer value) {
        return super.toValueJson(value);
    }

    @Override
    public JsonObject toJson() {
        return super.toJson();
    }

    @Override
    public JsonObject toJson(boolean includeMeta, boolean includePermissions, boolean includeType,
            boolean includeEvent) {
        return super.toJson(includeMeta, includePermissions, includeType, includeEvent);
    }

    @Override
    public JsonObject toReducedJson() {
        return super.toReducedJson();
    }

    @Override
    public State toState(Integer value) {
        return super.toState(value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        if (value == null)
            return false;
        try {
            StatusLowBattery.fromValue(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        java.util.Set<Integer> allowed = new java.util.HashSet<>();
        for (StatusLowBattery status : StatusLowBattery.values()) {
            allowed.add(status.getValue());
        }
        return allowed;
    }

    public void setValue(StatusLowBattery value) throws Exception {
        setValue(value.getValue());
    }
}
