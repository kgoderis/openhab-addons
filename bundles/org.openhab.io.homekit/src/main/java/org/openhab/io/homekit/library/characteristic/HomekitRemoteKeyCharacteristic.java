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
 * HomeKit Remote Key Characteristic.
 * This characteristic represents the remote key commands for a device.
 * The key can be one of: REWIND, FAST_FORWARD, NEXT_TRACK, PREVIOUS_TRACK, ARROW_UP,
 * ARROW_DOWN, ARROW_LEFT, ARROW_RIGHT, SELECT, BACK, EXIT, PLAY_PAUSE, or INFORMATION.
 *
 * @author Karel Goderis - Initial Contribution
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "000000E1-0000-1000-8000-0026BB765291", name = "Remote Key", tag = "remoteKey", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitRemoteKeyCharacteristic extends HomekitEnumCharacteristic {
    public enum RemoteKey {
        REWIND(0),
        FAST_FORWARD(1),
        NEXT_TRACK(2),
        PREVIOUS_TRACK(3),
        ARROW_UP(4),
        ARROW_DOWN(5),
        ARROW_LEFT(6),
        ARROW_RIGHT(7),
        SELECT(8),
        BACK(9),
        EXIT(10),
        PLAY_PAUSE(11),
        INFORMATION(12);

        private final int value;

        RemoteKey(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static RemoteKey fromValue(int value) {
            for (RemoteKey key : values()) {
                if (key.value == value) {
                    return key;
                }
            }
            throw new IllegalArgumentException("Invalid Remote Key value: " + value);
        }
    }

    public HomekitRemoteKeyCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, RemoteKey.values().length);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true)
                .withDescription("Remote Key");
    }

    public HomekitRemoteKeyCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(@Nullable Integer value) {
        if (value == null) {
            return false;
        }
        try {
            RemoteKey.fromValue(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(RemoteKey.REWIND.getValue(), RemoteKey.FAST_FORWARD.getValue(),
                RemoteKey.NEXT_TRACK.getValue(), RemoteKey.PREVIOUS_TRACK.getValue(), RemoteKey.ARROW_UP.getValue(),
                RemoteKey.ARROW_DOWN.getValue(), RemoteKey.ARROW_LEFT.getValue(), RemoteKey.ARROW_RIGHT.getValue(),
                RemoteKey.SELECT.getValue(), RemoteKey.BACK.getValue(), RemoteKey.EXIT.getValue(),
                RemoteKey.PLAY_PAUSE.getValue(), RemoteKey.INFORMATION.getValue());
    }
}
