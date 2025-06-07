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
 * HomeKit Target Door State Characteristic.
 * This characteristic represents the target state for a door.
 * The state can be one of: OPEN (0) or CLOSED (1).
 * This is used to set the desired state of a door.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000032-0000-1000-8000-0026BB765291", name = "Target Door State", tag = "targetDoorState", acceptedItemTypes = {
        "Number", "String" })
public class HomekitTargetDoorStateCharacteristic extends HomekitEnumCharacteristic {
    public enum TargetDoorState {
        OPEN(0),
        CLOSED(1);

        private final int value;

        TargetDoorState(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static TargetDoorState fromValue(int value) {
            for (TargetDoorState state : values()) {
                if (state.value == value) {
                    return state;
                }
            }
            throw new IllegalArgumentException("Invalid Target Door State value: " + value);
        }
    }

    public HomekitTargetDoorStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 2);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true)
                .withDescription("Target Door State");
    }

    public HomekitTargetDoorStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        if (value == null)
            return false;
        try {
            TargetDoorState.fromValue(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        java.util.Set<Integer> allowed = new java.util.HashSet<>();
        for (TargetDoorState state : TargetDoorState.values()) {
            allowed.add(state.getValue());
        }
        return allowed;
    }

    public void setValue(TargetDoorState value) {
        try {
            setValue(value.getValue());
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to set Target Door State value", e);
        }
    }
}
