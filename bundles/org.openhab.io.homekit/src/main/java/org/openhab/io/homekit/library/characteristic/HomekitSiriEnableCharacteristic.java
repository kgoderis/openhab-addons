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
 * HomeKit Siri Enable Characteristic.
 * This characteristic represents whether Siri is enabled.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/SiriEnable">HomeKit Documentation</a>
 * @author Karel Goderis - Initial contribution
 */
@HomekitCharacteristicType(type = "00000255-0000-1000-8000-0026BB765291", name = "Siri Enable", tag = "siriEnable", acceptedItemTypes = {
        "Switch", "Contact" })
public class HomekitSiriEnableCharacteristic extends HomekitBooleanCharacteristic {
    public HomekitSiriEnableCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true)
                .withDescription("Siri Enable");
    }

    public HomekitSiriEnableCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
}
