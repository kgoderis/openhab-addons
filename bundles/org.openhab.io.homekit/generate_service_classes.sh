#!/bin/bash

# Create output directory
mkdir -p src/main/java/org/openhab/io/homekit/library/service

# Function to convert to camelCase
to_camel_case() {
    echo "$1" | sed -E 's/(^|_)([a-z])/\U\2/g'
}

# Function to get required characteristics for a service
get_required_characteristics() {
    local name=$1
    case $name in
        "AccessCode")
            echo "AccessCodeControlPoint"
            ;;
        "AccessoryMetrics")
            echo "AccessoryMetrics"
            ;;
        "AccessoryRuntimeInformation")
            echo "AccessoryRuntimeInformation"
            ;;
        "AssetUpdate")
            echo "AssetUpdateReadiness"
            ;;
        "Assistant")
            echo "Assistant"
            ;;
        "CameraOperatingMode")
            echo "CameraOperatingModeIndicator"
            ;;
        "CameraRecordingManagement")
            echo "Active SelectedCameraRecordingConfiguration SupportedAudioRecordingConfiguration SupportedCameraRecordingConfiguration SupportedVideoRecordingConfiguration"
            ;;
        "CloudRelay")
            echo "CloudRelay"
            ;;
        "Diagnostics")
            echo "SupportedDiagnosticsSnapshot"
            ;;
        "FirmwareUpdate")
            echo "FirmwareUpdateReadiness"
            ;;
        "NFCAccess")
            echo "NFCAccessControlPoint"
            ;;
        "PowerManagement")
            echo "WakeConfiguration"
            ;;
        "SiriEndpoint")
            echo "SiriEndpoint"
            ;;
        "SmartSpeaker")
            echo "SmartSpeaker"
            ;;
        "TapManagement")
            echo "Active CryptoHash TapType Token"
            ;;
        "TargetControlManagement")
            echo "TargetControlSupportedConfiguration TargetControlList"
            ;;
        "ThreadTransport")
            echo "ThreadStatus"
            ;;
        "TransferTransportManagement")
            echo "SupportedTransferTransportConfiguration SetupTransferTransport"
            ;;
        "Tunnel")
            echo "TunnelConnectionTimeout"
            ;;
        "WiFiRouter")
            echo "WiFiRouterStatus"
            ;;
        "WiFiSatellite")
            echo "WiFiSatelliteStatus"
            ;;
        "WiFiTransport")
            echo "WiFiTransportConfiguration"
            ;;
    esac
}

# Function to generate service class
generate_service_class() {
    local name=$1
    local uuid=$2
    local tag=$(to_camel_case "$name")
    local filename="src/main/java/org/openhab/io/homekit/library/service/Homekit${name}Service.java"
    local characteristics=$(get_required_characteristics "$name")
    
    cat > "$filename" << EOF
package org.openhab.io.homekit.library.service;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.library.HomekitAccessory;
import org.openhab.io.homekit.library.HomekitCharacteristicFactory;
import org.openhab.io.homekit.library.HomekitEventManager;
import org.openhab.io.homekit.library.HomekitRequiredCharacteristic;
import org.openhab.io.homekit.library.HomekitServiceType;

/**
 * HomeKit ${name} Service.
 * This service represents the ${name} functionality in HomeKit.
 * For more information, see https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitServiceType(type = "${uuid}", name = "${name}", tag = "${tag}")
@NonNullByDefault
public class Homekit${name}Service extends AbstractHomekitService {
    /**
     * Creates a new ${name} service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for this service
     * @param characteristicFactory The factory for creating characteristics
     */
    public Homekit${name}Service(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("${name}")
            .withExtensible(true)
            .withPrimary(false)
            .withHidden(false);
    }

    /**
     * Adds the required characteristics for this service.
     * These characteristics are defined in the HomeKit Accessory Protocol specification.
     */
    @Override
    public void addCharacteristics() {
        // Add required characteristics based on HAP specification
EOF

    # Add required characteristics
    for char in $characteristics; do
        echo "        addCharacteristic(new HomekitRequiredCharacteristic(this, eventManager, characteristicFactory, \"$char\"));" >> "$filename"
    done

    cat >> "$filename" << EOF
    }
}
EOF
}

# Read non_solution_uuids.txt and generate service classes
while IFS= read -r line; do
    name=$(echo "$line" | awk '{print $1}')
    uuid=$(echo "$line" | awk '{print $2}')
    generate_service_class "$name" "$uuid"
done < "output/non_solution_uuids.txt"

echo "Service classes have been generated in src/main/java/org/openhab/io/homekit/library/service/" 