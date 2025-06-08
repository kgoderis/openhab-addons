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
 * HomeKit Target Heating Cooling State Characteristic.
 * This characteristic represents the target state for a heating/cooling system.
 * The state can be one of: OFF (0), HEAT (1), COOL (2), or AUTO (3).
 * This is used to set the desired operating mode of a heating/cooling system.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000033-0000-1000-8000-0026BB765291", name = "Target Heating Cooling State", tag = "targetHeatingCoolingState", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitTargetHeatingCoolingStateCharacteristic extends HomekitEnumCharacteristic {

    public enum TargetHeatingCoolingState {
        OFF(0),
        HEAT(1),
        COOL(2),
        AUTO(3);

        private final int value;

        TargetHeatingCoolingState(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static TargetHeatingCoolingState fromValue(int value) {
            for (TargetHeatingCoolingState state : values()) {
                if (state.value == value) {
                    return state;
                }
            }
            throw new IllegalArgumentException("Invalid Target Heating Cooling State value: " + value);
        }
    }

    public HomekitTargetHeatingCoolingStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 4);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true)
                .withDescription("Target Heating Cooling State");
    }

    public HomekitTargetHeatingCoolingStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        if (value == null)
            return false;
        try {
            TargetHeatingCoolingState.fromValue(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        java.util.Set<Integer> allowed = new java.util.HashSet<>();
        for (TargetHeatingCoolingState state : TargetHeatingCoolingState.values()) {
            allowed.add(state.getValue());
        }
        return allowed;
    }

    public void setValue(TargetHeatingCoolingState value) throws Exception {
        setValue(value.getValue());
    }
}
