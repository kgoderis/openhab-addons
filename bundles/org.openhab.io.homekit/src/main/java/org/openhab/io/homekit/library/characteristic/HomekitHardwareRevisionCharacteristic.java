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
import org.openhab.io.homekit.core.characteristic.HomekitReadOnlyStringCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Hardware Revision Characteristic.
 * This characteristic represents the hardware revision string.
 * This is used to identify the hardware version of a device, typically
 * following a version numbering scheme like "1.0.0" or "Rev A".
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
     */
@HomekitCharacteristicType(type = "00000053-0000-1000-8000-0026BB765291", name = "Hardware Revision", tag = "hardwareRevision", acceptedItemTypes = {
        "String", "Text" })
public class HomekitHardwareRevisionCharacteristic extends HomekitReadOnlyStringCharacteristic {
    public HomekitHardwareRevisionCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(false)
                .withDescription("Hardware Revision");
    }

    public HomekitHardwareRevisionCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }
}
