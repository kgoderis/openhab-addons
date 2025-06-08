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
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitBooleanCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Identify Characteristic.
 * <p>
 * This characteristic represents the identify action for a HomeKit accessory. When set to true, the accessory should
 * perform a physical identification action (such as blinking an LED or making a sound) to help the user locate it. The
 * value is a boolean and is write-only.
 * <p>
 * See the HomeKit Accessory Protocol (HAP) specification for details: https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitCharacteristicType(type = "00000014-0000-1000-8000-0026BB765291", name = "Identify", tag = "identify", acceptedItemTypes = {
        "Switch", "Contact" })
@NonNullByDefault
public class HomekitIdentifyCharacteristic extends HomekitBooleanCharacteristic {

    /**
     * Constructs a new Identify characteristic.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param instanceId the instance ID for this characteristic
     */
    public HomekitIdentifyCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedWrite(true).withPairedRead(false).withEvents(false)
                .withDescription("Identify");
    }

    /**
     * Constructs a new Identify characteristic from a JSON value.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param value the JSON value to initialize the characteristic with
     */
    public HomekitIdentifyCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Sets the value of the Identify characteristic. If set to true, triggers the accessory's identify action.
     *
     * @param value the value to set (true to identify, false otherwise)
     */
    @Override
    public void setValue(@Nullable Boolean value) {
        if (value != null && value) {
            getService().getAccessory().identify();
        }
    }

    /**
     * Gets the default value for the Identify characteristic.
     *
     * @return false (default is not to identify)
     */
    @Override
    public Boolean getDefault() {
        return false;
    }
}
