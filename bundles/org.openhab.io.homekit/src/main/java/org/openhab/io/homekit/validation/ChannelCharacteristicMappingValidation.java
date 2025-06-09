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
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.Thing;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.osgi.service.component.annotations.Component;

/**
 * Performs validation of channel mappings to ensure they are correctly associated with HomeKit characteristics.
 * This validation is crucial for maintaining proper communication between openHAB channels and HomeKit accessories.
 * 
 * @author Karel Goderis - Initial contribution
 */
@Component(service = Validation.class)
@NonNullByDefault
public class ChannelCharacteristicMappingValidation extends AbstractValidation {
    private static final String ID = "channel-characteristic-mapping";
    private static final int PRIORITY = 90; // High priority but after mandatory characteristics

    public ChannelCharacteristicMappingValidation() {
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

        // Check each service's characteristics against channels
        @SuppressWarnings("null") // get() is safe after isEmpty() check above
        List<HomekitService> services = servicesOpt.get();
        for (HomekitService service : services) {
            validateServiceChannels(service, thing, issues);
        }

        return Optional.of(createResult(issues));
    }

    private void validateServiceChannels(HomekitService service, Thing thing, List<ValidationIssue> issues) {
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
        Set<HomekitCharacteristic<?>> characteristics = service.getCharacteristics();
        if (characteristics.isEmpty()) {
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
        Optional<String> channelIdOpt = getChannelId(characteristic);
        if (channelIdOpt.isEmpty()) {
            // Skip validation if no channel mapping is required
            return;
        }
        @SuppressWarnings("null") // get() is safe after isEmpty() check above
        String channelId = channelIdOpt.get();

        // Check if the channel exists
        Channel channel = thing.getChannel(channelId);
        if (channel == null) {
            issues.add(createIssue(ValidationResult.Severity.ERROR,
                    String.format("Channel '%s' not found for characteristic '%s' in service '%s'", channelId,
                            characteristic.getClass().getSimpleName(), serviceName),
                    "MISSING_CHANNEL",
                    getContextKey(thing) + ":" + serviceUuid + ":" + characteristic.getClass().getSimpleName(), true,
                    true, Map.<String, Object> of("serviceName", serviceName, "serviceUuid", serviceUuid,
                            "characteristicType", characteristic.getClass().getSimpleName(), "channelId", channelId)));
            return;
        }

        // Validate channel type compatibility
        if (!isChannelTypeCompatible(channel, characteristic)) {
            var channelTypeUID = channel.getChannelTypeUID();
            String channelType = channelTypeUID != null ? channelTypeUID.toString() : "unknown";
            issues.add(createIssue(ValidationResult.Severity.ERROR,
                    String.format("Channel '%s' type '%s' is not compatible with characteristic '%s' in service '%s'",
                            channelId, channelTypeUID, characteristic.getClass().getSimpleName(), serviceName),
                    "INCOMPATIBLE_CHANNEL_TYPE",
                    getContextKey(thing) + ":" + serviceUuid + ":" + characteristic.getClass().getSimpleName(), true,
                    true,
                    Map.<String, Object> of("serviceName", serviceName, "serviceUuid", serviceUuid,
                            "characteristicType", characteristic.getClass().getSimpleName(), "channelId", channelId,
                            "channelType", channelType)));
        }
    }

    private Optional<String> getChannelId(HomekitCharacteristic<?> characteristic) {
        // TODO: Implement channel ID retrieval from characteristic configuration
        // This will depend on how channel mappings are stored in the characteristic configuration
        return Optional.empty();
    }

    private boolean isChannelTypeCompatible(Channel channel, HomekitCharacteristic<?> characteristic) {
        // TODO: Implement channel type compatibility check
        // This will depend on the mapping rules between channel types and characteristic types
        return true;
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
