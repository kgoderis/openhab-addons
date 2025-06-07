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
 * Carbon Monoxide Level characteristic.
 * This characteristic represents the current carbon monoxide level in parts per million (ppm).
 * The value range is 0-1000 ppm with 1 ppm step.
 *
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 * @author Karel Goderis - Initial contribution
 */
@HomekitCharacteristicType(type = "00000090-0000-1000-8000-0026BB765291", name = "Carbon Monoxide Level", tag = "carbonMonoxideLevel", acceptedItemTypes = {
        "Number" })
public class HomekitCarbonMonoxideLevelCharacteristic extends HomekitFloatCharacteristic {

    public HomekitCarbonMonoxideLevelCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 0.0, 1000.0, 1.0, "");
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("Carbon Monoxide Level");
    }

    public HomekitCarbonMonoxideLevelCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Double value) {
        return value != null && value >= 0.0 && value <= 1000.0;
    }

    @Override
    public java.util.Set<Double> getAllowedValues() {
        return java.util.Collections.emptySet();
    }
}
