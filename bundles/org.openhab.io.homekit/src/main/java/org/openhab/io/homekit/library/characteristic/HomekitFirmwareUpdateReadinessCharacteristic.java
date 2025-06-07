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
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Firmware Update Readiness Characteristic.
 * This characteristic represents the readiness state for firmware updates.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/FirmwareUpdateReadiness">HomeKit Documentation</a>
 * @author Karel Goderis - Initial contribution
     */
@HomekitCharacteristicType(type = "00000234-0000-1000-8000-0026BB765291", name = "Firmware Update Readiness", tag = "firmwareUpdateReadiness", acceptedItemTypes = {
        "Number", "String" })
public class HomekitFirmwareUpdateReadinessCharacteristic extends HomekitEnumCharacteristic {
    public static final int READY = 0;
    public static final int NOT_READY = 1;
    public static final int IN_PROGRESS = 2;

    public HomekitFirmwareUpdateReadinessCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 2);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Firmware Update Readiness");
    }

    public HomekitFirmwareUpdateReadinessCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= READY && value <= IN_PROGRESS;
    }
}
