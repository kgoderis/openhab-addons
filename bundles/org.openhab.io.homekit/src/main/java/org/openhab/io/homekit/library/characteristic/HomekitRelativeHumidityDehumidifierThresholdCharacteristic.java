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

import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitFloatCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Relative Humidity Dehumidifier Threshold Characteristic.
 * This characteristic represents the relative humidity threshold for a dehumidifier, as a percentage between 0 and 100.
 *
 * @see <a href=\"https://developer.apple.com/documentation/HomeKit\">HAP Specification</a>
 * @author Karel Goderis - Initial contribution
     */
@HomekitCharacteristicType(type = "000000C9-0000-1000-8000-0026BB765291", name = "Relative Humidity Dehumidifier Threshold", tag = "relativeHumidityDehumidifierThreshold", acceptedItemTypes = {
        "Number" })
public class HomekitRelativeHumidityDehumidifierThresholdCharacteristic extends HomekitFloatCharacteristic {
    public HomekitRelativeHumidityDehumidifierThresholdCharacteristic(HomekitService service,
            HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0.0, 100.0, 1.0, "%");
        withInstanceId(instanceId).withPairedWrite(true).withPairedRead(true).withEvents(true)
                .withDescription("Relative Humidity Dehumidifier Threshold");
    }

    public HomekitRelativeHumidityDehumidifierThresholdCharacteristic(HomekitService service,
            HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is a valid relative humidity threshold for a dehumidifier.
     *
     * @param value the value to check
     * @return true if the value is within the allowed range, false otherwise
     */
    @Override
    public boolean isAllowedValue(Double value) {
        return value != null && value >= 0.0 && value <= 100.0;
    }
}
