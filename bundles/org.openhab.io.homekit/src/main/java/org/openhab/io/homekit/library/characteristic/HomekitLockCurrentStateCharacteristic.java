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
 * Lock Current State characteristic.
 * <p>
 * This characteristic represents the current state of a lock mechanism.
 *
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 * @author Karel Goderis - Initial contribution
 */
@HomekitCharacteristicType(type = "0000001D-0000-1000-8000-0026BB765291", name = "Lock Current State", tag = "lockCurrentState", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitLockCurrentStateCharacteristic extends HomekitEnumCharacteristic {

    public enum LockCurrentState {
        UNSECURED(0),
        SECURED(1),
        JAMMED(2),
        UNKNOWN(3);

        private final int code;

        LockCurrentState(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static LockCurrentState fromCode(int code) {
            for (LockCurrentState state : values()) {
                if (state.code == code) {
                    return state;
                }
            }
            return UNKNOWN;
        }
    }

    public HomekitLockCurrentStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, LockCurrentState.values().length);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("Lock Current State");
    }

    public HomekitLockCurrentStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= LockCurrentState.UNSECURED.getCode()
                && value <= LockCurrentState.UNKNOWN.getCode();
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(LockCurrentState.UNSECURED.getCode(), LockCurrentState.SECURED.getCode(),
                LockCurrentState.JAMMED.getCode(), LockCurrentState.UNKNOWN.getCode());
    }
}
