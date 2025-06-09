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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.Thing;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.osgi.service.component.annotations.Component;

/**
 * Performs validation of mandatory characteristics to ensure all required HomeKit characteristics
 * are present and properly configured. This validation is essential for HomeKit protocol compliance
 * and accessory functionality.
 * 
 * @author Karel Goderis - Initial contribution
 */
@Component(service = Validation.class)
@NonNullByDefault
public class MandatoryCharacteristicValidation extends AbstractValidation {
    private static final String ID = "mandatory-characteristic";
    private static final int PRIORITY = 50; // High priority but after service type validation

    public MandatoryCharacteristicValidation() {
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
        @SuppressWarnings("null") // get() is safe after isEmpty() check above
        List<HomekitService> services = servicesOpt.get();

        // Check each service for mandatory characteristics
        for (HomekitService service : services) {
            validateServiceCharacteristics(service, thing, issues);
        }

        return Optional.of(createResult(issues));
    }

    private void validateServiceCharacteristics(HomekitService service, Thing thing, List<ValidationIssue> issues) {
        @SuppressWarnings("null") // getAnnotation() can return null, handled by null check below
        HomekitServiceType serviceType = service.getClass().getAnnotation(HomekitServiceType.class);
        if (serviceType == null) {
            issues.add(createIssue(ValidationResult.Severity.ERROR, "Service type annotation not found",
                    "MISSING_SERVICE_TYPE", getContextKey(thing), true, true));
            return;
        }

        String serviceName = serviceType.name();
        String serviceUuid = serviceType.type();

        // Get all characteristics for this service
        @NonNull
        Set<HomekitCharacteristic<?>> characteristics = service.getCharacteristics();
        if (characteristics == null) {
            issues.add(createIssue(ValidationResult.Severity.ERROR,
                    String.format("No characteristics found for service '%s'", serviceName), "NO_CHARACTERISTICS",
                    getContextKey(thing) + ":" + serviceUuid, true, true));
            return;
        }

        // Get mandatory characteristics for this service type
        Optional<List<String>> mandatoryCharacteristicsOpt = getMandatoryCharacteristics(serviceType);
        if (mandatoryCharacteristicsOpt.isEmpty()) {
            return;
        }
        @SuppressWarnings("null") // get() is safe after isEmpty() check above
        List<String> mandatoryCharacteristics = mandatoryCharacteristicsOpt.get();

        // Check each mandatory characteristic
        for (String characteristicName : mandatoryCharacteristics) {
            validateMandatoryCharacteristic(characteristicName, characteristics, service, thing, issues);
        }
    }

    private void validateMandatoryCharacteristic(String characteristicName,
            Set<HomekitCharacteristic<?>> characteristics, HomekitService service, Thing thing,
            List<ValidationIssue> issues) {
        boolean found = false;
        for (HomekitCharacteristic<?> characteristic : characteristics) {
            if (characteristic.getClass().getSimpleName().contains(characteristicName)) {
                found = true;
                break;
            }
        }

        if (!found) {
            issues.add(createIssue(ValidationResult.Severity.ERROR,
                    String.format("Mandatory characteristic '%s' not found in service '%s'", characteristicName,
                            service.getName()),
                    "MISSING_MANDATORY_CHARACTERISTIC",
                    getContextKey(thing) + ":" + service.getType() + ":" + characteristicName, true, true,
                    Map.<String, Object> of("serviceName", service.getName(), "serviceUuid", service.getType(),
                            "characteristicName", characteristicName)));
        }
    }

    private Optional<List<String>> getMandatoryCharacteristics(HomekitServiceType serviceType) {
        // TODO: Implement mandatory characteristic list retrieval
        // This will depend on the service type and should be configurable
        return Optional.empty();
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
