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
import org.openhab.io.homekit.core.characteristic.HomekitStringCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Staged Firmware Version Characteristic.
 * This characteristic represents the staged firmware version.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/StagedFirmwareVersion">HomeKit Documentation</a>
 * @author Karel Goderis - Initial contribution
 */
@HomekitCharacteristicType(type = "00000249-0000-1000-8000-0026BB765291", name = "Staged Firmware Version", tag = "stagedFirmwareVersion", acceptedItemTypes = {
        "String" })
public class HomekitStagedFirmwareVersionCharacteristic extends HomekitStringCharacteristic {
    public HomekitStagedFirmwareVersionCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Staged Firmware Version");
    }

    public HomekitStagedFirmwareVersionCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }
}
