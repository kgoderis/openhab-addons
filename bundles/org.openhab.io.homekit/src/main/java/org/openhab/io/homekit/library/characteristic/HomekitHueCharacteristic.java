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
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitFloatCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Hue Characteristic.
 * This characteristic represents the hue of a light in degrees.
 * The hue value ranges from 0 to 360 degrees, where 0/360 is red, 120 is green, and 240 is blue.
 *
 * @author Karel Goderis - Initial contribution
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000013-0000-1000-8000-0026BB765291", name = "Hue", tag = "hue", acceptedItemTypes = {
        "Number", "Color" })
@NonNullByDefault
public class HomekitHueCharacteristic extends HomekitFloatCharacteristic {
    public HomekitHueCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0.0, 360.0, 1.0, "arcdegrees");
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true).withDescription("Hue");
    }

    public HomekitHueCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public Double toValue(State state) {
        if (state instanceof HSBType hSBType) {
            DecimalType hue = hSBType.getHue();
            return hue.doubleValue();
        } else {
            DecimalType convertedState = state.as(DecimalType.class);
            if (convertedState == null) {
                return minValue;
            }
            return convertedState.doubleValue();
        }
    }

    @Override
    public State toState(Double value) {
        return new DecimalType(value);
    }
}
