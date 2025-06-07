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
import org.openhab.core.thing.Thing;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.osgi.service.component.annotations.Component;

/**
 * Performs validation of HomeKit characteristics to ensure they have valid types and are compatible
 * with their parent service. This validation is crucial for maintaining HomeKit protocol compliance.
 * 
 * @author Karel Goderis - Initial contribution
 */
@Component(service = Validation.class)
public class CharacteristicTypeValidation extends AbstractValidation {
    private static final String ID = "characteristic-type";
    private static final int PRIORITY = 70; // High priority but after service type validation

    public CharacteristicTypeValidation() {
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

        // Check each service's characteristics
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
        Set<@NonNull HomekitCharacteristic<?>> characteristics = service.getCharacteristics();
        if (characteristics == null) {
            issues.add(createIssue(ValidationResult.Severity.ERROR,
                    String.format("No characteristics found for service '%s'", serviceName), "NO_CHARACTERISTICS",
                    getContextKey(thing) + ":" + serviceUuid, true, true));
            return;
        }

        // Check each characteristic's type
        for (HomekitCharacteristic<?> characteristic : characteristics) {
            validateCharacteristicType(characteristic, service, thing, issues);
        }
    }

    private void validateCharacteristicType(HomekitCharacteristic<?> characteristic, HomekitService service,
            Thing thing, List<ValidationIssue> issues) {
        @SuppressWarnings("null") // getAnnotation() can return null, handled by null check below
        HomekitCharacteristicType characteristicType = characteristic.getClass()
                .getAnnotation(HomekitCharacteristicType.class);
        if (characteristicType == null) {
            issues.add(createIssue(ValidationResult.Severity.ERROR,
                    String.format("Characteristic type annotation not found for characteristic '%s'",
                            characteristic.getClass().getSimpleName()),
                    "MISSING_CHARACTERISTIC_TYPE",
                    getContextKey(thing) + ":" + service.getType() + ":" + characteristic.getClass().getSimpleName(),
                    true, true));
            return;
        }

        String characteristicName = characteristicType.name();
        String characteristicUuid = characteristicType.type();

        // Validate characteristic UUID format
        if (!isValidCharacteristicUuid(characteristicUuid)) {
            issues.add(createIssue(ValidationResult.Severity.ERROR,
                    String.format("Invalid characteristic UUID format for characteristic '%s': %s", characteristicName,
                            characteristicUuid),
                    "INVALID_CHARACTERISTIC_UUID",
                    getContextKey(thing) + ":" + service.getType() + ":" + characteristicUuid, true, true,
                    Map.<String, Object> of("serviceName", service.getName(), "serviceUuid", service.getType(),
                            "characteristicName", characteristicName, "characteristicUuid", characteristicUuid)));
            return;
        }

        // Check if characteristic type is compatible with service type
        if (!isCharacteristicTypeCompatible(characteristicType, service)) {
            issues.add(createIssue(ValidationResult.Severity.ERROR,
                    String.format("Characteristic type '%s' is not compatible with service type '%s'",
                            characteristicName, service.getName()),
                    "INCOMPATIBLE_CHARACTERISTIC_TYPE",
                    getContextKey(thing) + ":" + service.getType() + ":" + characteristicUuid, true, true,
                    Map.<String, Object> of("serviceName", service.getName(), "serviceUuid", service.getType(),
                            "characteristicName", characteristicName, "characteristicUuid", characteristicUuid)));
        }

        // Check for duplicate characteristic types within the service
        if (hasDuplicateCharacteristicType(characteristic, service)) {
            issues.add(createIssue(ValidationResult.Severity.ERROR,
                    String.format("Duplicate characteristic type '%s' found in service '%s'", characteristicName,
                            service.getName()),
                    "DUPLICATE_CHARACTERISTIC_TYPE",
                    getContextKey(thing) + ":" + service.getType() + ":" + characteristicUuid, true, true,
                    Map.<String, Object> of("serviceName", service.getName(), "serviceUuid", service.getType(),
                            "characteristicName", characteristicName, "characteristicUuid", characteristicUuid)));
        }
    }

    private boolean isValidCharacteristicUuid(String uuid) {
        // TODO: Implement UUID format validation
        // This should validate that the UUID follows the HomeKit characteristic UUID format
        return uuid != null && uuid.matches("^[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}$");
    }

    private boolean isCharacteristicTypeCompatible(HomekitCharacteristicType characteristicType,
            HomekitService service) {
        // TODO: Implement characteristic type compatibility check
        // This will depend on the mapping rules between service types and characteristic types
        return true;
    }

    private boolean hasDuplicateCharacteristicType(HomekitCharacteristic<?> characteristic, HomekitService service) {
        @SuppressWarnings("null") // getAnnotation() can return null, handled by null check below
        HomekitCharacteristicType characteristicType = characteristic.getClass()
                .getAnnotation(HomekitCharacteristicType.class);
        if (characteristicType == null) {
            return false;
        }

        @NonNull
        Set<@NonNull HomekitCharacteristic<?>> characteristics = service.getCharacteristics();
        if (characteristics == null) {
            return false;
        }

        int count = 0;
        for (HomekitCharacteristic<?> otherCharacteristic : characteristics) {
            @SuppressWarnings("null") // getAnnotation() can return null, handled by null check below
            HomekitCharacteristicType otherType = otherCharacteristic.getClass()
                    .getAnnotation(HomekitCharacteristicType.class);
            if (otherType != null && otherType.type().equals(characteristicType.type())) {
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
