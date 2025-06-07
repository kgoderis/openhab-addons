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

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitBooleanCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit On Characteristic.
 * This characteristic represents the on/off state of a device.
 * When true, the device is on; when false, the device is off.
 *
 * @author Karel Goderis - Initial contribution
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
     */
@HomekitCharacteristicType(type = "00000025-0000-1000-8000-0026BB765291", name = "On", tag = "on", acceptedItemTypes = {
        "Switch", "Contact" })
public class HomekitOnCharacteristic extends HomekitBooleanCharacteristic {

    public HomekitOnCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true).withDescription("On");
    }

    public HomekitOnCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public State toState(@Nullable Boolean value) {
        return OnOffType.from(value != null && value);
    }
}
