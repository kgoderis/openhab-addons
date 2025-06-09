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
 * HomeKit Target Air Purifier State Characteristic.
 * This characteristic represents the target state for an air purifier.
 * The state can be one of: MANUAL or AUTO.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "000000A8-0000-1000-8000-0026BB765291", name = "Target Air Purifier State", tag = "targetAirPurifierState", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitTargetAirPurifierStateCharacteristic extends HomekitEnumCharacteristic {
    public enum TargetAirPurifierState {
        MANUAL(0),
        AUTO(1);

        private final int value;

        TargetAirPurifierState(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static TargetAirPurifierState fromValue(int value) {
            for (TargetAirPurifierState state : values()) {
                if (state.value == value) {
                    return state;
                }
            }
            throw new IllegalArgumentException("Invalid Target Air Purifier State value: " + value);
        }
    }

    public HomekitTargetAirPurifierStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, TargetAirPurifierState.values().length);
        withInstanceId(instanceId).withPairedWrite(true).withPairedRead(true).withEvents(true)
                .withDescription("Target Air Purifier State");
    }

    public HomekitTargetAirPurifierStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(@Nullable Integer value) {
        return value != null && value >= TargetAirPurifierState.MANUAL.getValue()
                && value <= TargetAirPurifierState.AUTO.getValue();
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(TargetAirPurifierState.MANUAL.getValue(), TargetAirPurifierState.AUTO.getValue());
    }

    public void setValue(TargetAirPurifierState value) {
        try {
            setValue(value.getValue());
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to set Target Air Purifier State value", e);
        }
    }
}
