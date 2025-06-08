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
import org.openhab.io.homekit.core.characteristic.HomekitBooleanCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit AirPlay Enable Characteristic.
 * This characteristic represents whether AirPlay is enabled on the device.
 * When enabled, the device can receive and process AirPlay streams.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/homekit/hap-characteristic-types/airplay-enable">HAP
 *      Specification</a>
 */
@HomekitCharacteristicType(type = "0000025B-0000-1000-8000-0026BB765291", name = "AirPlay Enable", tag = "airPlayEnable", acceptedItemTypes = {
        "Switch", "Contact" })
@NonNullByDefault
public class HomekitAirPlayEnableCharacteristic extends HomekitBooleanCharacteristic {
    public HomekitAirPlayEnableCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true)
                .withDescription("AirPlay Enable");
    }

    public HomekitAirPlayEnableCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Boolean value) {
        return value != null;
    }
}
