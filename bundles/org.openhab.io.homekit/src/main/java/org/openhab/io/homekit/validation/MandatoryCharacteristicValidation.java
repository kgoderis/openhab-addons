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

/**
 * Performs validation of mandatory characteristics to ensure all required HomeKit characteristics
 * are present and properly configured. This validation is essential for HomeKit protocol compliance
 * and accessory functionality.
 */
@Component(service = Validation.class)
public class MandatoryCharacteristicValidation extends AbstractValidation {
    private static final String ID = "mandatory-characteristic";
    private static final int PRIORITY = 50; // High priority but after service type validation

    public MandatoryCharacteristicValidation() {
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

        // Check each service for mandatory characteristics
        for (HomekitService service : services) {
            validateServiceCharacteristics(service, thing, issues);
        }

        return createResult(issues);
    }

    private void validateServiceCharacteristics(HomekitService service, Thing thing, List<ValidationIssue> issues) {
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

        // Get all characteristics for this service
        Set<HomekitCharacteristic<?>> characteristics = service.getCharacteristics();
        if (characteristics == null) {
            issues.add(createIssue(
                ValidationResult.Severity.ERROR,
                String.format("No characteristics found for service '%s'", serviceName),
                "NO_CHARACTERISTICS",
                getContextKey(thing) + ":" + serviceUuid,
                true,
                true
            ));
            return;
        }

        // Get mandatory characteristics for this service type
        List<String> mandatoryCharacteristics = getMandatoryCharacteristics(serviceType);
        if (mandatoryCharacteristics == null || mandatoryCharacteristics.isEmpty()) {
            return;
        }

        // Check each mandatory characteristic
        for (String characteristicName : mandatoryCharacteristics) {
            validateMandatoryCharacteristic(characteristicName, characteristics, service, thing, issues);
        }
    }

    private void validateMandatoryCharacteristic(String characteristicName, Set<HomekitCharacteristic<?>> characteristics,
            HomekitService service, Thing thing, List<ValidationIssue> issues) {
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
                String.format("Mandatory characteristic '%s' not found in service '%s'", 
                    characteristicName, service.getName()),
                "MISSING_MANDATORY_CHARACTERISTIC",
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

    private List<String> getMandatoryCharacteristics(HomekitServiceType serviceType) {
        // TODO: Implement mandatory characteristic list retrieval
        // This will depend on the service type and should be configurable
        return null;
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