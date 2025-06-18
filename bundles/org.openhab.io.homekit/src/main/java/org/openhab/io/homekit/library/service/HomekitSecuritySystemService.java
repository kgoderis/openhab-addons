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
import org.openhab.io.homekit.library.characteristic.HomekitNameCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSecuritySystemCurrentStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSecuritySystemTargetStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusFaultCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusTamperedCharacteristic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomeKit Security System Service.
 * <p>
 * This service provides control and monitoring of security systems in HomeKit accessories.
 * It enables tracking of security system states, tamper detection, and fault conditions.
 * </p>
 *
 * <ul>
 * <li>Security system state control and monitoring</li>
 * <li>Target state configuration</li>
 * <li>Tamper detection</li>
 * <li>Fault condition monitoring</li>
 * </ul>
 *
 * <p>
 * Required characteristics:
 * <ul>
 * <li>SecuritySystemCurrentState - Current security system state</li>
 * <li>SecuritySystemTargetState - Desired security system state</li>
 * </ul>
 * </p>
 *
 * <p>
 * Optional characteristics:
 * <ul>
 * <li>Name - Security system name</li>
 * <li>StatusFault - Fault state indicator</li>
 * <li>StatusTampered - Tamper detection indicator</li>
 * </ul>
 * </p>
 *
 * <p>
 * For more information, see the
 * <a href="https://developer.apple.com/documentation/HomeKit">HomeKit Accessory Protocol Specification</a>.
 * </p>
 *
 * @author Karel Goderis - Initial Contribution
 * @version 1.0
 * @since 1.0
 */
@HomekitServiceType(type = "0000007E-0000-1000-8000-0026BB765291", name = "Security System", tag = "securitySystem")
@NonNullByDefault
public class HomekitSecuritySystemService extends AbstractHomekitService {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit SecuritySystemService: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_STATE = LOG_PREFIX + "State - ";
    private static final String LOG_TRACE = LOG_PREFIX + "Trace - ";

    private static final Logger logger = LoggerFactory.getLogger(HomekitSecuritySystemService.class);

    /**
     * Creates a new Security System service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @since 1.0
     */
    public HomekitSecuritySystemService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Security System").withPrimary(false).withHidden(false);
        logger.debug("{}Created SecuritySystemService for accessory {}", LOG_INIT, accessory.getLabel());
    }

    /**
     * Creates a new Security System service from a JSON configuration.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     * @since 1.0
     */
    public HomekitSecuritySystemService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
        logger.debug("{}Created SecuritySystemService from JSON for accessory {}", LOG_INIT, accessory.getLabel());
    }

    /**
     * Adds the required and optional characteristics for this service.
     * <p>
     * Required characteristics:
     * <ul>
     * <li>SecuritySystemCurrentState - Current security system state</li>
     * <li>SecuritySystemTargetState - Desired security system state</li>
     * </ul>
     * </p>
     * <p>
     * Optional characteristics:
     * <ul>
     * <li>Name - Security system name</li>
     * <li>StatusFault - Fault state indicator</li>
     * <li>StatusTampered - Tamper detection indicator</li>
     * </ul>
     * </p>
     * 
     * @since 1.0
     */
    @Override
    public void addCharacteristics() throws HomekitServiceException {
        logger.debug("{}Adding required characteristics to SecuritySystemService for accessory {}", LOG_TRACE,
                getAccessory().getLabel());

        addCharacteristic(new HomekitSecuritySystemCurrentStateCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        logger.debug("{}Added SecuritySystemCurrentStateCharacteristic to SecuritySystemService", LOG_STATE);

        addCharacteristic(new HomekitSecuritySystemTargetStateCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        logger.debug("{}Added SecuritySystemTargetStateCharacteristic to SecuritySystemService", LOG_STATE);

        addCharacteristic(new HomekitNameCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                .withMandatory(false));
        logger.debug("{}Added NameCharacteristic to SecuritySystemService", LOG_STATE);

        addCharacteristic(
                new HomekitStatusFaultCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        logger.debug("{}Added StatusFaultCharacteristic to SecuritySystemService", LOG_STATE);

        addCharacteristic(
                new HomekitStatusTamperedCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        logger.debug("{}Added StatusTamperedCharacteristic to SecuritySystemService", LOG_STATE);
    }

    /**
     * Indicates that this service is not extensible.
     * Security System service has a fixed set of characteristics.
     *
     * @return false, as this service is not extensible
     * @since 1.0
     */
    @Override
    public boolean isExtensible() {
        return false;
    }
}
