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
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Recording Audio Active Characteristic.
 * This characteristic represents whether audio recording is active or not.
 *
 * @see <a href=\"https://developer.apple.com/documentation/HomeKit\">HAP Specification</a>
 * @author Karel Goderis - Initial contribution
 */
@HomekitCharacteristicType(type = "00000226-0000-1000-8000-0026BB765291", name = "Recording Audio Active", tag = "recordingAudioActive", acceptedItemTypes = {
        "Switch" })
@NonNullByDefault
public class HomekitRecordingAudioActiveCharacteristic extends HomekitIntegerCharacteristic {
    /**
     * Enum representing the possible states for recording audio.
     */
    public enum RecordingAudioState {
        DISABLE(0),
        ENABLE(1);

        private final int code;

        RecordingAudioState(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static RecordingAudioState fromCode(int code) {
            for (RecordingAudioState s : values()) {
                if (s.code == code)
                    return s;
            }
            return DISABLE;
        }
    }

    public HomekitRecordingAudioActiveCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 0, 1, "");
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true).withTimedWrite(true)
                .withDescription("Recording Audio Active");
    }

    public HomekitRecordingAudioActiveCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is a valid recording audio state.
     *
     * @param value the value to check
     * @return true if the value is DISABLE or ENABLE, false otherwise
     */
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null
                && (value == RecordingAudioState.DISABLE.getCode() || value == RecordingAudioState.ENABLE.getCode());
    }

    /**
     * Returns the set of allowed recording audio state values.
     *
     * @return a set containing DISABLE and ENABLE
     */
    @Override
    public Set<Integer> getAllowedValues() {
        return Set.of(RecordingAudioState.DISABLE.getCode(), RecordingAudioState.ENABLE.getCode());
    }
}
