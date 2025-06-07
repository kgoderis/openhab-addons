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
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Signal To Noise Ratio Characteristic.
 * This characteristic represents the signal to noise ratio in dB.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/SignalToNoiseRatio">HomeKit Documentation</a>
 * @author Karel Goderis - Initial contribution
     */
@HomekitCharacteristicType(type = "00000241-0000-1000-8000-0026BB765291", name = "Signal To Noise Ratio", tag = "signalToNoiseRatio", acceptedItemTypes = {
        "Number" })
public class HomekitSignalToNoiseRatioCharacteristic extends HomekitIntegerCharacteristic {
    public HomekitSignalToNoiseRatioCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, -128, 127, "dB");
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Signal To Noise Ratio");
    }

    public HomekitSignalToNoiseRatioCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= -128 && value <= 127;
    }
}
