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

package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.exception.HomekitServiceException;
import org.openhab.io.homekit.library.characteristic.HomekitCurrentHorizontalTiltAngleCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitCurrentPositionCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitCurrentVerticalTiltAngleCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitHoldPositionCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitObstructionDetectedCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitPositionStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitTargetHorizontalTiltAngleCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitTargetPositionCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitTargetVerticalTiltAngleCharacteristic;

/**
 * Service that represents a window covering in HomeKit.
 * This service provides control over window coverings like blinds, shades, and curtains.
 * 
 * @author Karel Goderis - Initial Contribution
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitServiceType(type = "0000008C-0000-1000-8000-0026BB765291", name = "WindowCovering", tag = "windowCovering")
@NonNullByDefault
public class HomekitWindowCoveringService extends AbstractHomekitService {

    /**
     * Creates a new HomekitWindowCoveringService.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     */
    public HomekitWindowCoveringService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Window Covering").withPrimary(false).withHidden(false).withExtensible(false);
    }

    /**
     * Creates a new HomekitWindowCoveringService from a JSON value.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     */
    public HomekitWindowCoveringService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    @Override
    public void addCharacteristics() throws HomekitServiceException {
        addCharacteristic(new HomekitCurrentPositionCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(
                new HomekitTargetPositionCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        addCharacteristic(
                new HomekitPositionStateCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        addCharacteristic(
                new HomekitHoldPositionCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        addCharacteristic(new HomekitCurrentHorizontalTiltAngleCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        addCharacteristic(new HomekitTargetHorizontalTiltAngleCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        addCharacteristic(new HomekitCurrentVerticalTiltAngleCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        addCharacteristic(new HomekitTargetVerticalTiltAngleCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        addCharacteristic(new HomekitObstructionDetectedCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        logger.debug("{}Added characteristics for Window Covering service", LOG_CONFIG);
    }
}
