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

import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.exception.HomekitServiceException;
import org.openhab.io.homekit.library.characteristic.HomekitAdministratorOnlyAccessCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitAudioFeedbackCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitCurrentDoorStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitLockControlPointCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitLockLastKnownActionCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitLockManagementAutoSecurityTimeoutCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitLogsCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitMotionDetectedCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitNameCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitVersionCharacteristic;

/**
 * Service that represents lock management in HomeKit.
 * This service provides control over lock settings and operation.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitServiceType(type = "00000044-0000-1000-8000-0026BB765291", name = "Lock Management", tag = "lockManagement")
public class HomekitLockManagementService extends AbstractHomekitService {

    /**
     * Creates a new HomekitLockManagementService.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     */
    public HomekitLockManagementService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Lock Management").withPrimary(false).withHidden(false);
    }

    /**
     * Creates a new HomekitLockManagementService from a JSON value.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     */
    public HomekitLockManagementService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    @Override
    public void addCharacteristics() throws HomekitServiceException {
        addCharacteristic(new HomekitLockControlPointCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(
                new HomekitVersionCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        addCharacteristic(new HomekitLogsCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                .withMandatory(true));
        addCharacteristic(
                new HomekitAudioFeedbackCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        addCharacteristic(new HomekitLockManagementAutoSecurityTimeoutCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        addCharacteristic(new HomekitAdministratorOnlyAccessCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        addCharacteristic(new HomekitLockLastKnownActionCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        addCharacteristic(new HomekitCurrentDoorStateCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        addCharacteristic(
                new HomekitMotionDetectedCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        addCharacteristic(new HomekitNameCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                .withMandatory(false));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }
}
