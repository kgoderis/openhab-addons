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
import org.openhab.io.homekit.library.characteristic.HomekitInUseCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitNameCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitProgramModeCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitRemainingDurationCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSetDurationCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusFaultCharacteristic;

/**
 * Service that represents an irrigation system in HomeKit.
 * This service provides control over irrigation system operation and scheduling.
 *
 * @author Karel Goderis - Initial Contribution
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitServiceType(type = "000000CF-0000-1000-8000-0026BB765291", name = "Irrigation System", tag = "irrigationSystem")
@NonNullByDefault
public class HomekitIrrigationSystemService extends AbstractHomekitService {

        /**
         * Creates a new HomekitIrrigationSystemService.
         *
         * @param accessory The accessory this service belongs to
         * @param eventManager The event manager for handling HomeKit events
         * @param characteristicFactory Factory for creating HomeKit characteristics
         */
        public HomekitIrrigationSystemService(HomekitAccessory accessory, HomekitEventManager eventManager,
                        HomekitCharacteristicFactory characteristicFactory) {
                super(accessory, eventManager, characteristicFactory);
                withName("Irrigation System").withPrimary(false).withHidden(false).withExtensible(false);
        }

        /**
         * Creates a new HomekitIrrigationSystemService from a JSON value.
         *
         * @param accessory The accessory this service belongs to
         * @param eventManager The event manager for handling HomeKit events
         * @param characteristicFactory Factory for creating HomeKit characteristics
         * @param value JSON value containing service configuration
         */
        public HomekitIrrigationSystemService(HomekitAccessory accessory, HomekitEventManager eventManager,
                        HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
                super(accessory, eventManager, characteristicFactory, value);
        }

        @Override
        public void addCharacteristics() throws HomekitServiceException {
                addCharacteristic(
                                new HomekitActiveCharacteristic(this, eventManager,
                                                getAccessory().getNextAvailableInstanceId())
                                                .withMandatory(true));
                addCharacteristic(
                                new HomekitProgramModeCharacteristic(this, eventManager,
                                                getAccessory().getNextAvailableInstanceId())
                                                .withMandatory(true));
                addCharacteristic(
                                new HomekitInUseCharacteristic(this, eventManager,
                                                getAccessory().getNextAvailableInstanceId())
                                                .withMandatory(true));
                addCharacteristic(new HomekitRemainingDurationCharacteristic(this, eventManager,
                                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
                addCharacteristic(
                                new HomekitSetDurationCharacteristic(this, eventManager,
                                                getAccessory().getNextAvailableInstanceId())
                                                .withMandatory(true));
                addCharacteristic(new HomekitNameCharacteristic(this, eventManager,
                                getAccessory().getNextAvailableInstanceId())
                                .withMandatory(false));
                addCharacteristic(
                                new HomekitStatusFaultCharacteristic(this, eventManager,
                                                getAccessory().getNextAvailableInstanceId())
                                                .withMandatory(false));
                logger.debug("{}Added characteristics for Irrigation System service", LOG_CONFIG);
        }
}
