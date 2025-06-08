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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.Thing;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.osgi.service.component.annotations.Component;

/**
 * Performs validation of HomeKit services to ensure they have valid types and are compatible
 * with their parent accessory. This validation is essential for proper HomeKit service integration.
 * 
 * @author Karel Goderis - Initial contribution
 */
@Component(service = Validation.class)
@NonNullByDefault
public class ServiceTypeValidation extends AbstractValidation {
    private static final String ID = "service-type";
    private static final int PRIORITY = 80; // High priority but after channel mapping

    public ServiceTypeValidation() {
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

        // Check each service's type
        @SuppressWarnings("null") // get() is safe after isEmpty() check above
        List<HomekitService> services = servicesOpt.get();
        for (HomekitService service : services) {
            validateServiceType(service, thing, services, issues);
        }

        return Optional.of(createResult(issues));
    }

    private void validateServiceType(HomekitService service, Thing thing, List<HomekitService> allServices,
            List<ValidationIssue> issues) {
        @SuppressWarnings("null") // getAnnotation() can return null, handled by null check below
        HomekitServiceType serviceType = service.getClass().getAnnotation(HomekitServiceType.class);
        if (serviceType == null) {
            issues.add(createIssue(ValidationResult.Severity.ERROR, "Service type annotation not found",
                    "MISSING_SERVICE_TYPE", getContextKey(thing), true, true));
            return;
        }

        String serviceName = serviceType.name();
        String serviceUuid = serviceType.type();

        // Validate service UUID format
        if (!isValidServiceUuid(serviceUuid)) {
            issues.add(createIssue(ValidationResult.Severity.ERROR,
                    String.format("Invalid service UUID format for service '%s': %s", serviceName, serviceUuid),
                    "INVALID_SERVICE_UUID", getContextKey(thing) + ":" + serviceUuid, true, true,
                    Map.of("serviceName", serviceName, "serviceUuid", serviceUuid)));
            return;
        }

        // Check if service type is compatible with thing type
        if (!isServiceTypeCompatible(serviceType, thing)) {
            @SuppressWarnings("null") // Thing.getThingTypeUID() is guaranteed non-null in openHAB framework
            String thingTypeString = thing.getThingTypeUID().toString();
            issues.add(createIssue(ValidationResult.Severity.ERROR,
                    String.format("Service type '%s' is not compatible with thing type '%s'", serviceName,
                            thing.getThingTypeUID()),
                    "INCOMPATIBLE_SERVICE_TYPE", getContextKey(thing) + ":" + serviceUuid, true, true,
                    Map.of("serviceName", serviceName, "serviceUuid", serviceUuid, "thingType", thingTypeString)));
        }

        // Check for duplicate service types
        if (hasDuplicateServiceType(service, allServices)) {
            issues.add(createIssue(ValidationResult.Severity.ERROR,
                    String.format("Duplicate service type '%s' found", serviceName), "DUPLICATE_SERVICE_TYPE",
                    getContextKey(thing) + ":" + serviceUuid, true, true,
                    Map.of("serviceName", serviceName, "serviceUuid", serviceUuid)));
        }
    }

    private boolean isValidServiceUuid(String uuid) {
        // TODO: Implement UUID format validation
        // This should validate that the UUID follows the HomeKit service UUID format
        return uuid != null && uuid.matches("^[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}$");
    }

    private boolean isServiceTypeCompatible(HomekitServiceType serviceType, Thing thing) {
        // TODO: Implement service type compatibility check
        // This will depend on the mapping rules between thing types and service types
        return true;
    }

    private boolean hasDuplicateServiceType(HomekitService service, List<HomekitService> allServices) {
        @SuppressWarnings("null") // getAnnotation() can return null, handled by null check below
        HomekitServiceType serviceType = service.getClass().getAnnotation(HomekitServiceType.class);
        if (serviceType == null) {
            return false;
        }

        int count = 0;
        for (HomekitService otherService : allServices) {
            @SuppressWarnings("null") // getAnnotation() can return null, handled by null check below
            HomekitServiceType otherType = otherService.getClass().getAnnotation(HomekitServiceType.class);
            if (otherType != null && otherType.type().equals(serviceType.type())) {
                count++;
                if (count > 1) {
                    return true;
                }
            }
        }
        return false;
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
