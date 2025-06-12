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
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Target Fan State Characteristic.
 * This characteristic represents the target state for a fan.
 * The state can be one of: MANUAL (0) or AUTO (1).
 * This is used to control whether the fan should operate in manual mode or automatically adjust its speed.
 *
 * @author Karel Goderis - Initial Contribution
 * @see <a href="https://developer.apple.com/documentation/homekit/hap-characteristic-types/target-fan-state">HAP
 *      Specification</a>
 */
@HomekitCharacteristicType(type = "000000BF-0000-1000-8000-0026BB765291", name = "Target Fan State", tag = "targetFanState", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitTargetFanStateCharacteristic extends HomekitEnumCharacteristic {
    public enum TargetFanState {
        MANUAL(0),
        AUTO(1);

        private final int value;

        TargetFanState(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static TargetFanState fromValue(int value) {
            for (TargetFanState state : values()) {
                if (state.value == value) {
                    return state;
                }
            }
            throw new IllegalArgumentException("Invalid Target Fan State value: " + value);
        }
    }

    public HomekitTargetFanStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 2);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true)
                .withDescription("Target Fan State");
    }

    public HomekitTargetFanStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(@Nullable Integer value) {
        try {
            if (value == null) {
                return false;
            }
            TargetFanState.fromValue(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        java.util.Set<Integer> values = new java.util.HashSet<>();
        for (TargetFanState state : TargetFanState.values()) {
            values.add(state.getValue());
        }
        return values;
    }

    public void setValue(TargetFanState value) {
        try {
            setValue(value.getValue());
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to set Target Fan State value", e);
        }
    }
}
