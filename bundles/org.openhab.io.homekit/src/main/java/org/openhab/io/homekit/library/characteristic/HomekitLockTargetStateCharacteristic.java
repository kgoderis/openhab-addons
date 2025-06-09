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
 * Lock Target State characteristic.
 * <p>
 * This characteristic represents the target state of a lock mechanism.
 *
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 * @author Karel Goderis - Initial contribution
 */
@HomekitCharacteristicType(type = "0000001E-0000-1000-8000-0026BB765291", name = "Lock Target State", tag = "lockTargetState", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitLockTargetStateCharacteristic extends HomekitEnumCharacteristic {

    public enum LockTargetState {
        UNSECURED(0),
        SECURED(1);

        private final int code;

        LockTargetState(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static LockTargetState fromCode(int code) {
            for (LockTargetState state : values()) {
                if (state.code == code) {
                    return state;
                }
            }
            return UNSECURED;
        }
    }

    public HomekitLockTargetStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, LockTargetState.values().length);
        withInstanceId(instanceId).withPairedWrite(true).withPairedRead(true).withEvents(true)
                .withDescription("Lock Target State");
    }

    public HomekitLockTargetStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(@Nullable Integer value) {
        return value != null
                && (value == LockTargetState.UNSECURED.getCode() || value == LockTargetState.SECURED.getCode());
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(LockTargetState.UNSECURED.getCode(), LockTargetState.SECURED.getCode());
    }
}
