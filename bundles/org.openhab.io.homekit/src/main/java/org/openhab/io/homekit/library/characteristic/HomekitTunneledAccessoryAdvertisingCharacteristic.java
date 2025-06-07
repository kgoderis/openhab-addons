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
import org.openhab.io.homekit.core.characteristic.HomekitBooleanCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Tunneled Accessory Advertising Characteristic.
 * This characteristic represents whether a tunneled accessory is advertising.
 *
 * @see <a href=\"https://developer.apple.com/documentation/HomeKit\">HAP Specification</a>
 * @author Karel Goderis - Initial contribution
     */
@HomekitCharacteristicType(type = "00000060-0000-1000-8000-0026BB765291", name = "Tunneled Accessory Advertising", tag = "tunneledAccessoryAdvertising", acceptedItemTypes = {
        "Switch", "Contact" })
public class HomekitTunneledAccessoryAdvertisingCharacteristic extends HomekitBooleanCharacteristic {

    public HomekitTunneledAccessoryAdvertisingCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedWrite(true).withPairedRead(true).withEvents(true)
                .withDescription("Tunneled Accessory Advertising");
    }

    public HomekitTunneledAccessoryAdvertisingCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }
}
