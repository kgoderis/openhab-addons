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
 * HomeKit Programmable Switch Event Characteristic.
 * This characteristic represents the event for a programmable switch.
 *
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 * @author Karel Goderis - Initial contribution
 */
@HomekitCharacteristicType(type = "00000073-0000-1000-8000-0026BB765291", name = "Programmable Switch Event", tag = "programmableSwitchEvent", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitProgrammableSwitchEventCharacteristic extends HomekitEnumCharacteristic {
    /**
     * Enum representing the possible events for a programmable switch.
     * SINGLE_PRESS (0): The switch was pressed once
     * DOUBLE_PRESS (1): The switch was pressed twice in quick succession
     * LONG_PRESS (2): The switch was pressed and held
     */
    public enum ProgrammableSwitchEvent {
        /** Single press event */
        SINGLE_PRESS(0),
        /** Double press event */
        DOUBLE_PRESS(1),
        /** Long press event */
        LONG_PRESS(2);

        private final int code;

        ProgrammableSwitchEvent(int code) {
            this.code = code;
        }

        /**
         * Gets the integer code for this switch event.
         *
         * @return the integer code
         */
        public int getCode() {
            return code;
        }

        /**
         * Converts an integer code to the corresponding ProgrammableSwitchEvent.
         * If no matching event is found, returns SINGLE_PRESS as default.
         *
         * @param code the integer code to convert
         * @return the corresponding ProgrammableSwitchEvent, or SINGLE_PRESS if not found
         */
        public static ProgrammableSwitchEvent fromCode(int code) {
            for (ProgrammableSwitchEvent s : values()) {
                if (s.code == code)
                    return s;
            }
            return SINGLE_PRESS;
        }
    }

    /**
     * Creates a new Programmable Switch Event characteristic.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitProgrammableSwitchEventCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, ProgrammableSwitchEvent.values().length);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("Programmable Switch Event");
    }

    /**
     * Creates a new Programmable Switch Event characteristic from a JSON value.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitProgrammableSwitchEventCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is a valid switch event.
     *
     * @param value the integer value to check
     * @return true if the value corresponds to a valid switch event, false otherwise
     */
    @Override
    public boolean isAllowedValue(@Nullable Integer value) {
        return value != null && (value == ProgrammableSwitchEvent.SINGLE_PRESS.getCode()
                || value == ProgrammableSwitchEvent.DOUBLE_PRESS.getCode()
                || value == ProgrammableSwitchEvent.LONG_PRESS.getCode());
    }

    /**
     * Gets the set of all valid switch event values.
     *
     * @return a set containing all valid switch event codes
     */
    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(ProgrammableSwitchEvent.SINGLE_PRESS.getCode(),
                ProgrammableSwitchEvent.DOUBLE_PRESS.getCode(), ProgrammableSwitchEvent.LONG_PRESS.getCode());
    }

    /**
     * Sets the switch event value for this characteristic.
     *
     * @param event the ProgrammableSwitchEvent to set
     * @throws Exception if the value cannot be set
     */
    public void setValue(ProgrammableSwitchEvent event) throws Exception {
        setValueInternal(event.getCode());
    }
}
