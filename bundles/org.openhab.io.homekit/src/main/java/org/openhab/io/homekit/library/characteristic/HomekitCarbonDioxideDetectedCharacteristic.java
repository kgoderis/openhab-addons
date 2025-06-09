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
 * Carbon Dioxide Detected characteristic.
 * This characteristic represents whether carbon dioxide has been detected.
 * The value is an enumeration with two states: NOT_DETECTED and DETECTED.
 *
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 * @author Karel Goderis - Initial contribution
 */
@HomekitCharacteristicType(type = "00000092-0000-1000-8000-0026BB765291", name = "Carbon Dioxide Detected", tag = "carbonDioxideDetected", acceptedItemTypes = {
        "Number", "String", "Contact" })
@NonNullByDefault
public class HomekitCarbonDioxideDetectedCharacteristic extends HomekitEnumCharacteristic {

    public enum CarbonDioxideDetected {
        NOT_DETECTED(0),
        DETECTED(1);

        private final int code;

        CarbonDioxideDetected(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static CarbonDioxideDetected fromCode(int code) {
            for (CarbonDioxideDetected state : values()) {
                if (state.code == code) {
                    return state;
                }
            }
            return NOT_DETECTED;
        }
    }

    /**
     * Creates a new Carbon Dioxide Detected characteristic.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitCarbonDioxideDetectedCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, CarbonDioxideDetected.values().length);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("Carbon Dioxide Detected");
    }

    /**
     * Creates a new Carbon Dioxide Detected characteristic from a JSON value.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitCarbonDioxideDetectedCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(@Nullable Integer value) {
        return value != null && (value == CarbonDioxideDetected.NOT_DETECTED.getCode()
                || value == CarbonDioxideDetected.DETECTED.getCode());
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(CarbonDioxideDetected.NOT_DETECTED.getCode(), CarbonDioxideDetected.DETECTED.getCode());
    }
}
