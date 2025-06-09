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
 * Current Visibility State characteristic.
 * This characteristic represents whether a window covering or similar device is currently shown or hidden.
 *
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 * @author Karel Goderis - Initial contribution
 */
@HomekitCharacteristicType(type = "00000135-0000-1000-8000-0026BB765291", name = "Current Visibility State", tag = "currentVisibilityState", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitCurrentVisibilityStateCharacteristic extends HomekitEnumCharacteristic {
    public enum CurrentVisibilityState {
        SHOWN(0),
        HIDDEN(1);

        private final int code;

        CurrentVisibilityState(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static CurrentVisibilityState fromCode(int code) {
            for (CurrentVisibilityState s : values()) {
                if (s.code == code)
                    return s;
            }
            return SHOWN;
        }
    }

    public HomekitCurrentVisibilityStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, CurrentVisibilityState.values().length);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("Current Visibility State");
    }

    public HomekitCurrentVisibilityStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(@Nullable Integer value) {
        return value != null && (value == CurrentVisibilityState.SHOWN.getCode()
                || value == CurrentVisibilityState.HIDDEN.getCode());
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(CurrentVisibilityState.SHOWN.getCode(), CurrentVisibilityState.HIDDEN.getCode());
    }

    /**
     * Sets the value of the characteristic. Not supported for read-only characteristics.
     *
     * @param value the CurrentVisibilityState to set
     * @throws UnsupportedOperationException always
     */
    public void setValue(CurrentVisibilityState value) throws Exception {
        throw new UnsupportedOperationException("CurrentVisibilityStateCharacteristic is read-only");
    }
}
