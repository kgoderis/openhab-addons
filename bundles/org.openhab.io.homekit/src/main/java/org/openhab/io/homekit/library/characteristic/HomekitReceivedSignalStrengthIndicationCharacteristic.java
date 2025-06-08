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
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Received Signal Strength Indication Characteristic.
 * This characteristic represents the received signal strength in dBm.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/ReceivedSignalStrengthIndication">HomeKit
 *      Documentation</a>
 * @author Karel Goderis - Initial contribution
 */
@HomekitCharacteristicType(type = "0000023F-0000-1000-8000-0026BB765291", name = "Received Signal Strength Indication", tag = "receivedSignalStrengthIndication", acceptedItemTypes = {
        "Number" })
@NonNullByDefault
public class HomekitReceivedSignalStrengthIndicationCharacteristic extends HomekitIntegerCharacteristic {
    public HomekitReceivedSignalStrengthIndicationCharacteristic(HomekitService service,
            HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, -128, 127, "dBm");
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Received Signal Strength Indication");
    }

    public HomekitReceivedSignalStrengthIndicationCharacteristic(HomekitService service,
            HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= -128 && value <= 127;
    }
}
