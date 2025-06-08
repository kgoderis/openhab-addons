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
 * HomeKit Metrics Buffer Full State Characteristic.
 * This characteristic represents whether the metrics buffer is full.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/MetricsBufferFullState">HomeKit Documentation</a>
 * @author Karel Goderis - Initial contribution
 */
@HomekitCharacteristicType(type = "00000272-0000-1000-8000-0026BB765291", name = "Metrics Buffer Full State", tag = "metricsBufferFullState", acceptedItemTypes = {
        "Switch", "Contact" })
@NonNullByDefault
public class HomekitMetricsBufferFullStateCharacteristic extends HomekitBooleanCharacteristic {
    public HomekitMetricsBufferFullStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Metrics Buffer Full State");
    }

    public HomekitMetricsBufferFullStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }
}
