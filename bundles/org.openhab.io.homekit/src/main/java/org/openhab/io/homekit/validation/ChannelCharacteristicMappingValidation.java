package org.openhab.io.homekit.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openhab.core.thing.Channel;
import org.openhab.core.thing.Thing;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.osgi.service.component.annotations.Component;

/**
 * Performs validation of channel mappings to ensure they are correctly associated with HomeKit characteristics.
 * This validation is crucial for maintaining proper communication between openHAB channels and HomeKit accessories.
 */
@Component(service = Validation.class)
public class ChannelCharacteristicMappingValidation extends AbstractValidation {
    private static final String ID = "channel-characteristic-mapping";
    private static final int PRIORITY = 90; // High priority but after mandatory characteristics

    public ChannelCharacteristicMappingValidation() {
        super(ID, PRIORITY);
    }

    @Override
    protected ValidationResult doValidate(ValidationContext context) {
        Object object = context.getTarget();
        if (!(object instanceof Thing)) {
            List<ValidationIssue> issues = new ArrayList<>();
            issues.add(createIssue(ValidationResult.Severity.ERROR, "Invalid object type: expected Thing",
                    "INVALID_TYPE", getContextKey(object), true, true));
            return createResult(issues);
        }

        Thing thing = (Thing) object;
        List<ValidationIssue> issues = new ArrayList<>();

        // Get all services for this thing
        List<HomekitService> services = getServices(thing);
        if (services == null) {
            issues.add(createIssue(ValidationResult.Severity.ERROR, "No HomeKit services found for thing",
                    "NO_SERVICES", getContextKey(thing), true, true));
            return createResult(issues);
        }

        // Check each service's characteristics against channels
        for (HomekitService service : services) {
            validateServiceChannels(service, thing, issues);
        }

        return createResult(issues);
    }

    private void validateServiceChannels(HomekitService service, Thing thing, List<ValidationIssue> issues) {
        HomekitServiceType serviceType = service.getClass().getAnnotation(HomekitServiceType.class);
        if (serviceType == null) {
            issues.add(createIssue(ValidationResult.Severity.ERROR, "Service type annotation not found",
                    "MISSING_SERVICE_TYPE", getContextKey(thing), true, true));
            return;
        }

        String serviceName = serviceType.name();
        String serviceUuid = serviceType.type();

        // Get all characteristics for this service
        Set<HomekitCharacteristic<?>> characteristics = service.getCharacteristics();
        if (characteristics == null) {
            issues.add(createIssue(ValidationResult.Severity.ERROR,
                    String.format("No characteristics found for service '%s'", serviceName), "NO_CHARACTERISTICS",
                    getContextKey(thing) + ":" + serviceUuid, true, true));
            return;
        }

        // Check each characteristic for channel mapping
        for (HomekitCharacteristic<?> characteristic : characteristics) {
            validateCharacteristicChannel(characteristic, thing, serviceName, serviceUuid, issues);
        }
    }

    private void validateCharacteristicChannel(HomekitCharacteristic<?> characteristic, Thing thing, String serviceName,
            String serviceUuid, List<ValidationIssue> issues) {
        // Get the channel ID from the characteristic configuration
        String channelId = getChannelId(characteristic);
        if (channelId == null) {
            // Skip validation if no channel mapping is required
            return;
        }

        // Check if the channel exists
        Channel channel = thing.getChannel(channelId);
        if (channel == null) {
            issues.add(createIssue(ValidationResult.Severity.ERROR,
                    String.format("Channel '%s' not found for characteristic '%s' in service '%s'", channelId,
                            characteristic.getClass().getSimpleName(), serviceName),
                    "MISSING_CHANNEL",
                    getContextKey(thing) + ":" + serviceUuid + ":" + characteristic.getClass().getSimpleName(), true,
                    true, Map.of("serviceName", serviceName, "serviceUuid", serviceUuid, "characteristicType",
                            characteristic.getClass().getSimpleName(), "channelId", channelId)));
            return;
        }

        // Validate channel type compatibility
        if (!isChannelTypeCompatible(channel, characteristic)) {
            issues.add(createIssue(ValidationResult.Severity.ERROR,
                    String.format("Channel '%s' type '%s' is not compatible with characteristic '%s' in service '%s'",
                            channelId, channel.getChannelTypeUID(), characteristic.getClass().getSimpleName(),
                            serviceName),
                    "INCOMPATIBLE_CHANNEL_TYPE",
                    getContextKey(thing) + ":" + serviceUuid + ":" + characteristic.getClass().getSimpleName(), true,
                    true,
                    Map.of("serviceName", serviceName, "serviceUuid", serviceUuid, "characteristicType",
                            characteristic.getClass().getSimpleName(), "channelId", channelId, "channelType",
                            channel.getChannelTypeUID().toString())));
        }
    }

    private String getChannelId(HomekitCharacteristic<?> characteristic) {
        // TODO: Implement channel ID retrieval from characteristic configuration
        // This will depend on how channel mappings are stored in the characteristic configuration
        return null;
    }

    private boolean isChannelTypeCompatible(Channel channel, HomekitCharacteristic<?> characteristic) {
        // TODO: Implement channel type compatibility check
        // This will depend on the mapping rules between channel types and characteristic types
        return true;
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
