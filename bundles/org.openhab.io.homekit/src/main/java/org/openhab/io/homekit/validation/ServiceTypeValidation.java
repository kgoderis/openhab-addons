package org.openhab.io.homekit.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openhab.core.thing.Thing;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.api.service.HomekitServiceType;

/**
 * Performs validation of HomeKit services to ensure they have valid types and are compatible
 * with their parent accessory. This validation is essential for proper HomeKit service integration.
 */
@Component(service = Validation.class)
public class ServiceTypeValidation extends AbstractValidation {
    private static final String ID = "service-type";
    private static final int PRIORITY = 80; // High priority but after channel mapping

    public ServiceTypeValidation() {
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

        // Check each service's type
        for (HomekitService service : services) {
            validateServiceType(service, thing, services, issues);
        }

        return createResult(issues);
    }

    private void validateServiceType(HomekitService service, Thing thing, List<HomekitService> allServices, List<ValidationIssue> issues) {
        HomekitServiceType serviceType = service.getClass().getAnnotation(HomekitServiceType.class);
        if (serviceType == null) {
            issues.add(createIssue(
                ValidationResult.Severity.ERROR,
                "Service type annotation not found",
                "MISSING_SERVICE_TYPE",
                getContextKey(thing),
                true,
                true
            ));
            return;
        }

        String serviceName = serviceType.name();
        String serviceUuid = serviceType.type();

        // Validate service UUID format
        if (!isValidServiceUuid(serviceUuid)) {
            issues.add(createIssue(
                ValidationResult.Severity.ERROR,
                String.format("Invalid service UUID format for service '%s': %s", serviceName, serviceUuid),
                "INVALID_SERVICE_UUID",
                getContextKey(thing) + ":" + serviceUuid,
                true,
                true,
                Map.of(
                    "serviceName", serviceName,
                    "serviceUuid", serviceUuid
                )
            ));
            return;
        }

        // Check if service type is compatible with thing type
        if (!isServiceTypeCompatible(serviceType, thing)) {
            issues.add(createIssue(
                ValidationResult.Severity.ERROR,
                String.format("Service type '%s' is not compatible with thing type '%s'", 
                    serviceName, thing.getThingTypeUID()),
                "INCOMPATIBLE_SERVICE_TYPE",
                getContextKey(thing) + ":" + serviceUuid,
                true,
                true,
                Map.of(
                    "serviceName", serviceName,
                    "serviceUuid", serviceUuid,
                    "thingType", thing.getThingTypeUID().toString()
                )
            ));
        }

        // Check for duplicate service types
        if (hasDuplicateServiceType(service, allServices)) {
            issues.add(createIssue(
                ValidationResult.Severity.ERROR,
                String.format("Duplicate service type '%s' found", serviceName),
                "DUPLICATE_SERVICE_TYPE",
                getContextKey(thing) + ":" + serviceUuid,
                true,
                true,
                Map.of(
                    "serviceName", serviceName,
                    "serviceUuid", serviceUuid
                )
            ));
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
        HomekitServiceType serviceType = service.getClass().getAnnotation(HomekitServiceType.class);
        if (serviceType == null) {
            return false;
        }

        int count = 0;
        for (HomekitService otherService : allServices) {
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