package org.openhab.io.homekit.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openhab.core.thing.Thing;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.library.service.HomekitAccessoryInformationService;

/**
 * Performs validation of accessory information to ensure required characteristics are present and valid.
 * This validation is essential for HomeKit accessory functionality.
 */
@Component(service = Validation.class)
public class AccessoryInformationValidation extends AbstractValidation {
    private static final String ID = "accessory-information";
    private static final int PRIORITY = 60; // High priority but after characteristic validation

    public AccessoryInformationValidation() {
        super(ID, PRIORITY);
    }

    @Override
    protected ValidationResult doValidate(ValidationContext context) {
        Object object = context.getTarget();
        if (!(object instanceof Thing)) {
            List<ValidationIssue> issues = new ArrayList<>();
            issues.add(createIssue(
                ValidationResult.Severity.ERROR,
                "Invalid object type: expected Thing",
                "INVALID_TYPE",
                getContextKey(object),
                true,
                true
            ));
            return createResult(issues);
        }

        Thing thing = (Thing) object;
        List<ValidationIssue> issues = new ArrayList<>();

        // Get all services for this thing
        List<HomekitService> services = getServices(thing);
        if (services == null) {
            issues.add(createIssue(
                ValidationResult.Severity.ERROR,
                "No HomeKit services found for thing",
                "NO_SERVICES",
                getContextKey(thing),
                true,
                true
            ));
            return createResult(issues);
        }

        // Find the accessory information service
        HomekitService accessoryInfoService = findAccessoryInformationService(services);
        if (accessoryInfoService == null) {
            issues.add(createIssue(
                ValidationResult.Severity.ERROR,
                "Accessory Information service not found",
                "MISSING_ACCESSORY_INFO_SERVICE",
                getContextKey(thing),
                true,
                true
            ));
            return createResult(issues);
        }

        // Validate the accessory information service
        validateAccessoryInformationService(accessoryInfoService, thing, issues);

        return createResult(issues);
    }

    private HomekitService findAccessoryInformationService(List<HomekitService> services) {
        for (HomekitService service : services) {
            if (service instanceof HomekitAccessoryInformationService) {
                return service;
            }
        }
        return null;
    }

    private void validateAccessoryInformationService(HomekitService service, Thing thing, List<ValidationIssue> issues) {
        HomekitServiceType serviceType = service.getClass().getAnnotation(HomekitServiceType.class);
        if (serviceType == null) {
            issues.add(createIssue(
                ValidationResult.Severity.ERROR,
                "Service type annotation not found for Accessory Information service",
                "MISSING_SERVICE_TYPE",
                getContextKey(thing),
                true,
                true
            ));
            return;
        }

        String serviceName = serviceType.name();
        String serviceUuid = serviceType.type();

        // Get all characteristics for this service
        Set<HomekitCharacteristic<?>> characteristics = service.getCharacteristics();
        if (characteristics == null) {
            issues.add(createIssue(
                ValidationResult.Severity.ERROR,
                "No characteristics found for Accessory Information service",
                "NO_CHARACTERISTICS",
                getContextKey(thing) + ":" + serviceUuid,
                true,
                true
            ));
            return;
        }

        // Validate required characteristics
        validateRequiredCharacteristic(characteristics, "Manufacturer", thing, service, issues);
        validateRequiredCharacteristic(characteristics, "Model", thing, service, issues);
        validateRequiredCharacteristic(characteristics, "SerialNumber", thing, service, issues);
        validateRequiredCharacteristic(characteristics, "Name", thing, service, issues);
        validateRequiredCharacteristic(characteristics, "Identify", thing, service, issues);
    }

    private void validateRequiredCharacteristic(Set<HomekitCharacteristic<?>> characteristics, String characteristicName,
            Thing thing, HomekitService service, List<ValidationIssue> issues) {
        boolean found = false;
        for (HomekitCharacteristic<?> characteristic : characteristics) {
            if (characteristic.getClass().getSimpleName().contains(characteristicName)) {
                found = true;
                break;
            }
        }

        if (!found) {
            issues.add(createIssue(
                ValidationResult.Severity.ERROR,
                String.format("Required characteristic '%s' not found in Accessory Information service", characteristicName),
                "MISSING_REQUIRED_CHARACTERISTIC",
                getContextKey(thing) + ":" + service.getType() + ":" + characteristicName,
                true,
                true,
                Map.of(
                    "serviceName", service.getName(),
                    "serviceUuid", service.getType(),
                    "characteristicName", characteristicName
                )
            ));
        }
    }

    private List<HomekitService> getServices(Thing thing) {
        // TODO: Implement service retrieval from thing configuration
        // This will depend on how services are stored in the thing configuration
        return null;
    }

    @Override
    protected String getContextKey(Object object) {
        if (object instanceof Thing) {
            return ((Thing) object).getUID().toString();
        }
        return super.getContextKey(object);
    }
} 