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
import org.openhab.io.homekit.library.characteristic.HomekitListPairingsCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitPairSetupCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitPairVerifyCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitPairingFeaturesCharacteristic;

/**
 * Service that represents a pairing in HomeKit.
 * 
 * <p>
 * This service provides functionality for pairing HomeKit accessories with iOS devices. It handles:
 * <ul>
 * <li>Pairing setup and verification</li>
 * <li>Secure communication establishment</li>
 * <li>Accessory identification during pairing</li>
 * </ul>
 * </p>
 *
 * <p>
 * The pairing service is essential for the security of HomeKit accessories, as it ensures that:
 * <ul>
 * <li>Only authorized devices can control the accessory</li>
 * <li>Communication between the accessory and iOS device is encrypted</li>
 * <li>Each pairing is unique and secure</li>
 * </ul>
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
@HomekitServiceType(type = "00000055-0000-1000-8000-0026BB765291", name = "Pairing", tag = "pairing")
@NonNullByDefault
public class HomekitPairingService extends AbstractHomekitService {

    /**
     * Creates a new Pairing service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @since 1.0
     */
    public HomekitPairingService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Pairing").withExtensible(false).withPrimary(false).withHidden(false);
    }

    /**
     * Creates a new Pairing service from a JSON configuration.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     * @since 1.0
     */
    public HomekitPairingService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    /**
     * Adds the required characteristics for this service.
     * 
     * <p>
     * Required characteristics:
     * <ul>
     * <li>Name</li>
     * </ul>
     * </p>
     *
     * @since 1.0
     */
    @Override
    public void addCharacteristics() throws HomekitServiceException {
        addCharacteristic(
                new HomekitListPairingsCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        addCharacteristic(
                new HomekitPairSetupCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        addCharacteristic(
                new HomekitPairVerifyCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        addCharacteristic(new HomekitPairingFeaturesCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
    }
}
