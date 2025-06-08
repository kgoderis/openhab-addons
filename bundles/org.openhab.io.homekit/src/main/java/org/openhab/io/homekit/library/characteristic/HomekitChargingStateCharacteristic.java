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
 * HomeKit Charging State Characteristic.
 * This characteristic represents the charging state of a battery.
 *
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 * @author Karel Goderis - Initial contribution
 */
@HomekitCharacteristicType(type = "0000008F-0000-1000-8000-0026BB765291", name = "Charging State", tag = "chargingState", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitChargingStateCharacteristic extends HomekitEnumCharacteristic {
    /**
     * Enum representing the possible charging states of a battery.
     * NOT_CHARGING (0): The battery is not charging
     * CHARGING (1): The battery is charging
     * NOT_CHARGEABLE (2): The battery is not chargeable
     */
    public enum ChargingState {
        NOT_CHARGING(0),
        CHARGING(1),
        NOT_CHARGEABLE(2);

        private final int code;

        ChargingState(int code) {
            this.code = code;
        }

        /**
         * Gets the integer code for this charging state.
         *
         * @return the integer code
         */
        public int getCode() {
            return code;
        }

        /**
         * Converts an integer code to the corresponding ChargingState.
         * If no matching state is found, returns NOT_CHARGING as default.
         *
         * @param code the integer code to convert
         * @return the corresponding ChargingState, or NOT_CHARGING if not found
         */
        public static ChargingState fromCode(int code) {
            for (ChargingState s : values()) {
                if (s.code == code)
                    return s;
            }
            return NOT_CHARGING;
        }
    }

    /**
     * Creates a new Charging State characteristic.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitChargingStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, ChargingState.values().length);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("Charging State");
    }

    /**
     * Creates a new Charging State characteristic from a JSON value.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitChargingStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is a valid charging state.
     *
     * @param value the integer value to check
     * @return true if the value corresponds to a valid charging state, false otherwise
     */
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == ChargingState.NOT_CHARGING.getCode()
                || value == ChargingState.CHARGING.getCode() || value == ChargingState.NOT_CHARGEABLE.getCode());
    }

    /**
     * Gets the set of all valid charging state values.
     *
     * @return a set containing all valid charging state codes
     */
    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(ChargingState.NOT_CHARGING.getCode(), ChargingState.CHARGING.getCode(),
                ChargingState.NOT_CHARGEABLE.getCode());
    }

    /**
     * Sets the charging state value for this characteristic.
     *
     * @param state the ChargingState to set
     * @throws Exception if the value cannot be set
     */
    public void setValue(ChargingState state) throws Exception {
        setValueInternal(state.getCode());
    }
}
