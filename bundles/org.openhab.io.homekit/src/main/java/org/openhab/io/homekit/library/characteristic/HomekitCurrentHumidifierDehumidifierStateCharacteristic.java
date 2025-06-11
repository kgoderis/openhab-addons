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
 * Current Humidifier Dehumidifier State characteristic.
 * This characteristic represents the current state for a humidifier/dehumidifier.
 *
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 * @author Karel Goderis - Initial contribution
 */
@HomekitCharacteristicType(type = "000000B3-0000-1000-8000-0026BB765291", name = "Current Humidifier Dehumidifier State", tag = "currentHumidifierDehumidifierState", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitCurrentHumidifierDehumidifierStateCharacteristic extends HomekitEnumCharacteristic {

    public enum CurrentHumidifierDehumidifierState {
        INACTIVE(0),
        IDLE(1),
        HUMIDIFYING(2),
        DEHUMIDIFYING(3);

        private final int code;

        CurrentHumidifierDehumidifierState(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static CurrentHumidifierDehumidifierState fromCode(int code) {
            for (CurrentHumidifierDehumidifierState state : values()) {
                if (state.code == code) {
                    return state;
                }
            }
            return INACTIVE;
        }
    }

    public HomekitCurrentHumidifierDehumidifierStateCharacteristic(HomekitService service,
            HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, CurrentHumidifierDehumidifierState.values().length);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("Current Humidifier Dehumidifier State");
    }

    public HomekitCurrentHumidifierDehumidifierStateCharacteristic(HomekitService service,
            HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(@Nullable Integer value) {
        if (value == null) {
            return false;
        }
        return value >= 0 && value < CurrentHumidifierDehumidifierState.values().length;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(CurrentHumidifierDehumidifierState.INACTIVE.getCode(),
                CurrentHumidifierDehumidifierState.IDLE.getCode(),
                CurrentHumidifierDehumidifierState.HUMIDIFYING.getCode(),
                CurrentHumidifierDehumidifierState.DEHUMIDIFYING.getCode());
    }
}
