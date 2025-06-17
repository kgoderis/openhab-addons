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

import java.util.Set;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Power Mode Selection Characteristic.
 * <p>
 * This characteristic represents the power mode selection for a device, allowing the user to select between different
 * power modes as defined by the HAP specification.
 * <p>
 * See the HomeKit Accessory Protocol (HAP) specification for details: https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis - Initial Contribution
 */
@HomekitCharacteristicType(type = "000000DF-0000-1000-8000-0026BB765291", name = "Power Mode Selection", tag = "powerModeSelection", acceptedItemTypes = {
        "Number" })
@NonNullByDefault
public class HomekitPowerModeSelectionCharacteristic extends HomekitIntegerCharacteristic {
    /**
     * Enum representing the possible power modes.
     */
    public enum PowerMode {
        SHOW(0),
        HIDE(1);

        private final int code;

        PowerMode(int code) {
            this.code = code;
        }

        /**
         * Returns the integer code for this power mode.
         * 
         * @return the code
         */
        public int getCode() {
            return code;
        }

        /**
         * Returns the PowerMode enum for a given code.
         * 
         * @param code the code
         * @return the PowerMode
         */
        public static PowerMode fromCode(int code) {
            for (PowerMode m : values()) {
                if (m.code == code) {
                    return m;
                }
            }
            return SHOW;
        }
    }

    public HomekitPowerModeSelectionCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 0, 1, "");
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true)
                .withDescription("Power Mode Selection");
    }

    public HomekitPowerModeSelectionCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is an allowed power mode.
     * 
     * @param value the value to check
     * @return true if allowed, false otherwise
     */
    @Override
    public boolean isAllowedValue(@Nullable Integer value) {
        return value != null && (value == PowerMode.SHOW.getCode() || value == PowerMode.HIDE.getCode());
    }

    /**
     * Returns the set of allowed power mode values.
     * 
     * @return the set of allowed values
     */
    @Override
    public Set<Integer> getAllowedValues() {
        return Set.of(PowerMode.SHOW.getCode(), PowerMode.HIDE.getCode());
    }
}
