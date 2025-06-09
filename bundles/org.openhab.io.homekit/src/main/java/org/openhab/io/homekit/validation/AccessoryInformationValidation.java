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

package org.openhab.io.homekit.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.Thing;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.library.service.HomekitAccessoryInformationService;
import org.osgi.service.component.annotations.Component;

/**
 * Performs validation of accessory information to ensure required characteristics are present and valid.
 * This validation is essential for HomeKit accessory functionality.
 * 
 * @author Karel Goderis - Initial contribution
 */
@Component(service = Validation.class)
@NonNullByDefault
public class AccessoryInformationValidation extends AbstractValidation {
    private static final String ID = "accessory-information";
    private static final int PRIORITY = 60; // High priority but after characteristic validation

    public AccessoryInformationValidation() {
        super(ID, PRIORITY);
    }

    @Override
    protected Optional<ValidationResult> doValidate(ValidationContext context) {
        Object object = context.getTarget();
        if (!(object instanceof Thing)) {
            List<ValidationIssue> issues = new ArrayList<>();
            issues.add(createIssue(ValidationResult.Severity.ERROR, "Invalid object type: expected Thing",
                    "INVALID_TYPE", getContextKey(object), true, true));
            return Optional.of(createResult(issues));
        }

        Thing thing = (Thing) object;
        List<ValidationIssue> issues = new ArrayList<>();

        // Get all services for this thing
        Optional<List<HomekitService>> servicesOpt = getServices(thing);
        if (servicesOpt.isEmpty()) {
            issues.add(createIssue(ValidationResult.Severity.ERROR, "No HomeKit services found for thing",
                    "NO_SERVICES", getContextKey(thing), true, true));
            return Optional.of(createResult(issues));
        }

        // Find the accessory information service
        @SuppressWarnings("null") // get() is safe after isEmpty() check above
        List<HomekitService> services = servicesOpt.get();
        Optional<HomekitService> accessoryInfoServiceOpt = findAccessoryInformationService(services);
        if (accessoryInfoServiceOpt.isEmpty()) {
            issues.add(createIssue(ValidationResult.Severity.ERROR, "Accessory Information service not found",
                    "MISSING_ACCESSORY_INFO_SERVICE", getContextKey(thing), true, true));
            return Optional.of(createResult(issues));
        }

        // Validate the accessory information service
        @SuppressWarnings("null") // get() is safe after isEmpty() check above
        HomekitService accessoryInfoService = accessoryInfoServiceOpt.get();
        validateAccessoryInformationService(accessoryInfoService, thing, issues);

        return Optional.of(createResult(issues));
    }

    private Optional<HomekitService> findAccessoryInformationService(List<HomekitService> services) {
        for (HomekitService service : services) {
            if (service instanceof HomekitAccessoryInformationService) {
                return Optional.of(service);
            }
        }
        return Optional.empty();
    }

    private void validateAccessoryInformationService(HomekitService service, Thing thing,
            List<ValidationIssue> issues) {
        @SuppressWarnings("null") // getAnnotation() can return null, handled by null check below
        HomekitServiceType serviceType = service.getClass().getAnnotation(HomekitServiceType.class);
        if (serviceType == null) {
            issues.add(createIssue(ValidationResult.Severity.ERROR,
                    "Service type annotation not found for Accessory Information service", "MISSING_SERVICE_TYPE",
                    getContextKey(thing), true, true));
            return;
        }

        // String serviceName = serviceType.name();
        String serviceUuid = serviceType.type();

        // Get all characteristics for this service
        Set<HomekitCharacteristic<?>> characteristics = service.getCharacteristics();
        if (characteristics == null) {
            issues.add(createIssue(ValidationResult.Severity.ERROR,
                    "No characteristics found for Accessory Information service", "NO_CHARACTERISTICS",
                    getContextKey(thing) + ":" + serviceUuid, true, true));
            return;
        }

        // Validate required characteristics
        validateRequiredCharacteristic(characteristics, "Manufacturer", thing, service, issues);
        validateRequiredCharacteristic(characteristics, "Model", thing, service, issues);
        validateRequiredCharacteristic(characteristics, "SerialNumber", thing, service, issues);
        validateRequiredCharacteristic(characteristics, "Name", thing, service, issues);
        validateRequiredCharacteristic(characteristics, "Identify", thing, service, issues);
    }

    private void validateRequiredCharacteristic(Set<HomekitCharacteristic<?>> characteristics,
            String characteristicName, Thing thing, HomekitService service, List<ValidationIssue> issues) {
        boolean found = false;
        for (HomekitCharacteristic<?> characteristic : characteristics) {
            if (characteristic.getClass().getSimpleName().contains(characteristicName)) {
                found = true;
                break;
            }
        }

        if (!found) {
            issues.add(createIssue(ValidationResult.Severity.ERROR,
                    String.format("Required characteristic '%s' not found in Accessory Information service",
                            characteristicName),
                    "MISSING_REQUIRED_CHARACTERISTIC",
                    getContextKey(thing) + ":" + service.getType() + ":" + characteristicName, true, true,
                    Map.<String, Object> of("serviceName", service.getName(), "serviceUuid", service.getType(),
                            "characteristicName", characteristicName)));
        }
    }

    private Optional<List<HomekitService>> getServices(Thing thing) {
        // TODO: Implement service retrieval from thing configuration
        // This will depend on how services are stored in the thing configuration
        return Optional.empty();
    }

    @Override
    protected String getContextKey(Object object) {
        if (object instanceof Thing) {
            @SuppressWarnings("null") // Thing.getUID() is guaranteed non-null in openHAB framework
            String thingUID = ((Thing) object).getUID().toString();
            return thingUID;
        }
        return super.getContextKey(object);
    }
}
