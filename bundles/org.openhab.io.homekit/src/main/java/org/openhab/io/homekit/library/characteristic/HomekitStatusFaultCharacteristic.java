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
 * HomeKit Status Fault Characteristic.
 * This characteristic indicates if the accessory has a fault.
 * The status can be one of: NO_FAULT (0) or GENERAL_FAULT (1).
 * This is used to report the operational status of the accessory.
 *
 * @author Karel Goderis - Initial Contribution
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000077-0000-1000-8000-0026BB765291", name = "Status Fault", tag = "statusFault", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitStatusFaultCharacteristic extends HomekitEnumCharacteristic {

    public enum StatusFault {
        NO_FAULT(0),
        GENERAL_FAULT(1);

        private final int value;

        StatusFault(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static StatusFault fromValue(int value) {
            for (StatusFault status : values()) {
                if (status.value == value) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Invalid Status Fault value: " + value);
        }
    }

    public HomekitStatusFaultCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 2);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Status Fault");
    }

    public HomekitStatusFaultCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public JsonObject toEventJson(Integer value) {
        return super.toEventJson(value);
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
    public boolean isAllowedValue(@Nullable Integer value) {
        if (value == null) {
            return false;
        }
        try {
            StatusFault.fromValue(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        java.util.Set<Integer> allowed = new java.util.HashSet<>();
        for (StatusFault status : StatusFault.values()) {
            allowed.add(status.getValue());
        }
        return allowed;
    }

    public void setValue(StatusFault value) throws Exception {
        setValue(value.getValue());
    }
}
