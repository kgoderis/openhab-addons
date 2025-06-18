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
import org.openhab.io.homekit.library.characteristic.HomekitActiveCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitCurrentFanStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitLockPhysicalControlsCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitNameCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitRotationDirectionCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitRotationSpeedCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSwingModeCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitTargetFanStateCharacteristic;

/**
 * Service that represents a fan v2 in HomeKit.
 * This service provides enhanced control over fan settings and operation.
 *
 * @author Karel Goderis - Initial Contribution
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitServiceType(type = "000000B7-0000-1000-8000-0026BB765291", name = "Fan v2", tag = "fanV2")
@NonNullByDefault
public class HomekitFanV2Service extends AbstractHomekitService {

        /**
         * Creates a new HomekitFanV2Service.
         *
         * @param accessory The accessory this service belongs to
         * @param eventManager The event manager for handling HomeKit events
         * @param characteristicFactory Factory for creating HomeKit characteristics
         */
        public HomekitFanV2Service(HomekitAccessory accessory, HomekitEventManager eventManager,
                        HomekitCharacteristicFactory characteristicFactory) {
                super(accessory, eventManager, characteristicFactory);
                withName("Fan V2").withPrimary(false).withHidden(false).withExtensible(false);
                logger.debug("{}Created FanV2Service for accessory {}", LOG_INIT, accessory.getLabel());
        }

        /**
         * Creates a new HomekitFanV2Service from a JSON value.
         *
         * @param accessory The accessory this service belongs to
         * @param eventManager The event manager for handling HomeKit events
         * @param characteristicFactory Factory for creating HomeKit characteristics
         * @param value JSON value containing service configuration
         */
        public HomekitFanV2Service(HomekitAccessory accessory, HomekitEventManager eventManager,
                        HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
                super(accessory, eventManager, characteristicFactory, value);
        }

        @Override
        public void addCharacteristics() throws HomekitServiceException {
                addCharacteristic(
                                new HomekitActiveCharacteristic(this, eventManager,
                                                getAccessory().getNextAvailableInstanceId())
                                                .withMandatory(true));
                addCharacteristic(new HomekitCurrentFanStateCharacteristic(this, eventManager,
                                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
                addCharacteristic(
                                new HomekitTargetFanStateCharacteristic(this, eventManager,
                                                getAccessory().getNextAvailableInstanceId())
                                                .withMandatory(true));
                addCharacteristic(new HomekitRotationDirectionCharacteristic(this, eventManager,
                                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
                addCharacteristic(
                                new HomekitRotationSpeedCharacteristic(this, eventManager,
                                                getAccessory().getNextAvailableInstanceId())
                                                .withMandatory(false));
                addCharacteristic(
                                new HomekitSwingModeCharacteristic(this, eventManager,
                                                getAccessory().getNextAvailableInstanceId())
                                                .withMandatory(false));
                addCharacteristic(new HomekitLockPhysicalControlsCharacteristic(this, eventManager,
                                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
                addCharacteristic(new HomekitNameCharacteristic(this, eventManager,
                                getAccessory().getNextAvailableInstanceId())
                                .withMandatory(false));
        }
}
