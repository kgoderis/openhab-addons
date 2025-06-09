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
 * HomeKit Input Source Type Characteristic.
 * This characteristic represents the type of input source for a device.
 * The type can be one of: OTHER, HOME_SCREEN, TUNER, HDMI, COMPOSITE_VIDEO, S_VIDEO,
 * COMPONENT_VIDEO, DVI, AIRPLAY, USB, or APPLICATION.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/homekit/hap-characteristic-types/input-source-type">HAP
 *      Specification</a>
 */
@HomekitCharacteristicType(type = "000000DB-0000-1000-8000-0026BB765291", name = "Input Source Type", tag = "inputSourceType", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitInputSourceTypeCharacteristic extends HomekitEnumCharacteristic {
    public enum InputSourceType {
        OTHER(0),
        HOME_SCREEN(1),
        TUNER(2),
        HDMI(3),
        COMPOSITE_VIDEO(4),
        S_VIDEO(5),
        COMPONENT_VIDEO(6),
        DVI(7),
        AIRPLAY(8),
        USB(9),
        APPLICATION(10);

        private final int value;

        InputSourceType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static InputSourceType fromValue(int value) {
            for (InputSourceType type : values()) {
                if (type.value == value) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid Input Source Type value: " + value);
        }
    }

    public HomekitInputSourceTypeCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, InputSourceType.values().length);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Input Source Type");
    }

    public HomekitInputSourceTypeCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(@Nullable Integer value) {
        return value != null && value >= InputSourceType.OTHER.getValue()
                && value <= InputSourceType.APPLICATION.getValue();
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(InputSourceType.OTHER.getValue(), InputSourceType.HOME_SCREEN.getValue(),
                InputSourceType.TUNER.getValue(), InputSourceType.HDMI.getValue(),
                InputSourceType.COMPOSITE_VIDEO.getValue(), InputSourceType.S_VIDEO.getValue(),
                InputSourceType.COMPONENT_VIDEO.getValue(), InputSourceType.DVI.getValue(),
                InputSourceType.AIRPLAY.getValue(), InputSourceType.USB.getValue(),
                InputSourceType.APPLICATION.getValue());
    }
}
