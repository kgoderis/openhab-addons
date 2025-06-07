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
import org.openhab.io.homekit.library.characteristic.HomekitAccessoryFlagsCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitFirmwareRevisionCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitHardwareRevisionCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitIdentifyCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitManufacturerCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitModelCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitNameCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSerialNumberCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitVersionCharacteristic;

/**
 * HomeKit Accessory Information Service.
 * 
 * <p>
 * This service provides basic information about the accessory, such as:
 * <ul>
 * <li>Manufacturer name</li>
 * <li>Model name</li>
 * <li>Serial number</li>
 * <li>Firmware version</li>
 * <li>Hardware revision</li>
 * </ul>
 * </p>
 *
 * <p>
 * This service is required for all HomeKit accessories and must be included in every accessory.
 * It provides essential information that helps users identify and manage their accessories.
 * </p>
 *
 * <p>
 * For more information, see the
 * <a href="https://developer.apple.com/documentation/HomeKit">HomeKit Accessory Protocol Specification</a>.
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@HomekitServiceType(type = "0000003E-0000-1000-8000-0026BB765291", name = "Accessory Information", tag = "accessoryInformation")
public class HomekitAccessoryInformationService extends AbstractHomekitService {

    /**
     * Creates a new Accessory Information service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @since 1.0
     */
    public HomekitAccessoryInformationService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Accessory Information").withExtensible(false).withPrimary(false).withHidden(false);
    }

    /**
     * Creates a new Accessory Information service from a JSON configuration.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     * @since 1.0
     */
    public HomekitAccessoryInformationService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    /**
     * Adds the required and optional characteristics for this service.
     * 
     * <p>
     * Required characteristics:
     * <ul>
     * <li>Identify</li>
     * <li>Manufacturer</li>
     * <li>Model</li>
     * <li>Name</li>
     * <li>Serial Number</li>
     * <li>Version</li>
     * </ul>
     * </p>
     *
     * <p>
     * Optional characteristics:
     * <ul>
     * <li>Firmware Revision</li>
     * <li>Hardware Revision</li>
     * <li>Accessory Flags</li>
     * </ul>
     * </p>
     *
     * @since 1.0
     */
    @Override
    public void addCharacteristics() throws HomekitServiceException {
        // Required characteristics
        addCharacteristic(
                new HomekitIdentifyCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        addCharacteristic(
                new HomekitManufacturerCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        addCharacteristic(
                new HomekitModelCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        addCharacteristic(new HomekitNameCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                .withMandatory(true));
        addCharacteristic(
                new HomekitSerialNumberCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        addCharacteristic(
                new HomekitVersionCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));

        // Optional characteristics
        addCharacteristic(new HomekitFirmwareRevisionCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        addCharacteristic(new HomekitHardwareRevisionCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        addCharacteristic(
                new HomekitAccessoryFlagsCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
    }

    /**
     * Returns whether this service is extensible.
     * Accessory Information service is not extensible by default.
     *
     * @return false as this service is not extensible
     */
    @Override
    public boolean isExtensible() {
        return false;
    }
}
