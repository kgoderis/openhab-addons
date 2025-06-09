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
 * HomeKit Target Visibility State Characteristic.
 * <p>
 * This characteristic represents the target visibility state of a device, allowing the user to set whether the device
 * is shown or hidden as defined by the HAP specification.
 * <p>
 * See the HomeKit Accessory Protocol (HAP) specification for details: https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitCharacteristicType(type = "00000134-0000-1000-8000-0026BB765291", name = "Target Visibility State", tag = "targetVisibilityState", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitTargetVisibilityStateCharacteristic extends HomekitEnumCharacteristic {
    /**
     * Enum representing the possible target visibility states.
     */
    public enum TargetVisibilityState {
        SHOWN(0),
        HIDDEN(1);

        private final int code;

        TargetVisibilityState(int code) {
            this.code = code;
        }

        /**
         * Returns the integer code for this visibility state.
         * 
         * @return the code
         */
        public int getCode() {
            return code;
        }

        /**
         * Returns the TargetVisibilityState enum for a given code.
         * 
         * @param code the code
         * @return the TargetVisibilityState
         */
        public static TargetVisibilityState fromCode(int code) {
            for (TargetVisibilityState state : values()) {
                if (state.code == code) {
                    return state;
                }
            }
            return SHOWN;
        }
    }

    public HomekitTargetVisibilityStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, TargetVisibilityState.values().length);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true)
                .withDescription("Target Visibility State");
    }

    public HomekitTargetVisibilityStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is an allowed target visibility state.
     * 
     * @param value the value to check
     * @return true if allowed, false otherwise
     */
    @Override
    public boolean isAllowedValue(@Nullable Integer value) {
        return value != null
                && (value == TargetVisibilityState.SHOWN.getCode() || value == TargetVisibilityState.HIDDEN.getCode());
    }

    /**
     * Returns the set of allowed target visibility state values.
     * 
     * @return the set of allowed values
     */
    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(TargetVisibilityState.SHOWN.getCode(), TargetVisibilityState.HIDDEN.getCode());
    }
}
